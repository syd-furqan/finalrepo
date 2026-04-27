package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies Firestore map-to-model mapping for notifications.
 */
public class NotificationItemTest {

    @Test
    public void fromMap_mapsAllFields() {
        Timestamp createdAt = new Timestamp(1720022222L, 0);

        Map<String, Object> map = new HashMap<>();
        map.put("title", "Request approved");
        map.put("message", "Your guest was approved.");
        map.put("type", "approval");
        map.put("isRead", true);
        map.put("createdAt", createdAt);

        NotificationItem item = NotificationItem.fromMap("notif-1", map);

        assertEquals("notif-1", item.getId());
        assertEquals("Request approved", item.getTitle());
        assertEquals("Your guest was approved.", item.getMessage());
        assertEquals("approval", item.getType());
        assertEquals(true, item.isRead());
        assertNotNull(item.getCreatedAt());
    }
}

