package com.example.glitch.model;

import static org.junit.Assert.assertEquals;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies Firestore map-to-model mapping for automated security alerts.
 */
public class SecurityAlertTest {

    @Test
    public void fromMap_mapsAllFields() {
        Timestamp createdAt = new Timestamp(400, 0);
        Map<String, Object> map = new HashMap<>();
        map.put("identifier", "ID-204");
        map.put("failCount", 4);
        map.put("severity", "HIGH");
        map.put("message", "Repeated failed verification");
        map.put("createdAt", createdAt);

        SecurityAlert alert = SecurityAlert.fromMap("alert-1", map);

        assertEquals("alert-1", alert.getId());
        assertEquals("ID-204", alert.getIdentifier());
        assertEquals(4, alert.getFailCount());
        assertEquals("HIGH", alert.getSeverity());
        assertEquals("Repeated failed verification", alert.getMessage());
        assertEquals(createdAt, alert.getCreatedAt());
    }
}
