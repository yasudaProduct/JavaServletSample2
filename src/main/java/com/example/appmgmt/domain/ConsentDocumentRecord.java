package com.example.appmgmt.domain;

import java.time.LocalDateTime;

/** 申込者同意の同意事項（T_APPLICANT_CONSENT_DOCUMENT）。どの同意で、どの文書のどの版を開き、同意したか。 */
public class ConsentDocumentRecord {
    private long consentId;
    private String documentCd;
    private int versionNo;
    private LocalDateTime viewedAt;
    private LocalDateTime agreedAt;
    /** 表示用 */
    private String documentName;
    private LocalDateTime effectiveFrom;
    private String fileHash;
    private String consentType;
    private int contentVersionNo;

    public long getConsentId() { return consentId; }
    public void setConsentId(long consentId) { this.consentId = consentId; }
    public String getDocumentCd() { return documentCd; }
    public void setDocumentCd(String documentCd) { this.documentCd = documentCd; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
    public LocalDateTime getAgreedAt() { return agreedAt; }
    public void setAgreedAt(LocalDateTime agreedAt) { this.agreedAt = agreedAt; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }
    public String getConsentTypeName() { return Codes.label("CONSENT_TYPE", consentType); }
    /** 同意した申込内容の版番号。 */
    public int getContentVersionNo() { return contentVersionNo; }
    public void setContentVersionNo(int contentVersionNo) { this.contentVersionNo = contentVersionNo; }
}
