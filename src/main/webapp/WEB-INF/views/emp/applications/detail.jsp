<c:set var="pageTitle" value="SC03 申込確認"/>
<c:set var="a" value="${d.application}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="d-flex justify-content-between align-items-start mb-2">
  <div>
    <h2 class="h5 mb-1">申込番号 <c:out value="${a.applicationNo}"/>
      <span class="badge badge-${fn:endsWith(a.statusCd, '701') ? 'success' : a.statusCd == '90101' ? 'dark' : 'primary'} status-badge ml-2">${a.statusCd} <c:out value="${d.status.statusName}"/></span></h2>
    <div class="text-muted small"><c:out value="${d.status.description}"/></div>
  </div>
  <div>
    <a class="btn btn-primary btn-sm" href="${ctx}/emp/applications/${a.applicationId}/menu">申込メニューへ</a>
    <a class="btn btn-outline-secondary btn-sm" href="${ctx}/emp/applications">一覧へ戻る</a>
  </div>
</div>

<div class="row">
  <div class="col-lg-6">
    <h3 class="h6 section-title">申込基本情報</h3>
    <table class="table table-sm table-bordered">
      <tr><th style="width: 30%;">申込者</th><td><c:out value="${d.applicant.applicantNo}"/>／<c:out value="${d.applicant.applicantName}"/>（<c:out value="${d.applicant.mailAddress}"/>）</td></tr>
      <tr><th>担当社員</th><td><c:out value="${d.owner.employeeName}"/></td></tr>
      <tr><th>会社区分／部署コード</th><td><c:out value="${d.companyDiv.companyDivName}"/>（事前確認${d.companyDiv.preCheck ? 'あり' : 'なし'}、しきい値 ${app:ratio(d.companyDiv.amountRatioLimit)} 倍）／<c:out value="${a.deptCd}"/></td></tr>
      <tr><th>登録区分</th><td>${app:label('REGISTRATION_TYPE', a.registrationType)}
        <c:if test="${not empty d.sourceApplicationNo}">（元の申込 <a href="${ctx}/emp/applications/${a.sourceApplicationId}"><c:out value="${d.sourceApplicationNo}"/></a>）</c:if>
        <c:if test="${not empty a.importBatchId}">（取込 ID <a href="${ctx}/emp/import/${a.importBatchId}">${a.importBatchId}</a>）</c:if></td></tr>
      <tr><th>現行版／基準版／審査完了版</th><td>第 ${a.currentVersionNo} 版／${empty a.baseVersionNo ? '－' : '第 '.concat(a.baseVersionNo).concat(' 版')}／${empty a.reviewedVersionNo ? '－' : '第 '.concat(a.reviewedVersionNo).concat(' 版')}</td></tr>
      <tr><th>審査完了日時／更新日時</th><td>${app:datetime(a.reviewedAt)}／${app:datetime(a.updatedAt)}</td></tr>
    </table>
  </div>
  <div class="col-lg-6">
    <h3 class="h6 section-title">申込内容（現行版）<c:if test="${d.currentVersion.fixed}"> <span class="badge badge-secondary">確定版</span></c:if>
      <c:if test="${not empty d.currentVersion.amountRatio}"> <span class="badge badge-light">変更金額倍率 ${app:ratio(d.currentVersion.amountRatio)}</span></c:if></h3>
    <c:set var="cur" value="${d.currentVersion}"/>
    <c:set var="base" value="${d.showDiff ? d.baseVersion : null}"/>
    <c:set var="curLabel" value="現行版"/><c:set var="baseLabel" value="基準版"/>
    <%@ include file="/WEB-INF/views/common/version_view.jspf" %>
    <div class="small text-muted">確定日時：${app:datetime(cur.confirmedAt)}　複写元：${empty cur.copiedFromVersionNo ? '－' : '第 '.concat(cur.copiedFromVersionNo).concat(' 版')}</div>
  </div>
</div>

<h3 class="h6 section-title">承認状況（承認・申請履歴）</h3>
<c:if test="${empty d.approvalRequests}"><p class="text-muted small">承認申請はありません。</p></c:if>
<c:forEach var="r" items="${d.approvalRequests}">
  <div class="card mb-2">
    <div class="card-header py-1 small">
      <strong><c:out value="${r.approvalTypeName}"/></strong>　第 ${r.versionNo} 版　申請者：<c:out value="${r.requestEmployeeName}"/>　申請日時：${app:datetime(r.requestedAt)}
      　状態：<span class="badge badge-${r.requestStatus == '1' ? 'warning' : r.requestStatus == '2' ? 'success' : 'danger'}"><c:out value="${r.requestStatusName}"/></span>
      <c:if test="${not empty r.currentStepNo}">　ステップ ${r.currentStepNo}／${r.finalStepNo}</c:if>
      <c:if test="${not empty r.routeName}">　テンプレート：<c:out value="${r.routeName}"/></c:if>
      <c:if test="${not empty r.completedAt}">　完了日時：${app:datetime(r.completedAt)}</c:if>
    </div>
    <c:if test="${not empty r.steps}">
      <table class="table table-sm mb-0">
        <thead><tr><th style="width: 8%;">ステップ</th><th style="width: 22%;">承認者</th><th style="width: 12%;">結果</th><th>コメント</th><th style="width: 16%;">処理日時</th></tr></thead>
        <tbody>
          <c:forEach var="s" items="${r.steps}">
            <tr class="${r.inProgress and s.stepNo == r.currentStepNo ? 'table-warning' : ''}">
              <td>${s.stepNo}</td><td><c:out value="${s.approverName}"/></td><td><c:out value="${s.resultName}"/></td><td class="pre-wrap"><c:out value="${app:text(s.comment)}"/></td><td>${app:datetime(s.actedAt)}</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:if>
    <c:if test="${empty r.steps}"><div class="card-body py-1 small text-muted">回付先なし（承認を省略して申込者確認へ進みました）</div></c:if>
  </div>
