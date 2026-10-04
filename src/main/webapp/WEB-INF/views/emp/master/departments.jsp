<c:set var="pageTitle" value="SC16 部署マスタ"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<p class="small text-muted">会社 &gt; 部署 &gt; 担当者の「部署」です。申込の担当部署、社員の所属部署、承認ルートの部署はここから選びます。部署コードは会社ごとに一意で、登録後は変更できません。使わなくなった部署は削除せずに無効にします（無効にした部署は新しく選べませんが、設定済みの申込・社員・承認ルートはそのまま使えます）。</p>
<div class="card mb-3" style="max-width: 860px;">
  <div class="card-header py-2">新規登録</div>
  <div class="card-body py-2">
    <form method="post" action="${ctx}/emp/master/departments" id="newDepartment">
      <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="mode" value="new">
      <div class="form-row align-items-start">
        <div class="form-group col-md-3 mb-2"><label class="small mb-0">会社 <span class="badge badge-danger">必須</span></label>
          <select class="form-control form-control-sm ${not empty errors.companyDiv ? 'is-invalid' : ''}" name="companyDiv"><c:forEach var="c" items="${companyDivs}"><option value="${c.companyDiv}" ${form.companyDiv == c.companyDiv ? 'selected' : ''}><c:out value="${c.companyDivName}"/></option></c:forEach></select>
          <div class="invalid-feedback"><c:out value="${errors.companyDiv}"/></div></div>
        <div class="form-group col-md-3 mb-2"><label class="small mb-0">部署コード <span class="badge badge-danger">必須</span></label>
          <input type="text" class="form-control form-control-sm ${not empty errors.deptCd ? 'is-invalid' : ''}" name="deptCd" maxlength="10" value="<c:out value='${form.deptCd}'/>" placeholder="半角英数字 10 桁以内">
          <div class="invalid-feedback"><c:out value="${errors.deptCd}"/></div></div>
        <div class="form-group col-md-4 mb-2"><label class="small mb-0">部署名 <span class="badge badge-danger">必須</span></label>
          <input type="text" class="form-control form-control-sm ${not empty errors.deptName ? 'is-invalid' : ''}" name="deptName" maxlength="50" value="<c:out value='${form.deptName}'/>">
          <div class="invalid-feedback"><c:out value="${errors.deptName}"/></div></div>
        <div class="form-group col-md-2 mb-2 d-flex align-items-end" style="min-height: 52px;"><button type="submit" class="btn btn-success btn-sm">登録</button></div>
      </div>
    </form>
  </div>
</div>
<table class="table table-sm table-bordered bg-white" style="max-width: 860px;" id="departmentTable">
  <thead class="thead-light"><tr><th>会社</th><th>部署コード</th><th>部署名</th><th>有効</th><th></th></tr></thead>
  <tbody>
    <c:forEach var="d" items="${departments}">
      <c:set var="k" value="${d.companyDiv}:${d.deptCd}:"/>
      <tr class="${d.valid ? '' : 'text-muted'}">
        <form method="post" action="${ctx}/emp/master/departments">
          <td><c:out value="${d.companyDivName}"/></td>
          <td><c:out value="${d.deptCd}"/></td>
          <td><input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="mode" value="edit"><input type="hidden" name="companyDiv" value="${d.companyDiv}"><input type="hidden" name="deptCd" value="<c:out value='${d.deptCd}'/>"><input type="hidden" name="rowVersion" value="${d.rowVersion}">
            <input type="text" class="form-control form-control-sm ${not empty errors[k.concat('deptName')] ? 'is-invalid' : ''}" name="deptName" maxlength="50" value="<c:out value='${d.deptName}'/>"><div class="invalid-feedback"><c:out value="${errors[k.concat('deptName')]}"/></div></td>
          <td><div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="valid_${d.companyDiv}_${d.deptCd}" name="validFlg" value="1" ${d.valid ? 'checked' : ''}><label class="custom-control-label" for="valid_${d.companyDiv}_${d.deptCd}">有効</label></div></td>
          <td><button type="submit" class="btn btn-outline-primary btn-sm">保存</button></td>
        </form>
      </tr>
    </c:forEach>
  </tbody>
</table>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
