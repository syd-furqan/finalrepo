package com.example.glitch.notification;

/**
 * Canonical local-alert event types derived from guest-pass lifecycle changes.
 */
public enum GuestPassLocalAlertType {
    CREATED,
    CANCELLED,
    ADMITTED,
    DENIED,
    OVERDUE,
    EXITED
}
