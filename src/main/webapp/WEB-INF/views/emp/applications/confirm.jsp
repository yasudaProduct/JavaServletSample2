<c:set var="pageTitle" value="SC05 申込内容確認"/>
<c:set var="a" value="${d.application}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="card mb-3"><div class="card-body py-2 small">
  <strong>申込番号：</strong><c:out value="${a.applicationNo}"/>　<strong>ステータス：</strong><c:out value="${d.status.displayName}"/>　<strong>担当社員：</strong><c:out value="${d.owner.employeeName}"/>　<strong>登録区分：</strong>${app:label('REGISTRATION_TYPE', a.registrationType)}<br>
  <strong>申込者：</strong><c:out value="${d.applicant.applicantNo}"/>／<c:out value="${d.applicant.applicantName}"/>　<strong>確認依頼メールの宛先：</strong><c:out value="${d.applicant.mailAddress}"/>
</div></div>
<c:if test="${not empty errors}">
  <div class="alert alert-danger"><strong>入力内容に誤りがあります。「修正」で入力し直してください。</strong><ul class="mb-0"><c:forEach var="e" items="${errors}"><li><c:out value="${e.value}"/></li></c:forEach></ul></div>
</c:if>
<c:set var="cur" value="${d.currentVersion}"/><c:set var="base" value="${null}"/><c:set var="curLabel" value="申込内容"/>
<%@ include file="/WEB-INF/views/common/version_view.jspf" %>
<form method="post" action="${ctx}/emp/applications/${a.applicationId}/confirm">
  <input type="hidden" name="_csrf" value="${csrf}">
  <input type="hidden" name="rowVersion" value="${a.rowVersion}">
  <button type="submit" name="action" value="modify" class="btn btn-outline-primary mr-2">修正</button>
  <c:if test="${canConfirm}"><button type="submit" name="action" value="confirm" class="btn btn-primary mr-2">確定（一次承認申請待ちへ）</button></c:if>
  <a class="btn btn-outline-secondary" href="${ctx}/emp/applications/${a.applicationId}">戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
