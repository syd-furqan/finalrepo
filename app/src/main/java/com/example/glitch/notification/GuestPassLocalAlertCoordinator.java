package com.example.glitch.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.UserProfile;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Realtime coordinator that translates guest-pass document changes into local Android alerts.
 */
public class GuestPassLocalAlertCoordinator {
    private static final String COLLECTION_GUEST_PASSES = "guest_passes";
    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_FACULTY = "faculty";

    private final FirebaseFirestore firestore;
    private final LocalNotificationHelper notificationHelper;
    private final Map<String, String> lastKnownStatusByPassId = new HashMap<>();
    private final AtomicInteger nextNotificationId = new AtomicInteger(1000);

    private ListenerRegistration registration;
    private boolean bootstrapComplete;
    private String activeSponsorUid = "";

    public GuestPassLocalAlertCoordinator(@NonNull Context context) {
        this(context, FirebaseFirestore.getInstance());
    }

    GuestPassLocalAlertCoordinator(@NonNull Context context, @NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.notificationHelper = new LocalNotificationHelper(context.getApplicationContext());
    }

    public void start(@NonNull UserProfile profile) {
        String role = normalize(profile.getRole());
        String sponsorUid = profile.getUid().trim();
        if (!isSupportedSponsorRole(role) || sponsorUid.isEmpty()) {
            stop();
            return;
        }
        if (registration != null && sponsorUid.equals(activeSponsorUid)) {
            return;
        }

        stop();
        activeSponsorUid = sponsorUid;
        bootstrapComplete = false;
        registration = firestore.collection(COLLECTION_GUEST_PASSES)
                .whereEqualTo("sponsorUid", sponsorUid)
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
        activeSponsorUid = "";
        bootstrapComplete = false;
        lastKnownStatusByPassId.clear();
    }

    public static boolean isSupportedSponsorRole(@Nullable String role) {
        String normalized = normalize(role);
        return ROLE_STUDENT.equals(normalized) || ROLE_FACULTY.equals(normalized);
    }

    private void handleSnapshot(@NonNull QuerySnapshot snapshot) {
        if (!bootstrapComplete) {
            lastKnownStatusByPassId.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                GuestPass pass = GuestPass.fromMap(doc.getId(), doc.getData());
                lastKnownStatusByPassId.put(pass.getId(), normalize(pass.getStatus()));
            }
            bootstrapComplete = true;
            return;
        }

        for (DocumentChange change : snapshot.getDocumentChanges()) {
            String passId = change.getDocument().getId();
            if (DocumentChange.Type.REMOVED.equals(change.getType())) {
                lastKnownStatusByPassId.remove(passId);
                continue;
            }

            GuestPass pass = GuestPass.fromMap(passId, change.getDocument().getData());
            String currentStatus = normalize(pass.getStatus());
            String previousStatus = lastKnownStatusByPassId.get(passId);
            GuestPassLocalAlertType type = GuestPassLocalAlertMapper.resolve(
                    change.getType().name(),
                    previousStatus,
                    currentStatus
            );
            lastKnownStatusByPassId.put(passId, currentStatus);
            if (type != null) {
                dispatchNotification(type, pass);
            }
        }
    }

    private void dispatchNotification(@NonNull GuestPassLocalAlertType type, @NonNull GuestPass pass) {
        String title;
        switch (type) {
            case CREATED:
                title = "Guest Pass Created";
                break;
            case CANCELLED:
                title = "Guest Pass Cancelled";
                break;
            case ADMITTED:
                title = "Visitor Admitted";
                break;
            case DENIED:
                title = "Visitor Denied";
                break;
            case OVERDUE:
                title = "Visitor Overdue";
                break;
            case EXITED:
                title = "Visitor Exited";
                break;
            default:
                title = "Guest Pass Update";
                break;
        }
        String message = buildMessage(type, pass);
        notificationHelper.showGuestPassLifecycleNotification(
                title,
                message,
                nextNotificationId.incrementAndGet()
        );
    }

    @NonNull
    private String buildMessage(@NonNull GuestPassLocalAlertType type, @NonNull GuestPass pass) {
        String guestName = fallback(pass.getGuestName(), "Guest");
        String passCode = fallback(pass.getPassCode(), "N/A");
        String gate = GatePolicy.toDisplayLabel(pass.getGateLabel());

        switch (type) {
            case CREATED:
                return "Pass " + passCode + " created for " + guestName + " at " + gate + ".";
            case CANCELLED:
                return "Pass " + passCode + " for " + guestName + " was cancelled.";
            case ADMITTED:
                return guestName + " was admitted using pass " + passCode + " at " + gate + ".";
            case DENIED:
                return guestName + " was denied at " + gate + " (pass " + passCode + ").";
            case OVERDUE:
                return guestName + " is overdue for pass " + passCode + ".";
            case EXITED:
                return guestName + " has exited. Pass " + passCode + " is now closed.";
            default:
                return guestName + " • Pass " + passCode;
        }
    }

    @NonNull
    private String fallback(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    @NonNull
    private static String normalize(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.getDefault());
    }
}
