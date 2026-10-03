package com.example.appmgmt.service.application;

import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicantConsentDao;
import com.example.appmgmt.dao.ApplicantDao;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ApplicationVersionDao;
import com.example.appmgmt.dao.ApprovalRequestDao;
import com.example.appmgmt.dao.CompanyDivDao;
import com.example.appmgmt.dao.EmployeeDao;
import com.example.appmgmt.dao.ExternalLinkDao;
import com.example.appmgmt.dao.StatusDao;
import com.example.appmgmt.dao.StatusHistoryDao;
import com.example.appmgmt.dao.StatusTransitionDao;
import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationListRow;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.ApprovalRequest;
import com.example.appmgmt.domain.ApprovalStep;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.Employee;
import com.example.appmgmt.domain.ExternalLink;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.domain.Status;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.domain.StatusHistory;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** F02 申込一覧・検索と、申込詳細（SC03）の組み立て。 */
public class ApplicationQueryService {

    private final ApplicationDao applicationDao;
    private final ApplicationVersionDao versionDao;
    private final ApplicantDao applicantDao;
    private final EmployeeDao employeeDao;
    private final StatusDao statusDao;
    private final CompanyDivDao companyDivDao;
    private final ApprovalRequestDao approvalRequestDao;
    private final ApplicantConsentDao consentDao;
    private final ExternalLinkDao externalLinkDao;
    private final StatusHistoryDao historyDao;
    private final StatusTransitionDao transitionDao;

    public ApplicationQueryService(ApplicationDao applicationDao, ApplicationVersionDao versionDao, ApplicantDao applicantDao, EmployeeDao employeeDao, StatusDao statusDao,
                                   CompanyDivDao companyDivDao, ApprovalRequestDao approvalRequestDao, ApplicantConsentDao consentDao, ExternalLinkDao externalLinkDao, StatusHistoryDao historyDao,
                                   StatusTransitionDao transitionDao) {
        this.applicationDao = applicationDao;
        this.versionDao = versionDao;
        this.applicantDao = applicantDao;
        this.employeeDao = employeeDao;
        this.statusDao = statusDao;
        this.companyDivDao = companyDivDao;
        this.approvalRequestDao = approvalRequestDao;
        this.consentDao = consentDao;
        this.externalLinkDao = externalLinkDao;
        this.historyDao = historyDao;
        this.transitionDao = transitionDao;
    }

