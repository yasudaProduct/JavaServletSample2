package com.example.appmgmt.service.transition;

import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.OptimisticLockException;
import com.example.appmgmt.common.TransitionNotAllowedException;
import com.example.appmgmt.dao.ApplicantConsentDao;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ApplicationVersionDao;
import com.example.appmgmt.dao.ApprovalRequestDao;
import com.example.appmgmt.dao.CompanyDivDao;
import com.example.appmgmt.dao.ExternalLinkDao;
import com.example.appmgmt.dao.StatusHistoryDao;
import com.example.appmgmt.dao.StatusTransitionDao;
import com.example.appmgmt.domain.ApplicantConsent;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.ApprovalRequest;
import com.example.appmgmt.domain.ApprovalStep;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.CompanyDiv;
import com.example.appmgmt.domain.ExternalLink;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.domain.StatusHistory;
import com.example.appmgmt.domain.StatusTransition;
import com.example.appmgmt.service.auth.ApplicantAuthService;
import com.example.appmgmt.service.consent.ConsentIssuer;
import com.example.appmgmt.service.document.ApplicationPdfService;
import com.example.appmgmt.service.notification.NotificationService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * F14 ステータス遷移制御（10. 機能詳細 1 章）。
 * 遷移マスタから遷移先を 1 件に決め、操作主体を照合し、申込を更新し、履歴を登録し、後続処理を同一トランザクションで行う。
 */
public class StatusTransitionService {

    private static final Logger log = LoggerFactory.getLogger(StatusTransitionService.class);

    private final ApplicationDao applicationDao;
    private final ApplicationVersionDao versionDao;
    private final CompanyDivDao companyDivDao;
    private final StatusTransitionDao transitionDao;
    private final StatusHistoryDao historyDao;
    private final ApprovalRequestDao approvalRequestDao;
    private final ApplicantConsentDao consentDao;
    private final ExternalLinkDao externalLinkDao;
    private final NotificationService notificationService;
    private final ConsentIssuer consentIssuer;
    private final ApplicantAuthService applicantAuthService;
    /** 申込内容 PDF（同意時・審査完了時）の作成。未設定（単体テスト）なら作らない。 */
    private ApplicationPdfService pdfService;

    public StatusTransitionService(ApplicationDao applicationDao, ApplicationVersionDao versionDao, CompanyDivDao companyDivDao, StatusTransitionDao transitionDao,
                                   StatusHistoryDao historyDao, ApprovalRequestDao approvalRequestDao, ApplicantConsentDao consentDao, ExternalLinkDao externalLinkDao,
                                   NotificationService notificationService, ConsentIssuer consentIssuer, ApplicantAuthService applicantAuthService) {
        this.applicationDao = applicationDao;
        this.versionDao = versionDao;
        this.companyDivDao = companyDivDao;
        this.transitionDao = transitionDao;
        this.historyDao = historyDao;
        this.approvalRequestDao = approvalRequestDao;
        this.consentDao = consentDao;
        this.externalLinkDao = externalLinkDao;
        this.notificationService = notificationService;
        this.consentIssuer = consentIssuer;
        this.applicantAuthService = applicantAuthService;
    }

    public void setApplicationPdfService(ApplicationPdfService pdfService) {
        this.pdfService = pdfService;
    }

