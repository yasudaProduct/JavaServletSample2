package com.example.appmgmt.infra.mail;

import com.example.appmgmt.common.AppConfig;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/** Jakarta Mail（javax.mail 1.6）による SMTP 送信。 */
public class SmtpMailSender implements MailSender {

    private final Session session;
    private final String fromAddress;
    private final String fromName;
    private final String replyTo;
    private final String user;
    private final String password;
    private final boolean auth;

    public SmtpMailSender(AppConfig config) {
        Properties p = new Properties();
        p.put("mail.smtp.host", config.getString("mail.smtp.host", "localhost"));
        p.put("mail.smtp.port", String.valueOf(config.getInt("mail.smtp.port", 25)));
        p.put("mail.smtp.starttls.enable", String.valueOf(config.getBoolean("mail.smtp.starttls", false)));
        p.put("mail.smtp.connectiontimeout", String.valueOf(config.getInt("mail.smtp.connection-timeout-ms", 5000)));
        p.put("mail.smtp.timeout", String.valueOf(config.getInt("mail.smtp.timeout-ms", 30000)));
        p.put("mail.smtp.writetimeout", String.valueOf(config.getInt("mail.smtp.timeout-ms", 30000)));
        this.auth = config.getBoolean("mail.smtp.auth", false);
        p.put("mail.smtp.auth", String.valueOf(auth));
        this.user = config.getString("mail.smtp.user");
        this.password = config.getString("mail.smtp.password");
        this.fromAddress = config.getString("mail.from.address", "noreply@example.com");
        this.fromName = config.getString("mail.from.name", "申込管理システム");
        this.replyTo = config.getString("mail.reply-to.address");
        this.session = auth ? Session.getInstance(p, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        }) : Session.getInstance(p);
    }

    @Override
    public MailSession open() throws MailException {
        final Transport transport;
        try {
            transport = session.getTransport("smtp");
            if (auth) {
                transport.connect(user, password);
            } else {
                transport.connect();
            }
        } catch (MessagingException e) {
            throw new MailException(MailException.Kind.INFRA, "SMTP サーバに接続できません: " + e.getMessage(), e);
        }
        return new MailSession() {
            @Override
            public void send(String to, String subject, String body) throws MailException {
                try {
                    MimeMessage msg = new MimeMessage(session);
                    msg.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
                    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                    if (replyTo != null && !replyTo.isBlank()) {
                        msg.setReplyTo(new InternetAddress[] {new InternetAddress(replyTo)});
                    }
                    msg.setSubject(subject, "UTF-8");
                    msg.setText(body, "UTF-8");
                    msg.setSentDate(new java.util.Date());
                    msg.saveChanges();
                    transport.sendMessage(msg, msg.getAllRecipients());
                } catch (AddressException | UnsupportedEncodingException e) {
                    throw new MailException(MailException.Kind.PERMANENT, "宛先アドレスが不正です: " + e.getMessage(), e);
                } catch (SendFailedException e) {
                    throw new MailException(MailException.Kind.PERMANENT, "送信が拒否されました: " + e.getMessage(), e);
                } catch (AuthenticationFailedException e) {
                    throw new MailException(MailException.Kind.INFRA, "SMTP 認証に失敗しました", e);
                } catch (MessagingException e) {
                    throw new MailException(MailException.Kind.TEMPORARY, "送信中にエラーが発生しました: " + e.getMessage(), e);
                }
            }

            @Override
            public void close() {
                try {
                    transport.close();
                } catch (MessagingException ignore) {
                    // 無視
                }
            }
        };
    }
}
