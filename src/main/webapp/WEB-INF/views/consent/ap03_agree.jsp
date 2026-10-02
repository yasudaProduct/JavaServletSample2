<c:set var="pageTitle" value="同意のご確認"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<p class="small text-muted">申込番号 <c:out value="${v.application.applicationNo}"/>　<c:out value="${v.applicant.applicantName}"/> 様　手続きの状況：<strong><c:out value="${v.applicantStatusName}"/></strong>　確認期限：${app:datetime(v.consent.tokenExpiresAt)}</p>
<c:set var="cur" value="${v.version}"/><c:set var="base" value="${v.contractChange ? v.beforeVersion : null}"/><c:set var="curLabel" value="${v.contractChange ? '変更後' : '確定したお申込内容'}"/><c:set var="baseLabel" value="変更前"/>
<%@ include file="/WEB-INF/views/common/version_view.jspf" %>
<div class="card mb-3"><div class="card-body small">
  <strong>同意事項（サンプル文言）</strong><br>
  <c:choose>
    <c:when test="${v.contractChange}">上記の契約変更内容に相違がないことを確認し、変更後の内容で契約を継続することに同意します。審査担当部門の審査結果によっては変更をお受けできない場合があります。</c:when>
    <c:otherwise>上記のお申込内容に相違がないことを確認し、お申込に同意します。審査担当部門の審査結果によってはお申込をお受けできない場合があります。</c:otherwise>
  </c:choose>
</div></div>
<form method="post" action="${ctx}/consent/${token}/agree">
  <input type="hidden" name="_csrf" value="${csrf}">
  <div class="custom-control custom-checkbox mb-3">
    <input type="checkbox" class="custom-control-input" id="agreed" name="agreed" value="1" onchange="document.getElementById('agreeBtn').disabled = !this.checked;">
    <label class="custom-control-label" for="agreed">同意事項に同意します</label>
  </div>
  <button type="submit" name="action" value="agree" class="btn btn-primary mr-2" id="agreeBtn" disabled>同意する</button>
  <c:if test="${not v.contractChange}"><button type="submit" name="action" value="modify" class="btn btn-outline-secondary mr-2">修正</button></c:if>
  <hr>
  <div class="form-group">
    <label for="returnReason">差戻し理由（差し戻す場合は必須）</label>
    <textarea class="form-control" id="returnReason" name="returnReason" rows="3" maxlength="500"></textarea>
  </div>
  <button type="submit" name="action" value="return" class="btn btn-outline-danger">差戻し（担当者へ戻す）</button>
</form>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
