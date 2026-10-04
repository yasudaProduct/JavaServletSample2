package com.example.appmgmt.domain;

import java.time.LocalDateTime;

/** 同意事項の版（M_CONSENT_DOCUMENT_VERSION）。PDF 本体（data）は必要なときだけ読む。 */
public class ConsentDocumentVersion extends AuditedEntity {
    private String documentCd;
    private int versionNo;
    private LocalDateTime effectiveFrom;
    private String fileName;
    private int fileSize;
    private String fileHash;
    private String remarks;
    private byte[] data;

    public String getDocumentCd() { return documentCd; }
    public void setDocumentCd(String documentCd) { this.documentCd = documentCd; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public int getFileSize() { return fileSize; }
    public void setFileSize(int fileSize) { this.fileSize = fileSize; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    /** 適用前（適用開始日時が未来）か。 */
    public boolean isScheduled() { return effectiveFrom != null && effectiveFrom.isAfter(LocalDateTime.now()); }
}
