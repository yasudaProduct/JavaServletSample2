<c:set var="pageTitle" value="SC15 メンテナンス"/>
<c:set var="a" value="${d.application}"/>
<c:set var="ap" value="${d.applicant}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="card mb-3"><div class="card-body py-2 small">
  <strong>申込番号：</strong><c:out value="${a.applicationNo}"/>　<strong>ステータス：</strong><c:out value="${d.status.displayName}"/>　<strong>担当社員：</strong><c:out value="${d.owner.employeeName}"/>　
  <strong>申込者：</strong><c:out value="${ap.applicantName}"/>（<c:out value="${empty ap.applicantNo ? 'アカウント未発行' : ap.applicantNo}"/>）　<strong>メールアドレス：</strong><c:out value="${ap.mailAddress}"/>
</div></div>
<p class="small text-muted">申込者名・メールアドレスなどの申込者情報は申込データです。変更は申込内容の入力・修正（全体修正、契約変更）で行います。ここでは申込者ページへのログインに必要なデータ（申込者アカウント）を扱います。</p>
<div class="row">
  <div class="col-lg-5">
    <h3 class="h6 section-title">申込者アカウント（ログイン用のデータ）</h3>
    <table class="table table-sm table-bordered" id="accountTable">
      <tr><th style="width: 40%;">ユーザー ID</th><td><c:out value="${empty account ? '－' : account.applicantNo}"/></td></tr>
      <tr><th>アカウント</th><td>${empty account ? '未発行（一次承認が通って申込内容確認待ちになったときに発行）' : '発行済み'}</td></tr>
      <tr><th>発行日時</th><td>${empty account ? '－' : app:datetime(account.accountIssuedAt)}</td></tr>
      <tr><th>パスワード変更日時</th><td>${empty account ? '－' : (account.initialPassword ? '（初期パスワードのまま）' : app:datetime(account.passwordChangedAt))}</td></tr>
    </table>
    <form method="post" action="${ctx}/emp/applications/${a.applicationId}/maintenance/resetPassword" data-confirm="申込者ページのパスワードを初期化します。現在のパスワードは使えなくなり、新しい初期パスワードを申込者へメールで通知します。よろしいですか？">
      <input type="hidden" name="_csrf" value="${csrf}">
      <button type="submit" class="btn btn-warning" id="resetPasswordBtn" ${canReset ? '' : 'disabled'}>パスワードの初期化</button>
      <p class="small text-muted mt-2 mb-0">${canReset ? '新しい初期パスワードを発行し、パスワード初期化通知（通知 09）で申込者へ連携します。申込者は初回ログイン後に「パスワード変更」で変更します。' : 'アカウント未発行のため初期化できません。'}</p>
    </form>
  </div>
  <div class="col-lg-7">
    <h3 class="h6 section-title">申込者への通知の履歴</h3>
    <c:if test="${empty notices}"><p class="text-muted small">この申込の申込者宛の通知はまだありません。</p></c:if>
    <c:forEach var="n" items="${notices}">
      <div class="card mb-2 notice-card">
        <div class="card-header py-1 small">
          <strong>${n.notificationType} <c:out value="${n.notificationTypeName}"/></strong>　宛先 <c:out value="${n.toAddress}"/>　
          <span class="badge badge-${n.sendStatus == '1' ? 'success' : n.sendStatus == '2' ? 'danger' : 'secondary'}"><c:out value="${n.sendStatusName}"/></span>　登録 ${app:datetime(n.createdAt)}　送信 ${app:datetime(n.sentAt)}
          <c:if test="${not empty n.errorMessage}"><span class="text-danger"><c:out value="${n.errorMessage}"/></span></c:if>
        </div>
        <div class="card-body py-2">
          <div class="small font-weight-bold mb-1"><c:out value="${n.subject}"/></div>
          <details><summary class="small text-muted">本文を表示</summary><div class="mail-body"><c:out value="${n.body}"/></div></details>
        </div>
      </div>
    </c:forEach>
  </div>
</div>
<a class="btn btn-outline-secondary" href="${ctx}/emp/applications/${a.applicationId}/menu">メニューへ戻る</a>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
