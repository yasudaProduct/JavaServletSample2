package com.example.appmgmt.web.filter;

import com.example.appmgmt.domain.ApplicantUser;
import com.example.appmgmt.web.servlet.BaseServlet;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** 申込者ポータル（/my/*）の認証 Filter。未ログインは申込者ログイン画面へ。 */
public class ApplicantAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.equals("/my/login") || path.equals("/my/logout")) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        ApplicantUser user = session == null ? null : (ApplicantUser) session.getAttribute(BaseServlet.SESSION_APPLICANT);
        if (user == null) {
            boolean timedOut = req.getRequestedSessionId() != null && !req.isRequestedSessionIdValid();
            res.sendRedirect(req.getContextPath() + "/my/login" + (timedOut ? "?timeout=1" : ""));
            return;
        }
        chain.doFilter(request, response);
    }
}
