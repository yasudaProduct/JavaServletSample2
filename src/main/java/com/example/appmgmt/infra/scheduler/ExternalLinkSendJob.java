package com.example.appmgmt.infra.scheduler;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.AuditContext;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ExternalLinkDao;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ExternalLink;
import com.example.appmgmt.infra.extapi.ExternalApiClient;
import com.example.appmgmt.infra.extapi.ExternalApiException;
import com.example.appmgmt.service.external.ExternalRequestBuilder;
import com.example.appmgmt.service.notification.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** BT02 外部連携送信バッチ（13. 5 章）。 */
public class ExternalLinkSendJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ExternalLinkSendJob.class);
    public static final String BATCH_ID = "BT02";
    public static final String VERSION_MISMATCH = "版が更新されたため送信中止";

    private final ExternalLinkDao externalLinkDao;
    private final ApplicationDao applicationDao;
    private final ExternalRequestBuilder builder;
    private final ExternalApiClient client;
    private final NotificationService notificationService;
    private final int maxRows;
    private final int retryLimit;
    private final int abortAfter;

    public ExternalLinkSendJob(ExternalLinkDao externalLinkDao, ApplicationDao applicationDao, ExternalRequestBuilder builder, ExternalApiClient client,
                               NotificationService notificationService, AppConfig config) {
        this.externalLinkDao = externalLinkDao;
        this.applicationDao = applicationDao;
        this.builder = builder;
        this.client = client;
        this.notificationService = notificationService;
        this.maxRows = config.getInt("batch.external.max-rows", 100);
        this.retryLimit = config.getInt("batch.external.retry-limit", 3);
        this.abortAfter = config.getInt("batch.external.abort-after-consecutive-failures", 5);
    }

    @Override
    public synchronized void run() {
        MDC.put("batchId", "[" + BATCH_ID + "] ");
        AuditContext.set(BATCH_ID);
        long start = System.currentTimeMillis();
        int ok = 0;
        int retry = 0;
        int fixed = 0;
        int consecutiveConn = 0;
        try {
            List<ExternalLink> rows = Tx.execute(conn -> externalLinkDao.findPending(conn, maxRows));
            if (rows.isEmpty()) {
                log.debug("未送信の外部連携はありません");
                return;
            }
            log.info("開始 取得件数={} 送信先={}", rows.size(), client.describe());
            for (ExternalLink link : rows) {
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("停止要求により残りの行を次回へ回します");
                    break;
                }
                String outcome;
                try {
                    outcome = processOne(link);
                } catch (RuntimeException e) {
                    log.error("外部連携 ID={} の処理でエラー。次の行へ進みます", link.getExternalLinkId(), e);
                    continue;
                }
                switch (outcome) {
                    case "ok": ok++; consecutiveConn = 0; break;
                    case "retry": retry++; break;
                    case "retry-conn": retry++; consecutiveConn++; break;
                    case "fixed": fixed++; consecutiveConn = 0; break;
                    case "fixed-conn": fixed++; consecutiveConn++; break;
                    default: break;
                }
                if (consecutiveConn >= abortAfter) {
                    log.error("接続エラー・タイムアウトが連続 {} 回に達したため実行を打ち切ります。残りの行は次回に回します", consecutiveConn);
                    break;
                }
            }
            log.info("終了 成功={} 再送待ち={} 送信エラー確定={} 所要時間={}ms", ok, retry, fixed, System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("BT02 の実行でエラー", e);
        } finally {
            AuditContext.clear();
            MDC.remove("batchId");
        }
    }

    /** 1 行の送信。結果の区分（ok／retry／retry-conn／fixed／fixed-conn／skip）を返す。 */
    private String processOne(ExternalLink link) {
        // 版の一致確認と電文の組み立て（読み取り）
        Map<String, Object> payload;
        Application app;
        try {
            Object[] r = Tx.execute(conn -> {
                Application a = applicationDao.findById(conn, link.getApplicationId()).orElseThrow();
                if (a.getCurrentVersionNo() != link.getVersionNo()) {
                    return new Object[] {a, null};
                }
                return new Object[] {a, builder.build(conn, link)};
            });
            app = (Application) r[0];
            payload = (Map<String, Object>) r[1];
        } catch (RuntimeException e) {
            throw e;
        }
        if (payload == null) {
            Tx.execute(conn -> externalLinkDao.markFailed(conn, link.getExternalLinkId(), link.getRowVersion(), true, VERSION_MISMATCH, false));
            log.warn("版の不一致により送信中止 外部連携ID={} 申込ID={} 連携版={} 現行版={}", link.getExternalLinkId(), link.getApplicationId(), link.getVersionNo(), app.getCurrentVersionNo());
            return "skip";
        }
        try {
            ExternalApiClient.SendResult res = client.send(link.isPrecheck(), payload);
            String receipt = res.getExternalReceiptNo();
            Boolean dup = Tx.execute(conn -> externalLinkDao.existsReceiptNo(conn, receipt, link.getExternalLinkId()));
            if (dup) {
                return fail(link, true, "受付番号が重複: " + receipt, true, false);
            }
            int updated = Tx.execute(conn -> externalLinkDao.markSent(conn, link.getExternalLinkId(), link.getRowVersion(), receipt, LocalDateTime.now().withNano(0)));
            if (updated == 0) {
                log.warn("外部連携 ID={} は他で更新済みのため結果を反映しません", link.getExternalLinkId());
                return "skip";
            }
            log.info("送信成功 外部連携ID={} 申込ID={} 種別={} 受付番号={}", link.getExternalLinkId(), link.getApplicationId(), link.getLinkType(), receipt);
            return "ok";
        } catch (ExternalApiException e) {
            boolean conn = e.getKind() == ExternalApiException.Kind.CONNECTION;
            boolean fix = e.getKind() == ExternalApiException.Kind.PERMANENT || link.getRetryCount() + 1 >= retryLimit;
            return fail(link, fix, e.getMessage(), true, conn);
        }
    }

    private String fail(ExternalLink link, boolean fixed, String message, boolean countRetry, boolean connectionError) {
        Tx.executeVoid(conn -> {
            int updated = externalLinkDao.markFailed(conn, link.getExternalLinkId(), link.getRowVersion(), fixed, message, countRetry);
            if (updated == 1 && fixed) {
                Application app = applicationDao.findById(conn, link.getApplicationId()).orElseThrow();
                link.setErrorMessage(message);
                notificationService.registerSendError(conn, app, link);
            }
        });
        if (fixed) {
            log.error("送信エラー確定 外部連携ID={} 再送回数={} {}", link.getExternalLinkId(), link.getRetryCount() + 1, message);
            return connectionError ? "fixed-conn" : "fixed";
        }
        log.warn("送信失敗（再送待ち） 外部連携ID={} 再送回数={} {}", link.getExternalLinkId(), link.getRetryCount() + 1, message);
        return connectionError ? "retry-conn" : "retry";
    }
}
