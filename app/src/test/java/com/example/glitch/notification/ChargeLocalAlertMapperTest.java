package com.example.glitch.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ChargeLocalAlertMapperTest {

    @Test
    public void addedIssued_mapsToCreated() {
        assertEquals(
                ChargeLocalAlertType.CREATED,
                ChargeLocalAlertMapper.resolve("ADDED", null, "issued")
        );
    }

    @Test
    public void modifiedSettled_mapsToPaid() {
        assertEquals(
                ChargeLocalAlertType.PAID,
                ChargeLocalAlertMapper.resolve("MODIFIED", "issued", "settled")
        );
    }

    @Test
    public void modifiedWaived_mapsToRemoved() {
        assertEquals(
                ChargeLocalAlertType.REMOVED,
                ChargeLocalAlertMapper.resolve("MODIFIED", "issued", "waived")
        );
    }

    @Test
    public void unchangedOrUnsupported_returnsNull() {
        assertNull(ChargeLocalAlertMapper.resolve("MODIFIED", "issued", "issued"));
        assertNull(ChargeLocalAlertMapper.resolve("ADDED", null, "settled"));
    }
}
