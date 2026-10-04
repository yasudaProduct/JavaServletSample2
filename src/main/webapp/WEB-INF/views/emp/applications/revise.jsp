<c:set var="pageTitle" value="SC07 申込修正"/>
<c:set var="a" value="${d.application}"/>
<c:set var="base" value="${d.baseVersion}"/>
<c:set var="ratioLimit" value="${d.companyDiv.amountRatioLimit}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<script>document.body.setAttribute('data-base-total', '${empty base ? '' : base.totalAmount}'); document.body.setAttribute('data-ratio-limit', '${ratioLimit}');</script>
<div class="card mb-3"><div class="card-body py-2 small">
  <strong>申込番号：</strong><c:out value="${a.applicationNo}"/>　<strong>ステータス：</strong><c:out value="${d.status.displayName}"/>　<strong>担当：</strong><span id="assignHead" title="担当（会社・部署・担当者）は申込入力中だけ変更できます"><c:out value="${d.companyDiv.companyDivName}"/> ／ <c:out value="${empty d.department ? a.deptCd : d.department.deptName}"/> ／ <c:out value="${d.owner.employeeName}"/></span>　
  <strong>申込者：</strong><c:out value="${d.applicant.applicantName}"/>（<c:out value="${empty d.applicant.applicantNo ? 'アカウント未発行' : d.applicant.applicantNo}"/>）<br>
  <strong>現行版：</strong>第 ${d.currentVersion.versionNo} 版（<c:out value="${d.currentVersion.versionTypeName}"/>）　確定時に <strong>第 ${newVersionNo} 版</strong> を作成します。
  <c:if test="${amountsOnly}">　<span class="text-info">最終承認申請待ちの修正は金額項目のみ変更できます（他の項目は全体修正で変更します）。</span></c:if>
</div></div>
<c:if test="${not empty latestNg}">
  <div class="alert alert-warning"><strong>審査担当部門からの指摘内容</strong>（受信日時 ${app:datetime(latestNg.resultReceivedAt)}）<br><span class="pre-wrap"><c:out value="${latestNg.resultReason}"/></span></div>
</c:if>
<div class="alert alert-info small mb-3">
  基準版（申込者が同意した版）：第 ${base.versionNo} 版　申込金額合計 ${app:amount(base.totalAmount)} 円　金額倍率しきい値 ${app:ratio(ratioLimit)} 倍。
  変更金額倍率がしきい値以上の場合は一次承認申請待ちへ戻り、一次承認と申込者確認をやり直します（減額は常に基準内）。
</div>
<div class="alert alert-warning" id="ratioWarning" style="display: none;">W001 変更が所定の変更基準を超えるため、一次承認と申込者確認をやり直します。</div>
<form method="post" action="${ctx}/emp/applications/${a.applicationId}/revise">
  <input type="hidden" name="_csrf" value="${csrf}">
  <input type="hidden" name="rowVersion" value="${a.rowVersion}">
  <%@ include file="/WEB-INF/views/common/version_fields.jspf" %>
  <button type="submit" class="btn btn-primary mr-2">確定</button>
  <a class="btn btn-outline-secondary" href="${ctx}/emp/applications/${a.applicationId}/menu">戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
