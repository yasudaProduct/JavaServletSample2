package com.example.appmgmt.service.application;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.Formats;
import com.example.appmgmt.common.OptimisticLockException;
import com.example.appmgmt.common.TransitionNotAllowedException;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ApplicationVersionDao;
import com.example.appmgmt.dao.CompanyDivDao;
import com.example.appmgmt.dao.DepartmentDao;
import com.example.appmgmt.dao.EmployeeDao;
import com.example.appmgmt.dao.ExternalLinkDao;
import com.example.appmgmt.dao.StatusHistoryDao;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.Department;
import com.example.appmgmt.domain.Employee;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.domain.StatusHistory;
import com.example.appmgmt.service.consent.ConsentIssuer;
import com.example.appmgmt.service.transition.StatusTransitionService;
import com.example.appmgmt.service.transition.TransitionRequest;
import com.example.appmgmt.service.transition.TransitionResult;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * F03 申込入力・修正、F10 同意後の申込修正、F11 引戻し、F12 契約変更入力、F06 再送、外部連携再送。
 * ステータス更新はすべて F14（StatusTransitionService）を通す。
 */
public class ApplicationService {

    private final ApplicationDao applicationDao;
    private final ApplicationVersionDao versionDao;
    private final StatusHistoryDao historyDao;
    private final ExternalLinkDao externalLinkDao;
    private final StatusTransitionService transitionService;
    private final ConsentIssuer consentIssuer;
    private final CompanyDivDao companyDivDao;
    private final DepartmentDao departmentDao;
    private final EmployeeDao employeeDao;

    public ApplicationService(ApplicationDao applicationDao, ApplicationVersionDao versionDao, StatusHistoryDao historyDao,
                              ExternalLinkDao externalLinkDao, StatusTransitionService transitionService, ConsentIssuer consentIssuer,
                              CompanyDivDao companyDivDao, DepartmentDao departmentDao, EmployeeDao employeeDao) {
        this.applicationDao = applicationDao;
        this.versionDao = versionDao;
        this.historyDao = historyDao;
        this.externalLinkDao = externalLinkDao;
        this.transitionService = transitionService;
        this.consentIssuer = consentIssuer;
        this.companyDivDao = companyDivDao;
        this.departmentDao = departmentDao;
        this.employeeDao = employeeDao;
    }

    /**
     * 担当（会社区分 > 部署 > 担当社員）の検証。会社区分が存在すること、部署がその会社区分の有効な部署であること、
     * 担当社員がその会社区分の有効な担当者権限（01）の社員であること（部署は担当社員の所属と異なってよい）。
     * 保存済みの値と同じ部署は、後から無効になっていても受け付ける。
     */
    private void validateAssignment(Connection conn, Assignment a, Assignment saved) {
        if (a.getCompanyDiv() == null || companyDivDao.find(conn, a.getCompanyDiv()).isEmpty()) {
            throw new BusinessException("E011", "会社");
        }
        boolean sameDept = saved != null && a.getCompanyDiv().equals(saved.getCompanyDiv()) && a.getDeptCd().equals(saved.getDeptCd());
        Department d = departmentDao.find(conn, a.getCompanyDiv(), a.getDeptCd()).orElse(null);
        if (d == null || (!d.isValid() && !sameDept)) {
            throw new BusinessException("E011", "部署");
        }
        Employee e = employeeDao.findById(conn, a.getOwnerEmployeeId()).orElse(null);
        if (e == null || !e.isValid() || !Codes.ROLE_OWNER.equals(e.getRoleCd()) || !e.getCompanyDiv().equals(a.getCompanyDiv())) {
            throw new BusinessException("E012");
        }
    }

    /** 担当社員の氏名（保存後のメッセージ用）。 */
    public String employeeName(long employeeId) {
        return Tx.execute(conn -> employeeDao.findById(conn, employeeId).map(Employee::getEmployeeName).orElse(""));
    }

    /** 申込番号（担当者を自分以外にして保存したときの案内用。保存後は入力した社員が申込を参照できないため、参照権限を確認せずに引く）。 */
    public String applicationNo(long applicationId) {
        return Tx.execute(conn -> applicationDao.findById(conn, applicationId).map(Application::getApplicationNo).orElse(""));
    }

    private Application load(Connection conn, long applicationId, int expectedRowVersion, LoginUser user, Set<String> allowedStatuses) {
        Application app = applicationDao.findById(conn, applicationId).orElseThrow(ForbiddenException::new);
        if (!user.isOwner() || app.getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new ForbiddenException();
        }
        if (allowedStatuses != null && !allowedStatuses.contains(app.getStatusCd())) {
            throw new TransitionNotAllowedException();
        }
        if (app.getRowVersion() != expectedRowVersion) {
            throw new OptimisticLockException();
        }
        return app;
    }

