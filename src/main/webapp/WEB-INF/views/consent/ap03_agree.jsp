<c:set var="pageTitle" value="同意のご確認"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<p class="small text-muted">申込番号 <c:out value="${v.application.applicationNo}"/>　<c:out value="${v.applicant.applicantName}"/> 様　手続きの状況：<strong><c:out value="${v.applicantStatusName}"/></strong><c:if test="${not empty v.consent}">　確認期限：${app:datetime(v.consent.tokenExpiresAt)}</c:if></p>
<c:set var="cur" value="${v.version}"/><c:set var="base" value="${v.contractChange ? v.beforeVersion : null}"/><c:set var="curLabel" value="${v.contractChange ? '変更後' : '確定したお申込内容'}"/><c:set var="baseLabel" value="変更前"/>
<c:set var="applicantView" value="${true}"/>
<%@ include file="/WEB-INF/views/common/version_view.jspf" %>
<%-- 同意事項（PDF）。適用中の版をすべて開くと「同意事項に同意します」を選べる。開いた版はサーバーで記録し、同意時にも確認する --%>
<div class="card mb-3" id="consentDocs">
  <div class="card-header py-2">同意事項（PDF）</div>
  <div class="card-body py-2">
    <c:choose>
      <c:when test="${empty v.documents}"><p class="small text-muted mb-0">確認が必要な同意事項はありません。</p></c:when>
      <c:otherwise>
        <p class="small mb-2">次の同意事項をすべて開いて内容をご確認ください。すべて確認すると「同意する」を押せるようになります。</p>
        <ul class="list-group">
          <c:forEach var="doc" items="${v.documents}">
            <li class="list-group-item d-flex flex-wrap justify-content-between align-items-center py-2 consent-doc" data-doc="${doc.documentCd}" data-viewed="${doc.viewed ? '1' : '0'}">
              <span><c:out value="${doc.documentName}"/> <small class="text-muted">第 ${doc.currentVersion.versionNo} 版（${app:datetime(doc.currentVersion.effectiveFrom)} 適用）</small></span>
              <span class="text-nowrap"><span class="badge badge-success doc-viewed" ${doc.viewed ? '' : 'hidden'}>確認済み</span>
                <a class="btn btn-outline-primary btn-sm ml-2 doc-open" href="${ctx}${consentBase}/terms/${doc.documentCd}/${doc.currentVersion.versionNo}" target="_blank" rel="noopener">PDF を開く</a></span>
            </li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>
  </div>
</div>
<form method="post" action="${ctx}${consentBase}/agree">
  <input type="hidden" name="_csrf" value="${csrf}">
  <div class="custom-control custom-checkbox mb-3">
    <input type="checkbox" class="custom-control-input" id="agreed" name="agreed" value="1" ${v.allDocumentsViewed ? '' : 'disabled'} onchange="document.getElementById('agreeBtn').disabled = !this.checked;">
    <label class="custom-control-label" for="agreed">上記の内容を確認し、同意事項に同意します</label>
    <small class="form-text text-muted" id="agreeHint" ${v.allDocumentsViewed ? 'hidden' : ''}>同意事項の PDF をすべて開くと選べるようになります。</small>
  </div>
  <button type="submit" name="action" value="agree" class="btn btn-primary mr-2" id="agreeBtn" disabled>同意する</button>
  <c:if test="${not v.contractChange}"><button type="submit" name="action" value="modify" class="btn btn-outline-secondary mr-2">修正</button></c:if>
  <hr>
  <div class="form-group">
    <label for="returnReason">差戻し理由（差し戻す場合は必須）</label>
    <textarea class="form-control" id="returnReason" name="returnReason" rows="3" maxlength="500"></textarea>
  </div>
  <button type="submit" name="action" value="return" class="btn btn-outline-danger">差戻し（担当者へ戻す）</button>
  <c:if test="${portal}"><a class="btn btn-link" href="${ctx}/my/menu?app=${v.application.applicationId}">メニューへ戻る</a></c:if>
</form>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
