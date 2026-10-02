package com.example.appmgmt.domain;

import java.time.LocalDateTime;

public class ApplicantConsent extends AuditedEntity {
    private long consentId;
    private long applicationId;
    private int versionNo;
    private String consentType;
    private String accessTokenHash;
    private LocalDateTime tokenExpiresAt;
    private String consentStatus;
    private LocalDateTime contentConfirmedAt;
    private LocalDateTime consentedAt;
    private LocalDateTime returnedAt;
    private String returnReason;
    private String clientIp;

    public long getConsentId() { return consentId; }
    public void setConsentId(long consentId) { this.consentId = consentId; }
    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }
    public String getAccessTokenHash() { return accessTokenHash; }
    public void setAccessTokenHash(String accessTokenHash) { this.accessTokenHash = accessTokenHash; }
    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    public String getConsentStatus() { return consentStatus; }
    public void setConsentStatus(String consentStatus) { this.consentStatus = consentStatus; }
    public LocalDateTime getContentConfirmedAt() { return contentConfirmedAt; }
    public void setContentConfirmedAt(LocalDateTime contentConfirmedAt) { this.contentConfirmedAt = contentConfirmedAt; }
    public LocalDateTime getConsentedAt() { return consentedAt; }
    public void setConsentedAt(LocalDateTime consentedAt) { this.consentedAt = consentedAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getConsentTypeName() { return Codes.label("CONSENT_TYPE", consentType); }
    public String getConsentStatusName() { return Codes.label("CONSENT_STATUS", consentStatus); }
}