    private static String actor(LoginUser user) {
        return String.valueOf(user.getEmployeeId());
    }

    // ---------------------------------------------------------------- F03 新規申込・追加申込・入力

    /**
     * 新規申込（登録区分 2）または追加申込（登録区分 3）を作成する。ステータス 10101、第 1 版、履歴 00。
     * 申込者情報（申込者名・カナ・電話番号・メールアドレス・住所）は申込データとして第 1 版に持つ。
     * 新規申込は同じ氏名・メールアドレスでも別の申込者として扱い、申込者アカウントは一次承認が通ったときに新しく発行して紐づける。
     * 追加申込（同じ申込者の新しい申込）は元の申込が審査完了（10701／20701）のときだけ作れ、元の申込のアカウントを引き継ぐ。
     * 申込者情報・申込内容は画面で複写した値を初期値にした別データ（元の申込とは独立）。
     * 担当（会社区分・部署・担当社員）は入力値（未指定ならログインユーザーの値）。担当社員を自分以外にした場合、保存後の操作は担当社員が行う。
     */
    public long create(LoginUser user, ApplicationVersion content, Assignment assignment, Long sourceApplicationId) {
        if (!user.isOwner()) {
            throw new ForbiddenException();
        }
        return Tx.execute(conn -> {
            Long accountId = null;
            if (sourceApplicationId != null) {
                Application src = applicationDao.findById(conn, sourceApplicationId).orElseThrow(ForbiddenException::new);
                if (src.getOwnerEmployeeId() != user.getEmployeeId()) {
                    throw new ForbiddenException();
                }
                if (!StatusCd.REVIEWED_ALL.contains(src.getStatusCd())) {
                    throw new TransitionNotAllowedException();
                }
                accountId = src.getApplicantId();
            }
            Assignment a = assignment == null ? Assignment.of(user) : assignment;
            validateAssignment(conn, a, null);
            Application app = new Application();
            app.setApplicationNo(applicationDao.nextApplicationNo(conn));
            app.setApplicantId(accountId);
            app.setOwnerEmployeeId(a.getOwnerEmployeeId());
            app.setCompanyDiv(a.getCompanyDiv());
            app.setDeptCd(a.getDeptCd());
            app.setStatusCd(StatusCd.INPUT);
            app.setCurrentVersionNo(1);
            app.setRegistrationType(sourceApplicationId == null ? Codes.REG_SCREEN : Codes.REG_ADDITIONAL);
            app.setSourceApplicationId(sourceApplicationId);
            long id = applicationDao.insert(conn, app);

            ApplicationVersion v = content.copyContent();
            v.applyAssignmentFrom(a.getCompanyDiv(), a.getDeptCd(), a.getOwnerEmployeeId());
            v.setApplicationId(id);
            v.setVersionNo(1);
            v.setVersionType(Codes.VERSION_NEW);
            v.setFixedFlg(Codes.FLG_OFF);
            v.setCanceledFlg(Codes.FLG_OFF);
            versionDao.insert(conn, v);

            StatusHistory h = new StatusHistory();
            h.setApplicationId(id);
            h.setVersionNo(1);
            h.setToStatusCd(StatusCd.INPUT);
            h.setActionCd(Codes.ACTION_CREATE);
            h.setActorType(Codes.ACTOR_OWNER);
            h.setActorId(actor(user));
            h.setChangedAt(LocalDateTime.now().withNano(0));
            historyDao.insert(conn, h);
            return id;
        });
    }

    /** 一時保存（10101／20101）。現行版（申込者情報を含む申込内容）をそのまま更新する。遷移・履歴なし。 */
    public void saveDraft(long applicationId, int rowVersion, ApplicationVersion content, LoginUser user) {
        saveDraft(applicationId, rowVersion, content, null, user);
    }

