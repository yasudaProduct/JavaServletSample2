package com.example.appmgmt.web.view;

import com.example.appmgmt.common.Formats;
import com.example.appmgmt.domain.Codes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** JSP の EL 関数（/WEB-INF/tlds/app.tld）。 */
public final class Fn {

    private Fn() {
    }

    public static String date(LocalDate d) {
        return d == null ? "－" : Formats.date(d);
    }

    public static String datetime(LocalDateTime d) {
        return d == null ? "－" : Formats.dateTime(d);
    }

    public static String amount(BigDecimal v) {
        return v == null ? "－" : Formats.amount(v);
    }

    public static String ratio(BigDecimal v) {
        return v == null ? "－" : Formats.ratio(v);
    }

    public static String label(String group, String code) {
        return Codes.label(group, code);
    }

    /** メッセージ定義（messages.properties）の文言。 */
    public static String message(String id) {
        return com.example.appmgmt.common.Messages.get(id);
    }

    public static String text(Object v) {
        return v == null || v.toString().isEmpty() ? "－" : v.toString();
    }

    public static boolean differs(Object a, Object b) {
        if (a instanceof BigDecimal && b instanceof BigDecimal) {
            return ((BigDecimal) a).compareTo((BigDecimal) b) != 0;
        }
        if (a == null || (a instanceof String && ((String) a).isEmpty())) {
            return !(b == null || (b instanceof String && ((String) b).isEmpty()));
        }
        return !a.equals(b);
    }
}
