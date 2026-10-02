<c:set var="pageTitle" value="お手続き完了"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<div class="card"><div class="card-body">
  <p class="mb-2">申込番号 <c:out value="${v.application.applicationNo}"/>　手続きの状況：<strong><c:out value="${v.applicantStatusName}"/></strong></p>
  <c:choose>
    <c:when test="${v.consent.consentStatus == '2'}"><p class="mb-0">お手続きは完了しました。この画面を閉じてください。</p></c:when>
    <c:when test="${v.consent.consentStatus == '3'}"><p class="mb-0">差戻しを受け付けました。担当者からご連絡します。</p></c:when>
    <c:otherwise><p class="mb-0">お手続きは完了しています。</p></c:otherwise>
  </c:choose>
</div></div>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
