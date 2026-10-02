package com.example.appmgmt.domain;

public class ImportError extends AuditedEntity {
    private long importBatchId;
    private int lineNo;
    private String errorMessage;
    private String rawLine;

    public long getImportBatchId() { return importBatchId; }
    public void setImportBatchId(long importBatchId) { this.importBatchId = importBatchId; }
    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
}
