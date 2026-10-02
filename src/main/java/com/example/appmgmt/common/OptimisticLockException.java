package com.example.appmgmt.common;

/** 楽観排他エラー（E102）。 */
public class OptimisticLockException extends BusinessException {
    public OptimisticLockException() {
        super("E102");
    }
}
