package com.example.appmgmt.service.consent;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.TokenUtil;
import com.example.appmgmt.common.TransitionNotAllowedException;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicantConsentDao;
import com.example.appmgmt.dao.ApplicantAccountDao;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ApplicationVersionDao;
import com.example.appmgmt.dao.StatusDao;
import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.ApplicantAccount;
import com.example.appmgmt.domain.ApplicantConsent;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.ConsentDocumentVersion;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.service.document.ConsentDocumentService;
import com.example.appmgmt.service.transition.StatusTransitionService;
import com.example.appmgmt.service.transition.TransitionRequest;
import com.example.appmgmt.service.transition.TransitionResult;
import java.sql.Connection;
import java.time.LocalDateTime;

/**
 * F07 申込者確認・同意。
 * 申込者の特定は 2 通り：確認用 URL のトークン（メールのリンク）と、申込者ポータルにログインした申込者本人（申込 ID で進行中の同意を引く）。
 * どちらも同じ申込者同意レコードを使い、以降の処理は共通。
 */
public class ConsentService {

    /** 申込者同意の解決方法。 */
    @FunctionalInterface
    public interface Resolver {
        ConsentView resolve(Connection conn);
    }

    private final ApplicantConsentDao consentDao;
    private final ApplicationDao applicationDao;
    private final ApplicationVersionDao versionDao;
    private final ApplicantAccountDao accountDao;
    private final StatusDao statusDao;
    private final StatusTransitionService transitionService;
    private final ConsentDocumentService documentService;

    public ConsentService(ApplicantConsentDao consentDao, ApplicationDao applicationDao, ApplicationVersionDao versionDao, ApplicantAccountDao accountDao, StatusDao statusDao,
                          StatusTransitionService transitionService, ConsentDocumentService documentService) {
        this.consentDao = consentDao;
        this.applicationDao = applicationDao;
        this.versionDao = versionDao;
        this.accountDao = accountDao;
        this.statusDao = statusDao;
        this.transitionService = transitionService;
        this.documentService = documentService;
    }

    /** 確認用 URL のトークンで解決する（F07 8.3 トークン検証）。 */
    public Resolver byToken(String token) {
        return conn -> {
            if (!TokenUtil.looksLikeToken(token)) {
                return invalid();
            }
            ApplicantConsent c = consentDao.findByTokenHash(conn, TokenUtil.sha256Hex(token)).orElse(null);
            return c == null ? invalid() : build(conn, c, null);
        };
    }

    /** ログイン中の申込者本人として、申込の進行中の同意で解決する（申込者ポータル）。 */
    public Resolver byApplicant(long applicantId, long applicationId) {
        return conn -> {
            Application app = applicationDao.findById(conn, applicationId).orElse(null);
            if (app == null || app.getApplicantId() == null || app.getApplicantId() != applicantId) {
                throw new ForbiddenException();
            }
            ApplicantConsent c = consentDao.findActive(conn, applicationId).orElse(null);
            if (c == null) {
                // 進行中の同意がない：完了扱い（申込者確認中でなければお手続きなし）
                ConsentView v = new ConsentView();
                v.setApplication(app);
                v.setStatus(statusDao.find(conn, app.getStatusCd()).orElse(null));
                v.setVersion(versionDao.find(conn, app.getApplicationId(), app.getCurrentVersionNo()).orElse(null));
                v.setApplicant(applicantOf(conn, app, v.getVersion()));
                v.setOutcome(ConsentView.Outcome.COMPLETED);
                return v;
            }
            return build(conn, c, app);
        };
    }

    /** 申込者情報（確認対象の版の申込データ）と、申込者アカウントのユーザー ID をまとめる。 */
    private Applicant applicantOf(Connection conn, Application app, ApplicationVersion version) {
        ApplicantAccount account = app.getApplicantId() == null ? null : accountDao.findById(conn, app.getApplicantId()).orElse(null);
        return Applicant.of(version, account);
    }

    private static ConsentView invalid() {
        ConsentView v = new ConsentView();
        v.setOutcome(ConsentView.Outcome.INVALID);
        return v;
    }

    private ConsentView build(Connection conn, ApplicantConsent c, Application appOrNull) {
        ConsentView v = new ConsentView();
        v.setConsent(c);
        Application app = appOrNull != null ? appOrNull : applicationDao.findById(conn, c.getApplicationId()).orElse(null);
        if (app == null || Codes.CONSENT_INVALID.equals(c.getConsentStatus())) {
            v.setOutcome(ConsentView.Outcome.INVALID);
            return v;
        }
        v.setApplication(app);
        v.setStatus(statusDao.find(conn, app.getStatusCd()).orElse(null));
        v.setVersion(versionDao.find(conn, app.getApplicationId(), c.getVersionNo()).orElse(null));
        v.setApplicant(applicantOf(conn, app, v.getVersion()));
        if (v.isContractChange() && app.getReviewedVersionNo() != null) {
            v.setBeforeVersion(versionDao.find(conn, app.getApplicationId(), app.getReviewedVersionNo()).orElse(null));
        }
        if (Codes.CONSENT_AGREED.equals(c.getConsentStatus()) || Codes.CONSENT_RETURNED.equals(c.getConsentStatus())) {
            v.setOutcome(ConsentView.Outcome.COMPLETED);
            return v;
        }
        if (c.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            v.setOutcome(ConsentView.Outcome.EXPIRED);
            return v;
        }
        if (c.getVersionNo() != app.getCurrentVersionNo()) {
            v.setOutcome(ConsentView.Outcome.INVALID);
            return v;
        }
        if (StatusCd.CONTENT_CONFIRM_WAIT.contains(app.getStatusCd())) {
            v.setOutcome(ConsentView.Outcome.CONFIRM);
        } else if (StatusCd.AGREE_WAIT.contains(app.getStatusCd())) {
            v.setOutcome(ConsentView.Outcome.AGREE);
            v.setDocuments(documentService.forConsent(conn, c));
        } else {
            v.setOutcome(ConsentView.Outcome.COMPLETED);
        }
        return v;
    }

