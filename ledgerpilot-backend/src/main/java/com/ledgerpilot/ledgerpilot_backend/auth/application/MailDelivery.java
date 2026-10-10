package com.ledgerpilot.ledgerpilot_backend.auth.application;

public interface MailDelivery {
    void verification(String email, String token);

    void passwordReset(String email, String token);

    void invitation(String email, String token, String tenantName);
}
