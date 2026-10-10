package com.ledgerpilot.ledgerpilot_backend.auth.application;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Profile({"dev", "test"})
public class DevMailbox implements MailDelivery {
    public record Message(String type, String email, String token, String context, Instant createdAt) {
    }

    private final List<Message> messages = new CopyOnWriteArrayList<>();

    public void verification(String e, String t) {
        messages.add(new Message("VERIFICATION", e, t, null, Instant.now()));
    }

    public void passwordReset(String e, String t) {
        messages.add(new Message("PASSWORD_RESET", e, t, null, Instant.now()));
    }

    public void invitation(String e, String t, String n) {
        messages.add(new Message("INVITATION", e, t, n, Instant.now()));
    }

    public List<Message> forEmail(String email) {
        return messages.stream().filter(m -> m.email().equalsIgnoreCase(email)).toList();
    }
}
