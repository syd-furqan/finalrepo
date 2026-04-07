package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.AccessEvent;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Firestore audit log repository reading access_events collection.
 * Pattern: Realtime list adapter with simple CSV serialization helper.
 * Known issue: export reflects most recently loaded in-memory events only.
 */
public class FirestoreAuditLogRepository implements AuditLogRepository {
    private final FirebaseFirestore firestore;
    private final List<AccessEvent> cache = new ArrayList<>();
    private ListenerRegistration registration;

    public FirestoreAuditLogRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreAuditLogRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void listenAuditLogs(@NonNull AuditLogListener listener) {
        removeListeners();
        registration = firestore.collection("access_events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    cache.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            cache.add(AccessEvent.fromMap(document.getId(), document.getData()));
                        }
                    }
                    listener.onData(new ArrayList<>(cache));
                });
    }

    @Override
    public void exportCsv(@NonNull ExportCallback callback) {
        StringBuilder builder = new StringBuilder();
        builder.append("id,eventType,actorUid,actorRole,requestId,description,createdAt\n");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (AccessEvent event : cache) {
            builder.append(safe(event.getId())).append(',')
                    .append(safe(event.getEventType())).append(',')
                    .append(safe(event.getActorUid())).append(',')
                    .append(safe(event.getActorRole())).append(',')
                    .append(safe(event.getRequestId())).append(',')
                    .append(safe(event.getDescription())).append(',');
            if (event.getCreatedAt() != null) {
                builder.append(formatter.format(event.getCreatedAt().toDate()));
            }
            builder.append('\n');
        }
        callback.onComplete(true, builder.toString(), null);
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    @NonNull
    private String safe(@NonNull String value) {
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
