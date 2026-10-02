<c:set var="pageTitle" value="SC13 承認ルートマスタ"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<a class="btn btn-success btn-sm mb-2" href="${ctx}/emp/master/approval-routes/new">新規登録</a>
<table class="table table-sm table-bordered bg-white">
  <thead class="thead-light"><tr><th>会社区分</th><th>部署コード</th><th>承認種別</th><th>ルート名</th><th>適用期間</th><th>承認者（順）</th><th></th></tr></thead>
  <tbody>
    <c:forEach var="r" items="${routes}">
      <tr>
        <td>${app:label('COMPANY_DIV', r.companyDiv)}</td><td><c:out value="${r.deptCd}"/></td><td>${app:label('APPROVAL_TYPE', r.approvalType)}</td><td><c:out value="${r.routeName}"/></td>
        <td>${app:date(r.validFrom)} 〜 ${empty r.validTo ? '無期限' : app:date(r.validTo)}</td>
        <td class="small"><c:forEach var="s" items="${r.steps}" varStatus="st">${st.index > 0 ? ' → ' : ''}<c:out value="${s.approverName}"/></c:forEach></td>
        <td class="text-nowrap"><a class="btn btn-outline-primary btn-sm" href="${ctx}/emp/master/approval-routes/${r.routeId}">編集</a>
          <form method="post" action="${ctx}/emp/master/approval-routes/delete" class="d-inline" data-confirm="このテンプレートを削除します。よろしいですか？"><input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="routeId" value="${r.routeId}"><button type="submit" class="btn btn-outline-danger btn-sm">削除</button></form></td>
      </tr>
    </c:forEach>
  </tbody>
</table>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
