package com.example.appmgmt.infra.mail;

/** メール送信エラー。種別で再送可否を区別する（13. 4.4 節）。 */
public class MailException extends Exception {
    public enum Kind { TEMPORARY, PERMANENT, INFRA }

    private final Kind kind;

    public MailException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
