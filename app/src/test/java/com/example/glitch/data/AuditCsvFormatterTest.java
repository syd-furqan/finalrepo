package com.example.glitch.data;

import static org.junit.Assert.assertTrue;

import com.example.glitch.model.AccessEvent;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class AuditCsvFormatterTest {

    @Test
    public void toCsv_hasHeaderAndEscapesValues() {
        AccessEvent event = new AccessEvent(
                "evt-1",
                "ENTRY_DENIED",
                "guard-1",
                "guard",
                "req-1",
                "Denied, reason: \"No ID\"",
                new Timestamp(1720000000L, 0),
                2,
                "entry_request",
                "req-1",
                "guard_scan",
                "failure",
                "entry_denied",
                "in-gate",
                Collections.singletonMap("k", "v")
        );

        String csv = AuditCsvFormatter.toCsv(List.of(event), "Asia/Karachi");

        assertTrue(csv.startsWith("id,eventType,schemaVersion"));
        assertTrue(csv.contains("\"Denied, reason: \"\"No ID\"\"\""));
        assertTrue(csv.contains("ENTRY_DENIED"));
    }
}
