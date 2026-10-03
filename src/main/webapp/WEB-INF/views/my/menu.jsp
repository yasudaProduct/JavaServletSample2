<c:set var="pageTitle" value="申込者メニュー"/>
<c:set var="portal" value="${true}"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<ul class="nav nav-tabs mb-3" role="tablist">
  <li class="nav-item"><a class="nav-link ${tab == 'notice' ? 'active' : ''}" id="tab-notice" data-toggle="tab" href="#pane-notice" role="tab">お知らせ <span class="badge badge-pill badge-light">${fn:length(notices)}</span></a></li>
  <li class="nav-item"><a class="nav-link ${tab == 'menu' ? 'active' : ''}" id="tab-menu" data-toggle="tab" href="#pane-menu" role="tab">メニュー</a></li>
</ul>
<div class="tab-content">
  <div class="tab-pane fade ${tab == 'notice' ? 'show active' : ''}" id="pane-notice" role="tabpanel">
    <c:if test="${empty notices}"><p class="text-muted">お知らせはありません。</p></c:if>
    <c:forEach var="n" items="${notices}">
      <div class="card mb-2">
        <div class="card-header py-1 small"><strong><c:out value="${n.notificationTypeName}"/></strong>　申込番号 <c:out value="${n.applicationNo}"/>　${app:datetime(n.createdAt)}</div>
        <div class="card-body py-2">
          <div class="font-weight-bold small mb-1"><c:out value="${n.subject}"/></div>
          <div class="pre-wrap small"><c:out value="${n.body}"/></div>
        </div>
      </div>
    </c:forEach>
  </div>
  <div class="tab-pane fade ${tab == 'menu' ? 'show active' : ''}" id="pane-menu" role="tabpanel">
    <c:choose>
      <c:when test="${empty applications}">
        <p class="text-muted">お申込はありません。</p>
      </c:when>
      <c:otherwise>
        <c:set var="a" value="${d.application}"/>
        <div class="card mb-3 menu-head">
          <div class="card-body py-3">
            <div class="d-flex justify-content-between align-items-start flex-wrap">
              <div>
                <h2 class="h5 mb-1">申込番号 <c:out value="${a.applicationNo}"/>
                  <span class="badge badge-${fn:endsWith(a.statusCd, '701') ? 'success' : 'primary'} status-badge ml-2"><c:out value="${empty d.status.applicantStatusName ? '手続き中' : d.status.applicantStatusName}"/></span></h2>
                <div class="small mt-1">
                  <strong>商品コード：</strong><c:out value="${d.currentVersion.productCd}"/>　
                  <strong>申込金額合計：</strong>${app:amount(d.currentVersion.totalAmount)} 円　
                  <strong>契約期間：</strong>${app:date(d.currentVersion.contractStartDate)} 〜 ${app:date(d.currentVersion.contractEndDate)}　
                  <strong>担当者：</strong><c:out value="${d.owner.employeeName}"/>
                </div>
              </div>
              <c:if test="${fn:length(applications) > 1}">
                <form method="get" action="${ctx}/my/menu" class="form-inline">
                  <label class="small mr-2" for="appSelect">お申込を選択</label>
                  <select class="form-control form-control-sm" id="appSelect" name="app" onchange="this.form.submit()">
                    <c:forEach var="x" items="${applications}"><option value="${x.applicationId}" ${x.applicationId == a.applicationId ? 'selected' : ''}><c:out value="${x.applicationNo}"/></option></c:forEach>
                  </select>
                </form>
              </c:if>
            </div>
          </div>
        </div>
        <h3 class="h6 section-title">このお申込に対する操作</h3>
        <%@ include file="/WEB-INF/views/common/menu_tiles.jspf" %>
      </c:otherwise>
    </c:choose>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
