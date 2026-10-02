package com.example.appmgmt.web.filter;

import com.example.appmgmt.common.Messages;
import com.example.appmgmt.web.servlet.BaseServlet;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** 更新系（POST）リクエストの CSRF トークン検証（hidden の _csrf とセッションのトークンを比較）。 */
public class CsrfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        if ("POST".equalsIgnoreCase(req.getMethod())) {
            HttpSession session = req.getSession(false);
            String expected = session == null ? null : (String) session.getAttribute(BaseServlet.SESSION_CSRF);
            String actual = req.getParameter("_csrf");
            if (expected == null || actual == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                req.setAttribute("errorMessage", Messages.get("E111"));
                req.getRequestDispatcher("/WEB-INF/views/common/forbidden.jsp").forward(req, res);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
