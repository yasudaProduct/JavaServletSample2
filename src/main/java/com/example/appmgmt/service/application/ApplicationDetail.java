package com.example.appmgmt.service.application;

import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.ApplicantConsent;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.ApprovalRequest;
import com.example.appmgmt.domain.CompanyDiv;
import com.example.appmgmt.domain.Employee;
import com.example.appmgmt.domain.ExternalLink;
import com.example.appmgmt.domain.Status;
import com.example.appmgmt.domain.StatusHistory;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 申込詳細（SC03）や各画面の見出しに使う申込の集約ビュー。 */
public class ApplicationDetail {
    private Application application;
    private Applicant applicant;
    private Employee owner;
    private Status status;
    private CompanyDiv companyDiv;
    private ApplicationVersion currentVersion;
    private ApplicationVersion baseVersion;
    private ApplicationVersion reviewedVersion;
    private List<ApplicationVersion> versions = new ArrayList<>();
    private List<ApprovalRequest> approvalRequests = new ArrayList<>();
    private List<ApplicantConsent> consents = new ArrayList<>();
    private List<ExternalLink> externalLinks = new ArrayList<>();
    private List<StatusHistory> histories = new ArrayList<>();
    private String sourceApplicationNo;
    private ApprovalRequest activeRequest;
    private Set<String> actions = new LinkedHashSet<>();
    private boolean showDiff;

    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }
    public Applicant getApplicant() { return applicant; }
    public void setApplicant(Applicant applicant) { this.applicant = applicant; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public CompanyDiv getCompanyDiv() { return companyDiv; }
    public void setCompanyDiv(CompanyDiv companyDiv) { this.companyDiv = companyDiv; }
    public ApplicationVersion getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(ApplicationVersion currentVersion) { this.currentVersion = currentVersion; }
    public ApplicationVersion getBaseVersion() { return baseVersion; }
    public void setBaseVersion(ApplicationVersion baseVersion) { this.baseVersion = baseVersion; }
    public ApplicationVersion getReviewedVersion() { return reviewedVersion; }
    public void setReviewedVersion(ApplicationVersion reviewedVersion) { this.reviewedVersion = reviewedVersion; }
    public List<ApplicationVersion> getVersions() { return versions; }
    public void setVersions(List<ApplicationVersion> versions) { this.versions = versions; }
    public List<ApprovalRequest> getApprovalRequests() { return approvalRequests; }
    public void setApprovalRequests(List<ApprovalRequest> approvalRequests) { this.approvalRequests = approvalRequests; }
    public List<ApplicantConsent> getConsents() { return consents; }
    public void setConsents(List<ApplicantConsent> consents) { this.consents = consents; }
    public List<ExternalLink> getExternalLinks() { return externalLinks; }
    public void setExternalLinks(List<ExternalLink> externalLinks) { this.externalLinks = externalLinks; }
    public List<StatusHistory> getHistories() { return histories; }
    public void setHistories(List<StatusHistory> histories) { this.histories = histories; }
    public String getSourceApplicationNo() { return sourceApplicationNo; }
    public void setSourceApplicationNo(String sourceApplicationNo) { this.sourceApplicationNo = sourceApplicationNo; }
    public ApprovalRequest getActiveRequest() { return activeRequest; }
    public void setActiveRequest(ApprovalRequest activeRequest) { this.activeRequest = activeRequest; }
    public Set<String> getActions() { return actions; }
    public void setActions(Set<String> actions) { this.actions = actions; }
    public boolean isShowDiff() { return showDiff; }
    public void setShowDiff(boolean showDiff) { this.showDiff = showDiff; }
    public boolean has(String action) { return actions.contains(action); }
}