    /**
     * 一時保存。assignment を渡すと担当（会社区分・部署・担当社員）も変更する（入力中 10101 のみ。契約変更の入力中は変更できない）。
     * 申込と現行版の両方に記録する。
     */
    public void saveDraft(long applicationId, int rowVersion, ApplicationVersion content, Assignment assignment, LoginUser user) {
        Tx.executeVoid(conn -> {
            Application app = load(conn, applicationId, rowVersion, user, Set.of(StatusCd.INPUT, StatusCd.CHG_INPUT));
            ApplicationVersion v = versionDao.get(conn, applicationId, app.getCurrentVersionNo());
            if (assignment != null) {
                if (!StatusCd.INPUT.equals(app.getStatusCd())) {
                    throw new TransitionNotAllowedException();
                }
                Assignment saved = Assignment.of(app);
                if (!assignment.sameAs(saved)) {
                    validateAssignment(conn, assignment, saved);
                    applicationDao.updateAssignment(conn, applicationId, assignment.getCompanyDiv(), assignment.getDeptCd(), assignment.getOwnerEmployeeId());
                }
                versionDao.updateAssignment(conn, applicationId, v.getVersionNo(), assignment.getCompanyDiv(), assignment.getDeptCd(), assignment.getOwnerEmployeeId());
            }
            applyContent(v, content);
            v.setAmountRatio(null);
            versionDao.updateContent(conn, v);
            if (applicationDao.touch(conn, applicationId, rowVersion) != 1) {
                throw new OptimisticLockException();
            }
        });
    }

    private static void applyContent(ApplicationVersion target, ApplicationVersion content) {
        target.applyContentFrom(content);
    }

