package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GatePolicy;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shared builder for schema-v2 audit events written to access_events.
 */
final class AuditEventPayloadFactory {
    static final int SCHEMA_VERSION = 2;
    static final String COLLECTION_ACCESS_EVENTS = "access_events";

    private AuditEventPayloadFactory() {
    }

    @NonNull
    static Map<String, Object> build(
            @NonNull String eventType,
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String entityType,
            @NonNull String entityId,
            @NonNull String requestId,
            @NonNull String description,
            @NonNull String source,
            @NonNull String outcome,
            @NonNull String reasonCode,
            @Nullable String gateLabel,
            @Nullable Map<String, Object> metadata
    ) {
        Map<String, Object> event = new HashMap<>();
        event.put("schemaVersion", SCHEMA_VERSION);
        event.put("eventType", safe(eventType));
        event.put("actorUid", actorUid.trim().isEmpty() ? "unknown_actor" : actorUid.trim());
        event.put("actorRole", safe(actorRole));
        event.put("entityType", safe(entityType));
        event.put("entityId", safe(entityId));
        event.put("requestId", safe(requestId));
        event.put("description", safe(description));
        event.put("source", safe(source));
        event.put("outcome", safe(outcome));
        event.put("reasonCode", safe(reasonCode));
        event.put("gateLabel", GatePolicy.normalizeStoredValue(gateLabel));
        event.put("metadata", metadata == null ? Collections.emptyMap() : new HashMap<>(metadata));
        event.put("searchKeywords", buildSearchKeywords(
                eventType,
                actorUid,
                actorRole,
                entityId,
                requestId,
                description
        ));
        event.put("createdAt", FieldValue.serverTimestamp());
        return event;
    }

    @NonNull
    static List<String> buildSearchKeywords(@Nullable String... values) {
        Set<String> tokens = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                tokenizeInto(tokens, value);
            }
        }
        return new ArrayList<>(tokens);
    }

    private static void tokenizeInto(@NonNull Set<String> destination, @Nullable String raw) {
        if (raw == null) {
            return;
        }
        String normalized = raw.trim().toLowerCase(Locale.getDefault());
        if (normalized.isEmpty()) {
            return;
        }
        destination.add(normalized);
        String[] parts = normalized.split("[^a-z0-9_-]+");
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                continue;
            }
            destination.add(part.trim());
        }
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
