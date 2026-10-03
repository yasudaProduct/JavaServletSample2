<c:set var="pageTitle" value="申込者ログイン"/>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>申込者ログイン | 申込管理システム</title>
<link rel="stylesheet" href="${ctx}/static/vendor/bootstrap.min.css">
</head>
<body class="bg-light">
<nav class="navbar navbar-dark bg-primary"><span class="navbar-brand mb-0 h1">申込管理システム</span></nav>
<main class="container py-5" style="max-width: 480px;">
  <div class="card shadow-sm">
    <div class="card-body">
      <h1 class="h4 mb-3 text-center">申込者ページ ログイン</h1>
      <c:forEach var="m" items="${messages}"><div class="alert alert-${m.level}"><c:out value="${m.text}"/></div></c:forEach>
      <form method="post" action="${ctx}/my/login">
        <input type="hidden" name="_csrf" value="${csrf}">
        <div class="form-group">
          <label for="loginId">ユーザー ID（申込者番号）</label>
          <input type="text" class="form-control" id="loginId" name="loginId" maxlength="12" value="<c:out value='${loginId}'/>" autofocus autocomplete="username">
        </div>
        <div class="form-group">
          <label for="password">パスワード</label>
          <input type="password" class="form-control" id="password" name="password" maxlength="64" autocomplete="current-password">
        </div>
        <button type="submit" class="btn btn-primary btn-block">ログイン</button>
      </form>
      <p class="text-muted small mt-3 mb-0">ユーザー ID と初期パスワードは、お申込の一次承認が完了したときにメールでお知らせしています。</p>
    </div>
  </div>
</main>
</body>
</html>
