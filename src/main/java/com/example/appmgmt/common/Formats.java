package com.example.appmgmt.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** 日付・日時・金額の表示と入力の書式（14. 共通仕様 5 章）。 */
public final class Formats {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATE_ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm");
    public static final DateTimeFormatter DATETIME_SEC = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");

    private Formats() {
    }

    public static String date(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "" : DATETIME.format(d);
    }

    public static String amount(BigDecimal v) {
        if (v == null) {
            return "";
        }
        return new DecimalFormat("#,##0").format(v);
    }

    public static String ratio(BigDecimal v) {
        if (v == null) {
            return "";
        }
        return v.setScale(2, RoundingMode.DOWN).toPlainString();
    }

    /** yyyy/MM/dd または yyyy-MM-dd を日付に変換する。空は null、不正は null を返す（呼出元で E003）。 */
    public static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        try {
            if (t.contains("/")) {
                return LocalDate.parse(t, DATE);
            }
            return LocalDate.parse(t, DATE_ISO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** カンマ許容の整数文字列を BigDecimal に変換する。不正は null を返す。 */
    public static BigDecimal parseAmount(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim().replace(",", "");
        if (!t.matches("-?[0-9]{1,13}")) {
            return null;
        }
        return new BigDecimal(t);
    }

    /** 変更金額倍率（小数第 4 位まで切り捨て）。基準が 0 以下なら null。 */
    public static BigDecimal amountRatio(BigDecimal current, BigDecimal base) {
        if (current == null || base == null || base.signum() <= 0) {
            return null;
        }
        return current.divide(base, 4, RoundingMode.DOWN);
    }
}