    public TransitionResult transition(Connection conn, TransitionRequest req) {
        // 1. 申込取得と排他確認
        Application app = applicationDao.findById(conn, req.getApplicationId()).orElseThrow(TransitionNotAllowedException::new);
        if (app.getRowVersion() != req.getExpectedRowVersion()) {
            throw new OptimisticLockException();
        }
        CompanyDiv div = companyDivDao.find(conn, app.getCompanyDiv()).orElseThrow(() -> new IllegalStateException("会社区分がありません: " + app.getCompanyDiv()));

        // 2. 遷移候補の検索
        List<StatusTransition> candidates = transitionDao.findCandidates(conn, app.getStatusCd(), req.getActionCd(), div.getPreCheckFlg());
        if (candidates.isEmpty()) {
            throw new TransitionNotAllowedException();
        }

        // 3. 条件評価（評価順）
        StatusTransition adopted = null;
        for (StatusTransition t : candidates) {
            if (evaluate(conn, t.getConditionCd(), app, div, req)) {
                adopted = t;
                break;
            }
        }
        if (adopted == null) {
            throw new TransitionNotAllowedException();
        }

        // 4. 操作主体と操作者の照合
        verifyActor(conn, adopted, app, req);

        // 5. ステータス更新（行バージョン +1）
        String from = app.getStatusCd();
        int updated = applicationDao.updateStatus(conn, app.getApplicationId(), adopted.getToStatusCd(), req.getExpectedRowVersion());
        if (updated != 1) {
            throw new OptimisticLockException();
        }
        app.setStatusCd(adopted.getToStatusCd());
        app.setRowVersion(app.getRowVersion() + 1);

        // 7. 後続処理（版番号が変わる処理があるため履歴より先に行い、履歴には変更後の現行版を記録する）
        LocalDateTime now = LocalDateTime.now().withNano(0);
        postProcess(conn, adopted, app, div, req, now);

        // 6. ステータス履歴
        StatusHistory h = new StatusHistory();
        h.setApplicationId(app.getApplicationId());
        h.setVersionNo(app.getCurrentVersionNo());
        h.setFromStatusCd(from);
        h.setToStatusCd(adopted.getToStatusCd());
        h.setTransitionId(adopted.getTransitionId());
        h.setActionCd(req.getActionCd());
        h.setActorType(req.getActorType());
        h.setActorId(req.getActorId());
        h.setComment(req.getComment());
        h.setChangedAt(now);
        long historyId = historyDao.insert(conn, h);

        log.info("遷移 applicationId={} transitionId={} {} -> {} action={} actor={}:{}", app.getApplicationId(), adopted.getTransitionId(), from, adopted.getToStatusCd(), req.getActionCd(), req.getActorType(), req.getActorId());
        return new TransitionResult(adopted.getTransitionId(), from, adopted.getToStatusCd(), historyId, app, Codes.COND_OVER_LIMIT.equals(adopted.getConditionCd()));
    }

    /** 1.4 条件評価。 */
    boolean evaluate(Connection conn, String conditionCd, Application app, CompanyDiv div, TransitionRequest req) {
        switch (conditionCd) {
            case Codes.COND_NONE:
                return true;
            case Codes.COND_ROUTE_EXISTS:
                return Boolean.TRUE.equals(req.getHasRoute());
            case Codes.COND_ROUTE_NONE:
                return Boolean.FALSE.equals(req.getHasRoute());
            case Codes.COND_OVER_LIMIT: {
                if (req.getOverLimit() != null) {
                    return req.getOverLimit();
                }
                ApplicationVersion current = versionDao.get(conn, app.getApplicationId(), app.getCurrentVersionNo());
                return isOverLimit(current.getAmountRatio(), div.getAmountRatioLimit());
            }
            case Codes.COND_FIRST_CHANGE:
            case Codes.COND_LATER_CHANGE: {
                if (app.getReviewedVersionNo() == null) {
                    return false;
                }
                // 審査完了版が新規申込系（版種別 1／2）なら初回の契約変更、契約変更系（3／4）なら 2 回目以降
                ApplicationVersion reviewed = versionDao.get(conn, app.getApplicationId(), app.getReviewedVersionNo());
                boolean reviewedIsChange = isContractChangeVersion(reviewed.getVersionType());
                return Codes.COND_FIRST_CHANGE.equals(conditionCd) ? !reviewedIsChange : reviewedIsChange;
            }
            default:
                return false;
        }
    }

    /** 版種別が契約変更系（3：契約変更、4：契約変更の修正）か。 */
    public static boolean isContractChangeVersion(String versionType) {
        return Codes.VERSION_CHANGE.equals(versionType) || Codes.VERSION_CHANGE_REVISED.equals(versionType);
    }

    /** 変更基準超 = 変更金額倍率 ≧ しきい値。倍率未設定（null）は基準内。減額は常に基準内。 */
    public static boolean isOverLimit(BigDecimal ratio, BigDecimal limit) {
        return ratio != null && limit != null && ratio.compareTo(limit) >= 0;
    }

