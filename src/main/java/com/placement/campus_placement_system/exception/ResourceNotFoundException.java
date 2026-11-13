package com.placement.campus_placement_system.exception;

/**
 * Thrown when a requested domain resource (such as a user) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
