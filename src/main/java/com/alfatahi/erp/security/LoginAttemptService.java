package com.alfatahi.erp.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 10;

    private static final class Attempt {
        private int failures = 0;
        private Instant lockedUntil = null;
    }

    private final ConcurrentHashMap<String, Attempt> attemptsByUsername = new ConcurrentHashMap<>();

    public void recordFailure(String username) {
        String key = normalize(username);
        if (key == null) return;

        attemptsByUsername.compute(key, (k, existing) -> {
            Attempt attempt = existing != null ? existing : new Attempt();

            if (attempt.lockedUntil != null && Instant.now().isAfter(attempt.lockedUntil)) {
                attempt.failures = 0;
                attempt.lockedUntil = null;
            }

            attempt.failures++;
            if (attempt.failures >= MAX_ATTEMPTS) {
                attempt.lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60);
            }
            return attempt;
        });
    }

    public void recordSuccess(String username) {
        unlock(username);
    }

    public void unlock(String username) {
        String key = normalize(username);
        if (key == null) return;
        attemptsByUsername.remove(key);
    }

    public boolean isLocked(String username) {
        String key = normalize(username);
        if (key == null) return false;

        Attempt attempt = attemptsByUsername.get(key);
        if (attempt == null || attempt.lockedUntil == null) return false;

        if (Instant.now().isAfter(attempt.lockedUntil)) {
            attemptsByUsername.remove(key);
            return false;
        }
        return true;
    }

    private String normalize(String username) {
        if (username == null || username.isBlank()) return null;
        return username.trim().toLowerCase();
    }
}
