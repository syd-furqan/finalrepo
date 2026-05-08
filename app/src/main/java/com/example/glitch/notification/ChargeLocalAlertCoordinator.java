package com.example.glitch.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.FineCaseRecord;
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
 * Realtime coordinator translating charge lifecycle changes into local sponsor notifications.
 */
public class ChargeLocalAlertCoordinator {
    private static final String COLLECTION_FINE_CASES = "fine_cases";
    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_FACULTY = "faculty";

    private final FirebaseFirestore firestore;
    private final LocalNotificationHelper notificationHelper;
    private final AtomicInteger nextNotificationId = new AtomicInteger(2000);
    private final Map<String, String> lastStatusByChargeId = new HashMap<>();

    private ListenerRegistration registration;
    private boolean bootstrapComplete;
    private String activeSponsorUid = "";

    public ChargeLocalAlertCoordinator(@NonNull Context context) {
        this(context, FirebaseFirestore.getInstance());
    }

    ChargeLocalAlertCoordinator(@NonNull Context context, @NonNull FirebaseFirestore firestore) {
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
        registration = firestore.collection(COLLECTION_FINE_CASES)
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
        lastStatusByChargeId.clear();
    }

    private void handleSnapshot(@NonNull QuerySnapshot snapshot) {
        if (!bootstrapComplete) {
            lastStatusByChargeId.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                FineCaseRecord record = FineCaseRecord.fromMap(doc.getId(), doc.getData());
                lastStatusByChargeId.put(record.getId(), normalize(record.getStatus()));
            }
            bootstrapComplete = true;
            return;
        }

        for (DocumentChange change : snapshot.getDocumentChanges()) {
            String chargeId = change.getDocument().getId();
            if (DocumentChange.Type.REMOVED.equals(change.getType())) {
                lastStatusByChargeId.remove(chargeId);
                continue;
            }
            FineCaseRecord record = FineCaseRecord.fromMap(chargeId, change.getDocument().getData());
            String currentStatus = normalize(record.getStatus());
            String previousStatus = lastStatusByChargeId.get(chargeId);
            ChargeLocalAlertType type = ChargeLocalAlertMapper.resolve(
                    change.getType().name(),
                    previousStatus,
                    currentStatus
            );
            lastStatusByChargeId.put(chargeId, currentStatus);
            if (type != null) {
                dispatch(type, record);
            }
        }
    }

    private void dispatch(@NonNull ChargeLocalAlertType type, @NonNull FineCaseRecord record) {
        String guestName = fallback(record.getGuestName(), "Guest");
        String requestId = fallback(record.getViolationReportId(), "N/A");
        String title;
        String message;
        switch (type) {
            case CREATED:
                title = "Security Charge Created";
                message = "A security charge was created for " + guestName + " (Request " + requestId + ").";
                break;
            case PAID:
                title = "Security Charge Paid";
                message = "Charge marked paid for " + guestName + " (Request " + requestId + ").";
                break;
            case REMOVED:
                title = "Security Charge Removed";
                message = "Charge was removed for " + guestName + " (Request " + requestId + ").";
                break;
            default:
                title = "Security Charge Update";
                message = "Charge updated for Request " + requestId + ".";
                break;
        }
        notificationHelper.showGuestPassLifecycleNotification(title, message, nextNotificationId.incrementAndGet());
    }

    public static boolean isSupportedSponsorRole(@Nullable String role) {
        String normalized = normalize(role);
        return ROLE_STUDENT.equals(normalized) || ROLE_FACULTY.equals(normalized);
    }

    @NonNull
    private static String normalize(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.getDefault());
    }

    @NonNull
    private String fallback(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
