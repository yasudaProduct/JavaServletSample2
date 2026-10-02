package com.example.appmgmt.domain;

import java.time.LocalDateTime;

public class StatusHistory extends AuditedEntity {
    private long statusHistoryId;
    private long applicationId;
    private int versionNo;
    private String fromStatusCd;
    private String toStatusCd;
    private Integer transitionId;
    private String actionCd;
    private String actorType;
    private String actorId;
    private String actorName;
    private String comment;
    private LocalDateTime changedAt;

    public long getStatusHistoryId() { return statusHistoryId; }
    public void setStatusHistoryId(long statusHistoryId) { this.statusHistoryId = statusHistoryId; }
    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getFromStatusCd() { return fromStatusCd; }
    public void setFromStatusCd(String fromStatusCd) { this.fromStatusCd = fromStatusCd; }
    public String getToStatusCd() { return toStatusCd; }
    public void setToStatusCd(String toStatusCd) { this.toStatusCd = toStatusCd; }
    public Integer getTransitionId() { return transitionId; }
    public void setTransitionId(Integer transitionId) { this.transitionId = transitionId; }
    public String getActionCd() { return actionCd; }
    public void setActionCd(String actionCd) { this.actionCd = actionCd; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public String getActionName() { return Codes.label("ACTION", actionCd); }
    public String getActorTypeName() { return Codes.label("ACTOR", actorType); }
}
