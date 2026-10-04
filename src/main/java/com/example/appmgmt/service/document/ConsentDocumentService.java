package com.example.appmgmt.service.document;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.OptimisticLockException;
import com.example.appmgmt.common.TokenUtil;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ConsentDocumentDao;
import com.example.appmgmt.domain.ApplicantConsent;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.ConsentDocument;
import com.example.appmgmt.domain.ConsentDocumentRecord;
import com.example.appmgmt.domain.ConsentDocumentVersion;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 同意事項（F20）。申込同意確認画面（AP03）に適用中の版の PDF を表示し、開いた版を同意ごとに記録する。
 * 同意するときは、適用中の全文書の版を開いていることを確認し、その版を同意済みにする（表示後に改定された文書は開き直してもらう）。
 */
public class ConsentDocumentService {

    /** アップロードできる PDF の上限（10MB）。 */
    public static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final ConsentDocumentDao dao;

    public ConsentDocumentService(ConsentDocumentDao dao) {
        this.dao = dao;
    }

    /** 同意種別（1：新規申込、2：契約変更）の同意で表示する同意事項。有効で、指定日時に適用中の版がある文書だけ。 */
    public List<ConsentDocument> effectiveFor(Connection conn, String consentType, LocalDateTime at) {
        List<ConsentDocument> out = new ArrayList<>();
        for (ConsentDocument d : dao.findAll(conn)) {
            if (!d.isValid() || !d.appliesTo(consentType)) {
                continue;
            }
            Optional<ConsentDocumentVersion> v = dao.findEffective(conn, d.getDocumentCd(), at);
            if (v.isPresent()) {
                d.setCurrentVersion(v.get());
                out.add(d);
            }
        }
        return out;
    }

    /** AP03 の表示用。適用中の版と、この同意でその版を開いたか。 */
    public List<ConsentDocument> forConsent(Connection conn, ApplicantConsent c) {
        List<ConsentDocument> docs = effectiveFor(conn, c.getConsentType(), LocalDateTime.now());
        List<ConsentDocumentRecord> recs = dao.findRecords(conn, c.getConsentId());
        for (ConsentDocument d : docs) {
            d.setViewed(viewed(recs, d.getDocumentCd(), d.getCurrentVersion().getVersionNo()));
        }
        return docs;
    }

    private static boolean viewed(List<ConsentDocumentRecord> recs, String documentCd, int versionNo) {
        for (ConsentDocumentRecord r : recs) {
            if (r.getDocumentCd().equals(documentCd) && r.getVersionNo() == versionNo) {
                return true;
            }
        }
        return false;
    }

    /** AP03 で同意事項を開く。適用中の版だけ開ける（改定済みの版は E119）。閲覧を記録して PDF 本体付きの版を返す。 */
    public ConsentDocumentVersion open(Connection conn, ApplicantConsent c, String documentCd, int versionNo) {
        for (ConsentDocument d : effectiveFor(conn, c.getConsentType(), LocalDateTime.now())) {
            if (d.getDocumentCd().equals(documentCd)) {
                if (d.getCurrentVersion().getVersionNo() != versionNo) {
                    throw new BusinessException("E119");
                }
                dao.recordView(conn, c.getConsentId(), documentCd, versionNo, LocalDateTime.now().withNano(0));
                return dao.findVersionWithData(conn, documentCd, versionNo).orElseThrow(ForbiddenException::new);
            }
        }
        throw new ForbiddenException();
    }

    /**
     * 同意の確認（AP03「同意する」）。適用中の全文書について、その版を開いていなければ E118、
     * 前の版だけを開いていた（表示後に改定された）なら E119。確認できたら開いた版を同意済みにする。
     */
    public void agree(Connection conn, ApplicantConsent c, LocalDateTime at) {
        List<ConsentDocument> docs = effectiveFor(conn, c.getConsentType(), at);
        List<ConsentDocumentRecord> recs = dao.findRecords(conn, c.getConsentId());
        boolean revised = false;
        boolean missing = false;
        for (ConsentDocument d : docs) {
            if (viewed(recs, d.getDocumentCd(), d.getCurrentVersion().getVersionNo())) {
                continue;
            }
            boolean viewedOlder = recs.stream().anyMatch(r -> r.getDocumentCd().equals(d.getDocumentCd()));
            if (viewedOlder) {
                revised = true;
            } else {
                missing = true;
            }
        }
        if (revised) {
            throw new BusinessException("E119");
        }
        if (missing) {
            throw new BusinessException("E118");
        }
        for (ConsentDocument d : docs) {
            dao.markAgreed(conn, c.getConsentId(), d.getDocumentCd(), d.getCurrentVersion().getVersionNo(), at);
        }
    }