    public static class SearchResult {
        public List<ApplicationListRow> rows;
        public long total;
        public int page;
        public int pageSize;
        public int totalPages() { return (int) Math.max(1, (total + pageSize - 1) / pageSize); }
        public List<ApplicationListRow> getRows() { return rows; }
        public long getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return totalPages(); }
    }

    public SearchResult search(ApplicationDao.Criteria c, LoginUser user) {
        c.scopeRole = user.getRoleCd();
        c.loginEmployeeId = user.getEmployeeId();
        c.loginCompanyDiv = user.getCompanyDiv();
        c.loginDeptCd = user.getDeptCd();
        if (user.isOwner()) {
            c.ownerEmployeeId = user.getEmployeeId();
        }
        if (user.isAdmin()) {
            c.myTasksOnly = false;
        }
        return Tx.execute(conn -> {
            SearchResult r = new SearchResult();
            r.total = applicationDao.count(conn, c);
            r.page = Math.max(1, c.page);
            r.pageSize = c.pageSize;
            r.rows = applicationDao.search(conn, c);
            return r;
        });
    }

    public List<Status> allStatuses() {
        return Tx.execute(statusDao::findAll);
    }

    public List<Employee> validEmployees() {
        return Tx.execute(employeeDao::findValid);
    }

    /** 参照範囲（11. 1.2 節）の検証。範囲外は E103。 */
    public void checkViewable(Connection conn, Application app, LoginUser user) {
        if (user.isAdmin()) {
            return;
        }
        if (user.isOwner()) {
            if (app.getOwnerEmployeeId() != user.getEmployeeId()) {
                throw new ForbiddenException();
            }
            return;
        }
        if (user.isApprover()) {
            boolean sameDept = app.getCompanyDiv().equals(user.getCompanyDiv()) && app.getDeptCd().equals(user.getDeptCd());
            if (sameDept || applicationDao.existsForApprover(conn, app.getApplicationId(), user.getEmployeeId())) {
                return;
            }
        }
        throw new ForbiddenException();
    }

    public ApplicationDetail detail(long applicationId, LoginUser user) {
        return Tx.execute(conn -> detail(conn, applicationId, user, true));
    }

    /** 見出し用の軽量ビュー（履歴等を含めない）。 */
    public ApplicationDetail header(Connection conn, long applicationId, LoginUser user) {
        return detail(conn, applicationId, user, false);
    }

    public ApplicationDetail detail(Connection conn, long applicationId, LoginUser user, boolean full) {
        Application app = applicationDao.findById(conn, applicationId).orElseThrow(ForbiddenException::new);
        if (user != null) {
            checkViewable(conn, app, user);
        }
        ApplicationDetail d = new ApplicationDetail();
        d.setApplication(app);
        d.setApplicant(applicantDao.findById(conn, app.getApplicantId()).orElse(null));
        d.setOwner(employeeDao.findById(conn, app.getOwnerEmployeeId()).orElse(null));
        d.setStatus(statusDao.find(conn, app.getStatusCd()).orElse(null));
        d.setCompanyDiv(companyDivDao.find(conn, app.getCompanyDiv()).orElse(null));
        d.setCurrentVersion(versionDao.get(conn, applicationId, app.getCurrentVersionNo()));
        if (app.getBaseVersionNo() != null) {
            d.setBaseVersion(versionDao.find(conn, applicationId, app.getBaseVersionNo()).orElse(null));
        }
        if (app.getReviewedVersionNo() != null) {
            d.setReviewedVersion(versionDao.find(conn, applicationId, app.getReviewedVersionNo()).orElse(null));
        }
        d.setShowDiff(d.getBaseVersion() != null && d.getBaseVersion().getVersionNo() != app.getCurrentVersionNo());
        if (app.getSourceApplicationId() != null) {
            applicationDao.findById(conn, app.getSourceApplicationId()).ifPresent(s -> d.setSourceApplicationNo(s.getApplicationNo()));
        }
        d.setActiveRequest(approvalRequestDao.findActive(conn, applicationId).orElse(null));
        if (full) {
            d.setVersions(versionDao.findAll(conn, applicationId));
            d.setApprovalRequests(approvalRequestDao.findByApplication(conn, applicationId));
            d.setConsents(consentDao.findByApplication(conn, applicationId));
            d.setExternalLinks(externalLinkDao.findByApplication(conn, applicationId));
            List<StatusHistory> hs = historyDao.findByApplication(conn, applicationId);
            resolveActorNames(conn, hs, d.getApplicant());
            d.setHistories(hs);
        }
        if (user != null) {
            d.setActions(computeActions(conn, d, user));
        }
        return d;
    }

    private void resolveActorNames(Connection conn, List<StatusHistory> histories, Applicant applicant) {
        Map<String, String> cache = new HashMap<>();
        for (StatusHistory h : histories) {
            String name;
            switch (h.getActorType()) {
                case Codes.ACTOR_OWNER:
                case Codes.ACTOR_APPROVER:
                    name = cache.computeIfAbsent(h.getActorId(), id -> {
                        try {
                            return employeeDao.findById(conn, Long.parseLong(id)).map(Employee::getEmployeeName).orElse(id);
                        } catch (NumberFormatException e) {
                            return id;
                        }
                    });
                    break;
                case Codes.ACTOR_APPLICANT:
                    name = applicant == null ? h.getActorId() : applicant.getApplicantName();
                    break;
                default:
                    name = "審査担当部門（" + h.getActorId() + "）";
            }
            h.setActorName(name);
        }
    }

    /** ステータス×権限×操作者のボタン表示制御（11. 4.3 節）。 */
    private java.util.Set<String> computeActions(Connection conn, ApplicationDetail d, LoginUser user) {
        java.util.Set<String> a = new java.util.LinkedHashSet<>();
        Application app = d.getApplication();
        String st = app.getStatusCd();
        boolean owner = user.isOwner() && app.getOwnerEmployeeId() == user.getEmployeeId();
        boolean currentApprover = false;
        if (user.isApprover() && d.getActiveRequest() != null) {
            ApprovalStep step = d.getActiveRequest().currentStep();
            currentApprover = step != null && step.getApproverEmployeeId() == user.getEmployeeId();
        }
        boolean sendError = false;
        for (ExternalLink l : externalLinkDao.findByApplication(conn, app.getApplicationId())) {
            if (l.getVersionNo() == app.getCurrentVersionNo() && Codes.SEND_ERROR.equals(l.getSendStatus())) {
                sendError = true;
            }
        }
        if (owner) {
            if (!StatusCd.isCanceled(st)) {
                a.add("additional");
            }
            // 取消（操作コード 15）は遷移マスタに該当行があるステータスだけ
            if (!transitionDao.findCandidates(conn, st, Codes.ACTION_CANCEL, d.getCompanyDiv().getPreCheckFlg()).isEmpty()) {
                a.add(StatusCd.isContractChange(st) ? "cancelChange" : "cancel");
            }
            // 修正（操作コード 01）は遷移マスタに該当行があるステータスだけ（10100／10201／20201／20501 の修正、10402／10501 の全体修正）。
            // 入力画面へはメニューからではなく SC05／SC09 の「修正」ボタンから進む
            if (!transitionDao.findCandidates(conn, st, Codes.ACTION_MODIFY, d.getCompanyDiv().getPreCheckFlg()).isEmpty()) {
                a.add(StatusCd.isContractChange(st) ? "modifyChange" : "modify");
            }
            switch (st) {
                case StatusCd.IMPORTED: a.add("confirm"); break;
                case StatusCd.INPUT: a.add("input"); a.add("confirm"); break;
                case StatusCd.PRIMARY_WAIT: a.add("request"); break;
                case StatusCd.CONFIRM_WAIT: case StatusCd.CONSENT_WAIT: case StatusCd.CHG_CONFIRM_WAIT: case StatusCd.CHG_CONSENT_WAIT:
                    a.add("pullBack"); a.add("resendConsent"); break;
                case StatusCd.PRECHECK_WAIT: case StatusCd.REVIEWING: case StatusCd.CHG_PRECHECK_WAIT: case StatusCd.CHG_REVIEWING:
                    if (sendError) { a.add("resendExternal"); } break;
                case StatusCd.FIX_WAIT: a.add("fix"); a.add("fullRevise"); break;
                case StatusCd.FINAL_WAIT: a.add("request"); a.add("revise"); a.add("fullRevise"); break;
                case StatusCd.REVIEWED: case StatusCd.CHG_REVIEWED: a.add("startChange"); break;
                case StatusCd.CHG_INPUT: a.add("inputChange"); a.add("confirmChange"); break;
                case StatusCd.CHG_PRIMARY_WAIT: case StatusCd.CHG_FINAL_WAIT: a.add("request"); break;
                case StatusCd.CHG_FIX_WAIT: a.add("fix"); break;
                default: break;
            }
        }
        if (currentApprover && StatusCd.APPROVAL_IN_PROGRESS.contains(st)) {
            a.add("approvalFlow");
        } else if (user.isApprover() && StatusCd.APPROVAL_IN_PROGRESS.contains(st)) {
            a.add("approvalFlowView");
        } else if (owner && StatusCd.APPROVAL_IN_PROGRESS.contains(st)) {
            // 申請した担当者は申請中の状況を参照できる（申請ボタンは非活性、引戻し不可）
            a.add("approvalFlowView");
        }
        if (user.isAdmin() && StatusCd.EXTERNAL_WAIT.contains(st) && sendError) {
            a.add("resendExternal");
        }
        return a;
    }

    /** 申込者ポータル：申込者の申込一覧。 */
    public List<Application> applicationsOfApplicant(long applicantId) {
        return Tx.execute(conn -> applicationDao.findByApplicant(conn, applicantId));
    }

    /** 申込者ポータル：申込者本人の申込であることを確認して詳細を組み立てる（ボタン制御なし）。 */
    public ApplicationDetail detailForApplicant(long applicationId, long applicantId) {
        return Tx.execute(conn -> {
            Application app = applicationDao.findById(conn, applicationId).orElseThrow(ForbiddenException::new);
            if (app.getApplicantId() != applicantId) {
                throw new ForbiddenException();
            }
            return detail(conn, applicationId, null, true);
        });
    }

    public ApplicationVersion currentVersion(Connection conn, Application app) {
        return versionDao.get(conn, app.getApplicationId(), app.getCurrentVersionNo());
    }

    public ApprovalRequest activeRequest(Connection conn, long applicationId) {
        return approvalRequestDao.findActive(conn, applicationId).orElse(null);
    }
}
