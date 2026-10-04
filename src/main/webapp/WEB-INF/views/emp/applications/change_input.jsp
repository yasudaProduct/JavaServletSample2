<c:set var="pageTitle" value="SC08 契約変更入力"/>
<c:set var="a" value="${d.application}"/>
<c:set var="base" value="${d.reviewedVersion}"/>
<c:set var="ratioLimit" value="${d.companyDiv.amountRatioLimit}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<script>document.body.setAttribute('data-base-total', '${empty base ? '' : base.totalAmount}'); document.body.setAttribute('data-ratio-limit', '${ratioLimit}');</script>
<div class="card mb-3"><div class="card-body py-2 small">
  <strong>申込番号：</strong><c:out value="${a.applicationNo}"/>　<strong>ステータス：</strong><c:out value="${d.status.displayName}"/>　<strong>担当：</strong><span id="assignHead" title="担当（会社・部署・担当者）は申込入力中だけ変更できます"><c:out value="${d.companyDiv.companyDivName}"/> ／ <c:out value="${empty d.department ? a.deptCd : d.department.deptName}"/> ／ <c:out value="${d.owner.employeeName}"/></span>　
  <strong>申込者：</strong><c:out value="${d.applicant.applicantName}"/>（<c:out value="${empty d.applicant.applicantNo ? 'アカウント未発行' : d.applicant.applicantNo}"/>）<br>
  <strong>版：</strong>第 ${d.currentVersion.versionNo} 版（<c:out value="${d.currentVersion.versionTypeName}"/>、複写元 第 ${d.currentVersion.copiedFromVersionNo} 版）　<strong>変更前（審査完了版）：</strong>第 ${base.versionNo} 版　申込金額合計 ${app:amount(base.totalAmount)} 円　しきい値 ${app:ratio(ratioLimit)} 倍
</div></div>
<div class="alert alert-warning" id="ratioWarning" style="display: none;">W001 変更が所定の変更基準を超えるため、一次承認と申込者確認をやり直します。</div>
<form method="post" action="${ctx}/emp/applications/${a.applicationId}/change">
  <input type="hidden" name="_csrf" value="${csrf}">
  <input type="hidden" name="rowVersion" value="${a.rowVersion}">
  <c:set var="companyExtra" value="${d.application.companyExtraTarget ? 'fixed' : ''}"/>
  <%@ include file="/WEB-INF/views/common/version_fields.jspf" %>
  <button type="submit" name="action" value="save" class="btn btn-outline-primary mr-2">一時保存</button>
  <button type="submit" name="action" value="confirm" class="btn btn-primary mr-2">確認へ</button>
  <a class="btn btn-outline-secondary" href="${ctx}/emp/applications/${a.applicationId}/menu">戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
