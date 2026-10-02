package com.example.appmgmt.domain;

public class StatusTransition extends AuditedEntity {
    private int transitionId;
    private String fromStatusCd;
    private String actionCd;
    private String conditionCd;
    private int evalOrder;
    private String toStatusCd;
    private String actorType;
    private String preCheckCond;
    private String refNo;

    public int getTransitionId() { return transitionId; }
    public void setTransitionId(int transitionId) { this.transitionId = transitionId; }
    public String getFromStatusCd() { return fromStatusCd; }
    public void setFromStatusCd(String fromStatusCd) { this.fromStatusCd = fromStatusCd; }
    public String getActionCd() { return actionCd; }
    public void setActionCd(String actionCd) { this.actionCd = actionCd; }
    public String getConditionCd() { return conditionCd; }
    public void setConditionCd(String conditionCd) { this.conditionCd = conditionCd; }
    public int getEvalOrder() { return evalOrder; }
    public void setEvalOrder(int evalOrder) { this.evalOrder = evalOrder; }
    public String getToStatusCd() { return toStatusCd; }
    public void setToStatusCd(String toStatusCd) { this.toStatusCd = toStatusCd; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getPreCheckCond() { return preCheckCond; }
    public void setPreCheckCond(String preCheckCond) { this.preCheckCond = preCheckCond; }
    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }
}
