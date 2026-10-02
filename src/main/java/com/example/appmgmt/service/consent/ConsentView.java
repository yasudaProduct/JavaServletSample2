package com.example.appmgmt.service.consent;

import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.ApplicantConsent;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Status;

/** トークン検証の結果と申込者向け画面の表示データ（F07 8.3）。 */
public class ConsentView {
    public enum Outcome { INVALID, EXPIRED, COMPLETED, CONFIRM, AGREE }

    private Outcome outcome;
    private ApplicantConsent consent;
    private Application application;
    private Applicant applicant;
    private ApplicationVersion version;
    private ApplicationVersion beforeVersion;
    private Status status;

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public String getOutcomeName() { return outcome == null ? "" : outcome.name(); }
    public ApplicantConsent getConsent() { return consent; }
    public void setConsent(ApplicantConsent consent) { this.consent = consent; }
    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }
    public Applicant getApplicant() { return applicant; }
    public void setApplicant(Applicant applicant) { this.applicant = applicant; }
    public ApplicationVersion getVersion() { return version; }
    public void setVersion(ApplicationVersion version) { this.version = version; }
    public ApplicationVersion getBeforeVersion() { return beforeVersion; }
    public void setBeforeVersion(ApplicationVersion beforeVersion) { this.beforeVersion = beforeVersion; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isContractChange() { return consent != null && "2".equals(consent.getConsentType()); }
    public String getApplicantStatusName() { return status == null ? "" : status.getApplicantStatusName(); }
}
