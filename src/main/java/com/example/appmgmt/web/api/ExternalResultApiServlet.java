package com.example.appmgmt.web.api;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.AuditContext;
import com.example.appmgmt.service.Services;
import com.example.appmgmt.service.external.ReceiveResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** IF02 事前確認結果受信（/api/external/precheck-result）、IF04 審査結果受信（/api/external/review-result）。 */
public class ExternalResultApiServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ExternalResultApiServlet.class);
    private static final int MAX_BODY = 64 * 1024;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(405);
            write(res, ReceiveResult.error(405, "E405", null, null, "POST のみ受け付けます。"));
            return;
        }
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        boolean precheck;
        if ("/precheck-result".equals(path)) {
            precheck = true;
        } else if ("/review-result".equals(path)) {
            precheck = false;
        } else {
            write(res, ReceiveResult.error(404, "E404", null, null, "エンドポイントがありません。"));
            return;
        }
        String ct = req.getContentType();
        if (ct == null || !ct.toLowerCase().contains("application/json")) {
            write(res, ReceiveResult.error(400, "E400", null, null, "Content-Type は application/json を指定してください。"));
            return;
        }
        byte[] body = req.getInputStream().readAllBytes();
        if (body.length > MAX_BODY) {
            write(res, ReceiveResult.error(400, "E400", null, null, "本文が大きすぎます。"));
            return;
        }
        JsonNode n;
        try {
            n = mapper.readTree(new String(body, StandardCharsets.UTF_8));
        } catch (IOException e) {
            write(res, ReceiveResult.error(400, "E400", null, null, "JSON を解析できません。"));
            return;
        }
        String receiptNo = n.hasNonNull("externalReceiptNo") ? n.get("externalReceiptNo").asText() : null;
        Long requestId = n.hasNonNull("requestId") && n.get("requestId").canConvertToLong() ? n.get("requestId").asLong() : null;
        String result = n.hasNonNull("result") ? n.get("result").asText() : null;
        String reason = n.hasNonNull("reason") ? n.get("reason").asText() : null;
        AuditContext.set(AppConfig.get().getString("app.external-system-id", "EXT01"));
        long start = System.currentTimeMillis();
        try {
            ReceiveResult r = Services.get().getExternalResultService().receive(precheck, receiptNo, requestId, result, reason);
            log.info("IF{} 受信 receiptNo={} requestId={} result={} -> {} {} applicationNo={} newStatus={} {}ms ip={}", precheck ? "02" : "04", receiptNo, requestId, result,
                    r.getHttpStatus(), r.getErrorCode(), r.getApplicationNo(), r.getNewStatusCd(), System.currentTimeMillis() - start, req.getRemoteAddr());
            write(res, r);
        } catch (RuntimeException e) {
            log.error("IF 受信でシステムエラー receiptNo={}", receiptNo, e);
            write(res, ReceiveResult.error(500, "E500", null, null, "システムエラーが発生しました。"));
        } finally {
            AuditContext.clear();
        }
    }

    private void write(HttpServletResponse res, ReceiveResult r) throws IOException {
        res.setStatus(r.getHttpStatus());
        res.setContentType("application/json; charset=UTF-8");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", r.getStatus());
        m.put("errorCode", r.getErrorCode());
        m.put("applicationNo", r.getApplicationNo());
        m.put("newStatusCd", r.getNewStatusCd());
        m.put("message", r.getMessage());
        res.getWriter().write(mapper.writeValueAsString(m));
    }
}
