package com.example.glitch.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.Map;

public class UserNotificationPayloadFactoryTest {

    @Test
    public void deterministicId_sanitizesAndIsStable() {
        assertEquals(
                "guest_pass_created_pass_1",
                UserNotificationPayloadFactory.deterministicId("Guest Pass Created", "pass/1")
        );
    }

    @Test
    public void item_isUnreadAndStoresSourceFields() {
        Map<String, Object> payload = UserNotificationPayloadFactory.item(
                "Fine Issued",
                "A fine was issued.",
                "fine_issued",
                "fine_cases",
                "fine-1"
        );

        assertEquals("Fine Issued", payload.get("title"));
        assertEquals("fine_issued", payload.get("type"));
        assertEquals("fine_cases", payload.get("sourceCollection"));
        assertEquals("fine-1", payload.get("sourceId"));
        assertFalse((Boolean) payload.get("isRead"));
    }
}
