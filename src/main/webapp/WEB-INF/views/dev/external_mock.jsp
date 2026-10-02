<c:set var="pageTitle" value="開発支援：審査担当部門システム（モック）"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="alert alert-secondary small">
  外部連携（T_EXTERNAL_LINK）の送信は BT02 が行います（送信先：<c:out value="${clientDescription}"/>）。送信済で結果待ちの依頼に対し、審査担当部門システムの代わりに結果を返します。
  処理は IF02／IF04（<code>POST /api/external/precheck-result</code>、<code>/api/external/review-result</code>）と同じサービスを通ります。
  <form method="post" action="${ctx}/emp/dev/batch/run" class="d-inline"><input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="job" value="BT02">
    <button type="submit" class="btn btn-outline-primary btn-sm ml-2">BT02 外部連携送信バッチを今すぐ実行</button></form>
</div>
<h3 class="h6 section-title">結果待ちの依頼</h3>
<c:if test="${empty awaiting}"><p class="text-muted small">結果待ちの依頼はありません（未送信の依頼がある場合は BT02 を実行してください）。</p></c:if>
<c:forEach var="l" items="${awaiting}">
  <div class="card mb-2">
    <div class="card-body py-2">
      <form method="post" action="${ctx}/emp/dev/external-mock/result" class="form-inline">
        <input type="hidden" name="_csrf" value="${csrf}">
        <input type="hidden" name="externalLinkId" value="${l.externalLinkId}">
        <span class="mr-3 small">#${l.externalLinkId}　<strong><c:out value="${l.linkTypeName}"/></strong>　申込番号 <a href="${ctx}/emp/applications/${l.applicationId}"><c:out value="${l.applicationNo}"/></a>　第 ${l.versionNo} 版　受付番号 <c:out value="${l.externalReceiptNo}"/>　送信 ${app:datetime(l.sentAt)}</span>
        <select class="form-control form-control-sm mr-2" name="result">
          <c:choose>
            <c:when test="${l.precheck}"><option value="OK">OK（問題なし）</option><option value="NG">NG（修正必要）</option></c:when>
            <c:otherwise><option value="COMPLETED">COMPLETED（審査完了）</option><option value="RETURNED">RETURNED（審査差戻し）</option></c:otherwise>
          </c:choose>
        </select>
        <input type="text" class="form-control form-control-sm mr-2" name="reason" placeholder="指摘内容・差戻し理由（NG／RETURNED 時は必須）" style="width: 320px;" maxlength="1000">
        <button type="submit" class="btn btn-primary btn-sm">結果を返す</button>
      </form>
    </div>
  </div>
</c:forEach>
<h3 class="h6 section-title">外部連携の直近 30 件</h3>
<table class="table table-sm table-bordered">
  <thead class="thead-light"><tr><th>ID</th><th>申込番号</th><th>版</th><th>種別</th><th>送信状態</th><th>受付番号</th><th>結果</th><th>理由・エラー</th></tr></thead>
  <tbody>
    <c:forEach var="l" items="${recent}">
      <tr><td>${l.externalLinkId}</td><td><a href="${ctx}/emp/applications/${l.applicationId}"><c:out value="${l.applicationNo}"/></a></td><td>${l.versionNo}</td><td><c:out value="${l.linkTypeName}"/></td>
        <td><c:out value="${l.sendStatusName}"/> ${app:datetime(l.sentAt)}</td><td class="small"><c:out value="${app:text(l.externalReceiptNo)}"/></td><td><c:out value="${app:text(l.resultName)}"/> ${app:datetime(l.resultReceivedAt)}</td>
        <td class="small"><c:out value="${app:text(l.resultReason)}"/> <span class="text-danger"><c:out value="${l.errorMessage}"/></span></td></tr>
    </c:forEach>
  </tbody>
</table>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
