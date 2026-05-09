package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void fromMap_mapsEntryReportFields() {
        Timestamp createdAt = new Timestamp(401, 0);
        Map<String, Object> map = new HashMap<>();
        map.put("alertType", "entry_report");
        map.put("entryRequestId", "req-44");
        map.put("severity", "HIGH");
        map.put("message", "Guard reported violation");
        map.put("guestPhone", "+923001234567");
        map.put("reportedByUid", "guard-1");
        map.put("reportedByRole", "guard");
        map.put("reportReasonCode", "guard_violation");
        map.put("reportSource", "guard_manual");
        map.put("createdAt", createdAt);

        SecurityAlert alert = SecurityAlert.fromMap("alert-2", map);

        assertEquals("entry_report", alert.getAlertType());
        assertEquals("req-44", alert.getEntryRequestId());
        assertEquals("req-44", alert.getIdentifier());
        assertEquals("guard-1", alert.getReportedByUid());
        assertEquals("+923001234567", alert.getGuestPhone());
        assertEquals("guard", alert.getReportedByRole());
        assertEquals("guard_violation", alert.getReasonCode());
        assertEquals("guard_manual", alert.getSource());
        assertTrue(alert.isEntryReportAlert());
    }
}
