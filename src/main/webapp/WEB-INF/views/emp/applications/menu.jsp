<c:set var="pageTitle" value="SC14 申込メニュー"/>
<c:set var="a" value="${d.application}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="card mb-3 menu-head">
  <div class="card-body py-3">
    <div class="d-flex justify-content-between align-items-start flex-wrap">
      <div>
        <h2 class="h5 mb-1">申込番号 <c:out value="${a.applicationNo}"/>
          <span class="badge badge-${fn:endsWith(a.statusCd, '701') ? 'success' : a.statusCd == '90101' ? 'dark' : 'primary'} status-badge ml-2">${a.statusCd} <c:out value="${d.status.statusName}"/></span></h2>
        <div class="small text-muted"><c:out value="${d.status.description}"/></div>
        <div class="small mt-1">
          <strong>申込者：</strong><c:out value="${d.applicant.applicantNo}"/>／<c:out value="${d.applicant.applicantName}"/>　
          <strong>担当社員：</strong><c:out value="${d.owner.employeeName}"/>　
          <strong>会社区分：</strong><c:out value="${d.companyDiv.companyDivName}"/>　
          <strong>現行版：</strong>第 ${a.currentVersionNo} 版（<c:out value="${d.currentVersion.versionTypeName}"/>）　
          <strong>申込金額合計：</strong>${app:amount(d.currentVersion.totalAmount)} 円　
          <strong>更新日時：</strong>${app:datetime(a.updatedAt)}
        </div>
      </div>
      <a class="btn btn-outline-secondary btn-sm" href="${ctx}/emp/applications">一覧へ戻る</a>
    </div>
  </div>
</div>
<h3 class="h6 section-title">この申込に対する操作</h3>
<%@ include file="/WEB-INF/views/common/menu_tiles.jspf" %>
<p class="text-right mt-4 mb-0"><a class="small text-muted dev-link" id="devDetailLink" href="${ctx}/emp/applications/${a.applicationId}">開発者向け：申込確認（SC03 詳細）</a></p>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
