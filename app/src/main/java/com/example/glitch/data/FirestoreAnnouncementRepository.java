package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.InGateServiceControl;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Firestore implementation for high-priority announcements and in-gate inhibition state.
 */
public class FirestoreAnnouncementRepository implements AnnouncementRepository {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_ANNOUNCEMENTS = "announcements";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String SUBCOLLECTION_ITEMS = "items";
    private static final String COLLECTION_SERVICE_CONTROLS = "service_controls";
    private static final String DOC_IN_GATE = "in_gate";

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_FACULTY = "faculty";
    private static final String ROLE_GUARD = "guard";
    private static final String ROLE_MONITOR = "monitor";

    private static final int NOTIFICATION_BATCH_SIZE = 450;

    private final FirebaseFirestore firestore;

    public FirestoreAnnouncementRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreAnnouncementRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void publishAnnouncement(
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String actorName,
            @NonNull String message,
            boolean allCampus,
            @NonNull Set<String> selectedRoles,
            boolean inhibitInGate,
            @NonNull Set<String> affectedRoles,
            boolean timed,
            @Nullable Long endsAtMillis,
            @NonNull OperationCallback callback
    ) {
        String normalizedMessage = message.trim();
        if (normalizedMessage.isEmpty()) {
            callback.onComplete(false, "Announcement message is required.", null);
            return;
        }

        Set<String> normalizedAffectedRoles = normalizeAudienceRoles(affectedRoles);
        if (inhibitInGate && normalizedAffectedRoles.isEmpty()) {
            callback.onComplete(false, "Select at least one affected role.", null);
            return;
        }

        Timestamp endsAt = null;
        String inhibitionMode = "";
        if (inhibitInGate) {
            inhibitionMode = timed ? InGateServiceControl.MODE_TIMED : InGateServiceControl.MODE_MANUAL;
            if (timed) {
                if (endsAtMillis == null || endsAtMillis <= System.currentTimeMillis()) {
                    callback.onComplete(false, "Timed inhibition must end in the future.", null);
                    return;
                }
                endsAt = new Timestamp(new java.util.Date(endsAtMillis));
            }
        }

        Set<String> audienceRoles = resolveAudienceRoles(allCampus, selectedRoles, inhibitInGate, normalizedAffectedRoles);
        if (audienceRoles.isEmpty()) {
            callback.onComplete(false, "Select at least one audience role.", null);
            return;
        }

        DocumentReference announcementRef = firestore.collection(COLLECTION_ANNOUNCEMENTS).document();
        Map<String, Object> announcementPayload = buildAnnouncementPayload(
                actorUid,
                actorRole,
                actorName,
                inhibitInGate ? "In-Gate Service Advisory" : "Admin Announcement",
                normalizedMessage,
                audienceRoles,
                inhibitInGate,
                inhibitionMode,
                endsAt,
                normalizedAffectedRoles
        );

        Map<String, Object> mutableServiceControlPayload = null;
        if (inhibitInGate) {
            mutableServiceControlPayload = buildServiceControlPayload(
                    true,
                    normalizedAffectedRoles,
                    normalizedMessage,
                    inhibitionMode,
                    endsAt,
                    actorUid,
                    announcementRef.getId()
            );
        }
        final Map<String, Object> serviceControlPayload = mutableServiceControlPayload;

        fetchActiveUserIdsByRoles(audienceRoles, new UserIdListener() {
            @Override
            public void onData(@NonNull List<String> userIds) {
                commitAnnouncement(
                        announcementRef,
                        announcementPayload,
                        serviceControlPayload,
                        userIds,
                        "Admin Announcement",
                        normalizedMessage,
                        callback
                );
            }

            @Override
            public void onError(@NonNull Exception exception) {
                callback.onComplete(false, "Failed to resolve announcement audience.", exception);
            }
        });
    }

