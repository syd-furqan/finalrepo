package com.example.glitch.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditEventPayloadFactoryTest {

    @Test
    public void build_includesSchemaAndSearchKeywords() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("passCode", "ABC12345");

        Map<String, Object> payload = AuditEventPayloadFactory.build(
                "PASS_ISSUED",
                "user-1",
                "student",
                "guest_pass",
                "pass-1",
                "req-1",
                "Guest pass issued",
                "issue_flow",
                "success",
                "",
                "in-gate",
                metadata
        );

        assertEquals(2, payload.get("schemaVersion"));
        assertEquals("PASS_ISSUED", payload.get("eventType"));
        assertEquals("user-1", payload.get("actorUid"));
        assertEquals("guest_pass", payload.get("entityType"));
        assertEquals("req-1", payload.get("requestId"));
        assertTrue(payload.containsKey("createdAt"));
        assertTrue(payload.containsKey("searchKeywords"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void buildSearchKeywords_tokenizesInput() {
        List<String> tokens = AuditEventPayloadFactory.buildSearchKeywords("Guard-1", "Entry denied: No ID");
        assertTrue(tokens.contains("guard-1"));
        assertTrue(tokens.contains("entry"));
        assertTrue(tokens.contains("denied"));
        assertTrue(tokens.contains("id"));
    }

    @Test
    public void build_defaultsUnknownActorWhenUidMissing() {
        Map<String, Object> payload = AuditEventPayloadFactory.build(
                "ENTRY_ALLOWED",
                "",
                "guard",
                "entry_request",
                "req-1",
                "req-1",
                "allowed",
                "guard_scan",
                "success",
                "",
                "in-gate",
                Collections.emptyMap()
        );
        assertEquals("unknown_actor", payload.get("actorUid"));
    }
}
