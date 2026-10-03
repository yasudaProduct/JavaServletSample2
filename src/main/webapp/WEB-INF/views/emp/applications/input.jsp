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
  <%-- 申込者情報は申込データとして申込内容の入力欄（version_fields）で入力する。ここは申込者アカウント（ログイン用）の紐づけだけ --%>
  <div class="card mb-3" id="accountCard">
    <div class="card-header py-2">申込者アカウント（申込者ページのログイン）</div>
    <div class="card-body py-2">
      <c:choose>
        <c:when test="${canLinkAccount}">
          <div class="form-row align-items-end">
            <div class="form-group col-md-3 mb-2">
              <label for="applicantNo">申込者番号（ユーザー ID）</label>
              <input type="text" class="form-control ${not empty errors.applicantNo ? 'is-invalid' : ''}" id="applicantNo" name="applicantNo" maxlength="12" value="<c:out value='${form.applicantNo}'/>" placeholder="例：C0000000001">
              <div class="invalid-feedback"><c:out value="${errors.applicantNo}"/></div>
            </div>
            <div class="form-group col-md-9 mb-2">
              <p class="small text-muted mb-0">通常は空のまま保存します。アカウントは一次承認が通ったときに発行し、ユーザー ID と初期パスワードを申込者のメールアドレスへ通知します。
                同じ申込者の 2 件目以降で、発行済みのアカウントを使う場合だけ申込者番号を入力します。</p>
            </div>
          </div>
        </c:when>
        <c:when test="${mode == 'additional'}">
          <p class="small mb-2" id="accountNote">追加申込は元の申込の申込者アカウントを引き継ぎます（<c:out value="${empty sourceApplicant.applicantNo ? '元の申込もアカウント未発行のため、一次承認で発行します' : sourceApplicant.applicantNo}"/>）。</p>
        </c:when>
        <c:otherwise>
          <p class="small mb-2" id="accountNote"><strong>申込者番号：</strong><c:out value="${d.applicant.applicantNo}"/>（発行済み。申込者情報の変更はこの申込内容の修正として行います）</p>
        </c:otherwise>
      </c:choose>
      <c:if test="${canLinkAccount and not duplicateWarning and not empty linkCandidates}">
        <div class="alert alert-info mt-2 mb-2 small" id="linkCandidates">同じメールアドレスで申込者アカウントが発行済みです。同じ申込者なら申込者番号を入力してください：
          <c:forEach var="x" items="${linkCandidates}" varStatus="st"><code><c:out value="${x.applicantNo}"/></code> <c:out value="${x.applicantName}"/><c:if test="${not st.last}">、</c:if></c:forEach>
        </div>
      </c:if>
      <c:if test="${duplicateWarning}">
        <div class="alert alert-warning mt-2 mb-2" id="duplicateWarning">
          <strong>W003</strong> <c:out value="${app:message('W003')}"/>
          <ul class="mb-2">
            <c:forEach var="x" items="${duplicates}"><li><code><c:out value="${x.applicantNo}"/></code>　<c:out value="${x.applicantName}"/>（<c:out value="${x.mailAddress}"/>）</li></c:forEach>
          </ul>
          <div class="custom-control custom-checkbox">
            <input type="checkbox" class="custom-control-input" id="allowDuplicate" name="allowDuplicate" value="1">
            <label class="custom-control-label" for="allowDuplicate">同じメールアドレスの別の申込者として保存する（一次承認で新しいアカウントを発行）</label>
          </div>
        </div>
      </c:if>
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