    @Override
    public void restoreInGateServices(
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String actorName,
            @NonNull String message,
            @NonNull OperationCallback callback
    ) {
        firestore.collection(COLLECTION_SERVICE_CONTROLS)
                .document(DOC_IN_GATE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    InGateServiceControl current = snapshot.exists()
                            ? InGateServiceControl.fromMap(snapshot.getData())
                            : InGateServiceControl.inactive();

                    Set<String> audienceRoles = new LinkedHashSet<>();
                    audienceRoles.addAll(current.getAffectedRoles());
                    audienceRoles.add(ROLE_GUARD);
                    audienceRoles.add(ROLE_MONITOR);
                    if (audienceRoles.isEmpty()) {
                        audienceRoles.addAll(defaultAllCampusRoles());
                    }

                    String normalizedMessage = message.trim().isEmpty()
                            ? "In-Gate services are now operational."
                            : message.trim();

                    DocumentReference announcementRef = firestore.collection(COLLECTION_ANNOUNCEMENTS).document();
                    Map<String, Object> announcementPayload = buildAnnouncementPayload(
                            actorUid,
                            actorRole,
                            actorName,
                            "In-Gate Services Restored",
                            normalizedMessage,
                            audienceRoles,
                            false,
                            "",
                            null,
                            Collections.emptySet()
                    );
                    announcementPayload.put("restoration", true);

                    Map<String, Object> serviceControlPayload = buildServiceControlPayload(
                            false,
                            Collections.emptySet(),
                            normalizedMessage,
                            "",
                            null,
                            actorUid,
                            announcementRef.getId()
                    );

                    fetchActiveUserIdsByRoles(audienceRoles, new UserIdListener() {
                        @Override
                        public void onData(@NonNull List<String> userIds) {
                            commitAnnouncement(
                                    announcementRef,
                                    announcementPayload,
                                    serviceControlPayload,
                                    userIds,
                                    "In-Gate Services Restored",
                                    normalizedMessage,
                                    callback
                            );
                        }

                        @Override
                        public void onError(@NonNull Exception exception) {
                            callback.onComplete(false, "Failed to resolve restoration audience.", exception);
                        }
                    });
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to load service control state.", error));
    }

    @Override
    public void getCurrentInGateControl(@NonNull ControlStateListener listener) {
        firestore.collection(COLLECTION_SERVICE_CONTROLS)
                .document(DOC_IN_GATE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        listener.onData(InGateServiceControl.inactive());
                        return;
                    }
                    listener.onData(InGateServiceControl.fromMap(snapshot.getData()));
                })
                .addOnFailureListener(listener::onError);
    }

    private void commitAnnouncement(
            @NonNull DocumentReference announcementRef,
            @NonNull Map<String, Object> announcementPayload,
            @Nullable Map<String, Object> serviceControlPayload,
            @NonNull List<String> userIds,
            @NonNull String notificationTitle,
            @NonNull String notificationMessage,
            @NonNull OperationCallback callback
    ) {
        List<String> recipients = new ArrayList<>(new LinkedHashSet<>(userIds));
        commitAnnouncementChunk(
                announcementRef,
                announcementPayload,
                serviceControlPayload,
                recipients,
                0,
                notificationTitle,
                notificationMessage,
                callback
        );
    }

    private void commitAnnouncementChunk(
            @NonNull DocumentReference announcementRef,
            @NonNull Map<String, Object> announcementPayload,
            @Nullable Map<String, Object> serviceControlPayload,
            @NonNull List<String> recipients,
            int startIndex,
            @NonNull String notificationTitle,
            @NonNull String notificationMessage,
            @NonNull OperationCallback callback
    ) {
        WriteBatch batch = firestore.batch();
        boolean isFirstChunk = startIndex == 0;

        if (isFirstChunk) {
            batch.set(announcementRef, announcementPayload, SetOptions.merge());
            if (serviceControlPayload != null) {
                batch.set(
                        firestore.collection(COLLECTION_SERVICE_CONTROLS).document(DOC_IN_GATE),
                        serviceControlPayload,
                        SetOptions.merge()
                );
            }
        }

        int endExclusive = Math.min(startIndex + NOTIFICATION_BATCH_SIZE, recipients.size());
        for (int i = startIndex; i < endExclusive; i++) {
            String uid = recipients.get(i).trim();
            if (uid.isEmpty()) {
                continue;
            }
            String notificationId = UserNotificationPayloadFactory.deterministicId("announcement", announcementRef.getId());
            Map<String, Object> payload = UserNotificationPayloadFactory.item(
                    notificationTitle,
                    notificationMessage,
                    "announcement",
                    COLLECTION_ANNOUNCEMENTS,
                    announcementRef.getId()
            );
            payload.put("priority", "high");
            payload.put("category", "announcements");
            batch.set(
                    firestore.collection(COLLECTION_NOTIFICATIONS)
                            .document(uid)
                            .collection(SUBCOLLECTION_ITEMS)
                            .document(notificationId),
                    payload,
                    SetOptions.merge()
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (endExclusive >= recipients.size()) {
                        callback.onComplete(true, "Announcement published.", null);
                        return;
                    }
                    commitAnnouncementChunk(
                            announcementRef,
                            announcementPayload,
                            null,
                            recipients,
                            endExclusive,
                            notificationTitle,
                            notificationMessage,
                            callback
                    );
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to publish announcement.", error));
    }

    @NonNull
    private Map<String, Object> buildAnnouncementPayload(
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String actorName,
            @NonNull String title,
            @NonNull String message,
            @NonNull Set<String> audienceRoles,
            boolean inGateInhibition,
            @NonNull String inhibitionMode,
            @Nullable Timestamp endsAt,
            @NonNull Set<String> affectedRoles
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "announcement");
        payload.put("priority", "high");
        payload.put("title", title.trim());
        payload.put("message", message.trim());
        payload.put("audienceRoles", new ArrayList<>(audienceRoles));
        payload.put("createdByUid", actorUid.trim());
        payload.put("createdByRole", actorRole.trim().toLowerCase(Locale.US));
        payload.put("createdByName", actorName.trim());
        payload.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> lockdown = new HashMap<>();
        lockdown.put("inhibitionApplied", inGateInhibition);
        lockdown.put("mode", inhibitionMode);
        lockdown.put("affectedRoles", new ArrayList<>(affectedRoles));
        lockdown.put("endsAt", endsAt);
        payload.put("inGateLockdown", lockdown);
        return payload;
    }

    @NonNull
    private Map<String, Object> buildServiceControlPayload(
            boolean active,
            @NonNull Set<String> affectedRoles,
            @NonNull String reason,
            @NonNull String mode,
            @Nullable Timestamp endsAt,
            @NonNull String actorUid,
            @NonNull String announcementId
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("isActive", active);
        payload.put("affectedRoles", new ArrayList<>(affectedRoles));
        payload.put("reason", reason.trim());
        payload.put("mode", mode.trim().toLowerCase(Locale.US));
        payload.put("endsAt", endsAt);
        payload.put("updatedBy", actorUid.trim());
        payload.put("updatedAt", FieldValue.serverTimestamp());
        payload.put("announcementId", announcementId.trim());
        return payload;
    }

    private void fetchActiveUserIdsByRoles(@NonNull Set<String> roles, @NonNull UserIdListener listener) {
        if (roles.isEmpty()) {
            listener.onData(Collections.emptyList());
            return;
        }

        List<String> roleList = new ArrayList<>(roles);
        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("isActive", true)
                .whereIn("role", roleList)
                .get()
                .addOnSuccessListener(snapshot -> listener.onData(extractUserIds(snapshot, roles)))
                .addOnFailureListener(listener::onError);
    }

    @NonNull
    private List<String> extractUserIds(@NonNull QuerySnapshot snapshot, @NonNull Set<String> roles) {
        List<String> userIds = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String uid = doc.getId().trim();
            Object roleValue = doc.get("role");
            String role = roleValue == null ? "" : String.valueOf(roleValue).trim().toLowerCase(Locale.US);
            if (uid.isEmpty() || !roles.contains(role)) {
                continue;
            }
            userIds.add(uid);
        }
        return userIds;
    }

    @NonNull
    private Set<String> resolveAudienceRoles(
            boolean allCampus,
            @NonNull Set<String> selectedRoles,
            boolean inhibitionEnabled,
            @NonNull Set<String> affectedRoles
    ) {
        Set<String> roles = new LinkedHashSet<>();
        if (allCampus) {
            roles.addAll(defaultAllCampusRoles());
        } else {
            roles.addAll(normalizeAudienceRoles(selectedRoles));
        }

        if (inhibitionEnabled) {
            roles.addAll(affectedRoles);
            roles.add(ROLE_GUARD);
            roles.add(ROLE_MONITOR);
        }

        // Admins are intentionally excluded from broadcast audience.
        roles.remove("admin");
        return roles;
    }

    @NonNull
    private Set<String> normalizeAudienceRoles(@NonNull Set<String> roles) {
        Set<String> normalized = new HashSet<>();
        for (String raw : roles) {
            if (raw == null) {
                continue;
            }
            String role = raw.trim().toLowerCase(Locale.US);
            if (ROLE_STUDENT.equals(role)
                    || ROLE_FACULTY.equals(role)
                    || ROLE_GUARD.equals(role)
                    || ROLE_MONITOR.equals(role)) {
                normalized.add(role);
            }
        }
        return normalized;
    }

    @NonNull
    private List<String> defaultAllCampusRoles() {
        return Arrays.asList(ROLE_STUDENT, ROLE_FACULTY, ROLE_GUARD, ROLE_MONITOR);
    }

    private interface UserIdListener {
        void onData(@NonNull List<String> userIds);

        void onError(@NonNull Exception exception);
    }
}
