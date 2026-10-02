package com.example.appmgmt.domain;

import java.util.Set;

/** ステータスコード（03. ステータス定義）。 */
public final class StatusCd {

    private StatusCd() {
    }

    public static final String IMPORTED = "10100";
    public static final String INPUT = "10101";
    public static final String PRIMARY_WAIT = "10201";
    public static final String PRIMARY_IN_PROGRESS = "10202";
    public static final String CONFIRM_WAIT = "10301";
    public static final String CONSENT_WAIT = "10302";
    public static final String PRECHECK_WAIT = "10401";
    public static final String FIX_WAIT = "10402";
    public static final String FINAL_WAIT = "10501";
    public static final String FINAL_IN_PROGRESS = "10502";
    public static final String REVIEWING = "10601";
    public static final String REVIEWED = "10701";
    public static final String CHG_INPUT = "20101";
    public static final String CHG_PRIMARY_WAIT = "20201";
    public static final String CHG_PRIMARY_IN_PROGRESS = "20202";
    public static final String CHG_CONFIRM_WAIT = "20301";
    public static final String CHG_CONSENT_WAIT = "20302";
    public static final String CHG_PRECHECK_WAIT = "20401";
    public static final String CHG_FIX_WAIT = "20402";
    public static final String CHG_FINAL_WAIT = "20501";
    public static final String CHG_FINAL_IN_PROGRESS = "20502";
    public static final String CHG_REVIEWING = "20601";
    public static final String CHG_REVIEWED = "20701";

    public static final Set<String> APPROVAL_WAIT = Set.of(PRIMARY_WAIT, FINAL_WAIT, CHG_PRIMARY_WAIT, CHG_FINAL_WAIT);
    public static final Set<String> APPROVAL_IN_PROGRESS = Set.of(PRIMARY_IN_PROGRESS, FINAL_IN_PROGRESS, CHG_PRIMARY_IN_PROGRESS, CHG_FINAL_IN_PROGRESS);
    public static final Set<String> APPLICANT_CONFIRMING = Set.of(CONFIRM_WAIT, CONSENT_WAIT, CHG_CONFIRM_WAIT, CHG_CONSENT_WAIT);
    public static final Set<String> CONTENT_CONFIRM_WAIT = Set.of(CONFIRM_WAIT, CHG_CONFIRM_WAIT);
    public static final Set<String> AGREE_WAIT = Set.of(CONSENT_WAIT, CHG_CONSENT_WAIT);
    public static final Set<String> EXTERNAL_WAIT = Set.of(PRECHECK_WAIT, REVIEWING, CHG_PRECHECK_WAIT, CHG_REVIEWING);
    public static final Set<String> PRECHECK_RESULT_ACCEPTABLE = Set.of(PRECHECK_WAIT, CHG_PRECHECK_WAIT);
    public static final Set<String> REVIEW_RESULT_ACCEPTABLE = Set.of(REVIEWING, CHG_REVIEWING);
    public static final Set<String> REVISE = Set.of(FIX_WAIT, FINAL_WAIT, CHG_FIX_WAIT);
    public static final Set<String> REVIEWED_ALL = Set.of(REVIEWED, CHG_REVIEWED);

    public static boolean isContractChange(String statusCd) {
        return statusCd != null && statusCd.startsWith("2");
    }
}
