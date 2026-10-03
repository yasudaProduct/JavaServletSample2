<c:set var="pageTitle" value="SC02 申込一覧・検索"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<form method="get" action="${ctx}/emp/applications" class="card mb-3">
  <div class="card-body py-2">
    <input type="hidden" name="search" value="1">
    <div class="form-row">
      <div class="form-group col-md-2 mb-2">
        <label class="small mb-0">申込番号（前方一致）</label>
        <input type="text" class="form-control form-control-sm" name="applicationNo" maxlength="12" value="<c:out value='${criteria.applicationNo}'/>">
      </div>
      <div class="form-group col-md-2 mb-2">
        <label class="small mb-0">申込者番号／申込者名</label>
        <input type="text" class="form-control form-control-sm" name="applicantKey" maxlength="100" value="<c:out value='${criteria.applicantKey}'/>">
      </div>
      <div class="form-group col-md-2 mb-2">
        <label class="small mb-0">担当社員</label>
        <select class="form-control form-control-sm" name="ownerEmployeeId" ${user.owner ? 'disabled' : ''}>
          <option value="">（すべて）</option>
          <c:forEach var="e" items="${employees}"><option value="${e.employeeId}" ${criteria.ownerEmployeeId == e.employeeId ? 'selected' : ''}><c:out value="${e.employeeName}"/></option></c:forEach>
        </select>
      </div>
      <div class="form-group col-md-2 mb-2">
        <label class="small mb-0">登録日 From</label>
        <input type="text" class="form-control form-control-sm ${not empty errors.createdFrom ? 'is-invalid' : ''}" name="createdFrom" placeholder="yyyy/MM/dd" value="<c:out value='${param.createdFrom}'/>">
        <div class="invalid-feedback"><c:out value="${errors.createdFrom}"/></div>
      </div>
      <div class="form-group col-md-2 mb-2">
        <label class="small mb-0">登録日 To</label>
        <input type="text" class="form-control form-control-sm ${not empty errors.createdTo ? 'is-invalid' : ''}" name="createdTo" placeholder="yyyy/MM/dd" value="<c:out value='${param.createdTo}'/>">
        <div class="invalid-feedback"><c:out value="${errors.createdTo}"/></div>
      </div>
      <div class="form-group col-md-2 mb-2 d-flex align-items-end">
        <c:if test="${not user.admin}">
          <div class="custom-control custom-checkbox mb-1">
            <input type="checkbox" class="custom-control-input" id="myTasksOnly" name="myTasksOnly" value="1" ${criteria.myTasksOnly ? 'checked' : ''}>
            <label class="custom-control-label" for="myTasksOnly">自分の操作待ちのみ</label>
          </div>
        </c:if>
      </div>
    </div>
    <div class="form-row">
      <div class="form-group col-md-2 mb-2">
        <label class="small mb-0">大分類</label>
        <div>
          <c:forEach var="l" items="${['1','2']}">
            <div class="custom-control custom-checkbox custom-control-inline">
              <input type="checkbox" class="custom-control-input" id="catL${l}" name="categoryL" value="${l}" ${selectedCategoryL.contains(l) ? 'checked' : ''}>
              <label class="custom-control-label" for="catL${l}">${l == '1' ? '新規申込' : '契約変更'}</label>
            </div>
          </c:forEach>
        </div>
      </div>
      <div class="form-group col-md-5 mb-2">
        <label class="small mb-0">中分類</label>
        <div>
          <c:forEach var="m" items="${['01','02','03','04','05','06','07']}">
            <div class="custom-control custom-checkbox custom-control-inline">
              <input type="checkbox" class="custom-control-input" id="catM${m}" name="categoryM" value="${m}" ${selectedCategoryM.contains(m) ? 'checked' : ''}>
              <label class="custom-control-label" for="catM${m}">${m == '01' ? '入力中' : m == '02' ? '一次承認中' : m == '03' ? '申込者確認中' : m == '04' ? '事前確認中' : m == '05' ? '最終承認中' : m == '06' ? '審査受付中' : '審査完了'}</label>
            </div>
          </c:forEach>
        </div>
      </div>
      <div class="form-group col-md-3 mb-2">
        <label class="small mb-0">個別ステータス（複数選択可）</label>
        <select class="form-control form-control-sm" name="statusCd" multiple size="4">
          <c:forEach var="s" items="${statuses}"><option value="${s.statusCd}" ${criteria.statusCds.contains(s.statusCd) and empty selectedCategoryL and empty selectedCategoryM ? 'selected' : ''}>${s.statusCd} <c:out value="${s.statusName}"/></option></c:forEach>
        </select>
      </div>
      <div class="form-group col-md-2 mb-2 d-flex align-items-end justify-content-end">
        <button type="submit" class="btn btn-primary btn-sm mr-2">検索</button>
        <a class="btn btn-outline-secondary btn-sm mr-2" href="${ctx}/emp/applications?search=1">クリア</a>
        <c:if test="${user.owner}"><a class="btn btn-success btn-sm" href="${ctx}/emp/applications/new">新規申込</a></c:if>
      </div>
    </div>
  </div>
</form>

<div class="d-flex justify-content-between align-items-center mb-2">
  <span class="text-muted small">${result.total} 件中 ${result.rows.size() == 0 ? 0 : (result.page - 1) * result.pageSize + 1}〜${(result.page - 1) * result.pageSize + result.rows.size()} 件を表示</span>
  <nav>
    <ul class="pagination pagination-sm mb-0">
      <c:forEach var="p" begin="1" end="${result.totalPages()}">
        <li class="page-item ${p == result.page ? 'active' : ''}"><a class="page-link" href="${ctx}/emp/applications${empty queryString ? '?search=1' : queryString}&page=${p}">${p}</a></li>
      </c:forEach>
    </ul>
  </nav>
</div>
<table class="table table-sm table-hover table-bordered bg-white">
  <thead class="thead-light">
    <tr><th>申込番号</th><th>申込者番号／申込者名</th><th>ステータス</th><th class="text-right">申込金額合計</th><th>担当社員</th><th>登録区分</th><th>更新日時</th></tr>
  </thead>
  <tbody>
    <c:if test="${empty result.rows}"><tr><td colspan="7" class="text-muted text-center">該当する申込はありません。</td></tr></c:if>
    <c:forEach var="r" items="${result.rows}">
      <tr>
        <td><a href="${ctx}/emp/applications/${r.applicationId}/menu"><c:out value="${r.applicationNo}"/></a></td>
        <td><c:out value="${app:text(r.applicantName)}"/><c:if test="${not empty r.applicantNo}"><br><small class="text-muted"><c:out value="${r.applicantNo}"/></small></c:if></td>
        <td><span class="badge badge-${fn:endsWith(r.statusCd, '701') ? 'success' : 'primary'} status-badge">${r.statusCd}</span> <c:out value="${r.statusName}"/></td>
        <td class="text-right">${app:amount(r.totalAmount)}</td>
        <td><c:out value="${r.ownerName}"/></td>
        <td><c:out value="${r.registrationTypeName}"/></td>
        <td>${app:datetime(r.updatedAt)}</td>
      </tr>
    </c:forEach>
  </tbody>
</table>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
