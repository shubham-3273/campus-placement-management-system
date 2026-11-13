package com.placement.campus_placement_system.exception;

/**
 * Thrown when a public registration attempts to assign a restricted role (e.g., TPO or RECRUITER).
 */
public class InvalidRoleRegistrationException extends RuntimeException {

    public InvalidRoleRegistrationException(String message) {
        super(message);
    }
}
