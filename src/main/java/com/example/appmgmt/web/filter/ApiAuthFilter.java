package com.example.appmgmt.web.filter;

import com.example.appmgmt.common.AppConfig;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 外部 IF 受信（/api/external/*）の API 認証：X-API-Key と接続元 IP 制限（12. 2.2 節）。 */
public class ApiAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiAuthFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        AppConfig config = AppConfig.get();
        String expectedKey = config.getString("extapi.inbound.api-key");
        String allowedIps = config.getString("extapi.inbound.allowed-ips");
        if (!expectedKey.isEmpty()) {
            String key = req.getHeader("X-API-Key");
            if (key == null || !MessageDigest.isEqual(expectedKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
                unauthorized(res);
                return;
            }
        }
        if (!allowedIps.isBlank()) {
            List<String> allowed = Arrays.asList(allowedIps.split("\\s*,\\s*"));
            if (!allowed.contains(req.getRemoteAddr())) {
                log.warn("許可されていない接続元からの IF 受信: {}", req.getRemoteAddr());
                unauthorized(res);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static void unauthorized(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json; charset=UTF-8");
        res.getWriter().write("{\"status\":\"ERROR\",\"errorCode\":\"E401\",\"applicationNo\":null,\"newStatusCd\":null,\"message\":\"認証エラーです。\"}");
    }
}
