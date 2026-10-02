package com.example.appmgmt.infra.extapi;

import com.example.appmgmt.common.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** REST／JSON over HTTPS による実送信（12. 2 章・3 章）。 */
public class HttpExternalApiClient implements ExternalApiClient {

    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String precheckPath;
    private final String reviewPath;
    private final String apiKey;
    private final Duration readTimeout;

    public HttpExternalApiClient(AppConfig config) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(config.getInt("extapi.connect-timeout-ms", 5000))).build();
        this.baseUrl = config.getString("extapi.base-url");
        this.precheckPath = config.getString("extapi.precheck-path", "/precheck-requests");
        this.reviewPath = config.getString("extapi.review-path", "/review-requests");
        this.apiKey = config.getString("extapi.api-key");
        this.readTimeout = Duration.ofMillis(config.getInt("extapi.read-timeout-ms", 30000));
    }

    @Override
    public SendResult send(boolean precheck, Map<String, Object> payload) throws ExternalApiException {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (IOException e) {
            throw new ExternalApiException(ExternalApiException.Kind.PERMANENT, "電文の生成に失敗しました", e);
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + (precheck ? precheckPath : reviewPath)))
                .timeout(readTimeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header("X-API-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res;
        try {
            res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new ExternalApiException(ExternalApiException.Kind.CONNECTION, "タイムアウト: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ExternalApiException(ExternalApiException.Kind.CONNECTION, "接続エラー: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException(ExternalApiException.Kind.CONNECTION, "中断されました", e);
        }
        int st = res.statusCode();
        String body = res.body() == null ? "" : res.body();
        String head = body.length() > 300 ? body.substring(0, 300) : body;
        if (st >= 200 && st < 300) {
            try {
                JsonNode n = mapper.readTree(body);
                String receipt = n.hasNonNull("externalReceiptNo") ? n.get("externalReceiptNo").asText() : null;
                if (receipt == null || receipt.isBlank() || receipt.length() > 30) {
                    throw new ExternalApiException(ExternalApiException.Kind.TEMPORARY, "HTTP " + st + " 応答に受付番号がありません: " + head, null);
                }
                return new SendResult(receipt, n.hasNonNull("acceptedAt") ? n.get("acceptedAt").asText() : null);
            } catch (IOException e) {
                throw new ExternalApiException(ExternalApiException.Kind.TEMPORARY, "HTTP " + st + " 応答本文を解析できません: " + head, e);
            }
        }
        if (st >= 400 && st < 500) {
            throw new ExternalApiException(ExternalApiException.Kind.PERMANENT, "HTTP " + st + ": " + head, null);
        }
        throw new ExternalApiException(ExternalApiException.Kind.TEMPORARY, "HTTP " + st + ": " + head, null);
    }

    @Override
    public String describe() {
        return "HTTP 送信（" + baseUrl + "）";
    }
}
