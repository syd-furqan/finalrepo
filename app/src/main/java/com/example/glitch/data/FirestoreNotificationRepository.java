package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.NotificationItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore notification repository for per-user inbox streams.
 * Pattern: Nested collection adapter at notifications/{uid}/items.
 * Known issue: ordering assumes createdAt exists on all notification documents.
 */
public class FirestoreNotificationRepository implements NotificationRepository {
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String SUBCOLLECTION_ITEMS = "items";

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
        registration = firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)
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
    public void markNotificationRead(
            @NonNull String uid,
            @NonNull String notificationId,
            @NonNull OperationCallback callback
    ) {
        firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)
                .document(notificationId)
                .update(
                        "isRead", true,
                        "readAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> callback.onComplete(true, "Notification acknowledged", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Unable to acknowledge notification", error));
    }

    @Override
    public void markAllNotificationsRead(
            @NonNull String uid,
            @NonNull OperationCallback callback
    ) {
        firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = firestore.batch();
                    int unreadCount = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Object value = doc.get("isRead");
                        boolean isRead = value instanceof Boolean && (Boolean) value;
                        if (!isRead) {
                            unreadCount++;
                            batch.update(doc.getReference(),
                                    "isRead", true,
                                    "readAt", FieldValue.serverTimestamp());
                        }
                    }
                    if (unreadCount == 0) {
                        callback.onComplete(true, "All notifications are already read", null);
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onComplete(true, "All notifications marked as read", null))
                            .addOnFailureListener(error -> callback.onComplete(false, "Unable to mark notifications as read", error));
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Unable to load notifications", error));
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
