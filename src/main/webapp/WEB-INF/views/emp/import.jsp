<c:set var="pageTitle" value="SC10 申込一括取込"/>
<%@ include file="/WEB-INF/views/common/emp_top.jspf" %>
<div class="row">
  <div class="col-lg-5">
    <div class="card mb-3">
      <div class="card-header py-2">取込ファイル（CSV、UTF-8、5MB 以内、1,000 行まで）</div>
      <div class="card-body">
        <form method="post" action="${ctx}/emp/import" enctype="multipart/form-data">
          <input type="hidden" name="_csrf" value="${csrf}">
          <div class="form-group">
            <input type="file" class="form-control-file" name="file" accept=".csv" required>
          </div>
          <button type="submit" class="btn btn-primary">取込実行</button>
        </form>
        <hr>
        <p class="small mb-1">ヘッダ行（1 行目）は次のとおりです。</p>
        <pre class="small bg-light p-2 mb-1">申込者番号,申込者名,申込者名カナ,メールアドレス,電話番号,住所,商品コード,基本料金,オプション料金,事務手数料,契約開始日,契約終了日,備考
,山田 一郎,ヤマダ イチロウ,ichiro@example.com,03-0000-0001,東京都千代田区1-1-1,PRD001,1000000,0,0,2026-10-01,2027-09-30,
C0000000001,佐藤 花子,サトウ ハナコ,hanako@example.com,,,PRD002,2000000,500000,,2026-10-15,,"備考に、カンマを含む例"</pre>
        <p class="small text-muted mb-0">申込者情報（申込者名・メールアドレスは必須）は申込データとして取り込みます。申込者番号は通常は空にします（申込者アカウントは一次承認が通ったときに発行）。同じ申込者の 2 件目以降で発行済みのアカウントを使う場合だけ申込者番号を指定します。申込者番号が空で、同じメールアドレスのアカウントが発行済みの行はエラーになります。日付は yyyy-MM-dd、金額はカンマなしの整数。取込後のステータスは 10100（一括取込済）になります。</p>
      </div>
    </div>
  </div>
  <div class="col-lg-7">
    <c:if test="${not empty batch}">
      <div class="card mb-3">
        <div class="card-header py-2">取込結果（取込 ID ${batch.importBatchId}）</div>
        <div class="card-body py-2">
          <p class="mb-2 small"><strong>ファイル名：</strong><c:out value="${batch.fileName}"/>　<strong>取込日時：</strong>${app:datetime(batch.importedAt)}　
            <strong>総件数：</strong>${batch.totalCount}　<strong>成功：</strong>${batch.successCount}　<strong>エラー：</strong>${batch.errorCount}</p>
          <c:if test="${not empty errorsList}">
            <table class="table table-sm table-bordered mb-0">
              <thead class="thead-light"><tr><th style="width: 8%;">行番号</th><th>エラー内容</th><th>行データ</th></tr></thead>
              <tbody><c:forEach var="e" items="${errorsList}"><tr><td>${e.lineNo}</td><td class="small text-danger"><c:out value="${e.errorMessage}"/></td><td class="small text-break"><c:out value="${e.rawLine}"/></td></tr></c:forEach></tbody>
            </table>
          </c:if>
          <c:if test="${batch.successCount > 0}"><a class="btn btn-outline-primary btn-sm mt-2" href="${ctx}/emp/applications?search=1&statusCd=10100">取り込んだ申込を一覧で確認する</a></c:if>
        </div>
      </div>
    </c:if>
    <h3 class="h6 section-title">取込履歴（自分の取込）</h3>
    <table class="table table-sm table-bordered">
      <thead class="thead-light"><tr><th>取込日時</th><th>ファイル名</th><th>取込社員</th><th class="text-right">総件数</th><th class="text-right">成功</th><th class="text-right">エラー</th></tr></thead>
      <tbody>
        <c:if test="${empty history}"><tr><td colspan="6" class="text-muted">なし</td></tr></c:if>
        <c:forEach var="b" items="${history}">
          <tr><td><a href="${ctx}/emp/import/${b.importBatchId}">${app:datetime(b.importedAt)}</a></td><td><c:out value="${b.fileName}"/></td><td><c:out value="${b.importEmployeeName}"/></td><td class="text-right">${b.totalCount}</td><td class="text-right">${b.successCount}</td><td class="text-right">${b.errorCount}</td></tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>
<%@ include file="/WEB-INF/views/common/emp_bottom.jspf" %>
