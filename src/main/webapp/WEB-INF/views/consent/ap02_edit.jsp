<c:set var="pageTitle" value="お申込内容の修正"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<p class="small text-muted">申込番号 <c:out value="${v.application.applicationNo}"/>　<c:out value="${v.applicant.applicantName}"/> 様　手続きの状況：<strong><c:out value="${v.applicantStatusName}"/></strong><c:if test="${not empty v.consent}">　確認期限：${app:datetime(v.consent.tokenExpiresAt)}</c:if></p>
<form method="post" action="${ctx}${consentBase}/edit">
  <input type="hidden" name="_csrf" value="${csrf}">
  <%@ include file="/WEB-INF/views/common/version_fields.jspf" %>
  <button type="submit" class="btn btn-primary mr-2">保存</button>
  <a class="btn btn-outline-secondary" href="${ctx}${consentBase}">戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
