<c:set var="pageTitle" value="お申込内容のご確認"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<p class="small text-muted">申込番号 <c:out value="${v.application.applicationNo}"/>　<c:out value="${v.applicant.applicantName}"/> 様　手続きの状況：<strong><c:out value="${v.applicantStatusName}"/></strong><c:if test="${not empty v.consent}">　確認期限：${app:datetime(v.consent.tokenExpiresAt)}</c:if></p>
<c:set var="cur" value="${v.version}"/><c:set var="base" value="${v.contractChange ? v.beforeVersion : null}"/><c:set var="curLabel" value="${v.contractChange ? '変更後' : 'お申込内容'}"/><c:set var="baseLabel" value="変更前"/>
<%@ include file="/WEB-INF/views/common/version_view.jspf" %>
<c:set var="ap" value="${v.applicant}"/><c:set var="apTitle" value="お客様情報"/><c:set var="apNoLabel" value="お客様番号（ユーザー ID）"/>
<%@ include file="/WEB-INF/views/common/applicant_view.jspf" %>
<p class="small text-muted">お客様情報は申込受付会社が登録した内容です。誤りがある場合は、次の画面で「差戻し」を選び、理由に正しい内容をご記入ください。</p>
<form method="post" action="${ctx}${consentBase}/confirm">
  <input type="hidden" name="_csrf" value="${csrf}">
  <div class="custom-control custom-checkbox mb-3">
    <input type="checkbox" class="custom-control-input" id="checked" name="checked" value="1" onchange="document.getElementById('confirmBtn').disabled = !this.checked;">
    <label class="custom-control-label" for="checked">内容を確認しました</label>
  </div>
  <button type="submit" class="btn btn-primary mr-2" id="confirmBtn" disabled>確定</button>
  <c:if test="${not v.contractChange}"><a class="btn btn-outline-secondary" href="${ctx}${consentBase}/edit">内容を修正する</a></c:if>
  <c:if test="${portal}"><a class="btn btn-link" href="${ctx}/my/menu?app=${v.application.applicationId}">メニューへ戻る</a></c:if>
</form>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
