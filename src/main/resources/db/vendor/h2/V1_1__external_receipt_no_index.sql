-- H2（開発時の検証用）はフィルタ付きインデックスを持たないが、ユニークインデックスで NULL の重複を許容する
CREATE UNIQUE INDEX UK_T_EXTERNAL_LINK_01 ON T_EXTERNAL_LINK (EXTERNAL_RECEIPT_NO);
