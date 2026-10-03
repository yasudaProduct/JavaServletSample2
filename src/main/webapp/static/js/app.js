(function () {
  'use strict';

  function parseAmount(v) {
    var t = (v || '').replace(/,/g, '').trim();
    if (!/^[0-9]{1,13}$/.test(t)) { return null; }
    return parseInt(t, 10);
  }
  function format(n) { return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ','); }

  // 申込金額合計と変更金額倍率の参考表示
  function recalc() {
    var total = document.getElementById('totalAmount');
    if (!total) { return; }
    var ids = ['basicFee', 'optionFee', 'handlingFee'];
    var sum = 0;
    var ok = true;
    ids.forEach(function (id) {
      var el = document.getElementById(id);
      if (!el) { return; }
      if (el.value.trim() === '') { return; }
      var n = parseAmount(el.value);
      if (n === null) { ok = false; } else { sum += n; }
    });
    total.value = ok ? format(sum) : '－';
    var ratio = document.getElementById('ratioPreview');
    var baseTotal = document.body.getAttribute('data-base-total');
    var limit = parseFloat(document.body.getAttribute('data-ratio-limit') || '0');
    var warn = document.getElementById('ratioWarning');
    if (ratio && baseTotal && parseInt(baseTotal, 10) > 0 && ok) {
      var r = Math.floor(sum / parseInt(baseTotal, 10) * 10000) / 10000;
      ratio.textContent = r.toFixed(2);
      if (warn) { warn.style.display = (limit > 0 && r >= limit) ? '' : 'none'; }
    } else if (ratio) {
      ratio.textContent = '－';
      if (warn) { warn.style.display = 'none'; }
    }
  }
  document.querySelectorAll('.amount-input').forEach(function (el) { el.addEventListener('input', recalc); });
  recalc();

  // 申込メニューの領域（折りたたみ）：ボタンの文言を状態に合わせる
  if (window.jQuery) {
    window.jQuery('.menu-section .collapse').on('shown.bs.collapse', function () {
      var b = document.querySelector('[data-target="#' + this.id + '"]');
      if (b) { b.textContent = '折りたたむ'; }
    }).on('hidden.bs.collapse', function () {
      var b = document.querySelector('[data-target="#' + this.id + '"]');
      if (b) { b.textContent = '展開する'; }
    });
  }

  // SC04 申込者：新規の申込者／登録済みの申込者の切り替え
  document.querySelectorAll('input[name=applicantMode]').forEach(function (r) {
    r.addEventListener('change', function () {
      var isNew = document.getElementById('applicantModeNew').checked;
      document.querySelectorAll('.applicant-new').forEach(function (el) { el.style.display = isNew ? '' : 'none'; });
      document.querySelectorAll('.applicant-existing').forEach(function (el) { el.style.display = isNew ? 'none' : ''; });
    });
  });

  // 確認ダイアログ
  document.querySelectorAll('form[data-confirm]').forEach(function (f) {
    f.addEventListener('submit', function (e) {
      if (!window.confirm(f.getAttribute('data-confirm'))) { e.preventDefault(); }
    });
  });

  // 二重送信防止
  document.querySelectorAll('form').forEach(function (f) {
    f.addEventListener('submit', function () {
      window.setTimeout(function () {
        f.querySelectorAll('button[type=submit]').forEach(function (b) { b.disabled = true; });
      }, 0);
    });
  });

  // SC06 回付先の編集
  var routeTable = document.getElementById('routeRows');
  if (routeTable) {
    var candidates = JSON.parse(document.getElementById('routeCandidates').textContent);
    var initial = JSON.parse(document.getElementById('routeInitial').textContent);
    var state = initial.slice();
    function nameOf(id) { var c = candidates.filter(function (x) { return x.id === id; })[0]; return c ? c.name + '（' + c.dept + '）' : String(id); }
    function render() {
      routeTable.innerHTML = '';
      state.forEach(function (id, i) {
        var tr = document.createElement('tr');
        tr.innerHTML = '<td>' + (i + 1) + '</td><td>' + nameOf(id) + '<input type="hidden" name="approverId" value="' + id + '"></td>'
          + '<td class="text-right"><button type="button" class="btn btn-outline-secondary btn-sm mr-1" data-up="' + i + '">上へ</button>'
          + '<button type="button" class="btn btn-outline-secondary btn-sm mr-1" data-down="' + i + '">下へ</button>'
          + '<button type="button" class="btn btn-outline-danger btn-sm" data-remove="' + i + '">削除</button></td>';
        routeTable.appendChild(tr);
      });
      if (state.length === 0) {
        routeTable.innerHTML = '<tr><td colspan="3" class="text-muted">回付先なし（一次承認のみ申請できます）</td></tr>';
      }
      var used = document.getElementById('templateUsed');
      if (used) { used.value = (JSON.stringify(state) === JSON.stringify(initial) && initial.length > 0) ? '1' : '0'; }
      var finalName = document.getElementById('finalApproverName');
      if (finalName) { finalName.textContent = state.length ? nameOf(state[state.length - 1]) : '－'; }
    }
    routeTable.addEventListener('click', function (e) {
      var b = e.target;
      if (b.hasAttribute('data-up')) { var i = +b.getAttribute('data-up'); if (i > 0) { var t = state[i - 1]; state[i - 1] = state[i]; state[i] = t; } }
      if (b.hasAttribute('data-down')) { var j = +b.getAttribute('data-down'); if (j < state.length - 1) { var u = state[j + 1]; state[j + 1] = state[j]; state[j] = u; } }
      if (b.hasAttribute('data-remove')) { state.splice(+b.getAttribute('data-remove'), 1); }
      render();
    });
    document.getElementById('addRoute').addEventListener('click', function () {
      var sel = document.getElementById('candidateSelect');
      var id = parseInt(sel.value, 10);
      if (!id) { return; }
      if (state.indexOf(id) >= 0) { window.alert('同じ社員は重複して設定できません。'); return; }
      var max = parseInt(routeTable.getAttribute('data-max') || '5', 10);
      if (state.length >= max) { window.alert('回付先は ' + max + ' 人までです。'); return; }
      state.push(id);
      render();
    });
    document.getElementById('resetRoute').addEventListener('click', function () { state = initial.slice(); render(); });
    render();
  }
})();
