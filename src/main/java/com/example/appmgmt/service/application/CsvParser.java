package com.example.appmgmt.service.application;

import java.util.ArrayList;
import java.util.List;

/** RFC 4180 相当の簡易 CSV パーサ（ダブルクォート囲み、"" エスケープ、囲み内の改行・カンマに対応）。 */
public final class CsvParser {

    private CsvParser() {
    }

    public static List<List<String>> parse(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                        continue;
                    }
                    quoted = false;
                    i++;
                    continue;
                }
                field.append(c);
                i++;
                continue;
            }
            if (c == '"') {
                quoted = true;
                i++;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                i++;
            } else if (c == '\r' || c == '\n') {
                row.add(field.toString());
                field.setLength(0);
                records.add(row);
                row = new ArrayList<>();
                if (c == '\r' && i + 1 < n && text.charAt(i + 1) == '\n') {
                    i++;
                }
                i++;
            } else {
                field.append(c);
                i++;
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            records.add(row);
        }
        return records;
    }
}