    /** SC05 確定（10100／10101 → 10201）。確定日時を設定し F14 02。 */
    public TransitionResult confirm(long applicationId, int rowVersion, LoginUser user) {
        return Tx.execute(conn -> {
            Application app = load(conn, applicationId, rowVersion, user, Set.of(StatusCd.IMPORTED, StatusCd.INPUT));
            ApplicationVersion v = versionDao.get(conn, applicationId, app.getCurrentVersionNo());
            v.setConfirmedAt(LocalDateTime.now().withNano(0));
            versionDao.updateContent(conn, v);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_CONFIRM, Codes.ACTOR_OWNER, actor(user)));
        });
    }

    /** 修正（操作コード 01）：10100／10201 → 10101、20201／20501 → 20101。全体修正（10402／10501 → 10101）も同じ操作。 */
    public TransitionResult modify(long applicationId, int rowVersion, LoginUser user) {
        return Tx.execute(conn -> {
            load(conn, applicationId, rowVersion, user, null);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_MODIFY, Codes.ACTOR_OWNER, actor(user)));
        });
    }

    // ---------------------------------------------------------------- F10 同意後の修正（SC07）

    /**
     * SC07 確定。変更があれば現行版を複写した新しい版を作り、変更金額倍率を設定して F14 02 を呼ぶ。
     * 10501 は金額項目だけを反映し、変更なしは E104。10402／20402 は変更なしでも確定できる（版を作らず F14）。
     */
    public TransitionResult revise(long applicationId, int rowVersion, ApplicationVersion content, LoginUser user) {
        return Tx.execute(conn -> {
            Application app = load(conn, applicationId, rowVersion, user, StatusCd.REVISE);
            ApplicationVersion current = versionDao.get(conn, applicationId, app.getCurrentVersionNo());
            boolean amountsOnly = StatusCd.FINAL_WAIT.equals(app.getStatusCd());
            ApplicationVersion merged = current.copyContent();
            if (amountsOnly) {
                merged.setBasicFee(content.getBasicFee());
                merged.setOptionFee(content.getOptionFee());
                merged.setHandlingFee(content.getHandlingFee());
            } else {
                applyContent(merged, content);
            }
            merged.recalcTotal();
            boolean changed = !merged.sameContentAs(current);
            if (!changed) {
                if (amountsOnly) {
                    throw new BusinessException("E104");
                }
                // 変更なし：版を作らず再依頼（変更基準内として扱う）。現行版に残っている変更金額倍率（基準超の修正で同意した版など）では判定しない
                return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_CONFIRM, Codes.ACTOR_OWNER, actor(user)).overLimit(false));
            }
            int newNo = maxVersionNo(conn, applicationId) + 1;
            ApplicationVersion v = merged;
            v.setApplicationId(applicationId);
            v.setVersionNo(newNo);
            v.setVersionType(StatusCd.CHG_FIX_WAIT.equals(app.getStatusCd()) ? Codes.VERSION_CHANGE_REVISED : Codes.VERSION_NEW_REVISED);
            v.setCopiedFromVersionNo(current.getVersionNo());
            v.setConfirmedAt(LocalDateTime.now().withNano(0));
            v.setFixedFlg(Codes.FLG_OFF);
            v.setCanceledFlg(Codes.FLG_OFF);
            v.setAmountRatio(ratioAgainstBase(conn, app, v));
            versionDao.insert(conn, v);
            app.setCurrentVersionNo(newNo);
            applicationDao.updateVersions(conn, app);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_CONFIRM, Codes.ACTOR_OWNER, actor(user)));
        });
    }

    private java.math.BigDecimal ratioAgainstBase(Connection conn, Application app, ApplicationVersion v) {
        if (app.getBaseVersionNo() == null) {
            return null;
        }
        ApplicationVersion base = versionDao.get(conn, app.getApplicationId(), app.getBaseVersionNo());
        return Formats.amountRatio(v.getTotalAmount(), base.getTotalAmount());
    }

    private int maxVersionNo(Connection conn, long applicationId) {
        int max = 0;
        for (ApplicationVersion v : versionDao.findAll(conn, applicationId)) {
            max = Math.max(max, v.getVersionNo());
        }
        return max;
    }

    // ---------------------------------------------------------------- F11 引戻し

    public TransitionResult pullBack(long applicationId, int rowVersion, LoginUser user) {
        return Tx.execute(conn -> {
            load(conn, applicationId, rowVersion, user, StatusCd.APPLICANT_CONFIRMING);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_PULL_BACK, Codes.ACTOR_OWNER, actor(user)));
        });
    }

    // ---------------------------------------------------------------- 取消（操作コード 15）

    /** 申込取消（→ 90101）または契約変更の取消（→ 10701／20701）。遷移可否は遷移マスタに従う。 */
    public TransitionResult cancel(long applicationId, int rowVersion, String reason, LoginUser user) {
        return Tx.execute(conn -> {
            load(conn, applicationId, rowVersion, user, null);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_CANCEL, Codes.ACTOR_OWNER, actor(user)).comment(reason));
        });
    }

    // ---------------------------------------------------------------- F12 契約変更

    /** 契約変更手続きの開始（10701／20701 → 20101）。後続処理で審査完了版を複写した版が作られる。 */
    public TransitionResult startChange(long applicationId, int rowVersion, LoginUser user) {
        return Tx.execute(conn -> {
            load(conn, applicationId, rowVersion, user, StatusCd.REVIEWED_ALL);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_CHANGE_START, Codes.ACTOR_OWNER, actor(user)));
        });
    }

    /** SC09 確定（20101）。審査完了版と比べて変更がなければ E104。倍率と確定日時を設定して F14 02。 */
    public TransitionResult confirmChange(long applicationId, int rowVersion, LoginUser user) {
        return Tx.execute(conn -> {
            Application app = load(conn, applicationId, rowVersion, user, Set.of(StatusCd.CHG_INPUT));
            ApplicationVersion current = versionDao.get(conn, applicationId, app.getCurrentVersionNo());
            ApplicationVersion reviewed = versionDao.get(conn, applicationId, app.getReviewedVersionNo());
            current.recalcTotal();
            if (current.sameContentAs(reviewed)) {
                throw new BusinessException("E104");
            }
            current.setAmountRatio(ratioAgainstBase(conn, app, current));
            current.setConfirmedAt(LocalDateTime.now().withNano(0));
            versionDao.updateContent(conn, current);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_CONFIRM, Codes.ACTOR_OWNER, actor(user)));
        });
    }

    // ---------------------------------------------------------------- 再送

    /** 確認依頼メール再送（F06 7.3）。進行中の同意を無効にして新しい URL を発行する。遷移なし。 */
    public void resendConsent(long applicationId, int rowVersion, LoginUser user) {
        Tx.executeVoid(conn -> {
            Application app = load(conn, applicationId, rowVersion, user, StatusCd.APPLICANT_CONFIRMING);
            consentIssuer.issue(conn, app, StatusCd.isContractChange(app.getStatusCd()) ? Codes.CONSENT_TYPE_CHANGE : Codes.CONSENT_TYPE_NEW);
            if (applicationDao.touch(conn, applicationId, rowVersion) != 1) {
                throw new OptimisticLockException();
            }
        });
    }

    /** 外部連携再送。現行版の送信エラー行を未送信に戻す。担当者（担当の申込）または管理者。 */
    public void resendExternal(long applicationId, int rowVersion, LoginUser user) {
        Tx.executeVoid(conn -> {
            Application app = applicationDao.findById(conn, applicationId).orElseThrow(ForbiddenException::new);
            boolean owner = user.isOwner() && app.getOwnerEmployeeId() == user.getEmployeeId();
            if (!owner && !user.isAdmin()) {
                throw new ForbiddenException();
            }
            if (!StatusCd.EXTERNAL_WAIT.contains(app.getStatusCd())) {
                throw new TransitionNotAllowedException();
            }
            if (app.getRowVersion() != rowVersion) {
                throw new OptimisticLockException();
            }
            externalLinkDao.resetForResend(conn, applicationId, app.getCurrentVersionNo());
            if (applicationDao.touch(conn, applicationId, rowVersion) != 1) {
                throw new OptimisticLockException();
            }
        });
    }
}
