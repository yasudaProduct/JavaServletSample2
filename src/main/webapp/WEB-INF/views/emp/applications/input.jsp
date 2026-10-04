<c:set var="pageTitle" value="SC04 申込入力"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<c:choose>
  <c:when test="${mode == 'new'}"><c:set var="action" value="${ctx}/emp/applications/new"/></c:when>
  <c:when test="${mode == 'additional'}"><c:set var="action" value="${ctx}/emp/applications/${sourceApplication.applicationId}/additional"/></c:when>
  <c:otherwise><c:set var="action" value="${ctx}/emp/applications/${d.application.applicationId}/edit"/></c:otherwise>
</c:choose>
<div class="card mb-3">
  <div class="card-body py-2 small">
    <strong>申込番号：</strong><c:out value="${empty d ? '（未採番）' : d.application.applicationNo}"/>　
    <strong>ステータス：</strong><c:out value="${empty d ? '（未作成）' : d.status.displayName}"/>　
    <strong>入力者：</strong><c:out value="${user.employeeName}"/>
    <c:if test="${mode == 'additional'}">　<strong>追加申込元：</strong><c:out value="${sourceApplication.applicationNo}"/></c:if>
    <c:if test="${not empty d}">　<strong>版：</strong>第 ${d.currentVersion.versionNo} 版（<c:out value="${d.currentVersion.versionTypeName}"/>）<c:if test="${not empty d.currentVersion.copiedFromVersionNo}">、複写元 第 ${d.currentVersion.copiedFromVersionNo} 版</c:if></c:if>
  </div>
</div>
<form method="post" action="${action}" data-user-id="${user.employeeId}">
  <input type="hidden" name="_csrf" value="${csrf}">
  <c:if test="${not empty d}"><input type="hidden" name="rowVersion" value="${d.application.rowVersion}"></c:if>
  <%-- 担当（申込受付会社の会社 > 部署 > 担当者）。既定はログインユーザー。入力中だけ変更できる（全体修正後の再入力を含む） --%>
  <div class="card mb-3" id="assignCard">
    <div class="card-header py-2">担当（申込受付会社）</div>
    <div class="card-body py-2">
      <div class="form-row">
        <div class="form-group col-md-3 mb-2">
          <label for="companyDiv">会社 <span class="badge badge-danger">必須</span></label>
          <select class="form-control ${not empty errors.companyDiv ? 'is-invalid' : ''}" id="companyDiv" name="companyDiv">
            <c:forEach var="c" items="${assign.companies}"><option value="${c.companyDiv}" data-company-extra="${c.companyExtraTarget ? '1' : '0'}" ${form.companyDiv == c.companyDiv ? 'selected' : ''}><c:out value="${c.companyDivName}"/></option></c:forEach>
          </select>
          <div class="invalid-feedback"><c:out value="${errors.companyDiv}"/></div>
        </div>
        <div class="form-group col-md-4 mb-2">
          <label for="deptCd">部署 <span class="badge badge-danger">必須</span></label>
          <select class="form-control ${not empty errors.deptCd ? 'is-invalid' : ''}" id="deptCd" name="deptCd" data-company-source="companyDiv">
            <c:forEach var="dp" items="${assign.departments}"><option value="${dp.deptCd}" data-company="${dp.companyDiv}" ${form.companyDiv == dp.companyDiv and form.deptCd == dp.deptCd ? 'selected' : ''}><c:out value="${dp.deptName}"/>（<c:out value="${dp.deptCd}"/>）</option></c:forEach>
          </select>
          <div class="invalid-feedback"><c:out value="${errors.deptCd}"/></div>
        </div>
        <div class="form-group col-md-5 mb-2">
          <label for="ownerEmployeeId">担当者 <span class="badge badge-danger">必須</span></label>
          <select class="form-control ${not empty errors.ownerEmployeeId ? 'is-invalid' : ''}" id="ownerEmployeeId" name="ownerEmployeeId" data-company-source="companyDiv">
            <c:forEach var="e" items="${assign.owners}"><option value="${e.employeeId}" data-company="${e.companyDiv}" ${form.ownerEmployeeId == e.employeeId.toString() ? 'selected' : ''}><c:out value="${e.employeeName}"/>（<c:out value="${e.deptName}"/>）<c:if test="${e.employeeId == user.employeeId}"> ※自分</c:if></option></c:forEach>
          </select>
          <div class="invalid-feedback"><c:out value="${errors.ownerEmployeeId}"/></div>
        </div>
      </div>
      <p class="small text-muted mb-0">会社 &gt; 部署 &gt; 担当者の順に選びます（既定はログインユーザー）。部署は担当者の所属部署と異なってもかまいません。会社・部署を変えると、事前確認の有無・承認ルートの初期値・承認者の候補が変わります。担当者を自分以外にすると、保存後はその担当者だけが操作できます。</p>
    </div>
  </div>
  <%-- 申込者情報は申込データとして申込内容の入力欄（version_fields）で入力する。ここは申込者アカウント（ログイン用）の状態の案内だけ --%>
  <div class="card mb-3" id="accountCard">
    <div class="card-header py-2">申込者アカウント（申込者ページのログイン）</div>
    <div class="card-body py-2">
      <c:choose>
        <c:when test="${mode == 'additional'}">
          <p class="small mb-0" id="accountNote">追加申込は元の申込（<c:out value="${sourceApplication.applicationNo}"/>）の申込者アカウント <strong><c:out value="${sourceApplicant.applicantNo}"/></strong> を引き継ぎます。申込者情報と申込内容は元の申込から複写した値で、この申込のデータとして変更できます（元の申込は変わりません）。</p>
        </c:when>
        <c:when test="${not empty d and not empty d.account}">
          <p class="small mb-0" id="accountNote"><strong>申込者番号：</strong><c:out value="${d.account.applicantNo}"/>（発行済み。申込者情報の変更はこの申込内容の修正として行います）</p>
        </c:when>
        <c:otherwise>
          <p class="small text-muted mb-0" id="accountNote">新規申込は、同じ氏名・メールアドレスの申込があっても別の申込者として登録します。申込者アカウントは一次承認が通ったときに発行し、ユーザー ID と初期パスワードを申込者のメールアドレスへ通知します。同じ申込者の申込は、審査完了した申込の「追加申込」から作成してください。</p>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
  <c:set var="companyExtra" value="toggle"/>
  <%@ include file="/WEB-INF/views/common/version_fields.jspf" %>
  <div class="mt-3">
    <button type="submit" name="action" value="save" class="btn btn-outline-primary mr-2">一時保存</button>
    <button type="submit" name="action" value="confirm" class="btn btn-primary mr-2">確認へ</button>
    <a class="btn btn-outline-secondary" href="${empty d ? (empty sourceApplication ? ctx.concat('/emp/applications') : ctx.concat('/emp/applications/').concat(sourceApplication.applicationId).concat('/menu')) : ctx.concat('/emp/applications/').concat(d.application.applicationId).concat('/menu')}">戻る</a>
  </div>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
