package com.tailored.resume.exception;

/** Raised when a free-tier user has spent their monthly tailoring runs. */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
