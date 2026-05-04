package com.example.glitch.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.UserProfile;
import com.example.glitch.model.VehicleRequestRecord;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Realtime coordinator for local vehicle-application lifecycle alerts.
 */
public class VehicleApplicationLocalAlertCoordinator {
    private static final String COLLECTION_VEHICLE_REQUESTS = "vehicle_requests";

    private final FirebaseFirestore firestore;
    private final LocalNotificationHelper notificationHelper;
    private final Map<String, String> lastKnownStatusByRequestId = new HashMap<>();
    private final Map<String, String> requestKindByRequestId = new HashMap<>();
    private final AtomicInteger nextNotificationId = new AtomicInteger(4000);

    private ListenerRegistration registration;
    private boolean bootstrapComplete;
    private String activeRequesterUid = "";

    public VehicleApplicationLocalAlertCoordinator(@NonNull Context context) {
        this(context, FirebaseFirestore.getInstance());
    }

    VehicleApplicationLocalAlertCoordinator(@NonNull Context context, @NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.notificationHelper = new LocalNotificationHelper(context.getApplicationContext());
    }

    public void start(@NonNull UserProfile profile) {
        String role = normalize(profile.getRole());
        String requesterUid = profile.getUid().trim();
        if (!isSupportedSponsorRole(role) || requesterUid.isEmpty()) {
            stop();
            return;
        }
        if (registration != null && requesterUid.equals(activeRequesterUid)) {
            return;
        }

        stop();
        activeRequesterUid = requesterUid;
        bootstrapComplete = false;

        registration = firestore.collection(COLLECTION_VEHICLE_REQUESTS)
                .whereEqualTo("requesterUid", requesterUid)
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
        activeRequesterUid = "";
        bootstrapComplete = false;
        lastKnownStatusByRequestId.clear();
        requestKindByRequestId.clear();
    }

    private void handleSnapshot(@NonNull QuerySnapshot snapshot) {
        if (!bootstrapComplete) {
            lastKnownStatusByRequestId.clear();
            requestKindByRequestId.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                VehicleRequestRecord record = VehicleRequestRecord.fromMap(doc.getId(), doc.getData());
                lastKnownStatusByRequestId.put(record.getId(), normalize(record.getStatus()));
                requestKindByRequestId.put(record.getId(), normalize(record.getRequestKind()));
            }
            bootstrapComplete = true;
            return;
        }

        for (DocumentChange change : snapshot.getDocumentChanges()) {
            String requestId = change.getDocument().getId();
            if (DocumentChange.Type.REMOVED.equals(change.getType())) {
                lastKnownStatusByRequestId.remove(requestId);
                requestKindByRequestId.remove(requestId);
                continue;
            }
            VehicleRequestRecord record = VehicleRequestRecord.fromMap(requestId, change.getDocument().getData());
            String currentStatus = normalize(record.getStatus());
            String previousStatus = lastKnownStatusByRequestId.get(requestId);
            String currentKind = normalize(record.getRequestKind());

            VehicleApplicationLocalAlertType type = VehicleApplicationLocalAlertMapper.resolve(
                    change.getType().name(),
                    previousStatus,
                    currentStatus,
                    currentKind
            );

            lastKnownStatusByRequestId.put(requestId, currentStatus);
            requestKindByRequestId.put(requestId, currentKind);
            if (type != null) {
                dispatch(type, record);
            }
        }
    }

    private void dispatch(@NonNull VehicleApplicationLocalAlertType type, @NonNull VehicleRequestRecord record) {
        String title;
        String message;
        String plate = fallback(record.getPlateNumber(), "N/A");
        switch (type) {
            case SUBMITTED:
                title = "Vehicle Application Submitted";
                message = "Application submitted for " + plate + ".";
                break;
            case CANCELLED:
                title = "Vehicle Application Cancelled";
                message = "Application for " + plate + " was cancelled.";
                break;
            case APPROVED:
                title = "Vehicle Application Approved";
                message = "Vehicle " + plate + " has been approved.";
                break;
            case DENIED:
                title = "Vehicle Application Denied";
                message = "Vehicle application for " + plate + " was denied.";
                break;
            case REMOVAL_APPROVED:
                title = "Vehicle Removal Approved";
                message = "Registered vehicle " + plate + " has been removed.";
                break;
            default:
                title = "Vehicle Application Update";
                message = "Vehicle application updated for " + plate + ".";
                break;
        }
        notificationHelper.showVehicleProgramNotification(title, message, nextNotificationId.incrementAndGet());
    }

    public static boolean isSupportedSponsorRole(@Nullable String role) {
        String normalized = normalize(role);
        return "student".equals(normalized) || "faculty".equals(normalized);
    }

    @NonNull
    private String fallback(@Nullable String value, @NonNull String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
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
