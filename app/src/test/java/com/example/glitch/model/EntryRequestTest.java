package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies Firestore map-to-model mapping for entry requests.
 */
public class EntryRequestTest {

    @Test
    public void fromMap_mapsAllFields() {
        Timestamp entered = new Timestamp(1000, 0);
        Timestamp expires = new Timestamp(2000, 0);

        Map<String, Object> map = new HashMap<>();
        map.put("fullName", "Ahmed Mansoor");
        map.put("roleTag", "Guest");
        map.put("hostName", "Prof. Salman");
        map.put("gateLabel", "West Wing - 02");
        map.put("guestIdNumber", "35201-1234567-1");
        map.put("hasVehicle", true);
        map.put("vehiclePlate", "abc-123");
        map.put("guestType", "vehicle");
        map.put("requesterRole", "student");
        map.put("studentId", "STU-123");
        map.put("enteredAt", entered);
        map.put("status", "pending");
        map.put("expiresAt", expires);
        map.put("iconType", "guest");

        EntryRequest request = EntryRequest.fromMap("abc123", map);

        assertEquals("abc123", request.getId());
        assertEquals("Ahmed Mansoor", request.getFullName());
        assertEquals("Guest", request.getRoleTag());
        assertEquals("Prof. Salman", request.getHostName());
        assertEquals(GatePolicy.STORED_VALUE, request.getGateLabel());
        assertEquals("35201-1234567-1", request.getGuestIdNumber());
        assertEquals(true, request.hasVehicle());
        assertEquals("ABC-123", request.getVehiclePlate());
        assertEquals("vehicle", request.getGuestType());
        assertEquals("student", request.getRequesterRole());
        assertEquals("STU-123", request.getRequesterStudentId());
        assertEquals("pending", request.getStatus());
        assertEquals("guest", request.getIconType());
        assertNotNull(request.getEnteredAt());
        assertNotNull(request.getExpiresAt());
    }
}
