package com.example.appmgmt.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/**
 * アプリケーション設定。application.properties の値を、環境変数・システムプロパティで上書きする。
 * キー "db.url" は環境変数 "DB_URL"、システムプロパティ "db.url" に対応する。
 */
public final class AppConfig {

    private static volatile AppConfig instance;

    private final Properties props = new Properties();

    private AppConfig() {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IllegalStateException("application.properties を読み込めません", e);
        }
    }

    public static AppConfig get() {
        AppConfig c = instance;
        if (c == null) {
            synchronized (AppConfig.class) {
                c = instance;
                if (c == null) {
                    c = new AppConfig();
                    instance = c;
                }
            }
        }
        return c;
    }

    /** テストや組込み起動で設定を差し替えるために使う。 */
    public static void override(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    public String getString(String key) {
        String sys = System.getProperty(key);
        if (sys != null) {
            return sys;
        }
        String env = System.getenv(toEnvKey(key));
        if (env != null) {
            return env;
        }
        return props.getProperty(key, "");
    }

    public String getString(String key, String defaultValue) {
        String v = getString(key);
        return v.isEmpty() ? defaultValue : v;
    }

    public int getInt(String key, int defaultValue) {
        String v = getString(key).trim();
        if (v.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("設定値 " + key + " が数値ではありません: " + v);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = getString(key).trim();
        if (v.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(v);
    }

    static String toEnvKey(String key) {
        return key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }
}
