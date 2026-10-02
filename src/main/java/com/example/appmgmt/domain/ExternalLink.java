package com.example.appmgmt.domain;

import java.time.LocalDateTime;

public class ExternalLink extends AuditedEntity {
    private long externalLinkId;
    private long applicationId;
    private int versionNo;
    private String linkType;
    private String sendStatus;
    private int retryCount;
    private LocalDateTime sentAt;
    private String errorMessage;
    private String externalReceiptNo;
    private String resultCd;
    private LocalDateTime resultReceivedAt;
    private String resultReason;
    private String applicationNo;

    public long getExternalLinkId() { return externalLinkId; }
    public void setExternalLinkId(long externalLinkId) { this.externalLinkId = externalLinkId; }
    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getLinkType() { return linkType; }
    public void setLinkType(String linkType) { this.linkType = linkType; }
    public String getSendStatus() { return sendStatus; }
    public void setSendStatus(String sendStatus) { this.sendStatus = sendStatus; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getExternalReceiptNo() { return externalReceiptNo; }
    public void setExternalReceiptNo(String externalReceiptNo) { this.externalReceiptNo = externalReceiptNo; }
    public String getResultCd() { return resultCd; }
    public void setResultCd(String resultCd) { this.resultCd = resultCd; }
    public LocalDateTime getResultReceivedAt() { return resultReceivedAt; }
    public void setResultReceivedAt(LocalDateTime resultReceivedAt) { this.resultReceivedAt = resultReceivedAt; }
    public String getResultReason() { return resultReason; }
    public void setResultReason(String resultReason) { this.resultReason = resultReason; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public String getLinkTypeName() { return Codes.label("LINK_TYPE", linkType); }
    public String getSendStatusName() { return Codes.label("SEND_STATUS", sendStatus); }
    public String getResultName() { return Codes.label("EXT_RESULT", resultCd); }
    public boolean isPrecheck() { return Codes.LINK_PRECHECK.equals(linkType) || Codes.LINK_CHANGE_PRECHECK.equals(linkType); }
}
