package com.ledgerpilot.ledgerpilot_backend.auth.application;

import com.ledgerpilot.ledgerpilot_backend.auth.config.AuthProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("!dev & !test")
public class SmtpMailDelivery implements MailDelivery {
    private final JavaMailSender mail;
    private final AuthProperties props;
    private final String from;

    public SmtpMailDelivery(JavaMailSender mail, AuthProperties props, @Value("${app.mail.from}") String from) {
        this.mail = mail;
        this.props = props;
        this.from = from;
    }

    private void send(String to, String subject, String path, String token) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(to);
        m.setSubject(subject);
        m.setText(props.frontendBaseUrl() + path + token);
        mail.send(m);
    }

    public void verification(String e, String t) {
        send(e, "Verify your LedgerPilot email", "/verify-email?token=", t);
    }

    public void passwordReset(String e, String t) {
        send(e, "Reset your LedgerPilot password", "/reset-password?token=", t);
    }

    public void invitation(String e, String t, String n) {
        send(e, "Join " + n + " on LedgerPilot", "/accept-invitation?token=", t);
    }
}
