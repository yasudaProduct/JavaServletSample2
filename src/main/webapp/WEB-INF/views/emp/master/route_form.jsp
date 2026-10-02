<c:set var="pageTitle" value="SC13 承認ルートマスタ（${r.routeId == 0 ? '新規登録' : '編集'}）"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<form method="post" action="${ctx}/emp/master/approval-routes" style="max-width: 760px;">
  <input type="hidden" name="_csrf" value="${csrf}">
  <input type="hidden" name="routeId" value="${r.routeId}">
  <input type="hidden" name="rowVersion" value="${r.rowVersion}">
  <div class="form-row">
    <div class="form-group col-md-3"><label>会社区分</label>
      <select class="form-control" name="companyDiv"><c:forEach var="d" items="${companyDivs}"><option value="${d.companyDiv}" ${r.companyDiv == d.companyDiv ? 'selected' : ''}><c:out value="${d.companyDivName}"/></option></c:forEach></select></div>
    <div class="form-group col-md-3"><label>部署コード <span class="badge badge-danger">必須</span></label>
      <input type="text" class="form-control ${not empty errors.deptCd ? 'is-invalid' : ''}" name="deptCd" maxlength="10" value="<c:out value='${r.deptCd}'/>"><div class="invalid-feedback"><c:out value="${errors.deptCd}"/></div></div>
    <div class="form-group col-md-6"><label>承認種別</label>
      <select class="form-control" name="approvalType"><c:forEach var="t" items="${['01','02','03','04']}"><option value="${t}" ${r.approvalType == t ? 'selected' : ''}>${t} ${app:label('APPROVAL_TYPE', t)}</option></c:forEach></select></div>
  </div>
  <div class="form-group"><label>ルート名 <span class="badge badge-danger">必須</span></label>
    <input type="text" class="form-control ${not empty errors.routeName ? 'is-invalid' : ''}" name="routeName" maxlength="50" value="<c:out value='${r.routeName}'/>"><div class="invalid-feedback"><c:out value="${errors.routeName}"/></div></div>
  <div class="form-row">
    <div class="form-group col-md-3"><label>適用開始日 <span class="badge badge-danger">必須</span></label>
      <input type="text" class="form-control ${not empty errors.validFrom ? 'is-invalid' : ''}" name="validFrom" placeholder="yyyy/MM/dd" value="${app:date(r.validFrom) == '－' ? '' : app:date(r.validFrom)}"><div class="invalid-feedback"><c:out value="${errors.validFrom}"/></div></div>
    <div class="form-group col-md-3"><label>適用終了日（空は無期限）</label>
      <input type="text" class="form-control ${not empty errors.validTo ? 'is-invalid' : ''}" name="validTo" placeholder="yyyy/MM/dd" value="${app:date(r.validTo) == '－' ? '' : app:date(r.validTo)}"><div class="invalid-feedback"><c:out value="${errors.validTo}"/></div></div>
  </div>
  <div class="form-group">
    <label>承認者（ステップ順。会社区分が同じ有効な承認者を選択）</label>
    <c:if test="${not empty errors.approverId}"><div class="text-danger small"><c:out value="${errors.approverId}"/></div></c:if>
    <c:forEach var="i" begin="0" end="4">
      <select class="form-control form-control-sm mb-1" name="approverId">
        <option value="">（ステップ ${i + 1}：なし）</option>
        <c:forEach var="e" items="${candidates}"><option value="${e.employeeId}" ${not empty r.steps[i] and r.steps[i].approverEmployeeId == e.employeeId ? 'selected' : ''}>${app:label('COMPANY_DIV', e.companyDiv)}：<c:out value="${e.employeeName}"/>（<c:out value="${e.deptCd}"/>）</option></c:forEach>
      </select>
    </c:forEach>
  </div>
  <button type="submit" class="btn btn-primary mr-2">保存</button>
  <a class="btn btn-outline-secondary" href="${ctx}/emp/master/approval-routes">戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
