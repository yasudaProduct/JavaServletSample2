package com.example.appmgmt.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;

/** メッセージ（messages.properties）。ID で参照し {0}、{1} を置換する。 */
public final class Messages {

    private static final Properties PROPS = load();

    private Messages() {
    }

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = Messages.class.getClassLoader().getResourceAsStream("messages.properties")) {
            if (in != null) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IllegalStateException("messages.properties を読み込めません", e);
        }
        return p;
    }

    public static String get(String id, Object... args) {
        String pattern = PROPS.getProperty(id);
        if (pattern == null) {
            return id;
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        return new MessageFormat(pattern.replace("'", "''")).format(args);
    }
}
