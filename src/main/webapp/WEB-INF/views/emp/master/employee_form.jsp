<c:set var="pageTitle" value="SC11 社員マスタ（${e.employeeId == 0 ? '新規登録' : '編集'}）"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<form method="post" action="${ctx}/emp/master/employees" style="max-width: 640px;">
  <input type="hidden" name="_csrf" value="${csrf}">
  <input type="hidden" name="employeeId" value="${e.employeeId}">
  <input type="hidden" name="rowVersion" value="${e.rowVersion}">
  <div class="form-group"><label>社員番号 <span class="badge badge-danger">必須</span></label>
    <input type="text" class="form-control ${not empty errors.employeeNo ? 'is-invalid' : ''}" name="employeeNo" maxlength="10" value="<c:out value='${e.employeeNo}'/>" ${e.employeeId == 0 ? '' : 'readonly'}><div class="invalid-feedback"><c:out value="${errors.employeeNo}"/></div></div>
  <div class="form-group"><label>氏名 <span class="badge badge-danger">必須</span></label>
    <input type="text" class="form-control ${not empty errors.employeeName ? 'is-invalid' : ''}" name="employeeName" maxlength="50" value="<c:out value='${e.employeeName}'/>"><div class="invalid-feedback"><c:out value="${errors.employeeName}"/></div></div>
  <div class="form-group"><label>パスワード ${e.employeeId == 0 ? '<span class="badge badge-danger">必須</span>' : '（空なら変更しない）'}</label>
    <input type="password" class="form-control ${not empty errors.password ? 'is-invalid' : ''}" name="password" maxlength="64" autocomplete="new-password"><div class="invalid-feedback"><c:out value="${errors.password}"/></div></div>
  <div class="form-row">
    <div class="form-group col-md-4"><label>会社区分</label>
      <select class="form-control ${not empty errors.companyDiv ? 'is-invalid' : ''}" id="companyDiv" name="companyDiv"><c:forEach var="d" items="${companyDivs}"><option value="${d.companyDiv}" ${e.companyDiv == d.companyDiv ? 'selected' : ''}><c:out value="${d.companyDivName}"/></option></c:forEach></select><div class="invalid-feedback"><c:out value="${errors.companyDiv}"/></div></div>
    <div class="form-group col-md-4"><label>部署 <span class="badge badge-danger">必須</span></label>
      <select class="form-control ${not empty errors.deptCd ? 'is-invalid' : ''}" id="deptCd" name="deptCd" data-company-source="companyDiv"><c:forEach var="dp" items="${departments}"><c:if test="${dp.valid or (dp.companyDiv == e.companyDiv and dp.deptCd == e.deptCd)}"><option value="${dp.deptCd}" data-company="${dp.companyDiv}" ${dp.companyDiv == e.companyDiv and dp.deptCd == e.deptCd ? 'selected' : ''}><c:out value="${dp.deptName}"/>（<c:out value="${dp.deptCd}"/>）${dp.valid ? '' : '（無効）'}</option></c:if></c:forEach></select><div class="invalid-feedback"><c:out value="${errors.deptCd}"/></div></div>
    <div class="form-group col-md-4"><label>権限</label>
      <select class="form-control ${not empty errors.roleCd ? 'is-invalid' : ''}" name="roleCd"><option value="01" ${e.roleCd == '01' ? 'selected' : ''}>01 担当者</option><option value="02" ${e.roleCd == '02' ? 'selected' : ''}>02 承認者</option><option value="09" ${e.roleCd == '09' ? 'selected' : ''}>09 管理者</option></select><div class="invalid-feedback"><c:out value="${errors.roleCd}"/></div></div>
  </div>
  <div class="form-group"><label>メールアドレス <span class="badge badge-danger">必須</span></label>
    <input type="text" class="form-control ${not empty errors.mailAddress ? 'is-invalid' : ''}" name="mailAddress" maxlength="254" value="<c:out value='${e.mailAddress}'/>"><div class="invalid-feedback"><c:out value="${errors.mailAddress}"/></div></div>
  <div class="form-group"><div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="validFlg" name="validFlg" value="1" ${e.valid ? 'checked' : ''}><label class="custom-control-label" for="validFlg">有効</label></div></div>
  <button type="submit" class="btn btn-primary mr-2">保存</button>
  <a class="btn btn-outline-secondary" href="${ctx}/emp/master/employees">戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
