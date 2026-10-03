package com.example.appmgmt.common;

import java.util.LinkedHashMap;
import java.util.Map;

/** 入力チェック結果。項目名 → メッセージ（同じ項目は最初のエラーだけ保持する）。 */
public class Validation {

    private final Map<String, String> errors = new LinkedHashMap<>();

    public void reject(String field, String messageId, Object... args) {
        errors.putIfAbsent(field, Messages.get(messageId, args));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean has(String field) {
        return errors.containsKey(field);
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public static boolean hasControlChars(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        return false;
    }

    /** メールアドレスの簡易チェック（ローカル部@ドメイン、空白なし）。 */
    public static boolean isMail(String s) {
        return s != null && s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    }

    /** 電話番号（数字とハイフン）。 */
    public static boolean isTel(String s) {
        return s != null && s.matches("[0-9-]*");
    }

    public static boolean isAlnum(String s) {
        return s != null && s.matches("[A-Za-z0-9_-]*");
    }
}
