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
  <div class="card mb-3" id="applicantCard">
    <div class="card-header py-2">申込者</div>
    <div class="card-body pb-1">
      <c:choose>
        <c:when test="${mode == 'new'}">
          <div class="mb-3">
            <div class="custom-control custom-radio custom-control-inline">
              <input type="radio" class="custom-control-input" id="applicantModeNew" name="applicantMode" value="new" ${af.newApplicant ? 'checked' : ''}>
              <label class="custom-control-label" for="applicantModeNew">新規の申込者を登録する</label>
            </div>
            <div class="custom-control custom-radio custom-control-inline">
              <input type="radio" class="custom-control-input" id="applicantModeExisting" name="applicantMode" value="existing" ${af.newApplicant ? '' : 'checked'}>
              <label class="custom-control-label" for="applicantModeExisting">登録済みの申込者を指定する</label>
            </div>
          </div>
          <div class="applicant-new" ${af.newApplicant ? '' : 'style="display:none"'}>
            <p class="small text-muted mb-2">申込者番号は保存したときに採番し、申込者を登録します（申込者ページのユーザー ID になります）。</p>
            <%@ include file="/WEB-INF/views/common/applicant_fields.jspf" %>
          </div>
          <div class="applicant-existing" ${af.newApplicant ? 'style="display:none"' : ''}>
            <div class="form-row">
              <div class="form-group col-md-3">
                <label for="applicantNo">申込者番号 <span class="badge badge-danger">必須</span></label>
                <input type="text" class="form-control ${not empty errors.applicantNo ? 'is-invalid' : ''}" id="applicantNo" name="applicantNo" maxlength="12" value="<c:out value='${form.applicantNo}'/>" placeholder="例：C0000000001">
                <div class="invalid-feedback"><c:out value="${errors.applicantNo}"/></div>
              </div>
              <div class="form-group col-md-9">
                <label>申込者名／メールアドレス</label>
                <input type="text" class="form-control-plaintext" value="<c:out value='${empty applicant ? "（保存後に表示します）" : applicant.applicantName.concat("／").concat(applicant.mailAddress)}'/>" readonly>
              </div>
            </div>
            <p class="small text-muted">同じ申込者の 2 件目以降の申込で使います。</p>
          </div>
        </c:when>
        <c:when test="${mode == 'edit' and applicantEditable}">
          <input type="hidden" name="applicantNo" value="<c:out value='${applicant.applicantNo}'/>">
          <p class="small mb-2"><strong>申込者番号：</strong><c:out value="${applicant.applicantNo}"/>　<span class="text-muted">申込者ページのアカウント発行前のため、ここで申込者情報を修正できます。</span></p>
          <%@ include file="/WEB-INF/views/common/applicant_fields.jspf" %>
        </c:when>
        <c:otherwise>
          <c:set var="ap" value="${empty applicant ? sourceApplicant : applicant}"/>
          <input type="hidden" name="applicantNo" value="<c:out value='${form.applicantNo}'/>">
          <p class="small mb-2"><strong>申込者番号：</strong><c:out value="${ap.applicantNo}"/>　<strong>申込者名：</strong><c:out value="${ap.applicantName}"/>　<strong>メールアドレス：</strong><c:out value="${ap.mailAddress}"/></p>
          <p class="small text-muted" id="applicantReadonlyNote">${mode == 'additional' ? '追加申込は元の申込の申込者で登録します。' : '申込者ページのアカウントを発行済み、または他の申込でも使われている申込者です。申込者情報は申込メニューの「メンテナンス」から変更します。'}</p>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
  <%@ include file="/WEB-INF/views/common/version_fields.jspf" %>
  <div class="mt-3">
    <button type="submit" name="action" value="save" class="btn btn-outline-primary mr-2">一時保存</button>
    <button type="submit" name="action" value="confirm" class="btn btn-primary mr-2">確認へ</button>
    <a class="btn btn-outline-secondary" href="${empty d ? (empty sourceApplication ? ctx.concat('/emp/applications') : ctx.concat('/emp/applications/').concat(sourceApplication.applicationId).concat('/menu')) : ctx.concat('/emp/applications/').concat(d.application.applicationId).concat('/menu')}">戻る</a>
  </div>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
