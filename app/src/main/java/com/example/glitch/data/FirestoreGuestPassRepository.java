package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.GuestPass;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Firestore-backed guest pass repository.
 * Pattern: Collection adapter with realtime listing and pass-code lookup methods.
 * Known issue: expiry enforcement in v1 is checked client-side when scanning.
 */
public class FirestoreGuestPassRepository implements GuestPassRepository {
    private static final String COLLECTION_GUEST_PASSES = "guest_passes";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_CANCELLED = "cancelled";

    private final CollectionReference collection;
    private ListenerRegistration listRegistration;

    public FirestoreGuestPassRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreGuestPassRepository(@NonNull FirebaseFirestore firestore) {
        this.collection = firestore.collection(COLLECTION_GUEST_PASSES);
    }

    @Override
    public void createGuestPass(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String sponsorEmail,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            int expiryHours,
            @NonNull OperationCallback callback
    ) {
        int safeHours = Math.max(1, expiryHours);
        Timestamp expiresAt = new Timestamp(
                new java.util.Date(System.currentTimeMillis() + safeHours * 60L * 60L * 1000L)
        );

        Map<String, Object> data = new HashMap<>();
        data.put("sponsorUid", sponsorUid);
        data.put("sponsorRole", sponsorRole);
        data.put("sponsorName", sponsorName);
        data.put("sponsorEmail", sponsorEmail);
        data.put("guestName", guestName);
        data.put("guestIdNumber", guestIdNumber);
        data.put("passCode", UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        data.put("status", STATUS_ACTIVE);
        data.put("expiresAt", expiresAt);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        collection.add(data)
                .addOnSuccessListener(reference -> callback.onComplete(true, "Guest pass created", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to create guest pass", error));
    }

    @Override
    public void listenGuestPasses(@NonNull String sponsorUid, @NonNull PassListListener listener) {
        removeListeners();
        listRegistration = collection
                .whereEqualTo("sponsorUid", sponsorUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<GuestPass> passes = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            passes.add(GuestPass.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    // Avoid requiring composite indexes for sponsorUid + createdAt on early setups.
                    passes.sort(Comparator.comparing(
                            GuestPass::getCreatedAt,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                    ).reversed());
                    listener.onData(passes);
                });
    }

    @Override
    public void cancelGuestPass(@NonNull String passId, @NonNull OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_CANCELLED);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        collection.document(passId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onComplete(true, "Guest pass cancelled", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to cancel guest pass", error));
    }

    @Override
    public void findPassByCode(@NonNull String passCode, @NonNull PassLookupListener listener) {
        collection.whereEqualTo("passCode", passCode.trim().toUpperCase())
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        listener.onData(null);
                        return;
                    }
                    DocumentSnapshot document = snapshot.getDocuments().get(0);
                    listener.onData(GuestPass.fromMap(document.getId(), document.getData()));
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void removeListeners() {
        if (listRegistration != null) {
            listRegistration.remove();
            listRegistration = null;
        }
    }
}
