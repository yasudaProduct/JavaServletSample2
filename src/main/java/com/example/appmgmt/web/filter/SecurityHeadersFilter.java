package com.example.appmgmt.web.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** セキュリティ関連の HTTP ヘッダ（14. 共通仕様 10 章）。 */
public class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse r = (HttpServletResponse) res;
        r.setHeader("X-Frame-Options", "DENY");
        r.setHeader("X-Content-Type-Options", "nosniff");
        r.setHeader("Referrer-Policy", "same-origin");
        String uri = ((HttpServletRequest) req).getRequestURI();
        if (!uri.contains("/static/")) {
            r.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(req, res);
    }
}
