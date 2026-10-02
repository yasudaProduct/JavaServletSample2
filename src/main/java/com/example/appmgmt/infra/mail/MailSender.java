package com.example.appmgmt.infra.mail;

import com.example.appmgmt.common.AppConfig;

/** メール送信。1 回のバッチ実行で 1 接続（セッション）を使い回す。 */
public interface MailSender {

    interface MailSession extends AutoCloseable {
        void send(String to, String subject, String body) throws MailException;

        @Override
        void close();
    }

    /** 接続する。接続できない場合は Kind.INFRA の MailException。 */
    MailSession open() throws MailException;

    static MailSender create(AppConfig config) {
        return "smtp".equalsIgnoreCase(config.getString("mail.mode", "log")) ? new SmtpMailSender(config) : new LogMailSender();
    }
}
