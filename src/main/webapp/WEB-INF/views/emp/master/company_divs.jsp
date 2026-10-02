<c:set var="pageTitle" value="SC12 会社区分マスタ"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<p class="small text-muted">外部事前確認フラグとしきい値は遷移時に評価されるため、変更は進行中の申込にも即時に影響します。</p>
<form method="post" action="${ctx}/emp/master/company-divs" data-confirm="会社区分マスタを更新します。変更は進行中の申込にも即時に影響します。よろしいですか？" style="max-width: 860px;">
  <input type="hidden" name="_csrf" value="${csrf}">
  <table class="table table-sm table-bordered bg-white">
    <thead class="thead-light"><tr><th>会社区分</th><th>会社区分名</th><th>外部事前確認</th><th>金額倍率しきい値（1.00〜9.99）</th></tr></thead>
    <tbody>
      <c:forEach var="d" items="${companyDivs}">
        <tr>
          <td>${d.companyDiv}<input type="hidden" name="companyDiv" value="${d.companyDiv}"><input type="hidden" name="rowVersion_${d.companyDiv}" value="${d.rowVersion}"></td>
          <td><input type="text" class="form-control form-control-sm ${not empty errors['companyDivName_'.concat(d.companyDiv)] ? 'is-invalid' : ''}" name="companyDivName_${d.companyDiv}" maxlength="50" value="<c:out value='${d.companyDivName}'/>"><div class="invalid-feedback"><c:out value="${errors['companyDivName_'.concat(d.companyDiv)]}"/></div></td>
          <td><select class="form-control form-control-sm" name="preCheckFlg_${d.companyDiv}"><option value="0" ${d.preCheckFlg == '0' ? 'selected' : ''}>なし</option><option value="1" ${d.preCheckFlg == '1' ? 'selected' : ''}>あり</option></select></td>
          <td><input type="text" class="form-control form-control-sm ${not empty errors['amountRatioLimit_'.concat(d.companyDiv)] ? 'is-invalid' : ''}" name="amountRatioLimit_${d.companyDiv}" maxlength="4" value="${app:ratio(d.amountRatioLimit)}"><div class="invalid-feedback"><c:out value="${errors['amountRatioLimit_'.concat(d.companyDiv)]}"/></div></td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
  <button type="submit" class="btn btn-primary mr-2">保存</button>
  <a class="btn btn-outline-secondary" href="${ctx}/emp/applications">戻る</a>
</form>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
