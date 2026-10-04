<c:set var="pageTitle" value="申込内容（PDF）"/>
<c:set var="portal" value="${true}"/>
<c:set var="a" value="${d.application}"/>
<%@ include file="/WEB-INF/views/common/ap_top.jspf" %>
<p class="small text-muted">申込番号 <c:out value="${a.applicationNo}"/>　手続きの状況：<strong><c:out value="${empty d.status.applicantStatusName ? '手続き中' : d.status.applicantStatusName}"/></strong></p>
<h3 class="h6 section-title">お申込内容（PDF）</h3>
<p class="small text-muted">ご同意いただいたときと、審査が完了したときのお申込内容です。作成後に内容は変わりません。ご同意後に担当者が金額などを修正した場合は、審査完了時の PDF に変更された項目を赤字で示します。</p>
<c:choose>
  <c:when test="${empty pdfs}"><p class="text-muted small" id="noPdf">まだ PDF はありません。ご同意いただくと作成されます。</p></c:when>
  <c:otherwise>
    <table class="table table-sm table-bordered bg-white" id="pdfTable">
      <thead class="thead-light"><tr><th>手続き</th><th>種類</th><th>申込内容の版</th><th>作成日時</th><th>状態</th><th></th></tr></thead>
      <tbody>
        <c:forEach var="p" items="${pdfs}">
          <tr class="${p.current ? '' : 'text-muted'}">
            <td><c:out value="${p.phaseLabel}"/></td><td><c:out value="${p.pdfTypeName}"/></td><td>第 ${p.versionNo} 版</td><td>${app:datetime(p.createdAt)}</td>
            <td><span class="badge badge-${p.current ? 'success' : 'secondary'}"><c:out value="${p.stateLabel}"/></span></td>
            <td><a class="btn btn-outline-primary btn-sm pdf-open" href="${ctx}/my/applications/${a.applicationId}/documents/pdf/${p.pdfId}" target="_blank" rel="noopener">PDF を開く</a></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </c:otherwise>
</c:choose>
<h3 class="h6 section-title">ご同意いただいた同意事項</h3>
<c:choose>
  <c:when test="${empty agreedDocs}"><p class="text-muted small">まだありません。</p></c:when>
  <c:otherwise>
    <table class="table table-sm table-bordered bg-white" id="agreedDocTable">
      <thead class="thead-light"><tr><th>ご同意日時</th><th>手続き</th><th>同意事項</th><th>版</th><th></th></tr></thead>
      <tbody>
        <c:forEach var="r" items="${agreedDocs}">
          <tr><td>${app:datetime(r.agreedAt)}</td><td><c:out value="${r.consentTypeName}"/>（申込内容 第 ${r.contentVersionNo} 版）</td><td><c:out value="${r.documentName}"/></td><td>第 ${r.versionNo} 版</td>
            <td><a class="btn btn-outline-secondary btn-sm" href="${ctx}/my/applications/${a.applicationId}/documents/terms/${r.documentCd}/${r.versionNo}" target="_blank" rel="noopener">PDF を開く</a></td></tr>
        </c:forEach>
      </tbody>
    </table>
  </c:otherwise>
</c:choose>
<a class="btn btn-outline-secondary" href="${ctx}/my/menu?app=${a.applicationId}">メニューへ戻る</a>
<%@ include file="/WEB-INF/views/common/ap_bottom.jspf" %>