    /** 申込で同意した同意事項（同意日時の新しい順）。 */
    public List<ConsentDocumentRecord> agreedFor(long applicationId) {
        return Tx.execute(conn -> dao.findAgreedByApplication(conn, applicationId));
    }

    /** 申込で同意した版の同意事項 PDF（同意の記録がある版だけ開ける）。 */
    public ConsentDocumentVersion openAgreed(long applicationId, String documentCd, int versionNo) {
        return Tx.execute(conn -> {
            boolean agreed = dao.findAgreedByApplication(conn, applicationId).stream()
                    .anyMatch(r -> r.getDocumentCd().equals(documentCd) && r.getVersionNo() == versionNo);
            if (!agreed) {
                throw new ForbiddenException();
            }
            return dao.findVersionWithData(conn, documentCd, versionNo).orElseThrow(ForbiddenException::new);
        });
    }

    // ---------------------------------------------------------------- SC17 同意事項マスタ（管理者）

    /** 全文書と全版（版番号の降順）、適用中の版。 */
    public List<ConsentDocument> allWithVersions() {
        return Tx.execute(conn -> {
            LocalDateTime now = LocalDateTime.now();
            List<ConsentDocument> docs = dao.findAll(conn);
            for (ConsentDocument d : docs) {
                d.setVersions(dao.findVersions(conn, d.getDocumentCd()));
                d.setCurrentVersion(dao.findEffective(conn, d.getDocumentCd(), now).orElse(null));
            }
            return docs;
        });
    }

    public void addDocument(ConsentDocument d) {
        Tx.executeVoid(conn -> {
            if (dao.find(conn, d.getDocumentCd()).isPresent()) {
                throw new BusinessException("E006", "文書コード");
            }
            d.setValidFlg(Codes.FLG_ON);
            dao.insert(conn, d);
        });
    }

    public void updateDocument(ConsentDocument d, int expectedRowVersion) {
        Tx.executeVoid(conn -> {
            if (dao.updateDocument(conn, d, expectedRowVersion) != 1) {
                throw new OptimisticLockException();
            }
        });
    }

    /**
     * 新しい版を登録する。PDF（先頭が %PDF-）で 10MB 以内、適用開始日時は既存の最新版の適用開始日時より後。
     * 版は追加するだけで、既存の版は変更しない。
     */
    public int addVersion(String documentCd, LocalDateTime effectiveFrom, String fileName, byte[] data, String remarks) {
        if (!isPdf(data) || data.length > MAX_FILE_SIZE) {
            throw new BusinessException("E120");
        }
        return Tx.execute(conn -> {
            dao.find(conn, documentCd).orElseThrow(() -> new BusinessException("E011", "同意事項"));
            List<ConsentDocumentVersion> versions = dao.findVersions(conn, documentCd);
            for (ConsentDocumentVersion v : versions) {
                if (!effectiveFrom.isAfter(v.getEffectiveFrom())) {
                    throw new BusinessException("E007", "適用開始日時", com.example.appmgmt.common.Formats.dateTime(v.getEffectiveFrom().plusMinutes(1)));
                }
            }
            ConsentDocumentVersion v = new ConsentDocumentVersion();
            v.setDocumentCd(documentCd);
            v.setVersionNo(dao.nextVersionNo(conn, documentCd));
            v.setEffectiveFrom(effectiveFrom);
            v.setFileName(fileName);
            v.setFileSize(data.length);
            v.setFileHash(TokenUtil.sha256Hex(data));
            v.setData(data);
            v.setRemarks(remarks == null || remarks.isBlank() ? null : remarks.strip());
            dao.insertVersion(conn, v);
            return v.getVersionNo();
        });
    }

    public static boolean isPdf(byte[] data) {
        return data != null && data.length > 5 && new String(data, 0, 5, StandardCharsets.ISO_8859_1).equals("%PDF-");
    }

    public ConsentDocumentVersion versionFile(String documentCd, int versionNo) {
        return Tx.execute(conn -> dao.findVersionWithData(conn, documentCd, versionNo).orElseThrow(ForbiddenException::new));
    }
}
