package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.Formats;
import com.example.appmgmt.common.Validation;
import com.example.appmgmt.domain.ApprovalRoute;
import com.example.appmgmt.domain.ApprovalRouteStep;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.CompanyDiv;
import com.example.appmgmt.domain.Employee;
import com.example.appmgmt.domain.LoginUser;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** SC11 社員マスタ、SC12 会社区分マスタ、SC13 承認ルートマスタ（F15）。 */
public class MasterServlet extends BaseServlet {

    @Override
    protected String fallbackPath(HttpServletRequest req) {
        List<String> parts = pathParts(req);
        return parts.isEmpty() ? "/emp/master/employees" : "/emp/master/" + parts.get(0);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        requireUser(req);
        List<String> parts = pathParts(req);
        String page = parts.isEmpty() ? "employees" : parts.get(0);
        String sub = parts.size() > 1 ? parts.get(1) : "";
        switch (page) {
            case "employees":
                if (sub.isEmpty()) {
                    req.setAttribute("employees", services().getMasterService().employees());
                    req.setAttribute("companyDivs", services().getMasterService().companyDivs());
                    render(req, res, "emp/master/employees.jsp");
                } else {
                    Employee e = "new".equals(sub) ? new Employee() : services().getMasterService().employee(parseId(sub) == null ? 0 : parseId(sub)).orElse(null);
                    if (e == null) {
                        res.sendError(404);
                        return;
                    }
                    if ("new".equals(sub)) {
                        e.setValidFlg(Codes.FLG_ON);
                        e.setRoleCd(Codes.ROLE_OWNER);
                    }
                    req.setAttribute("e", e);
                    req.setAttribute("companyDivs", services().getMasterService().companyDivs());
                    req.setAttribute("errors", java.util.Map.of());
                    render(req, res, "emp/master/employee_form.jsp");
                }
                return;
            case "company-divs":
                req.setAttribute("companyDivs", services().getMasterService().companyDivs());
                req.setAttribute("errors", java.util.Map.of());
                render(req, res, "emp/master/company_divs.jsp");
                return;
            case "approval-routes":
                if (sub.isEmpty()) {
                    req.setAttribute("routes", services().getMasterService().routes());
                    render(req, res, "emp/master/routes.jsp");
                } else {
                    ApprovalRoute r = "new".equals(sub) ? new ApprovalRoute() : services().getMasterService().route(parseId(sub) == null ? 0 : parseId(sub)).orElse(null);
                    if (r == null) {
                        res.sendError(404);
                        return;
                    }
                    if ("new".equals(sub)) {
                        r.setCompanyDiv(Codes.COMPANY_DIV_A);
                        r.setApprovalType(Codes.APPROVAL_PRIMARY);
                        r.setValidFrom(LocalDate.now());
                    }
                    showRouteForm(req, res, r, null);
                }
                return;
            default:
                res.sendError(404);
        }
    }