    public ConsentView resolve(Resolver r) {
        return Tx.execute(r::resolve);
    }

    private ConsentView requireActive(Connection conn, Resolver r) {
        ConsentView v = r.resolve(conn);
        if (v.getOutcome() != ConsentView.Outcome.CONFIRM && v.getOutcome() != ConsentView.Outcome.AGREE) {
            throw new ForbiddenException();
        }
        return v;
    }

    private TransitionRequest request(ConsentView v, String actionCd, String ip) {
        Application app = v.getApplication();
        return TransitionRequest.of(app.getApplicationId(), app.getRowVersion(), actionCd, Codes.ACTOR_APPLICANT, String.valueOf(app.getApplicantId()))
                .consentId(v.getConsent().getConsentId()).clientIp(ip);
    }

    /** AP02 保存（10301 のみ）。現行版をそのまま更新する。 */
    public void saveEdit(Resolver r, ApplicationVersion content) {
        Tx.executeVoid(conn -> {
            ConsentView v = requireActive(conn, r);
            if (!StatusCd.CONFIRM_WAIT.equals(v.getApplication().getStatusCd())) {
                throw new TransitionNotAllowedException();
            }
            // 申込者は自分の申込者情報（連絡先）も申込内容として修正できる
            ApplicationVersion cur = v.getVersion();
            cur.applyContentFrom(content);
            versionDao.updateContent(conn, cur);
            applicationDao.touch(conn, cur.getApplicationId(), v.getApplication().getRowVersion());
        });
    }

    /** AP01 確定（10301／20301 → 10302／20302）。 */
    public TransitionResult confirm(Resolver r, String ip) {
        return Tx.execute(conn -> {
            ConsentView v = requireActive(conn, r);
            if (!StatusCd.CONTENT_CONFIRM_WAIT.contains(v.getApplication().getStatusCd())) {
                throw new TransitionNotAllowedException();
            }
            consentDao.updateContentConfirmed(conn, v.getConsent().getConsentId(), LocalDateTime.now().withNano(0), ip);
            return transitionService.transition(conn, request(v, Codes.ACTION_CONFIRM, ip));
        });
    }

    /** AP03 同意する。 */
    public TransitionResult agree(Resolver r, String ip) {
        return Tx.execute(conn -> {
            ConsentView v = requireActive(conn, r);
            if (!StatusCd.AGREE_WAIT.contains(v.getApplication().getStatusCd())) {
                throw new TransitionNotAllowedException();
            }
            // 同意事項：適用中の全文書の版を開いていること（E118／改定後は E119）。開いた版を同意済みにする
            documentService.agree(conn, v.getConsent(), LocalDateTime.now().withNano(0));
            return transitionService.transition(conn, request(v, Codes.ACTION_CONSENT, ip));
        });
    }

    /** AP03 で同意事項の PDF を開く（同意確認待ちの間だけ）。閲覧を記録する。 */
    public ConsentDocumentVersion openDocument(Resolver r, String documentCd, int versionNo) {
        return Tx.execute(conn -> {
            ConsentView v = requireActive(conn, r);
            if (!StatusCd.AGREE_WAIT.contains(v.getApplication().getStatusCd())) {
                throw new TransitionNotAllowedException();
            }
            return documentService.open(conn, v.getConsent(), documentCd, versionNo);
        });
    }

    /** AP03 差戻し（理由必須）。 */
    public TransitionResult returnToOwner(Resolver r, String reason, String ip) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("E105");
        }
        return Tx.execute(conn -> {
            ConsentView v = requireActive(conn, r);
            if (!StatusCd.AGREE_WAIT.contains(v.getApplication().getStatusCd())) {
                throw new TransitionNotAllowedException();
            }
            return transitionService.transition(conn, request(v, Codes.ACTION_APPLICANT_RETURN, ip).comment(reason));
        });
    }

    /** AP03 修正（10302 のみ → 10301）。 */
    public TransitionResult modify(Resolver r, String ip) {
        return Tx.execute(conn -> {
            ConsentView v = requireActive(conn, r);
            if (!StatusCd.CONSENT_WAIT.equals(v.getApplication().getStatusCd())) {
                throw new TransitionNotAllowedException();
            }
            return transitionService.transition(conn, request(v, Codes.ACTION_MODIFY, ip));
        });
    }
}
