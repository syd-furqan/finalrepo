package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Locale;
import java.util.Map;

public final class UserNotificationWriter {
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String SUBCOLLECTION_ITEMS = "items";

    private final FirebaseFirestore firestore;

    public UserNotificationWriter(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void write(
            @NonNull String uid,
            @NonNull String type,
            @NonNull String sourceId,
            @NonNull String title,
            @NonNull String message,
            @NonNull String sourceCollection
    ) {
        String trimmedUid = uid.trim();
        if (trimmedUid.isEmpty()) {
            return;
        }
        String notificationId = UserNotificationPayloadFactory.deterministicId(type, sourceId);
        firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(trimmedUid)
                .collection(SUBCOLLECTION_ITEMS)
                .document(notificationId)
                .set(UserNotificationPayloadFactory.item(title, message, type, sourceCollection, sourceId), SetOptions.merge());
    }

    public void addToBatch(
            @NonNull WriteBatch batch,
            @NonNull String uid,
            @NonNull String type,
            @NonNull String sourceId,
            @NonNull String title,
            @NonNull String message,
            @NonNull String sourceCollection
    ) {
        String trimmedUid = uid.trim();
        if (trimmedUid.isEmpty()) {
            return;
        }
        String notificationId = UserNotificationPayloadFactory.deterministicId(type, sourceId);
        DocumentReference ref = firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(trimmedUid)
                .collection(SUBCOLLECTION_ITEMS)
                .document(notificationId);
        Map<String, Object> payload = UserNotificationPayloadFactory.item(title, message, type, sourceCollection, sourceId);
        batch.set(ref, payload, SetOptions.merge());
    }

    public static boolean supportsRole(@NonNull String rawRole) {
        String role = rawRole.trim().toLowerCase(Locale.US);
        return "student".equals(role) || "faculty".equals(role);
    }
}
