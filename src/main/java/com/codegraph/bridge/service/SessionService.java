package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private static final long SESSION_TTL_SECONDS = 1800;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, Session> sessions =
            new ConcurrentHashMap<>();

    public String createSession() {

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        String token = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        sessions.put(
                token,
                new Session(
                        Instant.now().plusSeconds(SESSION_TTL_SECONDS)
                )
        );

        return token;
    }

    public boolean isValid(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        Session session = sessions.get(token);

        if (session == null) {
            return false;
        }

        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(token);
            return false;
        }

        return true;
    }

    public void revoke(String token) {

        if (token != null) {
            sessions.remove(token);
        }
    }

    public void revokeAll() {
        sessions.clear();
    }

    private record Session(
            Instant expiresAt
    ) {}
}