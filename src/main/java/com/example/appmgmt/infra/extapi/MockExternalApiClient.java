package com.example.appmgmt.infra.extapi;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 開発用モック。送信せずに受付番号を採番して返す。
 * 結果は開発支援画面（審査担当部門システムモック）から IF02／IF04 と同じ処理で返す。
 */
public class MockExternalApiClient implements ExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(MockExternalApiClient.class);

    @Override
    public SendResult send(boolean precheck, Map<String, Object> payload) {
        String receipt = "MOCK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + payload.get("requestId");
        log.info("[extapi.mode=mock] {} 依頼を受け付けたことにします requestId={} applicationNo={} receipt={}", precheck ? "IF01 事前確認" : "IF03 審査", payload.get("requestId"), payload.get("applicationNo"), receipt);
        return new SendResult(receipt, OffsetDateTime.now().withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @Override
    public String describe() {
        return "モック（送信せず受付番号を採番）";
    }
}
