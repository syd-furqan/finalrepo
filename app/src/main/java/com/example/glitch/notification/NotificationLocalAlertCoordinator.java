package com.example.glitch.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.NotificationItem;
import com.example.glitch.model.UserProfile;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationLocalAlertCoordinator {
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String SUBCOLLECTION_ITEMS = "items";
    private static final String TYPE_ANNOUNCEMENT = "announcement";

    private final FirebaseFirestore firestore;
    private final LocalNotificationHelper notificationHelper;
    private final AtomicInteger nextNotificationId = new AtomicInteger(5000);

    private ListenerRegistration registration;
    private boolean bootstrapComplete;
    private String activeUid = "";
    private String activeRole = "";

    public NotificationLocalAlertCoordinator(@NonNull Context context) {
        this(context, FirebaseFirestore.getInstance());
    }

    NotificationLocalAlertCoordinator(@NonNull Context context, @NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.notificationHelper = new LocalNotificationHelper(context.getApplicationContext());
    }

    public void start(@NonNull UserProfile profile) {
        String uid = profile.getUid().trim();
        if (!isSupportedRole(profile.getRole()) || uid.isEmpty()) {
            stop();
            return;
        }
        if (registration != null && uid.equals(activeUid)) {
            return;
        }
        stop();
        activeUid = uid;
        activeRole = profile.getRole();
        bootstrapComplete = false;
        registration = firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        return;
                    }
                    handleSnapshot(snapshot);
                });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        activeUid = "";
        activeRole = "";
        bootstrapComplete = false;
    }

    public static boolean isSupportedRole(@Nullable String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.US);
        return "student".equals(normalized)
                || "faculty".equals(normalized)
                || "guard".equals(normalized)
                || "monitor".equals(normalized);
    }

    private void handleSnapshot(@NonNull QuerySnapshot snapshot) {
        if (!bootstrapComplete) {
            bootstrapComplete = true;
            return;
        }
        for (DocumentChange change : snapshot.getDocumentChanges()) {
            if (!DocumentChange.Type.ADDED.equals(change.getType())) {
                continue;
            }
            NotificationItem item = NotificationItem.fromMap(change.getDocument().getId(), change.getDocument().getData());
            if (item.isRead()) {
                continue;
            }
            if (!TYPE_ANNOUNCEMENT.equalsIgnoreCase(item.getType())) {
                continue;
            }
            notificationHelper.showAnnouncementNotification(
                    fallback(item.getTitle(), "Announcement"),
                    fallback(item.getMessage(), "You have a new announcement."),
                    nextNotificationId.incrementAndGet(),
                    activeRole
            );
        }
    }

    @NonNull
    private String fallback(@Nullable String value, @NonNull String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
