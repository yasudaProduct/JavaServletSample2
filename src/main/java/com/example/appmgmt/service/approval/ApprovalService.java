package com.example.appmgmt.service.approval;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.OptimisticLockException;
import com.example.appmgmt.common.TransitionNotAllowedException;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ApprovalRequestDao;
import com.example.appmgmt.dao.ApprovalRouteDao;
import com.example.appmgmt.dao.EmployeeDao;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApprovalRequest;
import com.example.appmgmt.domain.ApprovalRoute;
import com.example.appmgmt.domain.ApprovalRouteStep;
import com.example.appmgmt.domain.ApprovalStep;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.Employee;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.service.application.ApplicationDetail;
import com.example.appmgmt.service.application.ApplicationQueryService;
import com.example.appmgmt.service.notification.NotificationService;
import com.example.appmgmt.service.transition.StatusTransitionService;
import com.example.appmgmt.service.transition.TransitionRequest;
import com.example.appmgmt.service.transition.TransitionResult;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** F04 承認申請、F05 承認・差戻し・審査申請。 */
public class ApprovalService {

    /** 回付先に設定できる最大ステップ数。 */
    public static final int MAX_STEPS = 5;

    private final ApplicationDao applicationDao;
    private final ApprovalRequestDao requestDao;
    private final ApprovalRouteDao routeDao;
    private final EmployeeDao employeeDao;
    private final ApplicationQueryService queryService;
    private final StatusTransitionService transitionService;
    private final NotificationService notificationService;

    public ApprovalService(ApplicationDao applicationDao, ApprovalRequestDao requestDao, ApprovalRouteDao routeDao, EmployeeDao employeeDao,
                           ApplicationQueryService queryService, StatusTransitionService transitionService, NotificationService notificationService) {
        this.applicationDao = applicationDao;
        this.requestDao = requestDao;
        this.routeDao = routeDao;
        this.employeeDao = employeeDao;
        this.queryService = queryService;
        this.transitionService = transitionService;
        this.notificationService = notificationService;
    }

    /** SC06 の表示データ。 */
    public ApprovalFlowView load(long applicationId, LoginUser user) {
        return Tx.execute(conn -> {
            ApplicationDetail d = queryService.detail(conn, applicationId, user, false);
            Application app = d.getApplication();
            String st = app.getStatusCd();
            ApprovalFlowView v = new ApprovalFlowView();
            v.setDetail(d);
            String type = Codes.approvalTypeOf(st);
            if (type == null) {
                throw new TransitionNotAllowedException();
            }
            v.setApprovalType(type);
            v.setFinalApproval(Codes.isFinalApprovalType(type));
            v.setHistory(requestDao.findByApplication(conn, applicationId));
            if (StatusCd.APPROVAL_WAIT.contains(st)) {
                if (!user.isOwner() || app.getOwnerEmployeeId() != user.getEmployeeId()) {
                    throw new ForbiddenException();
                }
                v.setMode(ApprovalFlowView.Mode.WAIT);
                v.setTemplate(routeDao.findTemplate(conn, app.getCompanyDiv(), app.getDeptCd(), type, LocalDate.now()).orElse(null));
                v.setCandidates(employeeDao.findApproverCandidates(conn, app.getCompanyDiv()));
                v.setCanOperate(true);
                resolveInitialRoute(conn, v, app, type, user);
            } else {
                ApprovalRequest active = requestDao.findActive(conn, applicationId).orElseThrow(TransitionNotAllowedException::new);
                v.setActiveRequest(active);
                ApprovalStep step = active.currentStep();
                boolean operator = user.isApprover() && step != null && step.getApproverEmployeeId() == user.getEmployeeId();
                v.setMode(operator ? ApprovalFlowView.Mode.IN_PROGRESS : ApprovalFlowView.Mode.VIEW);
                v.setCanOperate(operator);
                v.setCurrentIsFinalStep(active.getFinalStepNo() != null && active.getFinalStepNo().equals(active.getCurrentStepNo()));
            }
            return v;
        });
    }

