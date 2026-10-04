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
  <%@ include file="/WEB-INF/views/common/version_fields.jspf" %>
  <div class="mt-3">
    <button type="submit" name="action" value="save" class="btn btn-outline-primary mr-2">一時保存</button>
    <button type="submit" name="action" value="confirm" class="btn btn-primary mr-2">確認へ</button>
    <a class="btn btn-outline-secondary" href="${empty d ? (empty sourceApplication ? ctx.concat('/emp/applications') : ctx.concat('/emp/applications/').concat(sourceApplication.applicationId).concat('/menu')) : ctx.concat('/emp/applications/').concat(d.application.applicationId).concat('/menu')}">戻る</a>
  </div>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
