package com.example.appmgmt.common;

/** 業務エラー（遷移不可、権限、入力の業務チェックなど）。メッセージ ID とパラメータを持つ。 */
public class BusinessException extends RuntimeException {

    private final String messageId;
    private final Object[] args;

    public BusinessException(String messageId, Object... args) {
        super(Messages.get(messageId, args));
        this.messageId = messageId;
        this.args = args;
    }

    public String getMessageId() {
        return messageId;
    }

    public Object[] getArgs() {
        return args;
    }
}
