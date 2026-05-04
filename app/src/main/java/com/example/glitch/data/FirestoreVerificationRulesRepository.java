package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.VerificationRules;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore implementation of verification rules repository.
 * Pattern: Realtime single-document binding and save operations.
 * Known issue: concurrent admin updates are last-write-wins in v1.
 */
public class FirestoreVerificationRulesRepository implements VerificationRulesRepository {
    private static final String COLLECTION = "verification_rules";
    private static final String DOC_CURRENT = "current";

    private final FirebaseFirestore firestore;
    private ListenerRegistration registration;

    public FirestoreVerificationRulesRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreVerificationRulesRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void listenRules(@NonNull RulesListener listener) {
        removeListeners();
        registration = firestore.collection(COLLECTION)
                .document(DOC_CURRENT)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        listener.onData(VerificationRules.defaultRules());
                        return;
                    }
                    listener.onData(VerificationRules.fromMap(snapshot.getData()));
                });
    }

    @Override
    public void saveRules(@NonNull VerificationRules rules, @NonNull SaveCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("enforceIdExpiry", rules.isEnforceIdExpiry());
        payload.put("bannedIdentifiersCsv", rules.getBannedIdentifiersCsv());
        payload.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION).document(DOC_CURRENT)
                .set(payload)
                .addOnSuccessListener(unused -> callback.onComplete(true, "Rules updated", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to update rules", error));
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