    /** 03. 8 章 操作主体と操作者の照合。 */
    private void verifyActor(Connection conn, StatusTransition t, Application app, TransitionRequest req) {
        if (!t.getActorType().equals(req.getActorType())) {
            throw new ForbiddenException();
        }
        switch (t.getActorType()) {
            case Codes.ACTOR_OWNER:
                if (!String.valueOf(app.getOwnerEmployeeId()).equals(req.getActorId())) {
                    throw new ForbiddenException();
                }
                break;
            case Codes.ACTOR_APPROVER: {
                if (req.getApprovalRequestId() == null) {
                    throw new ForbiddenException();
                }
                ApprovalRequest r = approvalRequestDao.findById(conn, req.getApprovalRequestId()).orElseThrow(ForbiddenException::new);
                if (r.getApplicationId() != app.getApplicationId()) {
                    throw new ForbiddenException();
                }
                ApprovalStep step = r.currentStep();
                if (step == null || !String.valueOf(step.getApproverEmployeeId()).equals(req.getActorId())) {
                    throw new ForbiddenException();
                }
                if ((Codes.ACTION_APPROVE.equals(req.getActionCd()) || Codes.ACTION_REVIEW_REQUEST.equals(req.getActionCd()))
                        && (r.getFinalStepNo() == null || !r.getFinalStepNo().equals(r.getCurrentStepNo()))) {
                    throw new ForbiddenException();
                }
                break;
            }
            case Codes.ACTOR_APPLICANT: {
                if (req.getConsentId() == null) {
                    throw new ForbiddenException();
                }
                ApplicantConsent c = consentDao.findById(conn, req.getConsentId()).orElseThrow(ForbiddenException::new);
                if (c.getApplicationId() != app.getApplicationId() || !Codes.CONSENT_REQUESTED.equals(c.getConsentStatus())
                        || c.getTokenExpiresAt().isBefore(LocalDateTime.now()) || c.getVersionNo() != app.getCurrentVersionNo()) {
                    throw new ForbiddenException();
                }
                break;
            }
            case Codes.ACTOR_REVIEWER: {
                if (req.getExternalLinkId() == null) {
                    throw new ForbiddenException();
                }
                ExternalLink l = externalLinkDao.findById(conn, req.getExternalLinkId()).orElseThrow(ForbiddenException::new);
                if (l.getApplicationId() != app.getApplicationId()) {
                    throw new ForbiddenException();
                }
                break;
            }
            default:
                throw new ForbiddenException();
        }
    }

