package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        map.put("schemaVersion", 2);
        map.put("entityType", "entry_request");
        map.put("entityId", "req-1");
        map.put("source", "guard_decision");
        map.put("outcome", "success");
        map.put("reasonCode", "entry_allowed");
        map.put("gateLabel", "in-gate");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("k", "v");
        map.put("metadata", metadata);
        map.put("description", "Entry logged");
        map.put("createdAt", created);

        AccessEvent event = AccessEvent.fromMap("evt-1", map);

        assertEquals("evt-1", event.getId());
        assertEquals("ENTRY", event.getEventType());
        assertEquals("guard-1", event.getActorUid());
        assertEquals("guard", event.getActorRole());
        assertEquals("req-1", event.getRequestId());
        assertEquals(2, event.getSchemaVersion());
        assertEquals("entry_request", event.getEntityType());
        assertEquals("req-1", event.getEntityId());
        assertEquals("guard_decision", event.getSource());
        assertEquals("success", event.getOutcome());
        assertEquals("entry_allowed", event.getReasonCode());
        assertEquals("in-gate", event.getGateLabel());
        assertEquals("v", event.getMetadata().get("k"));
        assertEquals("Entry logged", event.getDescription());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    public void fromMap_legacyRecordIsBackwardCompatible() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventType", "ENTRY");
        map.put("requestId", "legacy-req");
        map.put("actorRole", "guard");

        AccessEvent event = AccessEvent.fromMap("evt-legacy", map);

        assertEquals("legacy-req", event.getRequestId());
        assertEquals("legacy-req", event.getCorrelationId());
        assertEquals(1, event.getSchemaVersion());
        assertTrue(event.getMetadata().isEmpty());
        assertFalse(event.getEventType().isEmpty());
    }
}
