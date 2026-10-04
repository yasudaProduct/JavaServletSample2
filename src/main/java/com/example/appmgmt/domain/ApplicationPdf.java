package com.example.appmgmt.domain;

/** 申込内容 PDF（T_APPLICATION_PDF）。種別 1：同意時、2：審査完了時。PDF 本体（data）は必要なときだけ読む。 */
public class ApplicationPdf extends AuditedEntity {
    private long pdfId;
    private long applicationId;
    private int versionNo;
    private String pdfType;
    private Long consentId;
    private String fileName;
    private int fileSize;
    private String fileHash;
    private byte[] data;
    /** 表示用：手続き（新規申込／契約変更 N）、状態（有効・置き換え・取消など）、版種別。 */
    private String phaseLabel;
    private String stateLabel;
    private boolean current;
    private String versionType;

    public long getPdfId() { return pdfId; }
    public void setPdfId(long pdfId) { this.pdfId = pdfId; }
    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getPdfType() { return pdfType; }
    public void setPdfType(String pdfType) { this.pdfType = pdfType; }
    public String getPdfTypeName() { return Codes.label("PDF_TYPE", pdfType); }
    public boolean isConsented() { return Codes.PDF_CONSENTED.equals(pdfType); }
    public Long getConsentId() { return consentId; }
    public void setConsentId(Long consentId) { this.consentId = consentId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public int getFileSize() { return fileSize; }
    public void setFileSize(int fileSize) { this.fileSize = fileSize; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    public String getPhaseLabel() { return phaseLabel; }
    public void setPhaseLabel(String phaseLabel) { this.phaseLabel = phaseLabel; }
    public String getStateLabel() { return stateLabel; }
    public void setStateLabel(String stateLabel) { this.stateLabel = stateLabel; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
    public String getVersionType() { return versionType; }
    public void setVersionType(String versionType) { this.versionType = versionType; }
    public String getVersionTypeName() { return Codes.label("VERSION_TYPE", versionType); }
}
