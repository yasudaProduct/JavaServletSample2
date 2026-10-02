package com.example.appmgmt.infra.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 開発用：送信せずにログへ出す（本文は出さない）。 */
public class LogMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LogMailSender.class);

    @Override
    public MailSession open() {
        return new MailSession() {
            @Override
            public void send(String to, String subject, String body) {
                log.info("[mail.mode=log] 送信したことにします to={} subject={}", mask(to), subject);
            }

            @Override
            public void close() {
            }
        };
    }

    static String mask(String address) {
        if (address == null) {
            return "";
        }
        int at = address.indexOf('@');
        if (at <= 1) {
            return "***" + address.substring(Math.max(at, 0));
        }
        return address.charAt(0) + "***" + address.substring(at);
    }
}
