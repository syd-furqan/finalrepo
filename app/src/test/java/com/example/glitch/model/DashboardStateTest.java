package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies Firestore map-to-model mapping for dashboard state.
 */
public class DashboardStateTest {

    @Test
    public void fromMap_mapsConfiguredFields() {
        Timestamp updatedAt = new Timestamp(1234, 0);

        Map<String, Object> map = new HashMap<>();
        map.put("systemStatusTitle", "System Status: Alert");
        map.put("systemStatusMessage", "Gate 2 requires manual verification.");
        map.put("protocolLevel", "Level 2 - Restricted");
        map.put("protocolDescription", "Only pre-approved visitors are allowed.");
        map.put("updatedAt", updatedAt);

        DashboardState state = DashboardState.fromMap(map);

        assertEquals("System Status: Alert", state.getSystemStatusTitle());
        assertEquals("Gate 2 requires manual verification.", state.getSystemStatusMessage());
        assertEquals("Level 2 - Restricted", state.getProtocolLevel());
        assertEquals("Only pre-approved visitors are allowed.", state.getProtocolDescription());
        assertNotNull(state.getUpdatedAt());
    }
}
