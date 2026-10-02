package com.example.appmgmt.common;

/** 権限エラー（E103）。操作主体と操作者の不一致、参照範囲外など。 */
public class ForbiddenException extends BusinessException {
    public ForbiddenException() {
        super("E103");
    }
}
