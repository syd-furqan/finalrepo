package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies Firestore map-to-model mapping for vehicle requests.
 */
public class VehicleRequestRecordTest {

    @Test
    public void fromMap_mapsAllFields() {
        Timestamp createdAt = new Timestamp(1720033333L, 0);

        Map<String, Object> map = new HashMap<>();
        map.put("requesterUid", "staff-1");
        map.put("plateNumber", "LEA-1234");
        map.put("vehicleModel", "Corolla");
        map.put("status", "pending");
        map.put("createdAt", createdAt);

        VehicleRequestRecord record = VehicleRequestRecord.fromMap("veh-1", map);

        assertEquals("veh-1", record.getId());
        assertEquals("staff-1", record.getRequesterUid());
        assertEquals("LEA-1234", record.getPlateNumber());
        assertEquals("Corolla", record.getVehicleModel());
        assertEquals("submitted", record.getStatus());
        assertNotNull(record.getCreatedAt());
    }
}
