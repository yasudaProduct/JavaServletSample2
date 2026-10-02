package com.example.appmgmt.infra.scheduler;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.AuditContext;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.NotificationDao;
import com.example.appmgmt.domain.Notification;
import com.example.appmgmt.infra.mail.MailException;
import com.example.appmgmt.infra.mail.MailSender;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** BT01 通知送信バッチ（13. 4 章）。 */
public class NotificationSendJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(NotificationSendJob.class);
    public static final String BATCH_ID = "BT01";

    private final NotificationDao notificationDao;
    private final MailSender mailSender;
    private final int maxRows;
    private final int retryLimit;

    public NotificationSendJob(NotificationDao notificationDao, MailSender mailSender, AppConfig config) {
        this.notificationDao = notificationDao;
        this.mailSender = mailSender;
        this.maxRows = config.getInt("batch.notification.max-rows", 100);
        this.retryLimit = config.getInt("batch.notification.retry-limit", 3);
    }

    @Override
    public synchronized void run() {
        MDC.put("batchId", "[" + BATCH_ID + "] ");
        AuditContext.set(BATCH_ID);
        long start = System.currentTimeMillis();
        int ok = 0;
        int retry = 0;
        int fixed = 0;
        try {
            List<Notification> rows = Tx.execute(conn -> notificationDao.findPending(conn, maxRows));
            if (rows.isEmpty()) {
                log.debug("未送信の通知はありません");
                return;
            }
            log.info("開始 取得件数={}", rows.size());
            try (MailSender.MailSession session = mailSender.open()) {
                for (Notification n : rows) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.warn("停止要求により残りの行を次回へ回します");
                        break;
                    }
                    try {
                        session.send(n.getToAddress(), n.getSubject(), n.getBody());
                        int updated = Tx.execute(conn -> notificationDao.markSent(conn, n.getNotificationId(), n.getRowVersion(), LocalDateTime.now().withNano(0)));
                        if (updated == 0) {
                            log.warn("通知 ID={} は他で更新済みのため結果を反映しません", n.getNotificationId());
                        } else {
                            ok++;
                            log.info("送信成功 通知ID={} 申込ID={} 種別={}", n.getNotificationId(), n.getApplicationId(), n.getNotificationType());
                        }
                    } catch (MailException e) {
                        if (e.getKind() == MailException.Kind.INFRA) {
                            log.error("メール基盤の障害により実行を打ち切ります: {}", e.getMessage(), e);
                            break;
                        }
                        boolean fix = e.getKind() == MailException.Kind.PERMANENT || n.getRetryCount() + 1 >= retryLimit;
                        Tx.execute(conn -> notificationDao.markFailed(conn, n.getNotificationId(), n.getRowVersion(), fix, e.getMessage()));
                        if (fix) {
                            fixed++;
                            log.error("送信エラー確定 通知ID={} 再送回数={} {}", n.getNotificationId(), n.getRetryCount() + 1, e.getMessage());
                        } else {
                            retry++;
                            log.warn("送信失敗（再送待ち） 通知ID={} 再送回数={} {}", n.getNotificationId(), n.getRetryCount() + 1, e.getMessage());
                        }
                    } catch (RuntimeException e) {
                        log.error("通知 ID={} の処理でエラー。次の行へ進みます", n.getNotificationId(), e);
                    }
                }
            } catch (MailException e) {
                log.error("SMTP サーバに接続できないため実行を打ち切ります: {}", e.getMessage());
            }
            log.info("終了 成功={} 再送待ち={} 送信エラー確定={} 所要時間={}ms", ok, retry, fixed, System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("BT01 の実行でエラー", e);
        } finally {
            AuditContext.clear();
            MDC.remove("batchId");
        }
    }
}
