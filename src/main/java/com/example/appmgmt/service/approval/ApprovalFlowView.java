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

    public ApplicationDetail getDetail() { return detail; }
    public void setDetail(ApplicationDetail detail) { this.detail = detail; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public String getModeName() { return mode.name(); }
    public String getApprovalType() { return approvalType; }
    public void setApprovalType(String approvalType) { this.approvalType = approvalType; }
    public String getApprovalTypeName() { return com.example.appmgmt.domain.Codes.label("APPROVAL_TYPE", approvalType); }
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
}
