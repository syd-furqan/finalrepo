package com.example.glitch.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for guest-pass change -> local alert type mapping rules.
 */
public class GuestPassLocalAlertMapperTest {

    @Test
    public void addedActivePass_mapsToCreatedAlert() {
        GuestPassLocalAlertType result = GuestPassLocalAlertMapper.resolve("ADDED", null, "active");
        assertEquals(GuestPassLocalAlertType.CREATED, result);
    }

    @Test
    public void addedNonActivePass_doesNotEmitAlert() {
        assertNull(GuestPassLocalAlertMapper.resolve("ADDED", null, "cancelled"));
        assertNull(GuestPassLocalAlertMapper.resolve("ADDED", null, "used"));
    }

    @Test
    public void modifiedStatusTransitions_mapToExpectedAlerts() {
        assertEquals(
                GuestPassLocalAlertType.CANCELLED,
                GuestPassLocalAlertMapper.resolve("MODIFIED", "active", "cancelled")
        );
        assertEquals(
                GuestPassLocalAlertType.ADMITTED,
                GuestPassLocalAlertMapper.resolve("MODIFIED", "active", "used")
        );
        assertEquals(
                GuestPassLocalAlertType.DENIED,
                GuestPassLocalAlertMapper.resolve("MODIFIED", "active", "denied")
        );
        assertEquals(
                GuestPassLocalAlertType.OVERDUE,
                GuestPassLocalAlertMapper.resolve("MODIFIED", "active", "overdue")
        );
        assertEquals(
                GuestPassLocalAlertType.EXITED,
                GuestPassLocalAlertMapper.resolve("MODIFIED", "used", "exited")
        );
    }

    @Test
    public void unchangedOrUnknownStatus_doesNotEmitAlert() {
        assertNull(GuestPassLocalAlertMapper.resolve("MODIFIED", "active", "active"));
        assertNull(GuestPassLocalAlertMapper.resolve("MODIFIED", "active", "expired"));
        assertNull(GuestPassLocalAlertMapper.resolve("REMOVED", "used", "used"));
    }
}
