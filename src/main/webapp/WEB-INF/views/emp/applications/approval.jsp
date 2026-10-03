<c:set var="pageTitle" value="SC06 承認フロー"/>
<c:set var="a" value="${d.application}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="card mb-3"><div class="card-body py-2 small">
  <strong>申込番号：</strong><c:out value="${a.applicationNo}"/>　<strong>申込者：</strong><c:out value="${d.applicant.applicantName}"/>　<strong>ステータス：</strong><c:out value="${d.status.displayName}"/>　
  <strong>担当社員：</strong><c:out value="${d.owner.employeeName}"/>　<strong>承認種別：</strong><c:out value="${v.approvalTypeName}"/>　<strong>モード：</strong>${v.modeName == 'WAIT' ? '申請待ち' : v.modeName == 'IN_PROGRESS' ? '申請中（承認者）' : '申請中（参照）'}
</div></div>
<div class="row">
  <div class="col-lg-6">
    <h3 class="h6 section-title">申込内容（現行版）</h3>
    <c:set var="cur" value="${d.currentVersion}"/><c:set var="base" value="${d.showDiff ? d.baseVersion : null}"/><c:set var="curLabel" value="現行版"/><c:set var="baseLabel" value="基準版"/>
    <%@ include file="/WEB-INF/views/common/version_view.jspf" %>
  </div>
  <div class="col-lg-6">
    <c:choose>
      <c:when test="${v.modeName == 'WAIT'}">
        <h3 class="h6 section-title">回付先の設定</h3>
        <p class="small text-muted mb-2">初期値：<c:out value="${v.routeSource}"/>　／　テンプレート：<c:out value="${empty v.template ? '（テンプレートなし）' : v.template.routeName}"/><c:if test="${not empty v.template}">（適用開始日 ${app:date(v.template.validFrom)}）</c:if>　／　設定できる階層は ${maxSteps} までです。
          <c:if test="${v.finalApproval}"><br><span class="text-danger">最終承認は回付先を 1 人以上設定してください。最終承認者の操作は「審査申請」になります。</span></c:if>
          <c:if test="${not v.finalApproval}"><br>一次承認は回付先なしで申請でき、その場合は承認を省略して申込者確認へ進みます。</c:if></p>
        <form method="post" action="${ctx}/emp/applications/${a.applicationId}/approval" id="applyForm">
          <input type="hidden" name="_csrf" value="${csrf}">
          <input type="hidden" name="rowVersion" value="${a.rowVersion}">
          <input type="hidden" name="routeId" value="${empty v.template ? '' : v.template.routeId}">
          <input type="hidden" name="templateUsed" id="templateUsed" value="0">
          <table class="table table-sm table-bordered route-table">
            <thead class="thead-light"><tr><th style="width: 12%;">ステップ</th><th>承認者（部署）</th><th style="width: 36%;"></th></tr></thead>
            <tbody id="routeRows" data-max="${maxSteps}"></tbody>
          </table>
          <div class="form-inline mb-2">
            <select class="form-control form-control-sm mr-2" id="candidateSelect">
              <option value="">承認者を選択</option>
              <c:forEach var="e" items="${v.candidates}"><option value="${e.employeeId}"><c:out value="${e.employeeName}"/>（<c:out value="${e.deptCd}"/>）</option></c:forEach>
            </select>
            <button type="button" class="btn btn-outline-secondary btn-sm mr-2" id="addRoute">行追加</button>
            <button type="button" class="btn btn-outline-secondary btn-sm" id="resetRoute">テンプレートに戻す</button>
          </div>
          <p class="small">最終承認者：<strong id="finalApproverName">－</strong></p>
          <button type="submit" name="action" value="apply" class="btn btn-success mr-2">申請</button>
          <a class="btn btn-outline-secondary" href="${ctx}/emp/applications/${a.applicationId}/menu">戻る</a>
        </form>
        <script type="application/json" id="routeCandidates">[<c:forEach var="e" items="${v.candidates}" varStatus="st">{"id":${e.employeeId},"name":"${fn:escapeXml(e.employeeName)}","dept":"${fn:escapeXml(e.deptCd)}"}${st.last ? '' : ','}</c:forEach>]</script>
        <script type="application/json" id="routeInitial">[<c:forEach var="id" items="${initialApproverIds}" varStatus="st">${id}${st.last ? '' : ','}</c:forEach>]</script>
      </c:when>
      <c:otherwise>
        <h3 class="h6 section-title">承認申請（進行中）</h3>
        <c:set var="r" value="${v.activeRequest}"/>
        <p class="small mb-2">申請者：<c:out value="${r.requestEmployeeName}"/>　申請日時：${app:datetime(r.requestedAt)}　現在ステップ：${r.currentStepNo}／${r.finalStepNo}</p>
        <table class="table table-sm table-bordered">
          <thead class="thead-light"><tr><th>ステップ</th><th>承認者</th><th>結果</th><th>コメント</th><th>処理日時</th></tr></thead>
          <tbody>
            <c:forEach var="s" items="${r.steps}">
              <tr class="${s.stepNo == r.currentStepNo ? 'table-warning' : ''}"><td>${s.stepNo}</td><td><c:out value="${s.approverName}"/></td><td><c:out value="${s.resultName}"/></td><td class="pre-wrap"><c:out value="${app:text(s.comment)}"/></td><td>${app:datetime(s.actedAt)}</td></tr>
            </c:forEach>
          </tbody>
        </table>
        <c:if test="${v.canOperate}">
          <form method="post" action="${ctx}/emp/applications/${a.applicationId}/approval">
            <input type="hidden" name="_csrf" value="${csrf}">
            <input type="hidden" name="rowVersion" value="${a.rowVersion}">
            <input type="hidden" name="approvalRequestId" value="${r.approvalRequestId}">
            <div class="form-group">
              <label for="comment">コメント（差戻し時は必須）</label>
              <textarea class="form-control" id="comment" name="comment" rows="3" maxlength="500"></textarea>
            </div>
            <c:choose>
              <c:when test="${v.finalApproval and v.currentIsFinalStep}">
                <button type="submit" name="action" value="review" class="btn btn-success mr-2">審査申請（審査担当部門へ）</button>
              </c:when>
              <c:otherwise>
                <button type="submit" name="action" value="approve" class="btn btn-success mr-2">承認</button>
              </c:otherwise>
            </c:choose>
            <button type="submit" name="action" value="return" class="btn btn-warning mr-2">差戻し</button>
            <a class="btn btn-outline-secondary" href="${ctx}/emp/applications/${a.applicationId}/menu">戻る</a>
          </form>
        </c:if>
        <c:if test="${not v.canOperate}">
          <p class="text-muted small">現在ステップの承認者ではないため参照のみです。</p>
          <a class="btn btn-outline-secondary" href="${ctx}/emp/applications/${a.applicationId}/menu">戻る</a>
        </c:if>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<h3 class="h6 section-title">承認・申請履歴</h3>
