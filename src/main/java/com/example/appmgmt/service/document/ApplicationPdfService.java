package com.example.appmgmt.service.document;

import com.example.appmgmt.common.TokenUtil;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicantConsentDao;
import com.example.appmgmt.dao.ApplicationPdfDao;
import com.example.appmgmt.dao.ApplicationVersionDao;
import com.example.appmgmt.dao.ConsentDocumentDao;
import com.example.appmgmt.domain.ApplicantConsent;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationPdf;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.ConsentDocumentRecord;
import com.example.appmgmt.service.application.PhaseGroup;
import com.example.appmgmt.service.transition.StatusTransitionService;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 申込内容 PDF（F19）。申込者が同意したとき（同意時 PDF）と審査が完了したとき（審査完了時 PDF）に作って保存する（F14 後続処理）。
 * 保存した PDF は作り直さない。同意後に同意を取り直さずに内容が変わった場合（一部修正・修正対応・契約変更で変更基準内）は、
 * 審査完了時 PDF で同意時からの変更を示す。
 */
public class ApplicationPdfService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final ApplicationPdfDao pdfDao;
    private final ApplicationVersionDao versionDao;
    private final ApplicantConsentDao consentDao;
    private final ConsentDocumentDao documentDao;

    public ApplicationPdfService(ApplicationPdfDao pdfDao, ApplicationVersionDao versionDao, ApplicantConsentDao consentDao, ConsentDocumentDao documentDao) {
        this.pdfDao = pdfDao;
        this.versionDao = versionDao;
        this.consentDao = consentDao;
        this.documentDao = documentDao;
    }

    /** 同意時 PDF（遷移 14／15／42／43 の後続処理）。同意した版と、同意した同意事項の版を載せる。 */
    public void createConsentPdf(Connection conn, Application app, long consentId, LocalDateTime agreedAt) {
        ApplicantConsent c = consentDao.findById(conn, consentId).orElseThrow(() -> new IllegalStateException("申込者同意がありません: " + consentId));
        ApplicationVersion v = versionDao.get(conn, app.getApplicationId(), c.getVersionNo());
        boolean change = Codes.CONSENT_TYPE_CHANGE.equals(c.getConsentType());
        ApplicationRendererInput in = new ApplicationRendererInput(app, v, agreedAt);
        in.r.title = change ? "契約変更内容（ご同意時）" : "お申込内容（ご同意時）";
        in.r.timeLabel = "ご同意日時";
        if (change && app.getReviewedVersionNo() != null) {
            in.r.compare = versionDao.get(conn, app.getApplicationId(), app.getReviewedVersionNo());
            in.r.compareLabel = "変更前";
            in.r.versionLabel = "変更後";
            in.r.notes.add("赤字は変更前から変更された項目です。");
        }
        for (ConsentDocumentRecord r : documentDao.findRecords(conn, consentId)) {
            if (r.getAgreedAt() != null) {
                in.r.agreedDocuments.add(r);
            }
        }
        in.r.notes.add("この書面は、お客様が申込者ページで同意された時点のお申込内容と同意事項を記録したものです。");
        save(conn, app, v, Codes.PDF_CONSENTED, consentId, ApplicationPdfRenderer.render(in.r), "consented", agreedAt);
    }

    /**
     * 審査完了時 PDF（遷移 27／52 の後続処理）。審査完了した版を載せる。比較元は次の順：
     * 同じ手続き（新規申込／契約変更 N）で同意した版と内容が異なればその版（同意後の変更を赤字）、契約変更なら変更前（直前の審査完了版）。
     */
    public void createReviewedPdf(Connection conn, Application app, Integer previousReviewedNo, LocalDateTime reviewedAt) {
        ApplicationVersion v = versionDao.get(conn, app.getApplicationId(), app.getCurrentVersionNo());
        boolean change = StatusTransitionService.isContractChangeVersion(v.getVersionType());
        ApplicationRendererInput in = new ApplicationRendererInput(app, v, reviewedAt);
        in.r.title = change ? "契約変更内容（審査完了時）" : "お申込内容（審査完了時）";
        in.r.timeLabel = "審査完了日時";
        in.r.versionLabel = "審査完了時";
        ApplicantConsent agreed = lastAgreedInPhase(conn, app, v.getVersionNo());
        ApplicationVersion agreedVersion = agreed == null ? null : versionDao.get(conn, app.getApplicationId(), agreed.getVersionNo());
        if (agreedVersion != null && !agreedVersion.sameContentAs(v)) {
            in.r.compare = agreedVersion;
            in.r.compareLabel = "ご同意時（第 " + agreedVersion.getVersionNo() + " 版）";
            in.r.notes.add("ご同意後に変更された項目があります（" + String.join("、", ApplicationPdfRenderer.changedLabels(agreedVersion, v))
                    + "）。赤字はご同意時から変更された項目です。");
        } else if (change && previousReviewedNo != null) {
            in.r.compare = versionDao.get(conn, app.getApplicationId(), previousReviewedNo);
            in.r.compareLabel = "変更前";
            in.r.notes.add("赤字は変更前から変更された項目です。");
            if (agreed == null) {
                in.r.notes.add("この契約変更は変更基準内のため、お客様の同意の手続きを経ずに審査を行いました。");
            }
        }
        in.r.notes.add("この書面は、審査が完了した時点のお申込内容を記録したものです。");
        save(conn, app, v, Codes.PDF_REVIEWED, null, ApplicationPdfRenderer.render(in.r), "reviewed", reviewedAt);
    }

    /** 審査完了した版と同じ手続き（領域）の版に対して、最後に同意された申込者同意。 */
    private ApplicantConsent lastAgreedInPhase(Connection conn, Application app, int versionNo) {
        List<ApplicationVersion> versions = versionDao.findAll(conn, app.getApplicationId());
        PhaseGroup phase = null;
        for (PhaseGroup g : PhaseGroup.build(app, versions)) {
            for (ApplicationVersion v : g.getVersions()) {
                if (v.getVersionNo() == versionNo) {
                    phase = g;
                }
            }
        }
        if (phase == null) {
            return null;
        }
        List<Integer> nos = phase.getVersions().stream().map(ApplicationVersion::getVersionNo).collect(Collectors.toList());
        ApplicantConsent last = null;
        for (ApplicantConsent c : consentDao.findByApplication(conn, app.getApplicationId())) {
            if (Codes.CONSENT_AGREED.equals(c.getConsentStatus()) && nos.contains(c.getVersionNo())
                    && (last == null || c.getConsentedAt().isAfter(last.getConsentedAt()))) {
                last = c;
            }
        }
        return last;
    }

    private void save(Connection conn, Application app, ApplicationVersion v, String type, Long consentId, byte[] pdf, String kind, LocalDateTime at) {
        ApplicationPdf p = new ApplicationPdf();
        p.setApplicationId(app.getApplicationId());
        p.setVersionNo(v.getVersionNo());
        p.setPdfType(type);
        p.setConsentId(consentId);
        p.setFileName(app.getApplicationNo() + "_v" + v.getVersionNo() + "_" + kind + "_" + FILE_TS.format(at) + ".pdf");
        p.setFileSize(pdf.length);
        p.setFileHash(TokenUtil.sha256Hex(pdf));
        p.setData(pdf);
        pdfDao.insert(conn, p);
    }

    /** 申込の PDF 一覧（新しい順）。手続き（新規申込／契約変更 N）と状態（有効・再同意で置き換え・取消）を付ける。 */
    public List<ApplicationPdf> list(Connection conn, Application app) {
        List<ApplicationPdf> pdfs = pdfDao.findByApplication(conn, app.getApplicationId());
        if (pdfs.isEmpty()) {
            return pdfs;
        }
        Map<Integer, PhaseGroup> phaseOf = new HashMap<>();
        for (PhaseGroup g : PhaseGroup.build(app, versionDao.findAll(conn, app.getApplicationId()))) {
            for (ApplicationVersion v : g.getVersions()) {
                phaseOf.put(v.getVersionNo(), g);
            }
        }
        for (int i = 0; i < pdfs.size(); i++) {
            ApplicationPdf p = pdfs.get(i);
            PhaseGroup g = phaseOf.get(p.getVersionNo());
            int idx = g == null ? 0 : g.getIndex();
            p.setPhaseLabel(idx == 0 ? "新規申込" : "契約変更 " + idx);
            boolean replaced = false;
            for (int j = i + 1; j < pdfs.size(); j++) {
                ApplicationPdf later = pdfs.get(j);
                if (later.getPdfType().equals(p.getPdfType()) && phaseOf.get(later.getVersionNo()) == g) {
                    replaced = true;
                }
            }
            if (g != null && g.getState() == PhaseGroup.State.CANCELED) {
                p.setStateLabel(idx == 0 ? "申込取消" : "契約変更の取消");
            } else if (replaced) {
                p.setStateLabel(p.isConsented() ? "再同意により置き換え" : "置き換え");
            } else {
                p.setStateLabel("有効");
                p.setCurrent(true);
            }
        }
        List<ApplicationPdf> out = new ArrayList<>(pdfs);
        Collections.reverse(out);
        return out;
    }

    public List<ApplicationPdf> list(Application app) {
        return Tx.execute(conn -> list(conn, app));
    }

    public Optional<ApplicationPdf> get(long applicationId, long pdfId) {
        return Tx.execute(conn -> pdfDao.findWithData(conn, applicationId, pdfId));
    }

    /** 表示内容の組み立て（申込番号・申込者名・版・日時）。 */
    private static final class ApplicationRendererInput {
        final ApplicationPdfRenderer.Input r = new ApplicationPdfRenderer.Input();

        ApplicationRendererInput(Application app, ApplicationVersion v, LocalDateTime at) {
            r.applicationNo = app.getApplicationNo();
            r.applicantName = v.getApplicantName();
            r.version = v;
            r.time = at;
        }
    }
}
