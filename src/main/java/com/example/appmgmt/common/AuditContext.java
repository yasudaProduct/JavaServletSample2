package com.example.appmgmt.common;

/**
 * 監査列（CREATED_BY／UPDATED_BY）に入れる操作者 ID をスレッド単位で保持する。
 * 社員 ID、申込者 ID（"AP:" 付き）、外部システム ID、バッチ ID のいずれか。
 */
public final class AuditContext {

    private static final ThreadLocal<String> ACTOR = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void set(String actor) {
        ACTOR.set(actor);
    }

    public static String get() {
        String a = ACTOR.get();
        return a == null ? "SYSTEM" : a;
    }

    public static void clear() {
        ACTOR.remove();
    }

    public static String employee(long employeeId) {
        return String.valueOf(employeeId);
    }

    public static String applicant(long applicantId) {
        return "AP:" + applicantId;
    }
}