    /** 1.5 後続処理。 */
    private void postProcess(Connection conn, StatusTransition t, Application app, CompanyDiv div, TransitionRequest req, LocalDateTime now) {
        boolean change = StatusCd.isContractChange(t.getToStatusCd()) || StatusCd.isContractChange(t.getFromStatusCd());
        switch (t.getTransitionId()) {
            case 5: case 21: case 34: case 49: {
                // 申請で申請中へ：ステップ 1 の承認者へ承認依頼
                ApprovalRequest r = approvalRequestDao.findById(conn, req.getApprovalRequestId()).orElseThrow(IllegalStateException::new);
                notificationService.registerApprovalRequest(conn, app, r, 1);
                break;
            }
            case 7: case 25: case 36: case 50: {
                // 承認者の差戻し：申請社員へ差戻し通知
                ApprovalRequest r = approvalRequestDao.findById(conn, req.getApprovalRequestId()).orElseThrow(IllegalStateException::new);
                notificationService.registerReturned(conn, app, r, Long.parseLong(req.getActorId()), req.getComment());
                break;
            }
            case 6: case 8: case 35: case 37: {
                // 内容確認待ちへ：申込者同意を作成し確認依頼を登録。初めて一次承認が通った申込者にはポータルのアカウントを発行する
                consentIssuer.issue(conn, app, change ? Codes.CONSENT_TYPE_CHANGE : Codes.CONSENT_TYPE_NEW);
                applicantAuthService.issueIfNeeded(conn, app);
                break;
            }
            case 9: case 11: case 38: case 40:
                // 引戻し：進行中の申込者同意を無効
                consentDao.invalidateActive(conn, app.getApplicationId());
                break;
            case 12: case 41:
                // 申込者差戻し
                consentDao.updateReturned(conn, req.getConsentId(), now, req.getComment(), req.getClientIp());
                notificationService.registerApplicantReturned(conn, app, req.getComment());
                break;
            case 14: case 15: case 42: case 43: {
                // 同意：同意状態 2、現行版を確定版に、基準版 = 現行版、（区分 2）事前確認依頼
                consentDao.updateAgreed(conn, req.getConsentId(), now, req.getClientIp());
                versionDao.updateFixed(conn, app.getApplicationId(), app.getCurrentVersionNo());
                app.setBaseVersionNo(app.getCurrentVersionNo());
                applicationDao.updateVersions(conn, app);
                if (StatusCd.PRECHECK_RESULT_ACCEPTABLE.contains(t.getToStatusCd())) {
                    externalLinkDao.insert(conn, app.getApplicationId(), app.getCurrentVersionNo(), change ? Codes.LINK_CHANGE_PRECHECK : Codes.LINK_PRECHECK);
                }
                // 同意時の申込内容 PDF（同意した版と同意事項の版）
                if (pdfService != null) {
                    pdfService.createConsentPdf(conn, app, req.getConsentId(), now);
                }
                break;
            }
            case 19: case 47: case 32:
                // 修正対応・契約変更の確定で事前確認待ちへ：事前確認依頼
                externalLinkDao.insert(conn, app.getApplicationId(), app.getCurrentVersionNo(), change ? Codes.LINK_CHANGE_PRECHECK : Codes.LINK_PRECHECK);
                break;
            case 17: case 45:
                // 事前確認 NG：担当社員へ事前確認結果通知（指摘内容は履歴コメントに記録済み）
                notificationService.registerPrecheckResult(conn, app, req.getComment());
                break;
            case 26: case 51:
                // 審査申請：審査依頼
                externalLinkDao.insert(conn, app.getApplicationId(), app.getCurrentVersionNo(), change ? Codes.LINK_CHANGE_REVIEW : Codes.LINK_REVIEW);
                break;
            case 27: case 52: {
                // 審査完了：審査完了版・審査完了日時、確定版化、審査結果通知、審査完了時の申込内容 PDF
                Integer previousReviewed = app.getReviewedVersionNo();
                app.setReviewedVersionNo(app.getCurrentVersionNo());
                app.setReviewedAt(now);
                applicationDao.updateVersions(conn, app);
                versionDao.updateFixed(conn, app.getApplicationId(), app.getCurrentVersionNo());
                notificationService.registerReviewCompleted(conn, app);
                if (pdfService != null) {
                    pdfService.createReviewedPdf(conn, app, previousReviewed, now);
                }
                break;
            }
            case 28:
                // 審査差戻し（新規申込）
                notificationService.registerReviewReturned(conn, app, req.getComment(), false);
                break;
            case 53: case 54: {
                // F13 契約変更審査差戻しの復元
                int reviewed = app.getReviewedVersionNo();
                versionDao.cancelAfter(conn, app.getApplicationId(), reviewed);
                app.setCurrentVersionNo(reviewed);
                app.setBaseVersionNo(reviewed);
                applicationDao.updateVersions(conn, app);
                consentDao.invalidateActive(conn, app.getApplicationId());
                notificationService.registerReviewReturned(conn, app, req.getComment(), true);
                break;
            }
            case 56: case 57: case 58: case 59: case 60: case 61: case 62:
                // 申込取消：進行中の申込者同意を無効にする
                consentDao.invalidateActive(conn, app.getApplicationId());
                break;
            case 63: case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71: case 72: case 73: case 74: {
                // 契約変更の取消：F13 と同じく契約変更で作った版を取り消し、現行版・基準版を審査完了版へ戻す
                int reviewed = app.getReviewedVersionNo();
                versionDao.cancelAfter(conn, app.getApplicationId(), reviewed);
                app.setCurrentVersionNo(reviewed);
                app.setBaseVersionNo(reviewed);
                applicationDao.updateVersions(conn, app);
                consentDao.invalidateActive(conn, app.getApplicationId());
                break;
            }
            case 29: case 55: {
                // 契約変更開始：審査完了版を複写した版（版種別 3）、現行版 = 新版、基準版 = 審査完了版
                ApplicationVersion reviewed = versionDao.get(conn, app.getApplicationId(), app.getReviewedVersionNo());
                int newNo = maxVersionNo(conn, app.getApplicationId()) + 1;
                versionDao.copyToNew(conn, reviewed, newNo, Codes.VERSION_CHANGE);
                app.setCurrentVersionNo(newNo);
                app.setBaseVersionNo(app.getReviewedVersionNo());
                applicationDao.updateVersions(conn, app);
                break;
            }
            case 20: case 22: {
                // 全体修正：現行版（確定版）を複写した版（版種別 2）、進行中の同意を無効
                ApplicationVersion current = versionDao.get(conn, app.getApplicationId(), app.getCurrentVersionNo());
                int newNo = maxVersionNo(conn, app.getApplicationId()) + 1;
                versionDao.copyToNew(conn, current, newNo, Codes.VERSION_NEW_REVISED);
                app.setCurrentVersionNo(newNo);
                applicationDao.updateVersions(conn, app);
                consentDao.invalidateActive(conn, app.getApplicationId());
                break;
            }
            default:
                break;
        }
    }

    private int maxVersionNo(Connection conn, long applicationId) {
        int max = 0;
        for (ApplicationVersion v : versionDao.findAll(conn, applicationId)) {
            max = Math.max(max, v.getVersionNo());
        }
        return max;
    }
}
