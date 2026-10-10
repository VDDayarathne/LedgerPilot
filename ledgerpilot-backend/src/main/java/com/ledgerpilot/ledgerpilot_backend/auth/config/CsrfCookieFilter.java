package com.ledgerpilot.ledgerpilot_backend.auth.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class CsrfCookieFilter extends OncePerRequestFilter {
    private static final Set<String> COOKIE_MUTATIONS = Set.of("/api/v1/auth/refresh", "/api/v1/auth/logout");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (COOKIE_MUTATIONS.contains(req.getRequestURI()) && !"OPTIONS".equals(req.getMethod())) {
            String cookie = Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0])).filter(c -> "XSRF-TOKEN".equals(c.getName())).map(Cookie::getValue).findFirst().orElse(null);
            if (cookie == null || !java.security.MessageDigest.isEqual(cookie.getBytes(), Optional.ofNullable(req.getHeader("X-CSRF-TOKEN")).orElse("").getBytes())) {
                res.sendError(HttpStatus.FORBIDDEN.value(), "CSRF token is missing or invalid");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
