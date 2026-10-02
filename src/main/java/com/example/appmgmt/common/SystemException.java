package com.example.appmgmt.common;

/** システムエラー（DB 障害、想定外の例外）。CM01 に集約する。 */
public class SystemException extends RuntimeException {
    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemException(String message) {
        super(message);
    }
}
