<c:set var="pageTitle" value="開発支援：通知一覧（メール）"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="alert alert-secondary small">
  通知（T_NOTIFICATION）の直近 50 件です。mail.mode=<code><c:out value="${mailMode}"/></code>。
  <c:if test="${mailMode == 'log'}">ログ出力モードのため実際には送信しません。</c:if>
  申込者確認依頼（通知 01）の本文には確認用 URL が含まれるので、ここから申込者向け画面を開けます（本番では表示しない情報です）。
  <form method="post" action="${ctx}/emp/dev/batch/run" class="d-inline"><input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="job" value="BT01"><input type="hidden" name="back" value="/emp/dev/notifications">
    <button type="submit" class="btn btn-outline-primary btn-sm ml-2">BT01 通知送信バッチを今すぐ実行</button></form>
</div>
<c:if test="${empty notifications}"><p class="text-muted">通知はありません。</p></c:if>
<c:forEach var="n" items="${notifications}">
  <div class="card mb-2">
    <div class="card-header py-1 small">
      #${n.notificationId}　<strong>${n.notificationType} <c:out value="${n.notificationTypeName}"/></strong>　申込番号 <a href="${ctx}/emp/applications/${n.applicationId}"><c:out value="${n.applicationNo}"/></a>　宛先 <c:out value="${n.toAddress}"/>　
      <span class="badge badge-${n.sendStatus == '1' ? 'success' : n.sendStatus == '2' ? 'danger' : 'secondary'}"><c:out value="${n.sendStatusName}"/></span>　登録 ${app:datetime(n.createdAt)}　送信 ${app:datetime(n.sentAt)}
      <c:if test="${not empty n.errorMessage}"><span class="text-danger"><c:out value="${n.errorMessage}"/></span></c:if>
    </div>
    <div class="card-body py-2">
      <div class="small font-weight-bold mb-1"><c:out value="${n.subject}"/></div>
      <div class="mail-body"><c:set var="body" value="${fn:escapeXml(n.body)}"/>${fn:replace(body, 'URL：'.concat(fn:escapeXml(ctx)), 'URL：'.concat(ctx))}</div>
      <c:set var="idx" value="${fn:indexOf(n.body, '/consent/')}"/>
      <c:if test="${idx >= 0}">
        <c:set var="rest" value="${fn:substring(n.body, idx, fn:length(n.body))}"/>
        <c:set var="line" value="${fn:substringBefore(rest, '
')}"/>
        <a class="btn btn-outline-success btn-sm mt-2" href="${ctx}${fn:trim(line)}" target="_blank">申込者向け画面を開く（${ctx}<c:out value="${fn:trim(line)}"/>）</a>
      </c:if>
    </div>
  </div>
</c:forEach>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
