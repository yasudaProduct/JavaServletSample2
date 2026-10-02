package com.example.appmgmt.domain;

import java.time.LocalDateTime;

public class ImportBatch extends AuditedEntity {
    private long importBatchId;
    private String fileName;
    private long importEmployeeId;
    private String importEmployeeName;
    private LocalDateTime importedAt;
    private int totalCount;
    private int successCount;
    private int errorCount;

    public long getImportBatchId() { return importBatchId; }
    public void setImportBatchId(long importBatchId) { this.importBatchId = importBatchId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getImportEmployeeId() { return importEmployeeId; }
    public void setImportEmployeeId(long importEmployeeId) { this.importEmployeeId = importEmployeeId; }
    public String getImportEmployeeName() { return importEmployeeName; }
    public void setImportEmployeeName(String importEmployeeName) { this.importEmployeeName = importEmployeeName; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
}
