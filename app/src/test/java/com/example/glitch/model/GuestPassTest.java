package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies Firestore map-to-model mapping for guest passes.
 */
public class GuestPassTest {

    @Test
    public void fromMap_mapsAllFields() {
        Timestamp expiresAt = new Timestamp(1720011111L, 0);
        Timestamp createdAt = new Timestamp(1720000000L, 0);

        Map<String, Object> map = new HashMap<>();
        map.put("sponsorUid", "student-1");
        map.put("sponsorRole", "student");
        map.put("sponsorName", "Ali");
        map.put("sponsorEmail", "ali@lums.edu.pk");
        map.put("guestName", "Hamza");
        map.put("guestIdNumber", "35201-1234567-1");
        map.put("passCode", "ABC12345");
        map.put("status", "active");
        map.put("expiresAt", expiresAt);
        map.put("createdAt", createdAt);

        GuestPass pass = GuestPass.fromMap("pass-1", map);

        assertEquals("pass-1", pass.getId());
        assertEquals("student-1", pass.getSponsorUid());
        assertEquals("student", pass.getSponsorRole());
        assertEquals("Ali", pass.getSponsorName());
        assertEquals("ali@lums.edu.pk", pass.getSponsorEmail());
        assertEquals("Hamza", pass.getGuestName());
        assertEquals("35201-1234567-1", pass.getGuestIdNumber());
        assertEquals("ABC12345", pass.getPassCode());
        assertEquals("active", pass.getStatus());
        assertNotNull(pass.getExpiresAt());
        assertNotNull(pass.getCreatedAt());
    }
}

