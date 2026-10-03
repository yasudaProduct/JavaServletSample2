package com.example.appmgmt.service.approval;

import com.example.appmgmt.domain.ApprovalRoute;
import com.example.appmgmt.domain.ApprovalRequest;
import com.example.appmgmt.domain.Employee;
import com.example.appmgmt.service.application.ApplicationDetail;
import java.util.ArrayList;
import java.util.List;

/** SC06 承認フロー画面の表示データ。 */
public class ApprovalFlowView {
    public enum Mode { WAIT, IN_PROGRESS, VIEW }

    private ApplicationDetail detail;
    private Mode mode;
    private String approvalType;
    private boolean finalApproval;
    private ApprovalRoute template;
    private List<Employee> candidates = new ArrayList<>();
    private ApprovalRequest activeRequest;
    private boolean canOperate;
    private boolean currentIsFinalStep;
    private List<ApprovalRequest> history = new ArrayList<>();
    private List<Long> initialApproverIds = new ArrayList<>();
    private String routeSource = "";
    private String viewNote = "";
    private boolean ownerViewer;
    private boolean approverViewer;

    public ApplicationDetail getDetail() { return detail; }
    public void setDetail(ApplicationDetail detail) { this.detail = detail; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public String getModeName() { return mode.name(); }
    public String getApprovalType() { return approvalType; }
    public void setApprovalType(String approvalType) { this.approvalType = approvalType; }
    public String getApprovalTypeName() { return approvalType == null ? "－" : com.example.appmgmt.domain.Codes.label("APPROVAL_TYPE", approvalType); }
    /** 参照モードのときに表示する説明（操作できない理由）。 */
    public String getViewNote() { return viewNote; }
    public void setViewNote(String viewNote) { this.viewNote = viewNote; }
    /** 閲覧者が担当者権限（申請ボタンを非活性で表示する）。 */
    public boolean isOwnerViewer() { return ownerViewer; }
    public void setOwnerViewer(boolean ownerViewer) { this.ownerViewer = ownerViewer; }
    /** 閲覧者が承認者権限（承認・差戻しボタンを非活性で表示する）。 */
    public boolean isApproverViewer() { return approverViewer; }
    public void setApproverViewer(boolean approverViewer) { this.approverViewer = approverViewer; }
    public boolean isFinalApproval() { return finalApproval; }
    public void setFinalApproval(boolean finalApproval) { this.finalApproval = finalApproval; }
    public ApprovalRoute getTemplate() { return template; }
    public void setTemplate(ApprovalRoute template) { this.template = template; }
    public List<Employee> getCandidates() { return candidates; }
    public void setCandidates(List<Employee> candidates) { this.candidates = candidates; }
    public ApprovalRequest getActiveRequest() { return activeRequest; }
    public void setActiveRequest(ApprovalRequest activeRequest) { this.activeRequest = activeRequest; }
    public boolean isCanOperate() { return canOperate; }
    public void setCanOperate(boolean canOperate) { this.canOperate = canOperate; }
    public boolean isCurrentIsFinalStep() { return currentIsFinalStep; }
    public void setCurrentIsFinalStep(boolean currentIsFinalStep) { this.currentIsFinalStep = currentIsFinalStep; }
    public List<ApprovalRequest> getHistory() { return history; }
    public void setHistory(List<ApprovalRequest> history) { this.history = history; }
    public List<Long> getInitialApproverIds() { return initialApproverIds; }
    public void setInitialApproverIds(List<Long> initialApproverIds) { this.initialApproverIds = initialApproverIds; }
    /** 回付先の初期値の出所（前回の申請／担当者の前回の申請／テンプレート／なし）。 */
    public String getRouteSource() { return routeSource; }
    public void setRouteSource(String routeSource) { this.routeSource = routeSource; }
}
