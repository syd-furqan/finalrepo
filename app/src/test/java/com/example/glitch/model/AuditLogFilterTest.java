package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AuditLogFilterTest {

    @Test
    public void last7Days_setsReasonableDefaultRange() {
        long now = System.currentTimeMillis();
        AuditLogFilter filter = AuditLogFilter.last7Days();

        assertTrue(filter.getToInclusiveMillis() <= now);
        assertTrue(filter.getToInclusiveMillis() >= filter.getFromInclusiveMillis());
    }

    @Test
    public void searchToken_usesFirstWordLowerCased() {
        AuditLogFilter filter = new AuditLogFilter(0L, 10L, Collections.emptyList(), Collections.emptyList(), "  Guard-1 Denied ");
        assertEquals("guard-1", filter.getSearchToken());
    }

    @Test
    public void matchesText_checksRequestEntityDescriptionAndActor() {
        AccessEvent event = new AccessEvent(
                "evt-1",
                "ENTRY_ALLOWED",
                "guard-99",
                "guard",
                "req-11",
                "Entry approved",
                null,
                2,
                "entry_request",
                "req-11",
                "scan",
                "success",
                "",
                "in-gate",
                Collections.emptyMap()
        );
        AuditLogFilter filter = new AuditLogFilter(
                0L,
                100L,
                Arrays.asList("ENTRY_ALLOWED"),
                Arrays.asList("guard"),
                "approved"
        );

        assertTrue(filter.matchesText(event));
        assertTrue(filter.matchesEventType(event.getEventType()));
        assertTrue(filter.matchesActorRole(event.getActorRole()));

        AuditLogFilter mismatch = new AuditLogFilter(0L, 100L, Collections.emptyList(), Collections.emptyList(), "vehicle");
        assertFalse(mismatch.matchesText(event));
    }
}
