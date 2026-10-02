package com.example.appmgmt.common;

/** 遷移不可エラー（E101）。現在ステータス・操作コード・会社区分に該当する遷移がない。 */
public class TransitionNotAllowedException extends BusinessException {
    public TransitionNotAllowedException() {
        super("E101");
    }
}