    private void showRouteForm(HttpServletRequest req, HttpServletResponse res, ApprovalRoute r, Validation v) throws ServletException, IOException {
        req.setAttribute("r", r);
        req.setAttribute("companyDivs", services().getMasterService().companyDivs());
        List<Employee> all = new ArrayList<>();
        for (CompanyDiv d : services().getMasterService().companyDivs()) {
            all.addAll(services().getMasterService().approverCandidates(d.getCompanyDiv()));
        }
        req.setAttribute("candidates", all);
        req.setAttribute("errors", v == null ? java.util.Map.of() : v.getErrors());
        render(req, res, "emp/master/route_form.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = requireUser(req);
        List<String> parts = pathParts(req);
        String page = parts.isEmpty() ? "" : parts.get(0);
        String sub = parts.size() > 1 ? parts.get(1) : "";
        switch (page) {
            case "employees": postEmployee(req, res, user, sub); return;
            case "company-divs": postCompanyDivs(req, res); return;
            case "approval-routes": postRoute(req, res, sub); return;
            default: res.sendError(404);
        }
    }

    private void postEmployee(HttpServletRequest req, HttpServletResponse res, LoginUser user, String sub) throws ServletException, IOException {
        if ("toggle".equals(sub)) {
            Long id = longParam(req, "employeeId");
            services().getMasterService().toggleEmployeeValid(id == null ? 0 : id, rowVersion(req), user.getEmployeeId());
            flashMessage(req, "success", "I001");
            redirect(req, res, "/emp/master/employees");
            return;
        }
        Employee e = new Employee();
        Long id = longParam(req, "employeeId");
        e.setEmployeeId(id == null ? 0 : id);
        e.setEmployeeNo(param(req, "employeeNo"));
        e.setEmployeeName(param(req, "employeeName"));
        e.setCompanyDiv(param(req, "companyDiv"));
        e.setDeptCd(param(req, "deptCd"));
        e.setRoleCd(param(req, "roleCd"));
        e.setMailAddress(param(req, "mailAddress"));
        e.setValidFlg("1".equals(param(req, "validFlg")) ? Codes.FLG_ON : Codes.FLG_OFF);
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        Validation v = new Validation();
        if (e.getEmployeeNo().isEmpty()) {
            v.reject("employeeNo", "E001", "社員番号");
        } else if (e.getEmployeeNo().length() > 10) {
            v.reject("employeeNo", "E002", "社員番号", 10);
        } else if (!Validation.isAlnum(e.getEmployeeNo())) {
            v.reject("employeeNo", "E003", "社員番号");
        }
        if (e.getEmployeeName().isEmpty()) {
            v.reject("employeeName", "E001", "氏名");
        } else if (e.getEmployeeName().length() > 50) {
            v.reject("employeeName", "E002", "氏名", 50);
        }
        if (e.getEmployeeId() == 0 && password.isEmpty()) {
            v.reject("password", "E001", "パスワード");
        } else if (password.length() > 64) {
            v.reject("password", "E002", "パスワード", 64);
        }
        if (e.getCompanyDiv().isEmpty()) {
            v.reject("companyDiv", "E001", "会社区分");
        }
        if (e.getDeptCd().isEmpty()) {
            v.reject("deptCd", "E001", "部署コード");
        } else if (e.getDeptCd().length() > 10 || !Validation.isAlnum(e.getDeptCd())) {
            v.reject("deptCd", "E003", "部署コード");
        }
        if (!java.util.Set.of(Codes.ROLE_OWNER, Codes.ROLE_APPROVER, Codes.ROLE_ADMIN).contains(e.getRoleCd())) {
            v.reject("roleCd", "E001", "権限");
        }
        if (e.getMailAddress().isEmpty()) {
            v.reject("mailAddress", "E001", "メールアドレス");
        } else if (e.getMailAddress().length() > 254 || !e.getMailAddress().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            v.reject("mailAddress", "E003", "メールアドレス");
        }
        if (v.hasErrors()) {
            req.setAttribute("e", e);
            req.setAttribute("companyDivs", services().getMasterService().companyDivs());
            req.setAttribute("errors", v.getErrors());
            render(req, res, "emp/master/employee_form.jsp");
            return;
        }
        try {
            services().getMasterService().saveEmployee(e, password, rowVersion(req));
        } catch (BusinessException ex) {
            if ("E006".equals(ex.getMessageId())) {
                v.reject("employeeNo", "E006", "社員番号");
                req.setAttribute("e", e);
                req.setAttribute("companyDivs", services().getMasterService().companyDivs());
                req.setAttribute("errors", v.getErrors());
                render(req, res, "emp/master/employee_form.jsp");
                return;
            }
            throw ex;
        }
        flashMessage(req, "success", "I001");
        redirect(req, res, "/emp/master/employees");
    }

    private void postCompanyDivs(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        List<CompanyDiv> divs = new ArrayList<>();
        Validation v = new Validation();
        String[] codes = req.getParameterValues("companyDiv");
        if (codes != null) {
            for (String cd : codes) {
                CompanyDiv d = new CompanyDiv();
                d.setCompanyDiv(cd);
                d.setCompanyDivName(param(req, "companyDivName_" + cd));
                d.setPreCheckFlg("1".equals(param(req, "preCheckFlg_" + cd)) ? Codes.FLG_ON : Codes.FLG_OFF);
                d.setRowVersion(intParam(req, "rowVersion_" + cd, -1));
                String limit = param(req, "amountRatioLimit_" + cd);
                if (d.getCompanyDivName().isEmpty()) {
                    v.reject("companyDivName_" + cd, "E001", "会社区分名");
                } else if (d.getCompanyDivName().length() > 50) {
                    v.reject("companyDivName_" + cd, "E002", "会社区分名", 50);
                }
                if (limit.isEmpty() || !limit.matches("[0-9]\\.[0-9]{1,2}|[0-9]")) {
                    v.reject("amountRatioLimit_" + cd, "E003", "金額倍率しきい値");
                } else {
                    BigDecimal b = new BigDecimal(limit);
                    if (b.compareTo(new BigDecimal("1.00")) < 0) {
                        v.reject("amountRatioLimit_" + cd, "E005", "金額倍率しきい値", "1.00");
                    } else if (b.compareTo(new BigDecimal("9.99")) > 0) {
                        v.reject("amountRatioLimit_" + cd, "E010", "金額倍率しきい値", "9.99");
                    }
                    d.setAmountRatioLimit(b.setScale(2));
                }
                divs.add(d);
            }
        }
        if (v.hasErrors()) {
            req.setAttribute("companyDivs", divs);
            req.setAttribute("errors", v.getErrors());
            render(req, res, "emp/master/company_divs.jsp");
            return;
        }
        services().getMasterService().saveCompanyDivs(divs);
        flashMessage(req, "success", "I001");
        redirect(req, res, "/emp/master/company-divs");
    }

    private void postRoute(HttpServletRequest req, HttpServletResponse res, String sub) throws ServletException, IOException {
        if ("delete".equals(sub)) {
            Long id = longParam(req, "routeId");
            if (id != null) {
                services().getMasterService().deleteRoute(id);
            }
            flashMessage(req, "success", "I001");
            redirect(req, res, "/emp/master/approval-routes");
            return;
        }
        ApprovalRoute r = new ApprovalRoute();
        Long id = longParam(req, "routeId");
        r.setRouteId(id == null ? 0 : id);
        r.setCompanyDiv(param(req, "companyDiv"));
        r.setDeptCd(param(req, "deptCd"));
        r.setApprovalType(param(req, "approvalType"));
        r.setRouteName(param(req, "routeName"));
        Validation v = new Validation();
        LocalDate from = Formats.parseDate(param(req, "validFrom"));
        LocalDate to = Formats.parseDate(param(req, "validTo"));
        if (param(req, "validFrom").isEmpty()) {
            v.reject("validFrom", "E001", "適用開始日");
        } else if (from == null) {
            v.reject("validFrom", "E003", "適用開始日");
        }
        if (!param(req, "validTo").isEmpty() && to == null) {
            v.reject("validTo", "E003", "適用終了日");
        } else if (from != null && to != null && to.isBefore(from)) {
            v.reject("validTo", "E007", "適用終了日", "適用開始日");
        }
        r.setValidFrom(from);
        r.setValidTo(to);
        if (r.getDeptCd().isEmpty()) {
            v.reject("deptCd", "E001", "部署コード");
        } else if (r.getDeptCd().length() > 10 || !Validation.isAlnum(r.getDeptCd())) {
            v.reject("deptCd", "E003", "部署コード");
        }
        if (r.getRouteName().isEmpty()) {
            v.reject("routeName", "E001", "ルート名");
        } else if (r.getRouteName().length() > 50) {
            v.reject("routeName", "E002", "ルート名", 50);
        }
        if (!Codes.labels("APPROVAL_TYPE").containsKey(r.getApprovalType())) {
            v.reject("approvalType", "E001", "承認種別");
        }
        String[] ids = req.getParameterValues("approverId");
        if (ids != null) {
            int no = 0;
            for (String s : ids) {
                Long eid = parseId(s);
                if (eid == null) {
                    continue;
                }
                ApprovalRouteStep st = new ApprovalRouteStep();
                st.setStepNo(++no);
                st.setApproverEmployeeId(eid);
                r.getSteps().add(st);
            }
        }
        if (r.getSteps().isEmpty()) {
            v.reject("approverId", "E001", "承認者");
        }
        if (v.hasErrors()) {
            showRouteForm(req, res, r, v);
            return;
        }
        try {
            services().getMasterService().saveRoute(r, rowVersion(req));
        } catch (BusinessException ex) {
            if ("E108".equals(ex.getMessageId())) {
                v.reject("approverId", "E108");
                showRouteForm(req, res, r, v);
                return;
            }
            throw ex;
        }
        flashMessage(req, "success", "I001");
        redirect(req, res, "/emp/master/approval-routes");
    }
}
