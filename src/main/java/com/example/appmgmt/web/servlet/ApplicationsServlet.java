package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.Formats;
import com.example.appmgmt.common.Messages;
import com.example.appmgmt.common.TransitionNotAllowedException;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.common.Validation;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.service.application.ApplicationDetail;
import com.example.appmgmt.service.application.ApplicationQueryService;
import com.example.appmgmt.service.approval.ApprovalFlowView;
import com.example.appmgmt.service.approval.ApprovalService;
import com.example.appmgmt.service.transition.StatusTransitionService;
import com.example.appmgmt.service.transition.TransitionResult;
import com.example.appmgmt.web.form.ApplicationForm;
import com.example.appmgmt.web.view.MenuTile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 申込受付会社向けの申込画面（/emp/applications/*）。
 * SC02 一覧、SC03 詳細、SC04 入力、SC05 内容確認、SC06 承認フロー、SC07 申込修正、SC08 契約変更入力、SC09 契約変更内容確認。
 */
public class ApplicationsServlet extends BaseServlet {

    @Override
    protected String fallbackPath(HttpServletRequest req) {
        List<String> parts = pathParts(req);
        if (!parts.isEmpty() && parseId(parts.get(0)) != null) {
            return "/emp/applications/" + parts.get(0) + "/menu";
        }
        return "/emp/applications";
    }

