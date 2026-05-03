package com.example.glitch.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GatePolicy;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One-time backfill runner that standardizes gateLabel to GatePolicy.STORED_VALUE.
 */
public class SingleGateMigrationRunner {
    private static final String TAG = "SingleGateMigration";
    private static final String COLLECTION_MIGRATIONS = "system_migrations";
    private static final String COLLECTION_ENTRY_REQUESTS = "entry_requests";
    private static final String COLLECTION_GUEST_PASSES = "guest_passes";
    private static final String DOC_SINGLE_GATE_V1 = "single_gate_v1";
    private static final int PAGE_SIZE = 200;

    private final FirebaseFirestore firestore;
    private boolean isRunning;

    public SingleGateMigrationRunner() {
        this(FirebaseFirestore.getInstance());
    }

    SingleGateMigrationRunner(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public synchronized void runIfNeeded(@Nullable String role, @NonNull String actorUid) {
        if (!isAdminRole(role) || actorUid.trim().isEmpty() || isRunning) {
            return;
        }
        isRunning = true;

        DocumentReference markerRef = firestore.collection(COLLECTION_MIGRATIONS).document(DOC_SINGLE_GATE_V1);
        markerRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (isAlreadyCompleted(snapshot)) {
                        finishRun();
                        return;
                    }
                    backfillCollection(COLLECTION_ENTRY_REQUESTS, new BackfillCallback() {
                        @Override
                        public void onComplete(int updatedEntryRequests) {
                            backfillCollection(COLLECTION_GUEST_PASSES, new BackfillCallback() {
                                @Override
                                public void onComplete(int updatedGuestPasses) {
                                    writeCompletionMarker(
                                            markerRef,
                                            actorUid,
                                            updatedEntryRequests,
                                            updatedGuestPasses
                                    );
                                }

                                @Override
                                public void onError(@NonNull Exception exception) {
                                    Log.w(TAG, "Guest-pass gate backfill failed", exception);
                                    finishRun();
                                }
                            });
                        }

                        @Override
                        public void onError(@NonNull Exception exception) {
                            Log.w(TAG, "Entry-request gate backfill failed", exception);
                            finishRun();
                        }
                    });
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Unable to check migration marker", error);
                    finishRun();
                });
    }

    private void backfillCollection(
            @NonNull String collectionName,
            @NonNull BackfillCallback callback
    ) {
        runBackfillPage(collectionName, null, 0, callback);
    }

    private void runBackfillPage(
            @NonNull String collectionName,
            @Nullable DocumentSnapshot cursor,
            int updatedSoFar,
            @NonNull BackfillCallback callback
    ) {
        Query query = firestore.collection(collectionName)
                .orderBy(FieldPath.documentId())
                .limit(PAGE_SIZE);
        if (cursor != null) {
            query = query.startAfter(cursor);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        callback.onComplete(updatedSoFar);
                        return;
                    }

                    WriteBatch batch = firestore.batch();
                    int updatedInPage = 0;
                    List<DocumentSnapshot> documents = snapshot.getDocuments();
                    for (DocumentSnapshot doc : documents) {
                        if (!shouldBackfillGateLabel(doc.get("gateLabel"))) {
                            continue;
                        }
                        batch.update(
                                doc.getReference(),
                                "gateLabel", GatePolicy.STORED_VALUE,
                                "updatedAt", FieldValue.serverTimestamp()
                        );
                        updatedInPage++;
                    }

                    DocumentSnapshot nextCursor = documents.get(documents.size() - 1);
                    int nextUpdatedTotal = updatedSoFar + updatedInPage;
                    if (updatedInPage == 0) {
                        runBackfillPage(collectionName, nextCursor, nextUpdatedTotal, callback);
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(unused ->
                                    runBackfillPage(collectionName, nextCursor, nextUpdatedTotal, callback))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    private void writeCompletionMarker(
            @NonNull DocumentReference markerRef,
            @NonNull String actorUid,
            int updatedEntryRequests,
            int updatedGuestPasses
    ) {
        Map<String, Object> marker = new HashMap<>();
        marker.put("completed", true);
        marker.put("version", DOC_SINGLE_GATE_V1);
        marker.put("gateLabel", GatePolicy.STORED_VALUE);
        marker.put("updatedByUid", actorUid.trim());
        marker.put("updatedEntryRequests", updatedEntryRequests);
        marker.put("updatedGuestPasses", updatedGuestPasses);
        marker.put("completedAt", FieldValue.serverTimestamp());
        marker.put("updatedAt", FieldValue.serverTimestamp());
        markerRef.set(marker)
                .addOnSuccessListener(unused -> finishRun())
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Unable to write migration completion marker", error);
                    finishRun();
                });
    }

    private boolean isAlreadyCompleted(@Nullable DocumentSnapshot markerSnapshot) {
        if (markerSnapshot == null || !markerSnapshot.exists()) {
            return false;
        }
        Object completed = markerSnapshot.get("completed");
        return completed instanceof Boolean && (Boolean) completed;
    }

    private synchronized void finishRun() {
        isRunning = false;
    }

    private boolean isAdminRole(@Nullable String role) {
        if (role == null) {
            return false;
        }
        return "admin".equals(role.trim().toLowerCase(Locale.getDefault()));
    }

    @NonNull
    static String safeString(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    static boolean shouldBackfillGateLabel(@Nullable Object gateLabelValue) {
        return !GatePolicy.isCanonicalStoredValue(safeString(gateLabelValue));
    }

    private interface BackfillCallback {
        void onComplete(int updatedCount);

        void onError(@NonNull Exception exception);
    }
}
