package com.example.appmgmt.infra.extapi;

/** 外部 API 送信エラー（12. 3.5 節の判定に使う）。 */
public class ExternalApiException extends Exception {
    /** TEMPORARY：5xx・本文不正（再送）、PERMANENT：4xx（再送しない）、CONNECTION：接続エラー・タイムアウト（再送。連続で打ち切り判定） */
    public enum Kind { TEMPORARY, PERMANENT, CONNECTION }

    private final Kind kind;

    public ExternalApiException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
