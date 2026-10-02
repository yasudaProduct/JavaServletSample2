package com.example.appmgmt.infra.scheduler;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.service.Services;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** WAR 内スケジューラ（13. 2.1 節）。バッチごとに 1 スレッドで scheduleWithFixedDelay する。 */
public class BatchScheduler implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(BatchScheduler.class);

    private ScheduledExecutorService notificationExecutor;
    private ScheduledExecutorService externalExecutor;
    private int shutdownWait = 30;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AppConfig config = AppConfig.get();
        if (!config.getBoolean("batch.enabled", true)) {
            log.info("batch.enabled=false のためスケジューラを起動しません");
            return;
        }
        Services s = Services.get();
        int initial = config.getInt("batch.initial-delay-seconds", 30);
        shutdownWait = config.getInt("batch.shutdown-wait-seconds", 30);
        notificationExecutor = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "BT01-notification"));
        externalExecutor = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "BT02-external"));
        notificationExecutor.scheduleWithFixedDelay(safe(s.getNotificationSendJob()), initial, config.getInt("batch.notification.interval-seconds", 60), TimeUnit.SECONDS);
        externalExecutor.scheduleWithFixedDelay(safe(s.getExternalLinkSendJob()), initial, config.getInt("batch.external.interval-seconds", 60), TimeUnit.SECONDS);
        log.info("スケジューラを起動しました（初回 {} 秒後、BT01 {} 秒周期、BT02 {} 秒周期）", initial,
                config.getInt("batch.notification.interval-seconds", 60), config.getInt("batch.external.interval-seconds", 60));
    }

    private static Thread daemon(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }

    /** 例外を外に投げるとスケジュールが止まるため、すべて捕捉してログに出す。 */
    private static Runnable safe(Runnable job) {
        return () -> {
            try {
                job.run();
            } catch (Throwable t) {
                log.error("バッチの実行で想定外のエラー", t);
            }
        };
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        shutdown(notificationExecutor, "BT01");
        shutdown(externalExecutor, "BT02");
    }

    private void shutdown(ScheduledExecutorService ex, String name) {
        if (ex == null) {
            return;
        }
        ex.shutdown();
        try {
            if (!ex.awaitTermination(shutdownWait, TimeUnit.SECONDS)) {
                log.warn("{} の停止を待ちきれないため中断します", name);
                ex.shutdownNow();
            }
        } catch (InterruptedException e) {
            ex.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
