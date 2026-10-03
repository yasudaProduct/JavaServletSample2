<c:set var="pageTitle" value="申込確認"/>
<c:set var="portal" value="${true}"/>
<c:set var="a" value="${d.application}"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<p class="small text-muted">申込番号 <c:out value="${a.applicationNo}"/>　手続きの状況：<strong><c:out value="${empty d.status.applicantStatusName ? '手続き中' : d.status.applicantStatusName}"/></strong>　担当者：<c:out value="${d.owner.employeeName}"/>　更新日時：${app:datetime(a.updatedAt)}</p>
<c:set var="cur" value="${d.currentVersion}"/>
<c:set var="base" value="${not empty d.reviewedVersion and d.reviewedVersion.versionNo != cur.versionNo ? d.reviewedVersion : null}"/>
<c:set var="curLabel" value="${empty base ? 'お申込内容' : '変更後'}"/><c:set var="baseLabel" value="変更前"/>
<%@ include file="/WEB-INF/views/common/version_view.jspf" %>
<c:if test="${not empty cv.consent}">
  <div class="card mb-3"><div class="card-body py-2 small">
    <strong>確認・同意の状況：</strong><c:out value="${cv.consent.consentStatusName}"/>
    <c:if test="${not empty cv.consent.contentConfirmedAt}">　内容確定 ${app:datetime(cv.consent.contentConfirmedAt)}</c:if>
    <c:if test="${not empty cv.consent.consentedAt}">　同意 ${app:datetime(cv.consent.consentedAt)}</c:if>
    <c:if test="${not empty cv.consent.returnedAt}">　差戻し ${app:datetime(cv.consent.returnedAt)}</c:if>
    <c:if test="${cv.outcomeName == 'CONFIRM' or cv.outcomeName == 'AGREE'}">　確認期限 ${app:datetime(cv.consent.tokenExpiresAt)}</c:if>
  </div></div>
</c:if>
<h3 class="h6 section-title">お手続きの履歴</h3>
<table class="table table-sm table-bordered bg-white">
  <thead class="thead-light"><tr><th>日時</th><th>手続き</th><th>状況</th></tr></thead>
  <tbody>
    <c:forEach var="h" items="${d.histories}">
      <c:if test="${h.actorType == '3' or h.actionCd == '00' or fn:endsWith(h.toStatusCd, '301') or fn:endsWith(h.toStatusCd, '701') or h.toStatusCd == '90101'}">
        <tr><td class="small">${app:datetime(h.changedAt)}</td><td><c:out value="${h.actorType == '3' ? h.actionName : h.actionCd == '00' ? '申込の受付' : '手続きの進行'}"/></td><td class="small">${h.toStatusCd == '90101' ? '取消' : fn:endsWith(h.toStatusCd, '701') ? '審査完了' : fn:endsWith(h.toStatusCd, '301') ? '内容確認のお願い' : h.actorType == '3' ? '申込者の操作' : '受付'}</td></tr>
      </c:if>
    </c:forEach>
  </tbody>
</table>
<c:if test="${cv.outcomeName == 'CONFIRM' or cv.outcomeName == 'AGREE'}"><a class="btn btn-primary mr-2" href="${ctx}/my/applications/${a.applicationId}/consent">内容確認・同意へ進む</a></c:if>
<a class="btn btn-outline-secondary" href="${ctx}/my/menu?app=${a.applicationId}">メニューへ戻る</a>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
