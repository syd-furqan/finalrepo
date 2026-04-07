package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies Firestore map-to-model mapping for access events.
 */
public class AccessEventTest {

    @Test
    public void fromMap_mapsAllFields() {
        Timestamp created = new Timestamp(1720000000L, 0);
        Map<String, Object> map = new HashMap<>();
        map.put("eventType", "ENTRY");
        map.put("actorUid", "guard-1");
        map.put("actorRole", "guard");
        map.put("requestId", "req-1");
        map.put("description", "Entry logged");
        map.put("createdAt", created);

        AccessEvent event = AccessEvent.fromMap("evt-1", map);

        assertEquals("evt-1", event.getId());
        assertEquals("ENTRY", event.getEventType());
        assertEquals("guard-1", event.getActorUid());
        assertEquals("guard", event.getActorRole());
        assertEquals("req-1", event.getRequestId());
        assertEquals("Entry logged", event.getDescription());
        assertNotNull(event.getCreatedAt());
    }
}

