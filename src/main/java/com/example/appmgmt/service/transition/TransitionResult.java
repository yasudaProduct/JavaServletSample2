package com.example.appmgmt.service.transition;

import com.example.appmgmt.domain.Application;

/** F14 の出力：採用した遷移 ID、遷移元・遷移先、履歴 ID、更新後の申込。 */
public class TransitionResult {
    private final int transitionId;
    private final String fromStatusCd;
    private final String toStatusCd;
    private final long historyId;
    private final Application application;
    private final boolean overLimit;

    public TransitionResult(int transitionId, String fromStatusCd, String toStatusCd, long historyId, Application application, boolean overLimit) {
        this.transitionId = transitionId;
        this.fromStatusCd = fromStatusCd;
        this.toStatusCd = toStatusCd;
        this.historyId = historyId;
        this.application = application;
        this.overLimit = overLimit;
    }

    public int getTransitionId() { return transitionId; }
    public String getFromStatusCd() { return fromStatusCd; }
    public String getToStatusCd() { return toStatusCd; }
    public long getHistoryId() { return historyId; }
    public Application getApplication() { return application; }
    /** 条件 21（変更基準超）の行を採用したか。画面で W001 を出す判断に使う。 */
    public boolean isOverLimit() { return overLimit; }
}
