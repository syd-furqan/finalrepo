package com.example.glitch.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.glitch.model.AuditEventType;

import org.junit.Test;

import java.util.List;

public class AdminAuditLogFragmentTest {

    @Test
    public void gateAuditEventTypesExcludePendingAndNonGateEvents() {
        List<String> eventTypes = AdminAuditLogFragment.getGateAuditEventTypes();

        assertTrue(eventTypes.contains(AuditEventType.ENTRY_ALLOWED));
        assertTrue(eventTypes.contains(AuditEventType.ENTRY_DENIED));
        assertTrue(eventTypes.contains(AuditEventType.EXIT_LOGGED));
        assertTrue(eventTypes.contains(AuditEventType.ENTRY_REPORTED_OVERDUE));

        assertFalse(eventTypes.contains(AuditEventType.PENDING_DECISION_CREATED));
        assertFalse(eventTypes.contains(AuditEventType.PENDING_DECISION_RESOLVED_ALLOW));
        assertFalse(eventTypes.contains(AuditEventType.GUEST_BANNED));
        assertFalse(eventTypes.contains(AuditEventType.FINE_ISSUED));
        assertFalse(eventTypes.contains("VEHICLE_APPLICATION_SUBMITTED"));
    }
}
