package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.NotificationItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore notification repository for per-user inbox streams.
 * Pattern: Nested collection adapter at notifications/{uid}/items.
 * Known issue: ordering assumes createdAt exists on all notification documents.
 */
public class FirestoreNotificationRepository implements NotificationRepository {
    private final FirebaseFirestore firestore;
    private ListenerRegistration registration;

    public FirestoreNotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreNotificationRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void listenNotifications(@NonNull String uid, @NonNull NotificationListener listener) {
        removeListeners();
        registration = firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<NotificationItem> result = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            result.add(NotificationItem.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(result);
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
