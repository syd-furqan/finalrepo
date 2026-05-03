package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * Lightweight writer for audit events outside repository scopes (for example UI flows).
 */
public class AuditEventLogger {
    private final FirebaseFirestore firestore;

    public AuditEventLogger() {
        this(FirebaseFirestore.getInstance());
    }

    AuditEventLogger(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void log(
            @NonNull String eventType,
            @NonNull String entityType,
            @NonNull String entityId,
            @NonNull String requestId,
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String description,
            @NonNull String source,
            @NonNull String outcome,
            @NonNull String reasonCode,
            @NonNull String gateLabel,
            @Nullable Map<String, Object> metadata
    ) {
        firestore.collection(AuditEventPayloadFactory.COLLECTION_ACCESS_EVENTS)
                .add(AuditEventPayloadFactory.build(
                        eventType,
                        actorUid,
                        actorRole,
                        entityType,
                        entityId,
                        requestId,
                        description,
                        source,
                        outcome,
                        reasonCode,
                        gateLabel,
                        metadata
                ));
    }
}
