package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GuestIdentityPolicyTest {

    @Test
    public void normalizeCnic_accepts13DigitsAndReturnsMasked() {
        assertEquals("35201-1234567-1", GuestIdentityPolicy.normalizeCnic("3520112345671"));
        assertEquals("35201-1234567-1", GuestIdentityPolicy.normalizeCnic("35201-1234567-1"));
        assertNull(GuestIdentityPolicy.normalizeCnic("35201-1234567"));
    }

    @Test
    public void cnicValidation_acceptsOnlyCanonicalMaskedFormat() {
        assertTrue(GuestIdentityPolicy.isValidCnic("35201-1234567-1"));
        assertFalse(GuestIdentityPolicy.isValidCnic("3520112345671"));
        assertFalse(GuestIdentityPolicy.isValidCnic("35201-123456-1"));
    }

    @Test
    public void normalizeVehiclePlate_convertsToCanonicalUppercase() {
        assertEquals("ABC-123", GuestIdentityPolicy.normalizeVehiclePlate("abc123"));
        assertEquals("ABC-1234", GuestIdentityPolicy.normalizeVehiclePlate("aBc-1234"));
        assertNull(GuestIdentityPolicy.normalizeVehiclePlate("AB12"));
    }

    @Test
    public void vehicleValidation_acceptsOnlyCanonicalPattern() {
        assertTrue(GuestIdentityPolicy.isValidVehiclePlate("ABC-123"));
        assertTrue(GuestIdentityPolicy.isValidVehiclePlate("ABC-1234"));
        assertFalse(GuestIdentityPolicy.isValidVehiclePlate("AB-1234"));
        assertFalse(GuestIdentityPolicy.isValidVehiclePlate("ABCD-123"));
    }
}
