package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.SecurityAlert;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Firestore implementation for security alerts feed.
 * Pattern: Realtime collection adapter over alerts.
 * Scope: important admin action/security alerts.
 */
public class FirestoreAlertRepository implements AlertRepository {
    private static final List<String> IMPORTANT_ALERT_TYPES = Arrays.asList(
            AdminAlertPayloadFactory.TYPE_ENTRY_REPORT,
            AdminAlertPayloadFactory.TYPE_MANUAL_VIOLATION,
            AdminAlertPayloadFactory.TYPE_SCAN_RISK,
            AdminAlertPayloadFactory.TYPE_VEHICLE_REVIEW,
            AdminAlertPayloadFactory.TYPE_CHARGE_REVIEW
    );

    private final FirebaseFirestore firestore;
    private ListenerRegistration registration;

    public FirestoreAlertRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreAlertRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void listenAlerts(@NonNull AlertListener listener) {
        removeListeners();
        registration = firestore.collection("alerts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<SecurityAlert> alerts = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            SecurityAlert alert = SecurityAlert.fromMap(doc.getId(), doc.getData());
                            if (isImportantAlert(alert)) {
                                alerts.add(alert);
                            }
                        }
                    }
                    listener.onData(alerts);
                });
    }

    @Override
    public void createAlert(
            @Nullable String alertId,
            @NonNull Map<String, Object> payload,
            @NonNull OperationCallback callback
    ) {
        DocumentReference ref = alertId == null || alertId.trim().isEmpty()
                ? firestore.collection("alerts").document()
                : firestore.collection("alerts").document(alertId.trim());
        ref.set(payload)
                .addOnSuccessListener(unused -> callback.onComplete(true, "Alert created.", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to create alert.", error));
    }

    @Override
    public void updateAlertStatus(
            @NonNull String alertId,
            @NonNull String status,
            @NonNull String interventionSummary,
            @NonNull String reviewedByUid,
            @NonNull OperationCallback callback
    ) {
        if (alertId.trim().isEmpty()) {
            callback.onComplete(false, "Alert ID is required.", null);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("incidentStatus", status.trim());
        updates.put("interventionSummary", interventionSummary.trim());
        updates.put("reviewedByUid", reviewedByUid.trim());
        updates.put("reviewedAt", FieldValue.serverTimestamp());
        firestore.collection("alerts")
                .document(alertId.trim())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onComplete(true, "Alert updated.", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to update alert.", error));
    }

    @Override
    public void updateLinkedAlertStatus(
            @NonNull String linkedField,
            @NonNull String linkedId,
            @NonNull String status,
            @NonNull String interventionSummary,
            @NonNull String reviewedByUid,
            @NonNull OperationCallback callback
    ) {
        if (linkedField.trim().isEmpty() || linkedId.trim().isEmpty()) {
            callback.onComplete(false, "Linked alert identifier is required.", null);
            return;
        }
        firestore.collection("alerts")
                .whereEqualTo(linkedField.trim(), linkedId.trim())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(true, "No linked alert found.", null);
                        return;
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("incidentStatus", status.trim());
                    updates.put("interventionSummary", interventionSummary.trim());
                    updates.put("reviewedByUid", reviewedByUid.trim());
                    updates.put("reviewedAt", FieldValue.serverTimestamp());
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.set(doc.getReference(), updates, SetOptions.merge());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onComplete(true, "Alert updated.", null))
                            .addOnFailureListener(error -> callback.onComplete(false, "Failed to update alert.", error));
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to find linked alert.", error));
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private boolean isImportantAlert(@NonNull SecurityAlert alert) {
        return isImportantAlertType(alert.getAlertType());
    }

    static boolean isImportantAlertType(@NonNull String alertType) {
        return IMPORTANT_ALERT_TYPES.contains(alertType.trim().toLowerCase(Locale.getDefault()));
    }
}
