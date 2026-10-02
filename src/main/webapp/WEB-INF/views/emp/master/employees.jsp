<c:set var="pageTitle" value="SC11 社員マスタ"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<a class="btn btn-success btn-sm mb-2" href="${ctx}/emp/master/employees/new">新規登録</a>
<table class="table table-sm table-bordered bg-white">
  <thead class="thead-light"><tr><th>社員番号</th><th>氏名</th><th>会社区分</th><th>部署コード</th><th>権限</th><th>メールアドレス</th><th>有効</th><th></th></tr></thead>
  <tbody>
    <c:forEach var="e" items="${employees}">
      <tr class="${e.valid ? '' : 'text-muted'}">
        <td><c:out value="${e.employeeNo}"/></td><td><c:out value="${e.employeeName}"/></td><td>${app:label('COMPANY_DIV', e.companyDiv)}</td><td><c:out value="${e.deptCd}"/></td><td><c:out value="${e.roleName}"/></td><td><c:out value="${e.mailAddress}"/></td>
        <td>${e.valid ? '有効' : '無効'}</td>
        <td class="text-nowrap">
          <a class="btn btn-outline-primary btn-sm" href="${ctx}/emp/master/employees/${e.employeeId}">編集</a>
          <form method="post" action="${ctx}/emp/master/employees/toggle" class="d-inline" data-confirm="${e.valid ? '無効化' : '有効化'}します。よろしいですか？"><input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="employeeId" value="${e.employeeId}"><input type="hidden" name="rowVersion" value="${e.rowVersion}">
            <button type="submit" class="btn btn-outline-${e.valid ? 'danger' : 'success'} btn-sm">${e.valid ? '無効化' : '有効化'}</button></form>
        </td>
      </tr>
    </c:forEach>
  </tbody>
</table>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
