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
    public static final String ENTRY_REPORTED_MANUAL = "ENTRY_REPORTED_MANUAL";
    public static final String ENTRY_REPORTED_OVERDUE = "ENTRY_REPORTED_OVERDUE";

    public static final String PASS_ISSUED = "PASS_ISSUED";
    public static final String PASS_CANCELLED = "PASS_CANCELLED";
    public static final String PASS_EXPIRED = "PASS_EXPIRED";
    public static final String PASS_USED = "PASS_USED";
    public static final String PASS_DENIED = "PASS_DENIED";
    public static final String PASS_REPORTED = "PASS_REPORTED";
    public static final String PASS_EXITED = "PASS_EXITED";

    public static final String PENDING_DECISION_CREATED = "PENDING_DECISION_CREATED";
    public static final String PENDING_DECISION_RESOLVED_ALLOW = "PENDING_DECISION_RESOLVED_ALLOW";
    public static final String PENDING_DECISION_RESOLVED_DENY = "PENDING_DECISION_RESOLVED_DENY";
    public static final String PENDING_DECISION_INVALIDATED = "PENDING_DECISION_INVALIDATED";

    public static final String GUEST_BANNED = "GUEST_BANNED";
    public static final String GUEST_UNBANNED = "GUEST_UNBANNED";
    public static final String FINE_ISSUED = "FINE_ISSUED";
    public static final String FINE_WAIVED = "FINE_WAIVED";
    public static final String FINE_SETTLED = "FINE_SETTLED";
    public static final String INCIDENT_CLOSED = "INCIDENT_CLOSED";
    public static final String ENTRY_INVALIDATED_BAN = "ENTRY_INVALIDATED_BAN";
    public static final String CHARGE_CREATED = "CHARGE_CREATED";
    public static final String CHARGE_PAID = "CHARGE_PAID";
    public static final String CHARGE_REMOVED = "CHARGE_REMOVED";

    private AuditEventType() {
    }
}
