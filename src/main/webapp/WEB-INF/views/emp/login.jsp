<c:set var="pageTitle" value="ログイン"/>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>ログイン | 申込管理システム</title>
<link rel="stylesheet" href="${ctx}/static/vendor/bootstrap.min.css">
</head>
<body class="bg-light">
<main class="container py-5" style="max-width: 480px;">
  <div class="card shadow-sm">
    <div class="card-body">
      <h1 class="h4 mb-3 text-center">申込管理システム</h1>
      <c:forEach var="m" items="${messages}"><div class="alert alert-${m.level}"><c:out value="${m.text}"/></div></c:forEach>
      <form method="post" action="${ctx}/emp/login">
        <input type="hidden" name="_csrf" value="${csrf}">
        <div class="form-group">
          <label for="employeeNo">社員番号</label>
          <input type="text" class="form-control" id="employeeNo" name="employeeNo" maxlength="10" value="<c:out value='${employeeNo}'/>" autofocus autocomplete="username">
        </div>
        <div class="form-group">
          <label for="password">パスワード</label>
          <input type="password" class="form-control" id="password" name="password" maxlength="64" autocomplete="current-password">
        </div>
        <button type="submit" class="btn btn-primary btn-block">ログイン</button>
      </form>
      <c:if test="${applicationScope.devTools}">
        <div class="text-muted small mt-3">
          サンプル社員（パスワードはすべて <code>password</code>）：A001 担当者（会社A）、A002／A003 承認者、A009 管理者、B001 担当者（会社B）、B002／B003 承認者
        </div>
      </c:if>
    </div>
  </div>
</main>
</body>
</html>
