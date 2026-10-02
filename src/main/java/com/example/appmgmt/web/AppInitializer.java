package com.example.appmgmt.web;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.DataSourceProvider;
import com.example.appmgmt.service.Services;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Web アプリ起動時の初期化：設定、DataSource、Flyway マイグレーション、サービスの組み立て。 */
public class AppInitializer implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AppConfig config = AppConfig.get();
        log.info("申込管理システムを初期化します db.url={}", config.getString("db.url").replaceAll("password=[^;]*", "password=***"));
        DataSourceProvider.init(config);
        migrate(config);
        Services.init(config);
        sce.getServletContext().setAttribute("devTools", config.getBoolean("app.dev-tools.enabled", false));
        sce.getServletContext().setAttribute("appVersion", "0.1.0");
        log.info("初期化が完了しました");
    }

    /** Flyway でスキーマと初期データを適用する。共通のほか、DBMS 別（sqlserver／h2）のスクリプトを読む。 */
    public static void migrate(AppConfig config) {
        String url = config.getString("db.url");
        List<String> locations = new ArrayList<>();
        locations.add("classpath:db/migration");
        if (url.startsWith("jdbc:sqlserver")) {
            locations.add("classpath:db/vendor/sqlserver");
        } else if (url.startsWith("jdbc:h2")) {
            locations.add("classpath:db/vendor/h2");
        }
        Flyway flyway = Flyway.configure()
                .dataSource(DataSourceProvider.get())
                .locations(locations.toArray(new String[0]))
                .placeholderReplacement(false)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataSourceProvider.shutdown();
    }
}
