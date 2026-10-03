<c:set var="pageTitle" value="SC05 申込内容確認${empty group or group.index == 0 ? '' : '（'.concat(group.title).concat('）')}"/>
<c:set var="a" value="${d.application}"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="card mb-3"><div class="card-body py-2 small">
  <strong>申込番号：</strong><c:out value="${a.applicationNo}"/>　<strong>ステータス：</strong><c:out value="${d.status.displayName}"/>　<strong>担当社員：</strong><c:out value="${d.owner.employeeName}"/>　<strong>登録区分：</strong>${app:label('REGISTRATION_TYPE', a.registrationType)}<br>
  <strong>申込者：</strong><c:out value="${d.applicant.applicantNo}"/>／<c:out value="${d.applicant.applicantName}"/>　<strong>確認依頼メールの宛先：</strong><c:out value="${d.applicant.mailAddress}"/>
</div></div>
<c:if test="${not empty errors}">
  <div class="alert alert-danger"><strong>入力内容に誤りがあります。「修正」で入力し直してください。</strong><ul class="mb-0"><c:forEach var="e" items="${errors}"><li><c:out value="${e.value}"/></li></c:forEach></ul></div>
</c:if>
<c:if test="${not empty diffBase}"><p class="small mb-1"><span class="text-danger font-weight-bold">赤字</span>：全体修正で変更した項目（複写元：第 ${diffBase.versionNo} 版 <c:out value="${diffBase.versionTypeName}"/>）</p></c:if>
<c:set var="cur" value="${viewVersion}"/><c:set var="base" value="${null}"/><c:set var="curLabel" value="申込内容"/>
<%@ include file="/WEB-INF/views/common/version_view.jspf" %>
<div class="d-flex flex-wrap align-items-start">
  <form method="post" action="${ctx}/emp/applications/${a.applicationId}/confirm" class="mr-2 mb-2" ${fullRevise and canModify ? 'data-confirm="申込内容全体を修正し直すため入力中へ戻します。申込者の同意は取り直しになります。よろしいですか？"' : ''}>
    <input type="hidden" name="_csrf" value="${csrf}">
    <input type="hidden" name="rowVersion" value="${a.rowVersion}">
    <button type="submit" name="action" value="modify" class="btn ${fullRevise and canModify ? 'btn-outline-warning' : 'btn-outline-primary'}" id="modifyBtn" ${canModify ? '' : 'disabled'}>修正${fullRevise and canModify ? '（全体修正）' : ''}</button>
  </form>
  <form method="post" action="${ctx}/emp/applications/${a.applicationId}/confirm" class="mr-2 mb-2">
    <input type="hidden" name="_csrf" value="${csrf}">
    <input type="hidden" name="rowVersion" value="${a.rowVersion}">
    <button type="submit" name="action" value="confirm" class="btn btn-primary" id="confirmBtn" ${canConfirm ? '' : 'disabled'}>確定（一次承認申請待ちへ）</button>
  </form>
  <a class="btn btn-outline-secondary mb-2" href="${ctx}/emp/applications/${a.applicationId}/menu">メニューへ戻る</a>
</div>
<c:if test="${not empty modifyNote}"><p class="small text-muted mb-1"><c:out value="${modifyNote}"/></p></c:if>
<c:if test="${not empty confirmNote}"><p class="small text-muted mb-1"><c:out value="${confirmNote}"/></p></c:if>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