    private static String menuPath(long id) {
        return "/emp/applications/" + id + "/menu";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = requireUser(req);
        List<String> parts = pathParts(req);
        if (parts.isEmpty()) {
            list(req, res, user);
            return;
        }
        if (parts.size() == 1 && "new".equals(parts.get(0))) {
            showInput(req, res, user, null, "new", ApplicationForm.bind(req), null);
            return;
        }
        Long id = parseId(parts.get(0));
        if (id == null) {
            res.sendError(404);
            return;
        }
        String sub = parts.size() > 1 ? parts.get(1) : "";
        String sub2 = parts.size() > 2 ? parts.get(2) : "";
        switch (sub) {
            case "": detail(req, res, user, id); break;
            case "menu": menu(req, res, user, id); break;
            case "additional": showAdditional(req, res, user, id); break;
            case "edit": showEdit(req, res, user, id); break;
            case "confirm": showConfirm(req, res, user, id); break;
            case "approval": showApproval(req, res, user, id); break;
            case "revise": showRevise(req, res, user, id, null, null); break;
            case "change":
                if ("confirm".equals(sub2)) {
                    showChangeConfirm(req, res, user, id);
                } else {
                    showChangeInput(req, res, user, id, null, null);
                }
                break;
            default: res.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = requireUser(req);
        List<String> parts = pathParts(req);
        if (parts.size() == 1 && "new".equals(parts.get(0))) {
            postInput(req, res, user, null, null);
            return;
        }
        Long id = parts.isEmpty() ? null : parseId(parts.get(0));
        if (id == null) {
            res.sendError(404);
            return;
        }
        String sub = parts.size() > 1 ? parts.get(1) : "";
        String sub2 = parts.size() > 2 ? parts.get(2) : "";
        switch (sub) {
            case "additional": postInput(req, res, user, null, id); break;
            case "edit": postInput(req, res, user, id, null); break;
            case "confirm": postConfirm(req, res, user, id); break;
            case "approval": postApproval(req, res, user, id); break;
            case "revise": postRevise(req, res, user, id); break;
            case "change":
                if ("confirm".equals(sub2)) {
                    postChangeConfirm(req, res, user, id);
                } else {
                    postChangeInput(req, res, user, id);
                }
                break;
            default: postAction(req, res, user, id, sub);
        }
    }

    // ------------------------------------------------------------------ SC02

    private void list(HttpServletRequest req, HttpServletResponse res, LoginUser user) throws ServletException, IOException {
        ApplicationDao.Criteria c = new ApplicationDao.Criteria();
        boolean searched = req.getParameter("search") != null;
        c.applicationNo = param(req, "applicationNo");
        c.applicantKey = param(req, "applicantKey");
        String[] sts = req.getParameterValues("statusCd");
        if (sts != null) {
            c.statusCds = new ArrayList<>(Arrays.asList(sts));
        }
        String[] cats = req.getParameterValues("categoryL");
        String[] mids = req.getParameterValues("categoryM");
        c.ownerEmployeeId = longParam(req, "ownerEmployeeId");
        Validation v = new Validation();
        c.createdFrom = parseDateParam(req, "createdFrom", v);
        c.createdTo = parseDateParam(req, "createdTo", v);
        if (c.createdFrom != null && c.createdTo != null && c.createdTo.isBefore(c.createdFrom)) {
            v.reject("createdTo", "E007", "登録日 To", "登録日 From");
        }
        c.myTasksOnly = searched ? "1".equals(param(req, "myTasksOnly")) : !user.isAdmin();
        c.page = intParam(req, "page", 1);
        c.sort = param(req, "sort").isEmpty() ? "updatedAt" : param(req, "sort");
        c.desc = !"asc".equals(param(req, "order"));
        // 大分類・中分類の選択を個別ステータスに展開する
        List<com.example.appmgmt.domain.Status> statuses = services().getApplicationQueryService().allStatuses();
        if ((cats != null && cats.length > 0) || (mids != null && mids.length > 0)) {
            Set<String> catSet = cats == null ? Set.of() : Set.of(cats);
            Set<String> midSet = mids == null ? Set.of() : Set.of(mids);
            for (com.example.appmgmt.domain.Status s : statuses) {
                boolean catOk = catSet.isEmpty() || catSet.contains(s.getCategoryL());
                boolean midOk = midSet.isEmpty() || midSet.contains(s.getCategoryM());
                if (catOk && midOk && !c.statusCds.contains(s.getStatusCd())) {
                    c.statusCds.add(s.getStatusCd());
                }
            }
        }
        req.setAttribute("criteria", c);
        req.setAttribute("statuses", statuses);
        req.setAttribute("employees", services().getApplicationQueryService().validEmployees());
        req.setAttribute("errors", v.getErrors());
        req.setAttribute("selectedCategoryL", cats == null ? List.of() : Arrays.asList(cats));
        req.setAttribute("selectedCategoryM", mids == null ? List.of() : Arrays.asList(mids));
        if (v.hasErrors()) {
            ApplicationQueryService.SearchResult empty = new ApplicationQueryService.SearchResult();
            empty.rows = List.of();
            empty.page = 1;
            empty.pageSize = 20;
            req.setAttribute("result", empty);
        } else {
            req.setAttribute("result", services().getApplicationQueryService().search(c, user));
        }
        StringBuilder qs = new StringBuilder();
        for (java.util.Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            if ("page".equals(e.getKey())) {
                continue;
            }
            for (String val : e.getValue()) {
                qs.append(qs.length() == 0 ? "?" : "&").append(java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8)).append('=')
                        .append(java.net.URLEncoder.encode(val, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        req.setAttribute("queryString", qs.toString());
        render(req, res, "emp/applications/list.jsp");
    }

    private static java.time.LocalDate parseDateParam(HttpServletRequest req, String name, Validation v) {
        String s = param(req, name);
        if (s.isEmpty()) {
            return null;
        }
        java.time.LocalDate d = Formats.parseDate(s);
        if (d == null) {
            v.reject(name, "E003", "createdFrom".equals(name) ? "登録日 From" : "登録日 To");
        }
        return d;
    }

    // ------------------------------------------------------------------ SC03

    private void detail(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        req.setAttribute("d", d);
        render(req, res, "emp/applications/detail.jsp");
    }

    /** SC14 申込メニュー：申込に関する操作の起点。ステータス×権限で押せるタイルが変わる。 */
    private void menu(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        String base = "/emp/applications/" + id;
        List<MenuTile> tiles = new ArrayList<>();
        tiles.add(new MenuTile("detail", "申込確認", "申込の基本情報、申込内容、版履歴、承認状況、申込者同意、外部連携、ステータス履歴を確認します。").link(base).style("outline-primary"));
        // 入力・確認系
        if (d.has("input")) {
            tiles.add(new MenuTile("input", "申込入力", "入力中の申込内容を入力・一時保存して、確認へ進みます。").link(base + "/edit"));
        }
        if (d.has("inputChange")) {
            tiles.add(new MenuTile("inputChange", "契約変更入力", "契約変更の内容を入力・一時保存して、確認へ進みます。").link(base + "/change"));
        }
        if (d.has("confirm")) {
            tiles.add(new MenuTile("confirm", "申込内容確認", "入力内容を確認して確定（一次承認申請待ちへ）するか、修正に戻します。").link(base + "/confirm"));
        }
        if (d.has("confirmChange")) {
            tiles.add(new MenuTile("confirmChange", "契約変更内容確認", "変更前後を比較し、変更基準の判定を確認して確定します。").link(base + "/change/confirm"));
        }
        if (d.has("modify") || d.has("modifyChange")) {
            tiles.add(new MenuTile("modify", "修正", "入力中に戻して申込内容を修正します。").post(base + "/modify").style("outline-primary"));
        }
        if (d.has("revise")) {
            tiles.add(new MenuTile("revise", "修正（金額）", "同意後の金額項目を修正します。基準版との倍率で遷移先が決まります。").link(base + "/revise").style("outline-primary"));
        }
        if (d.has("fix")) {
            tiles.add(new MenuTile("fix", "修正対応", "審査担当部門の指摘に対して申込内容を修正し、事前確認を再依頼します。").link(base + "/revise").style("outline-primary"));
        }
        if (d.has("fullRevise")) {
            tiles.add(new MenuTile("fullRevise", "全体修正", "申込内容全体を修正し直すため入力中へ戻します。申込者の同意を取り直します。").post(base + "/fullRevise").style("outline-warning")
                    .confirm("全体修正を開始します。申込内容全体を修正し直し、申込者の同意を取り直します。よろしいですか？"));
        }
        // 承認フロー
        if (d.has("request")) {
            tiles.add(new MenuTile("approval", "承認フロー", "回付先を設定して承認を申請します。").link(base + "/approval").style("success"));
        } else if (d.has("approvalFlow")) {
            tiles.add(new MenuTile("approval", "承認フロー", "承認・差戻し・審査申請を行います。").link(base + "/approval").style("success"));
        } else if (d.has("approvalFlowView")) {
            tiles.add(new MenuTile("approval", "承認フロー", "進行中の承認申請を参照します。").link(base + "/approval").style("outline-success"));
        } else {
            tiles.add(new MenuTile("approval", "承認フロー", "回付先の設定・申請、承認・差戻し・審査申請を行います。").disabled("現在のステータスでは申請・承認の操作はありません。"));
        }
        // 契約変更
        if (d.has("startChange")) {
            tiles.add(new MenuTile("startChange", "契約変更", "審査完了版を複写した版を作り、契約変更の入力を始めます。").post(base + "/startChange").style("info")
                    .confirm("契約変更手続きを開始します。審査完了版を複写した新しい版を作成します。よろしいですか？"));
        } else {
            tiles.add(new MenuTile("startChange", "契約変更", "審査完了後に契約変更手続きを始めます。").disabled(StatusCd.isContractChange(d.getApplication().getStatusCd()) ? "契約変更の手続き中です。" : "審査完了（10701／20701）の申込だけ開始できます。"));
        }
        // 引戻し
        if (d.has("pullBack")) {
            tiles.add(new MenuTile("pullBack", "引戻し", "申込者確認中の申込を一次承認申請待ちへ戻します。確認用 URL は無効になります。").post(base + "/pullBack").style("warning")
                    .confirm("引戻しを行うと申込者の確認用 URL は無効になります。よろしいですか？"));
        } else {
            tiles.add(new MenuTile("pullBack", "引戻し", "申込者確認中の申込を一次承認申請待ちへ戻します。").disabled("申込者確認中（内容確認待ち・同意確認待ち）の申込だけ行えます。"));
        }
        // 取消
        if (d.has("cancel")) {
            tiles.add(new MenuTile("cancel", "申込取消", "申込を取り消します。取り消した申込は以降操作できません。").post(base + "/cancel").style("danger")
                    .confirm("この申込を取り消します。取り消した申込は元に戻せません。よろしいですか？").withReasonInput());
        } else if (d.has("cancelChange")) {
            tiles.add(new MenuTile("cancel", "契約変更の取消", "契約変更を取り消し、契約変更前の審査完了の状態へ戻します。").post(base + "/cancel").style("danger")
                    .confirm("契約変更を取り消し、契約変更前の状態へ戻します。よろしいですか？").withReasonInput());
        } else {
            tiles.add(new MenuTile("cancel", "申込取消", "申込を取り消します。").disabled(StatusCd.isCanceled(d.getApplication().getStatusCd()) ? "取消済みです。" : "現在のステータスでは取り消せません（承認申請中・事前確認待ち・審査中・審査完了は不可）。"));
        }
        // 追加申込
        if (d.has("additional")) {
            tiles.add(new MenuTile("additional", "追加申込", "この申込の申込者と申込内容を複写して、新しい申込を作ります。").link(base + "/additional").style("outline-primary"));
        } else {
            tiles.add(new MenuTile("additional", "追加申込", "この申込を元に新しい申込を作ります。").disabled(user.isOwner() ? "取消済みの申込からは作れません。" : "担当者だけが行えます。"));
        }
        // 再送
        if (d.has("resendConsent")) {
            tiles.add(new MenuTile("resendConsent", "確認依頼メール再送", "新しい確認用 URL を発行して申込者へ再送します。旧 URL は無効になります。").post(base + "/resendConsent").style("outline-secondary")
                    .confirm("確認依頼メールを再送します。旧 URL は使えなくなります。よろしいですか？"));
        }
        if (d.has("resendExternal")) {
            tiles.add(new MenuTile("resendExternal", "外部連携再送", "送信エラーになった審査担当部門への依頼を再送します。").post(base + "/resendExternal").style("outline-danger")
                    .confirm("送信エラーの外部連携を再送します。よろしいですか？"));
        }
        req.setAttribute("d", d);
        req.setAttribute("tiles", tiles);
        req.setAttribute("rowVersion", d.getApplication().getRowVersion());
        render(req, res, "emp/applications/menu.jsp");
    }

    /** SC14 の操作ボタン（POST /emp/applications/{id}/{action}）。 */
    private void postAction(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id, String action) throws ServletException, IOException {
        int rv = rowVersion(req);
        switch (action) {
            case "modify": {
                TransitionResult r = services().getApplicationService().modify(id, rv, user);
                redirect(req, res, StatusCd.CHG_INPUT.equals(r.getToStatusCd()) ? "/emp/applications/" + id + "/change" : "/emp/applications/" + id + "/edit");
                return;
            }
            case "fullRevise": {
                services().getApplicationService().modify(id, rv, user);
                flashMessage(req, "success", "I010");
                redirect(req, res, "/emp/applications/" + id + "/edit");
                return;
            }
            case "pullBack":
                services().getApplicationService().pullBack(id, rv, user);
                flashMessage(req, "success", "I009");
                break;
            case "startChange":
                services().getApplicationService().startChange(id, rv, user);
                flashMessage(req, "success", "I011");
                redirect(req, res, "/emp/applications/" + id + "/change");
                return;
            case "resendConsent":
                services().getApplicationService().resendConsent(id, rv, user);
                flashMessage(req, "success", "I006");
                break;
            case "resendExternal":
                services().getApplicationService().resendExternal(id, rv, user);
                flashMessage(req, "success", "I006");
                break;
            case "cancel": {
                String reason = req.getParameter("reason") == null ? "" : req.getParameter("reason").strip();
                if (reason.length() > 500) {
                    throw new BusinessException("E002", "取消理由", 500);
                }
                TransitionResult r = services().getApplicationService().cancel(id, rv, reason.isEmpty() ? null : reason, user);
                flashMessage(req, "success", StatusCd.isCanceled(r.getToStatusCd()) ? "I015" : "I016");
                break;
            }
            default:
                res.sendError(404);
                return;
        }
        redirect(req, res, menuPath(id));
    }

    // ------------------------------------------------------------------ SC04 申込入力

    private void showInput(HttpServletRequest req, HttpServletResponse res, LoginUser user, ApplicationDetail d, String mode, ApplicationForm form, Validation errors)
            throws ServletException, IOException {
        if (!user.isOwner()) {
            throw new ForbiddenException();
        }
        req.setAttribute("d", d);
        req.setAttribute("mode", mode);
        req.setAttribute("form", form);
        req.setAttribute("errors", errors == null ? java.util.Map.of() : errors.getErrors());
        if (d != null) {
            req.setAttribute("applicant", d.getApplicant());
        } else if (!form.getApplicantNo().isEmpty()) {
            Applicant a = Tx.execute(conn -> services().getApplicantDao().findByNo(conn, form.getApplicantNo()).orElse(null));
            req.setAttribute("applicant", a);
        }
        render(req, res, "emp/applications/input.jsp");
    }

    private void showAdditional(HttpServletRequest req, HttpServletResponse res, LoginUser user, long sourceId) throws ServletException, IOException {
        ApplicationDetail src = services().getApplicationQueryService().detail(sourceId, user);
        if (src.getApplication().getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new ForbiddenException();
        }
        ApplicationForm form = ApplicationForm.from(src.getCurrentVersion());
        req.setAttribute("sourceApplication", src.getApplication());
        req.setAttribute("sourceApplicant", src.getApplicant());
        showInput(req, res, user, null, "additional", withApplicant(form, src.getApplicant().getApplicantNo()), null);
    }

    private static ApplicationForm withApplicant(ApplicationForm f, String applicantNo) {
        java.util.Map<String, String[]> m = new java.util.HashMap<>();
        m.put("applicantNo", new String[] {applicantNo});
        m.put("productCd", new String[] {f.getProductCd()});
        m.put("basicFee", new String[] {f.getBasicFee()});
        m.put("optionFee", new String[] {f.getOptionFee()});
        m.put("handlingFee", new String[] {f.getHandlingFee()});
        m.put("contractStartDate", new String[] {f.getContractStartDate()});
        m.put("contractEndDate", new String[] {f.getContractEndDate()});
        m.put("remarks", new String[] {f.getRemarks()});
        return ApplicationForm.bind(new ParamRequest(m));
    }

    private void showEdit(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        if (!StatusCd.INPUT.equals(d.getApplication().getStatusCd())) {
            throw new TransitionNotAllowedException();
        }
        showInput(req, res, user, d, "edit", ApplicationForm.from(d.getCurrentVersion()), null);
    }

    /** SC04 の一時保存／確認へ。applicationId が null なら新規申込・追加申込（作成）。 */
    private void postInput(HttpServletRequest req, HttpServletResponse res, LoginUser user, Long applicationId, Long sourceId) throws ServletException, IOException {
        String action = param(req, "action");
        boolean toConfirm = "confirm".equals(action);
        ApplicationForm form = ApplicationForm.bind(req);
        boolean creating = applicationId == null;
        String mode = creating ? (sourceId == null ? "new" : "additional") : "edit";
        Validation v = form.validate(toConfirm, creating, false);
        ApplicationDetail d = creating ? null : services().getApplicationQueryService().detail(applicationId, user);
        if (sourceId != null) {
            ApplicationDetail src = services().getApplicationQueryService().detail(sourceId, user);
            req.setAttribute("sourceApplication", src.getApplication());
            req.setAttribute("sourceApplicant", src.getApplicant());
        }
        if (v.hasErrors()) {
            showInput(req, res, user, d, mode, form, v);
            return;
        }
        try {
            long id;
            if (creating) {
                id = services().getApplicationService().create(user, form.getApplicantNo(), form.toVersion(), sourceId);
            } else {
                services().getApplicationService().saveDraft(applicationId, rowVersion(req), form.toVersion(), user);
                id = applicationId;
            }
            if (toConfirm) {
                redirect(req, res, "/emp/applications/" + id + "/confirm");
            } else {
                flashMessage(req, "success", "I012");
                redirect(req, res, "/emp/applications/" + id + "/edit");
            }
        } catch (BusinessException e) {
            if ("E009".equals(e.getMessageId())) {
                v.reject("applicantNo", "E009", "申込者番号");
                showInput(req, res, user, d, mode, form, v);
                return;
            }
            throw e;
        }
    }

    // ------------------------------------------------------------------ SC05 申込内容確認

    private void showConfirm(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        String st = d.getApplication().getStatusCd();
        if (!Set.of(StatusCd.IMPORTED, StatusCd.INPUT, StatusCd.PRIMARY_WAIT).contains(st) || d.getApplication().getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new TransitionNotAllowedException();
        }
        ApplicationForm form = ApplicationForm.from(d.getCurrentVersion());
        Validation v = form.validate(true, false, false);
        req.setAttribute("d", d);
        req.setAttribute("errors", v.getErrors());
        req.setAttribute("canConfirm", !StatusCd.PRIMARY_WAIT.equals(st) && !v.hasErrors());
        render(req, res, "emp/applications/confirm.jsp");
    }

    private void postConfirm(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        String action = param(req, "action");
        if ("modify".equals(action)) {
            ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
            if (StatusCd.INPUT.equals(d.getApplication().getStatusCd())) {
                redirect(req, res, "/emp/applications/" + id + "/edit");
                return;
            }
            services().getApplicationService().modify(id, rowVersion(req), user);
            redirect(req, res, "/emp/applications/" + id + "/edit");
            return;
        }
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        Validation v = ApplicationForm.from(d.getCurrentVersion()).validate(true, false, false);
        if (v.hasErrors()) {
            flash(req, "danger", "入力内容に誤りがあります。「修正」で入力し直してください。");
            redirect(req, res, "/emp/applications/" + id + "/confirm");
            return;
        }
        services().getApplicationService().confirm(id, rowVersion(req), user);
        flashMessage(req, "success", "I005");
        redirect(req, res, menuPath(id));
    }

    // ------------------------------------------------------------------ SC06 承認フロー

    private void showApproval(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApprovalFlowView v = services().getApprovalService().load(id, user);
        req.setAttribute("v", v);
        req.setAttribute("d", v.getDetail());
        req.setAttribute("initialApproverIds", v.getInitialApproverIds());
        req.setAttribute("maxSteps", ApprovalService.MAX_STEPS);
        render(req, res, "emp/applications/approval.jsp");
    }

    private void postApproval(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        String action = param(req, "action");
        int rv = rowVersion(req);
        String comment = req.getParameter("comment") == null ? "" : req.getParameter("comment").strip();
        if (comment.length() > 500) {
            throw new BusinessException("E002", "コメント", 500);
        }
        switch (action) {
            case "apply": {
                String[] ids = req.getParameterValues("approverId");
                List<Long> approverIds = new ArrayList<>();
                if (ids != null) {
                    for (String s : ids) {
                        Long v = parseId(s);
                        if (v != null) {
                            approverIds.add(v);
                        }
                    }
                }
                Long routeId = "1".equals(param(req, "templateUsed")) ? longParam(req, "routeId") : null;
                services().getApprovalService().apply(id, rv, approverIds, routeId, user);
                flashMessage(req, "success", "I002");
                break;
            }
            case "approve": {
                Long requestId = longParam(req, "approvalRequestId");
                services().getApprovalService().approve(id, rv, requestId == null ? 0 : requestId, comment, user);
                flashMessage(req, "success", "I003");
                break;
            }
            case "return": {
                Long requestId = longParam(req, "approvalRequestId");
                services().getApprovalService().returnRequest(id, rv, requestId == null ? 0 : requestId, comment, user);
                flashMessage(req, "success", "I004");
                break;
            }
            case "review": {
                Long requestId = longParam(req, "approvalRequestId");
                services().getApprovalService().requestReview(id, rv, requestId == null ? 0 : requestId, comment, user);
                flashMessage(req, "success", "I002");
                break;
            }
            default:
                res.sendError(404);
                return;
        }
        redirect(req, res, menuPath(id));
    }

    // ------------------------------------------------------------------ SC07 申込修正

    private void showRevise(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id, ApplicationForm form, Validation errors) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        String st = d.getApplication().getStatusCd();
        if (!StatusCd.REVISE.contains(st) || d.getApplication().getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new TransitionNotAllowedException();
        }
        req.setAttribute("d", d);
        req.setAttribute("amountsOnly", StatusCd.FINAL_WAIT.equals(st));
        req.setAttribute("form", form == null ? ApplicationForm.from(d.getCurrentVersion()) : form);
        req.setAttribute("errors", errors == null ? java.util.Map.of() : errors.getErrors());
        req.setAttribute("newVersionNo", maxVersion(d) + 1);
        com.example.appmgmt.domain.ExternalLink latestNg = null;
        for (com.example.appmgmt.domain.ExternalLink l : d.getExternalLinks()) {
            if (Codes.EXT_RESULT_NG.equals(l.getResultCd())) {
                latestNg = l;
                break;
            }
        }
        req.setAttribute("latestNg", latestNg);
        render(req, res, "emp/applications/revise.jsp");
    }

    private static int maxVersion(ApplicationDetail d) {
        int max = 0;
        for (ApplicationVersion v : d.getVersions()) {
            max = Math.max(max, v.getVersionNo());
        }
        return max;
    }

    private void postRevise(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        boolean amountsOnly = StatusCd.FINAL_WAIT.equals(d.getApplication().getStatusCd());
        ApplicationForm form = ApplicationForm.bind(req);
        if (amountsOnly) {
            ApplicationForm base = ApplicationForm.from(d.getCurrentVersion());
            java.util.Map<String, String[]> m = new java.util.HashMap<>();
            m.put("productCd", new String[] {base.getProductCd()});
            m.put("basicFee", new String[] {form.getBasicFee()});
            m.put("optionFee", new String[] {form.getOptionFee()});
            m.put("handlingFee", new String[] {form.getHandlingFee()});
            m.put("contractStartDate", new String[] {base.getContractStartDate()});
            m.put("contractEndDate", new String[] {base.getContractEndDate()});
            m.put("remarks", new String[] {base.getRemarks()});
            form = ApplicationForm.bind(new ParamRequest(m));
        }
        Validation v = form.validate(true, false, false);
        if (v.hasErrors()) {
            showRevise(req, res, user, id, form, v);
            return;
        }
        TransitionResult r = services().getApplicationService().revise(id, rowVersion(req), form.toVersion(), user);
        flashMessage(req, "success", "I005");
        if (r.isOverLimit()) {
            flashMessage(req, "warning", "W001");
        }
        redirect(req, res, menuPath(id));
    }

    // ------------------------------------------------------------------ SC08 契約変更入力

    private void showChangeInput(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id, ApplicationForm form, Validation errors) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        if (!StatusCd.CHG_INPUT.equals(d.getApplication().getStatusCd()) || d.getApplication().getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new TransitionNotAllowedException();
        }
        req.setAttribute("d", d);
        req.setAttribute("form", form == null ? ApplicationForm.from(d.getCurrentVersion()) : form);
        req.setAttribute("errors", errors == null ? java.util.Map.of() : errors.getErrors());
        render(req, res, "emp/applications/change_input.jsp");
    }

    private void postChangeInput(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        boolean toConfirm = "confirm".equals(param(req, "action"));
        ApplicationForm form = ApplicationForm.bind(req);
        Validation v = form.validate(toConfirm, false, false);
        if (v.hasErrors()) {
            showChangeInput(req, res, user, id, form, v);
            return;
        }
        services().getApplicationService().saveDraft(id, rowVersion(req), form.toVersion(), user);
        if (toConfirm) {
            redirect(req, res, "/emp/applications/" + id + "/change/confirm");
        } else {
            flashMessage(req, "success", "I012");
            redirect(req, res, "/emp/applications/" + id + "/change");
        }
    }

    // ------------------------------------------------------------------ SC09 契約変更内容確認

    private void showChangeConfirm(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        String st = d.getApplication().getStatusCd();
        if (!Set.of(StatusCd.CHG_INPUT, StatusCd.CHG_PRIMARY_WAIT, StatusCd.CHG_FINAL_WAIT).contains(st) || d.getApplication().getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new TransitionNotAllowedException();
        }
        ApplicationVersion cur = d.getCurrentVersion();
        ApplicationVersion reviewed = d.getReviewedVersion();
        java.math.BigDecimal ratio = reviewed == null ? null : Formats.amountRatio(cur.getTotalAmount(), d.getBaseVersion() == null ? reviewed.getTotalAmount() : d.getBaseVersion().getTotalAmount());
        boolean over = StatusTransitionService.isOverLimit(ratio, d.getCompanyDiv().getAmountRatioLimit());
        Validation v = ApplicationForm.from(cur).validate(true, false, false);
        req.setAttribute("d", d);
        req.setAttribute("ratio", ratio);
        req.setAttribute("overLimit", over);
        req.setAttribute("noChange", reviewed != null && cur.sameContentAs(reviewed));
        req.setAttribute("errors", v.getErrors());
        req.setAttribute("canConfirm", StatusCd.CHG_INPUT.equals(st) && !v.hasErrors());
        String next;
        if (over) {
            next = "20201 【契約変更】一次承認申請待ち（一次承認・申込者確認を経る）";
        } else if (d.getCompanyDiv().isPreCheck()) {
            next = "20401 【契約変更】事前確認待ち（事前確認依頼を登録）";
        } else {
            next = "20501 【契約変更】最終承認申請待ち";
        }
        req.setAttribute("nextStatus", next);
        render(req, res, "emp/applications/change_confirm.jsp");
    }

    private void postChangeConfirm(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        String action = param(req, "action");
        if ("modify".equals(action)) {
            ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
            if (!StatusCd.CHG_INPUT.equals(d.getApplication().getStatusCd())) {
                services().getApplicationService().modify(id, rowVersion(req), user);
            }
            redirect(req, res, "/emp/applications/" + id + "/change");
            return;
        }
        TransitionResult r = services().getApplicationService().confirmChange(id, rowVersion(req), user);
        flashMessage(req, "success", "I005");
        if (r.isOverLimit()) {
            flashMessage(req, "warning", "W001");
        }
        redirect(req, res, menuPath(id));
    }

    /** フォーム再構築用の簡易リクエスト。 */
    private static class ParamRequest extends javax.servlet.http.HttpServletRequestWrapper {
        private final java.util.Map<String, String[]> params;

        ParamRequest(java.util.Map<String, String[]> params) {
            super(DummyRequest.create());
            this.params = params;
        }

        @Override
        public String getParameter(String name) {
            String[] v = params.get(name);
            return v == null || v.length == 0 ? null : v[0];
        }
    }

    /** HttpServletRequestWrapper の生成に必要なダミー（メソッドは使わない）。 */
    private static class DummyRequest implements java.lang.reflect.InvocationHandler {
        static HttpServletRequest create() {
            return (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class}, new DummyRequest());
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return null;
        }
    }
}
