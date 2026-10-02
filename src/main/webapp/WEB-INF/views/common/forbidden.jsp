<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="ja">
<head><meta charset="UTF-8"><title>不正なリクエスト | 申込管理システム</title><link rel="stylesheet" href="${ctx}/static/vendor/bootstrap.min.css"></head>
<body class="bg-light"><main class="container py-5" style="max-width: 720px;">
<div class="alert alert-danger"><c:out value="${requestScope.errorMessage}"/></div>
<a class="btn btn-outline-secondary btn-sm" href="${ctx}/emp/applications">申込一覧へ</a>
</main></body></html>
