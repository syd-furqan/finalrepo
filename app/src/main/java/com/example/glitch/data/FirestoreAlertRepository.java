package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.SecurityAlert;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore implementation for security alerts feed.
 * Pattern: Realtime collection adapter over alerts.
 * Scope: reported entry alerts only.
 */
public class FirestoreAlertRepository implements AlertRepository {
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
                .whereEqualTo("alertType", "entry_report")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<SecurityAlert> alerts = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            alerts.add(SecurityAlert.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(alerts);
                });
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
