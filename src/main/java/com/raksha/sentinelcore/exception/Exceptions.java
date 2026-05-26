package com.raksha.sentinelcore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// ─────────────────────────────────────────────────────────────
// Domain Exceptions
// ─────────────────────────────────────────────────────────────

public class Exceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email already registered: " + email);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidPlanTransitionException extends RuntimeException {
        public InvalidPlanTransitionException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class SubscriptionNotActiveException extends RuntimeException {
        public SubscriptionNotActiveException(String message) { super(message); }
    }
}