<c:if test="${empty v.history}"><p class="text-muted small">この申込の承認申請はまだありません。</p></c:if>
<c:if test="${not empty v.history}">
<table class="table table-sm table-bordered">
  <thead class="thead-light"><tr><th>日時</th><th>操作</th><th>操作者</th><th>対象版</th><th>結果・コメント</th></tr></thead>
  <tbody>
    <c:forEach var="r" items="${v.history}">
      <tr><td class="small">${app:datetime(r.requestedAt)}</td><td><c:out value="${r.approvalTypeName}"/> 申請</td><td><c:out value="${r.requestEmployeeName}"/></td><td>第 ${r.versionNo} 版</td>
        <td class="small">${empty r.steps ? '回付先なし（承認済として記録）' : '回付先 '.concat(r.finalStepNo).concat(' 名')}<c:if test="${not empty r.routeName}">　テンプレート：<c:out value="${r.routeName}"/></c:if></td></tr>
      <c:forEach var="s" items="${r.steps}">
        <c:if test="${s.resultCd != '0'}">
          <tr><td class="small">${app:datetime(s.actedAt)}</td><td><c:out value="${s.resultName}"/>（ステップ ${s.stepNo}）</td><td><c:out value="${s.approverName}"/></td><td>第 ${r.versionNo} 版</td><td class="small pre-wrap"><c:out value="${app:text(s.comment)}"/></td></tr>
        </c:if>
        <c:if test="${s.resultCd == '0'}">
          <tr class="text-muted"><td class="small">－</td><td>未処理（ステップ ${s.stepNo}）</td><td><c:out value="${s.approverName}"/></td><td>第 ${r.versionNo} 版</td><td class="small">${r.requestStatus == '3' ? '差戻しにより終了' : r.requestStatus == '1' and s.stepNo == r.currentStepNo ? '操作待ち' : '－'}</td></tr>
        </c:if>
      </c:forEach>
    </c:forEach>
  </tbody>
</table>
</c:if>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
