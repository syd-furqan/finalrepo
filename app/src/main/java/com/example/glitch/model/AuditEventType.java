package com.example.glitch.model;

/**
 * Canonical audit event-type constants.
 */
public final class AuditEventType {
    public static final String REQUEST_CREATED = "REQUEST_CREATED";
    public static final String ENTRY_ALLOWED = "ENTRY_ALLOWED";
    public static final String ENTRY_DENIED = "ENTRY_DENIED";
    public static final String EXIT_LOGGED = "EXIT_LOGGED";
    public static final String REQUEST_OVERDUE = "REQUEST_OVERDUE";

    public static final String PASS_ISSUED = "PASS_ISSUED";
    public static final String PASS_CANCELLED = "PASS_CANCELLED";
    public static final String PASS_EXPIRED = "PASS_EXPIRED";
    public static final String PASS_USED = "PASS_USED";
    public static final String PASS_DENIED = "PASS_DENIED";
    public static final String PASS_EXITED = "PASS_EXITED";

    public static final String PENDING_DECISION_CREATED = "PENDING_DECISION_CREATED";
    public static final String PENDING_DECISION_RESOLVED_ALLOW = "PENDING_DECISION_RESOLVED_ALLOW";
    public static final String PENDING_DECISION_RESOLVED_DENY = "PENDING_DECISION_RESOLVED_DENY";
    public static final String PENDING_DECISION_INVALIDATED = "PENDING_DECISION_INVALIDATED";

    private AuditEventType() {
    }
}
