package com.example.appmgmt.service.transition;

/** F14 への入力（10. 機能詳細 1.2 節）。 */
public class TransitionRequest {
    private long applicationId;
    private int expectedRowVersion;
    private String actionCd;
    private String actorType;
    private String actorId;
    private String comment;
    private Long approvalRequestId;
    private Long consentId;
    private Long externalLinkId;
    private Boolean hasRoute;
    private String clientIp;

    public static TransitionRequest of(long applicationId, int expectedRowVersion, String actionCd, String actorType, String actorId) {
        TransitionRequest r = new TransitionRequest();
        r.applicationId = applicationId;
        r.expectedRowVersion = expectedRowVersion;
        r.actionCd = actionCd;
        r.actorType = actorType;
        r.actorId = actorId;
        return r;
    }

    public TransitionRequest comment(String comment) { this.comment = comment; return this; }
    public TransitionRequest approvalRequestId(Long id) { this.approvalRequestId = id; return this; }
    public TransitionRequest consentId(Long id) { this.consentId = id; return this; }
    public TransitionRequest externalLinkId(Long id) { this.externalLinkId = id; return this; }
    public TransitionRequest hasRoute(Boolean hasRoute) { this.hasRoute = hasRoute; return this; }
    public TransitionRequest clientIp(String ip) { this.clientIp = ip; return this; }

    public long getApplicationId() { return applicationId; }
    public int getExpectedRowVersion() { return expectedRowVersion; }
    public String getActionCd() { return actionCd; }
    public String getActorType() { return actorType; }
    public String getActorId() { return actorId; }
    public String getComment() { return comment; }
    public Long getApprovalRequestId() { return approvalRequestId; }
    public Long getConsentId() { return consentId; }
    public Long getExternalLinkId() { return externalLinkId; }
    public Boolean getHasRoute() { return hasRoute; }
    public String getClientIp() { return clientIp; }
}