    /**
     * 回付先の初期値：同じ申込・同じ承認種別の前回の申請 → この担当者が同じ承認種別で前回使った回付先 → 承認ルートマスタのテンプレート の順に採用する。
     * 無効になった社員・承認者権限を失った社員・会社区分の異なる社員は除く（候補にないため）。
     */
    private void resolveInitialRoute(Connection conn, ApprovalFlowView v, Application app, String type, LoginUser user) {
        Set<Long> candidateIds = new HashSet<>();
        for (Employee e : v.getCandidates()) {
            candidateIds.add(e.getEmployeeId());
        }
        ApprovalRequest last = requestDao.findLatestForApplication(conn, app.getApplicationId(), type).orElse(null);
        String source = "前回の申請（この申込）";
        if (last == null) {
            last = requestDao.findLatestByRequester(conn, user.getEmployeeId(), type).orElse(null);
            source = "前回の申請（担当者が最後に使った回付先）";
        }
        List<Long> ids = new ArrayList<>();
        if (last != null) {
            for (ApprovalStep st : last.getSteps()) {
                if (candidateIds.contains(st.getApproverEmployeeId()) && !ids.contains(st.getApproverEmployeeId())) {
                    ids.add(st.getApproverEmployeeId());
                }
            }
        }
        if (ids.isEmpty()) {
            source = v.getTemplate() == null ? "（初期値なし）" : "テンプレート：" + v.getTemplate().getRouteName();
            for (Long id : templateApproverIds(v.getTemplate())) {
                if (candidateIds.contains(id) && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        if (ids.size() > MAX_STEPS) {
            ids = new ArrayList<>(ids.subList(0, MAX_STEPS));
        }
        v.setInitialApproverIds(ids);
        v.setRouteSource(source);
    }

    /** F04 申請。回付先ありは承認申請と明細を作って F14 03（条件 11）、回付先なし（一次承認のみ）は承認済の申請を作って F14 03（条件 12）。 */
    public TransitionResult apply(long applicationId, int rowVersion, List<Long> approverIds, Long routeId, LoginUser user) {
        return Tx.execute(conn -> {
            Application app = applicationDao.findById(conn, applicationId).orElseThrow(ForbiddenException::new);
            if (!user.isOwner() || app.getOwnerEmployeeId() != user.getEmployeeId()) {
                throw new ForbiddenException();
            }
            if (!StatusCd.APPROVAL_WAIT.contains(app.getStatusCd())) {
                throw new TransitionNotAllowedException();
            }
            if (app.getRowVersion() != rowVersion) {
                throw new OptimisticLockException();
            }
            if (requestDao.findActive(conn, applicationId).isPresent()) {
                throw new IllegalStateException("進行中の承認申請が残っています: " + applicationId);
            }
            String type = Codes.approvalTypeOf(app.getStatusCd());
            List<Long> ids = approverIds == null ? new ArrayList<>() : approverIds;
            if (ids.isEmpty() && Codes.isFinalApprovalType(type)) {
                throw new BusinessException("E109");
            }
            if (ids.size() > MAX_STEPS) {
                throw new BusinessException("E112", MAX_STEPS);
            }
            // 回付先の検証：有効・権限 02・会社区分一致・重複なし
            Set<Long> seen = new HashSet<>();
            List<ApprovalStep> steps = new ArrayList<>();
            int no = 0;
            for (Long id : ids) {
                Employee e = employeeDao.findById(conn, id).orElseThrow(() -> new BusinessException("E108"));
                if (!e.isValid() || !Codes.ROLE_APPROVER.equals(e.getRoleCd()) || !e.getCompanyDiv().equals(app.getCompanyDiv()) || !seen.add(id)) {
                    throw new BusinessException("E108");
                }
                ApprovalStep s = new ApprovalStep();
                s.setStepNo(++no);
                s.setApproverEmployeeId(id);
                s.setResultCd(Codes.RESULT_PENDING);
                steps.add(s);
            }
            LocalDateTime now = LocalDateTime.now().withNano(0);
            ApprovalRequest r = new ApprovalRequest();
            r.setApplicationId(applicationId);
            r.setVersionNo(app.getCurrentVersionNo());
            r.setApprovalType(type);
            r.setRouteId(routeId);
            r.setRequestEmployeeId(user.getEmployeeId());
            r.setRequestedAt(now);
            if (steps.isEmpty()) {
                r.setRequestStatus(Codes.REQUEST_APPROVED);
                r.setCompletedAt(now);
            } else {
                r.setRequestStatus(Codes.REQUEST_IN_PROGRESS);
                r.setCurrentStepNo(1);
                r.setFinalStepNo(steps.size());
                r.setSteps(steps);
            }
            long requestId = requestDao.insert(conn, r);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_REQUEST, Codes.ACTOR_OWNER, String.valueOf(user.getEmployeeId()))
                    .approvalRequestId(requestId).hasRoute(!steps.isEmpty()));
        });
    }

    private ApprovalRequest loadActive(Connection conn, long applicationId, int rowVersion, long requestId, LoginUser user, boolean requireFinalStep) {
        Application app = applicationDao.findById(conn, applicationId).orElseThrow(ForbiddenException::new);
        if (!StatusCd.APPROVAL_IN_PROGRESS.contains(app.getStatusCd())) {
            throw new TransitionNotAllowedException();
        }
        if (app.getRowVersion() != rowVersion) {
            throw new OptimisticLockException();
        }
        ApprovalRequest r = requestDao.findActive(conn, applicationId).orElseThrow(TransitionNotAllowedException::new);
        if (r.getApprovalRequestId() != requestId) {
            throw new OptimisticLockException();
        }
        ApprovalStep step = r.currentStep();
        if (!user.isApprover() || step == null || step.getApproverEmployeeId() != user.getEmployeeId()) {
            throw new ForbiddenException();
        }
        boolean finalStep = r.getFinalStepNo() != null && r.getFinalStepNo().equals(r.getCurrentStepNo());
        if (requireFinalStep && !finalStep) {
            throw new TransitionNotAllowedException();
        }
        return r;
    }

    /** F05 6.2 承認。最終ステップでなければ次の承認者へ回し（遷移なし）、一次承認の最終承認者なら F14 04。 */
    public TransitionResult approve(long applicationId, int rowVersion, long requestId, String comment, LoginUser user) {
        return Tx.execute(conn -> {
            ApprovalRequest r = loadActive(conn, applicationId, rowVersion, requestId, user, false);
            LocalDateTime now = LocalDateTime.now().withNano(0);
            boolean finalStep = r.getFinalStepNo().equals(r.getCurrentStepNo());
            if (finalStep && Codes.isFinalApprovalType(r.getApprovalType())) {
                // 最終承認の最終承認者は「承認」ではなく「審査申請」
                throw new TransitionNotAllowedException();
            }
            requestDao.updateStepResult(conn, requestId, r.getCurrentStepNo(), Codes.RESULT_APPROVED, comment, now);
            if (!finalStep) {
                int next = r.getCurrentStepNo() + 1;
                requestDao.updateProgress(conn, requestId, Codes.REQUEST_IN_PROGRESS, next, null);
                Application app = applicationDao.findById(conn, applicationId).orElseThrow();
                notificationService.registerApprovalRequest(conn, app, r, next);
                if (applicationDao.touch(conn, applicationId, rowVersion) != 1) {
                    throw new OptimisticLockException();
                }
                return null;
            }
            requestDao.updateProgress(conn, requestId, Codes.REQUEST_APPROVED, r.getCurrentStepNo(), now);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_APPROVE, Codes.ACTOR_APPROVER, String.valueOf(user.getEmployeeId()))
                    .approvalRequestId(requestId).comment(comment));
        });
    }

    /** F05 6.3 差戻し。コメント必須。 */
    public TransitionResult returnRequest(long applicationId, int rowVersion, long requestId, String comment, LoginUser user) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessException("E105");
        }
        return Tx.execute(conn -> {
            ApprovalRequest r = loadActive(conn, applicationId, rowVersion, requestId, user, false);
            LocalDateTime now = LocalDateTime.now().withNano(0);
            requestDao.updateStepResult(conn, requestId, r.getCurrentStepNo(), Codes.RESULT_RETURNED, comment, now);
            requestDao.updateProgress(conn, requestId, Codes.REQUEST_RETURNED, r.getCurrentStepNo(), now);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_RETURN, Codes.ACTOR_APPROVER, String.valueOf(user.getEmployeeId()))
                    .approvalRequestId(requestId).comment(comment));
        });
    }

    /** F05 6.4 審査申請。最終承認（02／04）の最終ステップのみ。 */
    public TransitionResult requestReview(long applicationId, int rowVersion, long requestId, String comment, LoginUser user) {
        return Tx.execute(conn -> {
            ApprovalRequest r = loadActive(conn, applicationId, rowVersion, requestId, user, true);
            if (!Codes.isFinalApprovalType(r.getApprovalType())) {
                throw new TransitionNotAllowedException();
            }
            LocalDateTime now = LocalDateTime.now().withNano(0);
            requestDao.updateStepResult(conn, requestId, r.getCurrentStepNo(), Codes.RESULT_REVIEW_REQUESTED, comment, now);
            requestDao.updateProgress(conn, requestId, Codes.REQUEST_APPROVED, r.getCurrentStepNo(), now);
            return transitionService.transition(conn, TransitionRequest.of(applicationId, rowVersion, Codes.ACTION_REVIEW_REQUEST, Codes.ACTOR_APPROVER, String.valueOf(user.getEmployeeId()))
                    .approvalRequestId(requestId).comment(comment));
        });
    }

    /** テンプレートの承認者 ID 一覧（画面の初期値）。 */
    public static List<Long> templateApproverIds(ApprovalRoute route) {
        List<Long> ids = new ArrayList<>();
        if (route != null) {
            for (ApprovalRouteStep s : route.getSteps()) {
                ids.add(s.getApproverEmployeeId());
            }
        }
        return ids;
    }
}
