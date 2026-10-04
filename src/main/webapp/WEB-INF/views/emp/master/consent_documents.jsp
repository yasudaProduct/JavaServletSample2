<c:set var="pageTitle" value="SC17 同意事項マスタ"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<p class="small text-muted">申込同意確認画面（AP03）で申込者に開いてもらう同意事項の PDF です。改定するときは新しい版を登録します（版は追加するだけで、登録済みの版は変更・削除しません）。適用開始日時を過ぎた最新の版が表示され、申込者がどの版に同意したかは同意ごとに記録されます。表示中に改定された場合、申込者は改定後の版を開き直してから同意します。</p>
<c:forEach var="d" items="${documents}">
  <div class="card mb-3 consent-document" id="doc-${d.documentCd}">
    <div class="card-header py-2 d-flex flex-wrap justify-content-between align-items-center">
      <span><strong><c:out value="${d.documentName}"/></strong> <small class="text-muted">（<c:out value="${d.documentCd}"/>／対象：<c:out value="${d.targetTypeName}"/>／表示順 ${d.displayOrder}）</small>
        <c:if test="${not d.valid}"><span class="badge badge-secondary">無効</span></c:if></span>
      <span class="small">適用中：<c:choose><c:when test="${empty d.currentVersion}">なし</c:when><c:otherwise>第 ${d.currentVersion.versionNo} 版</c:otherwise></c:choose></span>
    </div>
    <div class="card-body py-2">
      <table class="table table-sm table-bordered mb-2">
        <thead class="thead-light"><tr><th>版</th><th>適用開始日時</th><th>ファイル</th><th>サイズ</th><th>SHA-256</th><th>改定内容</th><th>登録</th><th></th></tr></thead>
        <tbody>
          <c:forEach var="v" items="${d.versions}">
            <tr class="${not empty d.currentVersion and d.currentVersion.versionNo == v.versionNo ? 'table-success' : ''}">
              <td>第 ${v.versionNo} 版<c:if test="${v.scheduled}"> <span class="badge badge-info">適用前</span></c:if></td><td>${app:datetime(v.effectiveFrom)}</td>
              <td class="small"><c:out value="${v.fileName}"/></td><td class="small text-right">${v.fileSize}</td><td class="small text-monospace" title="${v.fileHash}">${fn:substring(v.fileHash, 0, 12)}…</td>
              <td class="small"><c:out value="${v.remarks}"/></td><td class="small">${app:datetime(v.createdAt)}<br><c:out value="${v.createdBy}"/></td>
              <td><a class="btn btn-outline-primary btn-sm" href="${ctx}/emp/master/consent-documents/${d.documentCd}/${v.versionNo}" target="_blank" rel="noopener">開く</a></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
      <details>
        <summary class="small">新しい版を登録する／文書の設定を変える</summary>
        <div class="row mt-2">
          <div class="col-lg-7">
            <form method="post" action="${ctx}/emp/master/consent-documents" enctype="multipart/form-data" class="upload-form">
              <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="mode" value="upload"><input type="hidden" name="documentCd" value="${d.documentCd}">
              <div class="form-row">
                <div class="form-group col-md-6 mb-2"><label class="small mb-0">PDF ファイル（10MB 以内）</label><input type="file" class="form-control-file" name="file" accept="application/pdf" required></div>
                <div class="form-group col-md-6 mb-2"><label class="small mb-0">適用開始日時</label><input type="datetime-local" class="form-control form-control-sm" name="effectiveFrom" required></div>
              </div>
              <div class="form-group mb-2"><label class="small mb-0">改定内容（任意）</label><input type="text" class="form-control form-control-sm" name="remarks" maxlength="200"></div>
              <button type="submit" class="btn btn-success btn-sm">新しい版を登録</button>
            </form>
          </div>
          <div class="col-lg-5">
            <form method="post" action="${ctx}/emp/master/consent-documents">
              <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="mode" value="edit"><input type="hidden" name="documentCd" value="${d.documentCd}"><input type="hidden" name="rowVersion" value="${d.rowVersion}">
              <div class="form-group mb-2"><label class="small mb-0">文書名</label><input type="text" class="form-control form-control-sm" name="documentName" maxlength="100" value="<c:out value='${d.documentName}'/>"></div>
              <div class="form-row">
                <div class="form-group col-6 mb-2"><label class="small mb-0">対象</label><select class="form-control form-control-sm" name="targetType"><option value="1" ${d.targetType == '1' ? 'selected' : ''}>新規申込</option><option value="2" ${d.targetType == '2' ? 'selected' : ''}>契約変更</option><option value="9" ${d.targetType == '9' ? 'selected' : ''}>共通</option></select></div>
                <div class="form-group col-6 mb-2"><label class="small mb-0">表示順</label><input type="number" class="form-control form-control-sm" name="displayOrder" min="0" max="999" value="${d.displayOrder}"></div>
              </div>
              <div class="custom-control custom-checkbox mb-2"><input type="checkbox" class="custom-control-input" id="valid_${d.documentCd}" name="validFlg" value="1" ${d.valid ? 'checked' : ''}><label class="custom-control-label small" for="valid_${d.documentCd}">有効（無効にすると新しい同意で表示しない）</label></div>
              <button type="submit" class="btn btn-outline-primary btn-sm">設定を保存</button>
            </form>
          </div>
        </div>
      </details>
    </div>
  </div>
</c:forEach>
<div class="card mb-3" style="max-width: 860px;">
  <div class="card-header py-2">文書の追加</div>
  <div class="card-body py-2">
    <form method="post" action="${ctx}/emp/master/consent-documents" id="newConsentDocument">
      <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="mode" value="new">
      <div class="form-row">
        <div class="form-group col-md-3 mb-2"><label class="small mb-0">文書コード</label><input type="text" class="form-control form-control-sm" name="documentCd" maxlength="20" placeholder="例：CANCEL_POLICY"></div>
        <div class="form-group col-md-4 mb-2"><label class="small mb-0">文書名</label><input type="text" class="form-control form-control-sm" name="documentName" maxlength="100"></div>
        <div class="form-group col-md-2 mb-2"><label class="small mb-0">対象</label><select class="form-control form-control-sm" name="targetType"><option value="1">新規申込</option><option value="2">契約変更</option><option value="9">共通</option></select></div>
        <div class="form-group col-md-1 mb-2"><label class="small mb-0">表示順</label><input type="number" class="form-control form-control-sm" name="displayOrder" min="0" max="999" value="50"></div>
        <div class="form-group col-md-2 mb-2 d-flex align-items-end"><button type="submit" class="btn btn-success btn-sm">追加</button></div>
      </div>
      <p class="small text-muted mb-0">追加した文書は、版（PDF）を登録して適用開始日時を過ぎると申込者に表示されます。</p>
    </form>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
