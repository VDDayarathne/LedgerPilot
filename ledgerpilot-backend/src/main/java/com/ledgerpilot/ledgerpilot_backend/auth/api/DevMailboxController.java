package com.ledgerpilot.ledgerpilot_backend.auth.api;

import com.ledgerpilot.ledgerpilot_backend.auth.application.DevMailbox;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/api/v1/dev/mailbox")
public class DevMailboxController {
    private final DevMailbox box;

    public DevMailboxController(DevMailbox b) {
        box = b;
    }

    @GetMapping
    public List<DevMailbox.Message> messages(@RequestParam String email) {
        return box.forEmail(email);
    }
}
