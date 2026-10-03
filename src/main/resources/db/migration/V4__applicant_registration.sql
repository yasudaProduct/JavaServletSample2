-- V4 申込者データの起点を「申込受付会社の新規申込（画面入力・一括取込）」に変更する
-- 申込者番号は申込者の登録時に採番する（C ＋ 10 桁ゼロ埋め）。採番済みの番号と重なる値はアプリ側で読み飛ばす
CREATE SEQUENCE SEQ_APPLICANT_NO AS BIGINT START WITH 1 INCREMENT BY 1;

-- 初期データ（V2）で登録していたサンプル申込者は、申込から参照されていないものを削除する。
-- 既に申込で使われている申込者（既存環境で動作確認したデータ）は残す
DELETE FROM M_APPLICANT
 WHERE APPLICANT_NO IN ('C0000000001', 'C0000000002', 'C0000000003', 'C0000000004', 'C0000000005')
   AND NOT EXISTS (SELECT 1 FROM T_APPLICATION a WHERE a.APPLICANT_ID = M_APPLICANT.APPLICANT_ID);
