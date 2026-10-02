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
    <strong>担当社員：</strong><c:out value="${user.employeeName}"/>
    <c:if test="${mode == 'additional'}">　<strong>追加申込元：</strong><c:out value="${sourceApplication.applicationNo}"/></c:if>
    <c:if test="${not empty d}">　<strong>版：</strong>第 ${d.currentVersion.versionNo} 版（<c:out value="${d.currentVersion.versionTypeName}"/>）<c:if test="${not empty d.currentVersion.copiedFromVersionNo}">、複写元 第 ${d.currentVersion.copiedFromVersionNo} 版</c:if></c:if>
  </div>
</div>
<form method="post" action="${action}">
  <input type="hidden" name="_csrf" value="${csrf}">
  <c:if test="${not empty d}"><input type="hidden" name="rowVersion" value="${d.application.rowVersion}"></c:if>
  <div class="form-row">
    <div class="form-group col-md-3">
      <label for="applicantNo">申込者番号 <c:if test="${mode == 'new'}"><span class="badge badge-danger">必須</span></c:if></label>
      <c:choose>
        <c:when test="${mode == 'new'}">
          <input type="text" class="form-control ${not empty errors.applicantNo ? 'is-invalid' : ''}" id="applicantNo" name="applicantNo" maxlength="12" value="<c:out value='${form.applicantNo}'/>" placeholder="例：C0000000001">
          <div class="invalid-feedback"><c:out value="${errors.applicantNo}"/></div>
          <small class="form-text text-muted">申込者マスタの申込者番号を入力します（サンプル：C0000000001〜C0000000005）。</small>
        </c:when>
        <c:otherwise>
          <input type="hidden" name="applicantNo" value="<c:out value='${form.applicantNo}'/>">
          <input type="text" class="form-control-plaintext" value="<c:out value='${empty applicant ? sourceApplicant.applicantNo : applicant.applicantNo}'/>" readonly>
        </c:otherwise>
      </c:choose>
    </div>
    <div class="form-group col-md-6">
      <label>申込者名／メールアドレス</label>
      <c:set var="ap" value="${empty applicant ? sourceApplicant : applicant}"/>
      <c:choose>
        <c:when test="${empty ap}"><c:set var="apText" value="（申込者番号から表示します）"/></c:when>
        <c:otherwise><c:set var="apText" value="${ap.applicantName}／${ap.mailAddress}"/></c:otherwise>
      </c:choose>
      <input type="text" class="form-control-plaintext" value="<c:out value='${apText}'/>" readonly>
    </div>
  </div>
  <%@ include file="/WEB-INF/views/common/version_fields.jspf" %>
  <div class="mt-3">
    <button type="submit" name="action" value="save" class="btn btn-outline-primary mr-2">一時保存</button>
    <button type="submit" name="action" value="confirm" class="btn btn-primary mr-2">確認へ</button>
    <a class="btn btn-outline-secondary" href="${empty d ? (empty sourceApplication ? ctx.concat('/emp/applications') : ctx.concat('/emp/applications/').concat(sourceApplication.applicationId)) : ctx.concat('/emp/applications/').concat(d.application.applicationId)}">戻る</a>
  </div>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
