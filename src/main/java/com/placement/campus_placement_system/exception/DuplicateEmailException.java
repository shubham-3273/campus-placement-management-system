package com.placement.campus_placement_system.exception;

/**
 * Thrown when attempting to register a user with an email address that already exists.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
