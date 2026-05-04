package com.example.glitch.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class VehicleApplicationLocalAlertMapperTest {

    @Test
    public void addedSubmittedMapsToSubmittedAlert() {
        VehicleApplicationLocalAlertType type = VehicleApplicationLocalAlertMapper.resolve(
                "ADDED",
                null,
                "submitted",
                "register"
        );
        assertEquals(VehicleApplicationLocalAlertType.SUBMITTED, type);
    }

    @Test
    public void approvedRemovalMapsToRemovalApprovedAlert() {
        VehicleApplicationLocalAlertType type = VehicleApplicationLocalAlertMapper.resolve(
                "MODIFIED",
                "received",
                "approved",
                "remove"
        );
        assertEquals(VehicleApplicationLocalAlertType.REMOVAL_APPROVED, type);
    }

    @Test
    public void sameStatusDoesNotAlert() {
        VehicleApplicationLocalAlertType type = VehicleApplicationLocalAlertMapper.resolve(
                "MODIFIED",
                "received",
                "received",
                "register"
        );
        assertNull(type);
    }
}
