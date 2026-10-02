package com.example.appmgmt.domain;

public class Status extends AuditedEntity {
    private String statusCd;
    private String statusName;
    private String applicantStatusName;
    private String categoryL;
    private String categoryM;
    private String categoryS;
    private String actorType;
    private String editableFlg;
    private int displayOrder;
    private String description;

    public String getStatusCd() { return statusCd; }
    public void setStatusCd(String statusCd) { this.statusCd = statusCd; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getApplicantStatusName() { return applicantStatusName; }
    public void setApplicantStatusName(String applicantStatusName) { this.applicantStatusName = applicantStatusName; }
    public String getCategoryL() { return categoryL; }
    public void setCategoryL(String categoryL) { this.categoryL = categoryL; }
    public String getCategoryM() { return categoryM; }
    public void setCategoryM(String categoryM) { this.categoryM = categoryM; }
    public String getCategoryS() { return categoryS; }
    public void setCategoryS(String categoryS) { this.categoryS = categoryS; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getEditableFlg() { return editableFlg; }
    public void setEditableFlg(String editableFlg) { this.editableFlg = editableFlg; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDisplayName() { return statusCd + " " + statusName; }
}
