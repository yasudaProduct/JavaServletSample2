package com.example.appmgmt.local;

import java.io.File;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * ローカル起動（Docker なしの動作確認用）：組込み Tomcat 9 ＋ H2 インメモリ DB（SQL Server 互換モード）。
 * 使い方：mvn -Plocal（または mvn test-compile exec:java）。http://localhost:8080/emp/login
 * システムプロパティ -Ddb.url などで設定を上書きできる。DB は JVM 終了で消える。
 */
public final class LocalServer {

    private LocalServer() {
    }

    public static void main(String[] args) throws Exception {
        setDefault("db.url", "jdbc:h2:mem:appmgmt;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=TRUE");
        setDefault("db.user", "sa");
        setDefault("db.password", "");
        setDefault("mail.mode", "log");
        setDefault("extapi.mode", "mock");
        setDefault("batch.initial-delay-seconds", "5");
        setDefault("batch.notification.interval-seconds", "5");
        setDefault("batch.external.interval-seconds", "5");
        setDefault("app.dev-tools.enabled", "true");
        int port = Integer.parseInt(System.getProperty("local.port", "8080"));
        setDefault("app.applicant-base-url", "http://localhost:" + port);
        setDefault("app.employee-base-url", "http://localhost:" + port);

        // コンパイル済み JSP のキャッシュを捨てる（web.xml や断片の変更を確実に反映するため）
        deleteRecursively(new File("target/tomcat-local/work"));
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(new File("target/tomcat-local").getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector().setURIEncoding("UTF-8");
        Context ctx = tomcat.addWebapp("", new File("src/main/webapp").getAbsolutePath());
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", new File("target/classes").getAbsolutePath(), "/"));
        ctx.setResources(resources);
        tomcat.start();
        System.out.println("申込管理システム（ローカル起動）: http://localhost:" + port + "/emp/login");
        tomcat.getServer().await();
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursively(c);
            }
        }
        if (!f.delete()) {
            System.err.println("削除できません: " + f);
        }
    }

    private static void setDefault(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
