<c:set var="pageTitle" value="SC09 契約変更内容確認"/>
<c:set var="a" value="${d.application}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="card mb-3"><div class="card-body py-2 small">
  <strong>申込番号：</strong><c:out value="${a.applicationNo}"/>　<strong>ステータス：</strong><c:out value="${d.status.displayName}"/>　<strong>担当社員：</strong><c:out value="${d.owner.employeeName}"/>　
  <strong>申込者：</strong><c:out value="${d.applicant.applicantNo}"/>／<c:out value="${d.applicant.applicantName}"/>　<strong>会社区分：</strong><c:out value="${d.companyDiv.companyDivName}"/>
</div></div>
<c:if test="${not empty errors}">
  <div class="alert alert-danger"><strong>入力内容に誤りがあります。「修正」で入力し直してください。</strong><ul class="mb-0"><c:forEach var="e" items="${errors}"><li><c:out value="${e.value}"/></li></c:forEach></ul></div>
</c:if>
<c:if test="${noChange}"><div class="alert alert-danger">E104 変更がないため確定できません。</div></c:if>
<c:set var="cur" value="${d.currentVersion}"/><c:set var="base" value="${d.reviewedVersion}"/><c:set var="curLabel" value="変更後"/><c:set var="baseLabel" value="変更前（審査完了版）"/>
<%@ include file="/WEB-INF/views/common/version_view.jspf" %>
<table class="table table-sm table-bordered" style="max-width: 720px;">
  <tr><th style="width: 40%;">変更金額倍率／金額倍率しきい値</th><td>${app:ratio(ratio)}／${app:ratio(d.companyDiv.amountRatioLimit)}</td></tr>
  <tr><th>判定結果</th><td>${overLimit ? '変更基準超' : '変更基準内'}</td></tr>
  <tr><th>確定後の遷移先</th><td><c:out value="${nextStatus}"/></td></tr>
</table>
<c:if test="${overLimit}"><div class="alert alert-warning">W001 変更が所定の変更基準を超えるため、一次承認と申込者確認をやり直します。</div></c:if>
<form method="post" action="${ctx}/emp/applications/${a.applicationId}/change/confirm">
  <input type="hidden" name="_csrf" value="${csrf}">
  <input type="hidden" name="rowVersion" value="${a.rowVersion}">
  <button type="submit" name="action" value="modify" class="btn btn-outline-primary mr-2">修正</button>
  <c:if test="${canConfirm and not noChange}"><button type="submit" name="action" value="confirm" class="btn btn-primary mr-2">確定</button></c:if>
  <a class="btn btn-outline-secondary" href="${ctx}/emp/applications/${a.applicationId}/menu">戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
