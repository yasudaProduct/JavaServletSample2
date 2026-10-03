package com.example.appmgmt.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataSource（HikariCP）の生成と保持。 */
public final class DataSourceProvider {

    private static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);
    private static volatile DataSource dataSource;

    private DataSourceProvider() {
    }

    public static DataSource get() {
        DataSource ds = dataSource;
        if (ds == null) {
            throw new IllegalStateException("DataSource が初期化されていません");
        }
        return ds;
    }

    /** テスト等で外部から DataSource を差し込む。 */
    public static void set(DataSource ds) {
        dataSource = ds;
    }

    public static synchronized void init(AppConfig config) {
        if (dataSource != null) {
            return;
        }
        String url = config.getString("db.url");
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);
        // Tomcat では WEB-INF/lib のドライバを DriverManager が自動登録しないため、クラス名を明示する
        String driver = driverClassName(url);
        if (driver != null) {
            hc.setDriverClassName(driver);
        }
        hc.setUsername(config.getString("db.user"));
        hc.setPassword(config.getString("db.password"));
        hc.setMaximumPoolSize(config.getInt("db.pool.max-size", 10));
        hc.setPoolName("appmgmt");
        hc.setAutoCommit(false);
        hc.setInitializationFailTimeout(-1);
        hc.setConnectionTimeout(10_000);
        HikariDataSource ds = new HikariDataSource(hc);
        waitForDatabase(ds, config.getInt("db.startup-wait-seconds", 120));
        dataSource = ds;
    }

    /** JDBC URL からドライバクラスを決める。未知の URL は HikariCP の自動解決に任せる。 */
    private static String driverClassName(String url) {
        if (url != null && url.startsWith("jdbc:sqlserver:")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }
        if (url != null && url.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        }
        return null;
    }

    private static void waitForDatabase(DataSource ds, int waitSeconds) {
        long deadline = System.currentTimeMillis() + waitSeconds * 1000L;
        SQLException last = null;
        int attempt = 0;
        while (true) {
            attempt++;
            try (Connection c = ds.getConnection()) {
                if (attempt > 1) {
                    log.info("DB に接続できました（試行 {} 回目）", attempt);
                }
                return;
            } catch (SQLException e) {
                last = e;
                if (System.currentTimeMillis() > deadline) {
                    break;
                }
                log.warn("DB に接続できません。再試行します（{}）", e.getMessage());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("DB に接続できませんでした", last);
    }

    public static synchronized void shutdown() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
        dataSource = null;
    }
}
