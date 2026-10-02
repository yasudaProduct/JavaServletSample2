<%@ page isErrorPage="true" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pageTitle" value="システムエラー"/>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>システムエラー | 申込管理システム</title>
<link rel="stylesheet" href="${ctx}/static/vendor/bootstrap.min.css">
</head>
<body class="bg-light">
<main class="container py-5" style="max-width: 720px;">
  <div class="card border-danger">
    <div class="card-header bg-danger text-white">CM01 システムエラー</div>
    <div class="card-body">
      <p class="mb-2">E901 システムエラーが発生しました。管理者に連絡してください。</p>
      <p class="text-muted small mb-2">発生日時：<%= new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date()) %>　エラー ID：<c:out value="${requestScope.errorId}"/></p>
      <c:if test="${not empty sessionScope.loginUser}"><a class="btn btn-outline-secondary btn-sm" href="${ctx}/emp/applications">申込一覧へ</a></c:if>
    </div>
  </div>
</main>
</body>
</html>
