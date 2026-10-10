package com.ledgerpilot.ledgerpilot_backend.common.security;

import com.ledgerpilot.ledgerpilot_backend.common.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.concurrent.*;

@Component
public class FixedWindowRateLimiter {
    private record Bucket(long window, int count) {
    }

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void check(String key, int limit, Duration window) {
        long now = System.currentTimeMillis() / window.toMillis();
        Bucket b = buckets.compute(key, (k, v) -> v == null || v.window() != now ? new Bucket(now, 1) : new Bucket(now, v.count() + 1));
        if (b.count() > limit)
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Too many attempts. Please try again later.");
    }
}
