package com.example.appmgmt.infra.extapi;

import com.example.appmgmt.common.AppConfig;
import java.util.Map;

/** 審査担当部門システム API クライアント（IF01／IF03）。 */
public interface ExternalApiClient {

    class SendResult {
        private final String externalReceiptNo;
        private final String acceptedAt;

        public SendResult(String externalReceiptNo, String acceptedAt) {
            this.externalReceiptNo = externalReceiptNo;
            this.acceptedAt = acceptedAt;
        }

        public String getExternalReceiptNo() { return externalReceiptNo; }
        public String getAcceptedAt() { return acceptedAt; }
    }

    /** @param precheck true = IF01 事前確認依頼、false = IF03 審査依頼 */
    SendResult send(boolean precheck, Map<String, Object> payload) throws ExternalApiException;

    String describe();

    static ExternalApiClient create(AppConfig config) {
        return "http".equalsIgnoreCase(config.getString("extapi.mode", "mock")) ? new HttpExternalApiClient(config) : new MockExternalApiClient();
    }
}
