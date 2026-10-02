<c:set var="pageTitle" value="確認 URL エラー"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<div class="card border-danger"><div class="card-body">
  <p class="mb-2 text-danger"><c:out value="${errorMessage}"/></p>
  <p class="mb-0 small text-muted">お手数ですが担当者にお問い合わせください。</p>
</div></div>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
