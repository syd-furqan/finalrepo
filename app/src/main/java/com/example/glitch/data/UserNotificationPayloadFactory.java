package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class UserNotificationPayloadFactory {
    private UserNotificationPayloadFactory() {
    }

    @NonNull
    public static String deterministicId(@NonNull String type, @NonNull String sourceId) {
        String raw = type.trim().toLowerCase(Locale.US) + "_" + sourceId.trim();
        return raw.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    @NonNull
    public static Map<String, Object> item(
            @NonNull String title,
            @NonNull String message,
            @NonNull String type,
            @NonNull String sourceCollection,
            @NonNull String sourceId
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title.trim());
        payload.put("message", message.trim());
        payload.put("type", type.trim());
        payload.put("sourceCollection", sourceCollection.trim());
        payload.put("sourceId", sourceId.trim());
        payload.put("isRead", false);
        payload.put("createdAt", FieldValue.serverTimestamp());
        return payload;
    }
}