</c:forEach>

<div class="row">
  <div class="col-lg-6">
    <h3 class="h6 section-title">版履歴</h3>
    <table class="table table-sm table-bordered">
      <thead class="thead-light"><tr><th>版</th><th>版種別</th><th>複写元</th><th class="text-right">合計</th><th>倍率</th><th>確定日時</th><th>状態</th></tr></thead>
      <tbody>
        <c:forEach var="v" items="${d.versions}">
          <tr class="${v.canceled ? 'text-muted' : ''}">
            <td>第 ${v.versionNo} 版</td><td><c:out value="${v.versionTypeName}"/></td><td>${empty v.copiedFromVersionNo ? '－' : v.copiedFromVersionNo}</td>
            <td class="text-right">${app:amount(v.totalAmount)}</td><td>${app:ratio(v.amountRatio)}</td><td>${app:datetime(v.confirmedAt)}</td>
            <td><c:if test="${v.fixed}"><span class="badge badge-secondary">確定版</span></c:if> <c:if test="${v.canceled}"><span class="badge badge-dark">取消</span></c:if> <c:if test="${v.versionNo == a.currentVersionNo}"><span class="badge badge-primary">現行</span></c:if></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
    <h3 class="h6 section-title">申込者同意</h3>
    <table class="table table-sm table-bordered">
      <thead class="thead-light"><tr><th>版</th><th>種別</th><th>状態</th><th>有効期限</th><th>内容確定</th><th>同意</th><th>差戻し</th></tr></thead>
      <tbody>
        <c:if test="${empty d.consents}"><tr><td colspan="7" class="text-muted">なし</td></tr></c:if>
        <c:forEach var="c" items="${d.consents}">
          <tr><td>第 ${c.versionNo} 版</td><td><c:out value="${c.consentTypeName}"/></td><td><c:out value="${c.consentStatusName}"/></td><td>${app:datetime(c.tokenExpiresAt)}</td><td>${app:datetime(c.contentConfirmedAt)}</td><td>${app:datetime(c.consentedAt)}</td>
            <td>${app:datetime(c.returnedAt)}<c:if test="${not empty c.returnReason}"><br><span class="text-danger small pre-wrap"><c:out value="${c.returnReason}"/></span></c:if></td></tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
  <div class="col-lg-6">
    <h3 class="h6 section-title">外部連携（審査担当部門システム）</h3>
    <table class="table table-sm table-bordered">
      <thead class="thead-light"><tr><th>ID</th><th>版</th><th>種別</th><th>送信</th><th>受付番号</th><th>結果</th></tr></thead>
      <tbody>
        <c:if test="${empty d.externalLinks}"><tr><td colspan="6" class="text-muted">なし</td></tr></c:if>
        <c:forEach var="l" items="${d.externalLinks}">
          <tr class="${l.sendStatus == '2' ? 'table-danger' : ''}">
            <td>${l.externalLinkId}</td><td>第 ${l.versionNo} 版</td><td><c:out value="${l.linkTypeName}"/></td>
            <td><c:out value="${l.sendStatusName}"/>（${l.retryCount} 回）<br><span class="small">${app:datetime(l.sentAt)}</span><c:if test="${not empty l.errorMessage}"><br><span class="small text-danger"><c:out value="${l.errorMessage}"/></span></c:if></td>
            <td class="small"><c:out value="${app:text(l.externalReceiptNo)}"/></td>
            <td><c:out value="${app:text(l.resultName)}"/><br><span class="small">${app:datetime(l.resultReceivedAt)}</span><c:if test="${not empty l.resultReason}"><br><span class="small text-danger pre-wrap"><c:out value="${l.resultReason}"/></span></c:if></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
    <h3 class="h6 section-title">ステータス履歴</h3>
    <table class="table table-sm table-bordered">
      <thead class="thead-light"><tr><th>日時</th><th>遷移</th><th>操作</th><th>操作者</th><th>版</th><th>コメント</th></tr></thead>
      <tbody>
        <c:forEach var="h" items="${d.histories}">
          <tr><td class="small">${app:datetime(h.changedAt)}</td><td class="small">${empty h.fromStatusCd ? '（新規）' : h.fromStatusCd} → ${h.toStatusCd}<c:if test="${not empty h.transitionId}"> <span class="text-muted">#${h.transitionId}</span></c:if></td>
            <td><c:out value="${h.actionName}"/></td><td class="small"><c:out value="${h.actorTypeName}"/>：<c:out value="${h.actorName}"/></td><td>${h.versionNo}</td><td class="small pre-wrap"><c:out value="${app:text(h.comment)}"/></td></tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
