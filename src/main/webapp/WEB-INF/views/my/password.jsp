<c:set var="pageTitle" value="パスワード変更"/>
<c:set var="portal" value="${true}"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<form method="post" action="${ctx}/my/password" style="max-width: 480px;">
  <input type="hidden" name="_csrf" value="${csrf}">
  <div class="form-group"><label for="currentPassword">現在のパスワード</label><input type="password" class="form-control" id="currentPassword" name="currentPassword" maxlength="64" autocomplete="current-password"></div>
  <div class="form-group"><label for="newPassword">新しいパスワード（8〜64 桁）</label><input type="password" class="form-control" id="newPassword" name="newPassword" maxlength="64" autocomplete="new-password"></div>
  <div class="form-group"><label for="confirmPassword">新しいパスワード（確認）</label><input type="password" class="form-control" id="confirmPassword" name="confirmPassword" maxlength="64" autocomplete="new-password"></div>
  <button type="submit" class="btn btn-primary mr-2">変更する</button>
  <a class="btn btn-outline-secondary" href="${ctx}/my/menu">メニューへ戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
