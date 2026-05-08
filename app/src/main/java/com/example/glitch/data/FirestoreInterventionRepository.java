package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.FineCaseRecord;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestBanRecord;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.StudentWarning;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreInterventionRepository implements InterventionRepository {
    private static final String COLLECTION_GUEST_BANS = "guest_bans";
    private static final String COLLECTION_FINE_CASES = "fine_cases";
    private static final String COLLECTION_STUDENT_WARNINGS = "student_warnings";
    private static final String COLLECTION_ENTRY_REQUESTS = "entry_requests";
    private static final String COLLECTION_GUEST_PASSES = "guest_passes";

    private final FirebaseFirestore firestore;
    private final AuditEventLogger auditEventLogger;
    private ListenerRegistration bansRegistration;
    private ListenerRegistration fineRegistration;
    private ListenerRegistration studentFineRegistration;
    private ListenerRegistration warningRegistration;

    public FirestoreInterventionRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreInterventionRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.auditEventLogger = new AuditEventLogger(firestore);
    }

    @Override
    public void isGuestBanned(@NonNull String cnic, @NonNull BanCheckCallback callback) {
        String normalizedCnic = GuestIdentityPolicy.normalizeCnic(cnic);
        if (normalizedCnic == null) {
            callback.onResult(false, null, "ERROR_INVALID_CNIC");
            return;
        }
        firestore.collection(COLLECTION_GUEST_BANS)
                .whereEqualTo("cnic", normalizedCnic)
                .whereEqualTo("status", GuestBanRecord.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    GuestBanRecord matching = null;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        GuestBanRecord record = GuestBanRecord.fromMap(doc.getId(), doc.getData());
                        if (record.isActiveNow()) {
                            matching = record;
                            break;
                        }
                    }
                    if (matching != null) {
                        callback.onResult(true, matching, "Guest is banned.");
                    } else {
                        callback.onResult(false, null, "Guest is not banned.");
                    }
                })
                .addOnFailureListener(error -> callback.onResult(false, null, "ERROR_BAN_CHECK_FAILED"));
    }

    @Override
    public void banGuest(
            @NonNull String cnic,
            @NonNull String adminUid,
            @NonNull String reasonCode,
            @NonNull String sourceReportId,
            @NonNull OperationCallback callback
    ) {
        String normalizedCnic = GuestIdentityPolicy.normalizeCnic(cnic);
        if (normalizedCnic == null) {
            callback.onComplete(false, "Enter CNIC as xxxxx-xxxxxxx-x.", null);
            return;
        }
        isGuestBanned(normalizedCnic, (banned, record, message) -> {
            if (banned) {
                callback.onComplete(true, "Guest is already banned.", null);
                return;
            }
            DocumentReference banRef = firestore.collection(COLLECTION_GUEST_BANS).document();
            Map<String, Object> payload = new HashMap<>();
            payload.put("cnic", normalizedCnic);
            payload.put("status", GuestBanRecord.STATUS_ACTIVE);
            payload.put("reasonCode", reasonCode.trim());
            payload.put("sourceReportId", sourceReportId.trim());
            payload.put("bannedByUid", adminUid.trim());
            payload.put("startAt", FieldValue.serverTimestamp());
            payload.put("endAt", null);
            payload.put("createdAt", FieldValue.serverTimestamp());
            payload.put("updatedAt", FieldValue.serverTimestamp());
            banRef.set(payload)
                    .addOnSuccessListener(unused -> {
                        invalidateActivePassesForBannedGuest(normalizedCnic, adminUid);
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("cnic", normalizedCnic);
                        metadata.put("sourceReportId", sourceReportId.trim());
                        auditEventLogger.log(
                                AuditEventType.GUEST_BANNED,
                                "guest_ban",
                                banRef.getId(),
                                sourceReportId.trim().isEmpty() ? banRef.getId() : sourceReportId.trim(),
                                adminUid.trim(),
                                "admin",
                                "Guest CNIC added to banned list",
                                "admin_intervention",
                                "success",
                                reasonCode.trim().isEmpty() ? "ban_guest" : reasonCode.trim(),
                                GatePolicy.STORED_VALUE,
                                metadata
                        );
                        callback.onComplete(true, "Guest banned.", null);
                    })
                    .addOnFailureListener(error -> callback.onComplete(false, "Failed to ban guest.", error));
        });
    }

    @Override
    public void unbanGuest(@NonNull String banId, @NonNull String adminUid, @NonNull OperationCallback callback) {
        if (banId.trim().isEmpty()) {
            callback.onComplete(false, "Ban ID is required.", null);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", GuestBanRecord.STATUS_INACTIVE);
        updates.put("endAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_GUEST_BANS)
                .document(banId.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    auditEventLogger.log(
                            AuditEventType.GUEST_UNBANNED,
                            "guest_ban",
                            banId.trim(),
                            banId.trim(),
                            adminUid.trim(),
                            "admin",
                            "Guest ban removed",
                            "admin_intervention",
                            "success",
                            "unban_guest",
                            GatePolicy.STORED_VALUE,
                            new HashMap<>()
                    );
                    callback.onComplete(true, "Guest unbanned.", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to unban guest.", error));
    }

    @Override
    public void createChargeForReport(
            @NonNull String violationReportId,
            @NonNull String sponsorUid,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String violationLevel,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        if (sponsorUid.trim().isEmpty()) {
            callback.onComplete(false, "Sponsor is missing on this report.", null);
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("sponsorUid", sponsorUid.trim());
        payload.put("violationReportId", violationReportId.trim());
        payload.put("guestName", guestName.trim());
        payload.put("guestIdNumber", guestIdNumber.trim());
        payload.put("amount", 0.0);
        payload.put("currency", "PKR");
        payload.put("reasonCode", violationLevel.trim());
        payload.put("status", FineCaseRecord.STATUS_ISSUED);
        payload.put("issuedByUid", adminUid.trim());
        payload.put("paymentNote", "");
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("updatedAt", FieldValue.serverTimestamp());
        payload.put("resolvedAt", null);
        firestore.collection(COLLECTION_FINE_CASES)
                .add(payload)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("violationReportId", violationReportId.trim());
                    metadata.put("sponsorUid", sponsorUid.trim());
                    auditEventLogger.log(
                            AuditEventType.FINE_ISSUED,
                            "fine_case",
                            ref.getId(),
                            violationReportId.trim(),
                            adminUid.trim(),
                            "admin",
                            "Charge issued to sponsor from violation report",
                            "admin_violation",
                            "success",
                            violationLevel.trim(),
                            GatePolicy.STORED_VALUE,
                            metadata
                    );
                    callback.onComplete(true, "Charge issued.", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to issue charge.", error));
    }

    @Override
    public void issueWarning(
            @NonNull String targetUid,
            @NonNull String targetName,
            @NonNull String targetRole,
            @NonNull String violationReportId,
            @NonNull String violationLevel,
            @NonNull String detail,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        if (targetUid.trim().isEmpty()) {
            callback.onComplete(false, "Target user is missing.", null);
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetUid", targetUid.trim());
        payload.put("targetName", targetName.trim());
        payload.put("targetRole", targetRole.trim());
        payload.put("violationReportId", violationReportId.trim());
        payload.put("violationLevel", violationLevel.trim());
        payload.put("detail", detail.trim().isEmpty() ? "Warning issued for " + violationLevel.toUpperCase() + " violation." : detail.trim());
        payload.put("issuedByUid", adminUid.trim());
        payload.put("createdAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_STUDENT_WARNINGS)
                .add(payload)
                .addOnSuccessListener(ref -> callback.onComplete(true, "Warning issued.", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to issue warning.", error));
    }

    @Override
    public void requestChargeRemoval(
            @NonNull String chargeId,
            @NonNull String paymentNote,
            @NonNull String studentUid,
            @NonNull OperationCallback callback
    ) {
        if (chargeId.trim().isEmpty()) {
            callback.onComplete(false, "Charge ID is required.", null);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", FineCaseRecord.STATUS_REMOVAL_REQUESTED);
        updates.put("paymentNote", paymentNote.trim());
        updates.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_FINE_CASES)
                .document(chargeId.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onComplete(true, "Removal request submitted.", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to submit removal request.", error));
    }

    @Override
    public void approveChargeRemoval(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        updateChargeStatus(chargeId, FineCaseRecord.STATUS_WAIVED, adminUid, "Charge removal approved.", callback);
    }

    @Override
    public void rejectChargeRemoval(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        updateChargeStatus(chargeId, FineCaseRecord.STATUS_ISSUED, adminUid, "Charge removal rejected.", callback);
    }

    private void updateChargeStatus(
            @NonNull String chargeId,
            @NonNull String newStatus,
            @NonNull String adminUid,
            @NonNull String successMessage,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        if (FineCaseRecord.STATUS_WAIVED.equals(newStatus) || FineCaseRecord.STATUS_SETTLED.equals(newStatus)) {
            updates.put("resolvedAt", FieldValue.serverTimestamp());
        }
        firestore.collection(COLLECTION_FINE_CASES)
                .document(chargeId.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onComplete(true, successMessage, null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to update charge.", error));
    }

    @Override
    public void listenBans(@NonNull BanListListener listener) {
        if (bansRegistration != null) bansRegistration.remove();
        bansRegistration = firestore.collection(COLLECTION_GUEST_BANS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<GuestBanRecord> bans = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            bans.add(GuestBanRecord.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(bans);
                });
    }

    @Override
    public void listenFineCases(@NonNull FineListListener listener) {
        if (fineRegistration != null) fineRegistration.remove();
        fineRegistration = firestore.collection(COLLECTION_FINE_CASES)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<FineCaseRecord> cases = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            cases.add(FineCaseRecord.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(cases);
                });
    }

    @Override
    public void listenChargesByStudent(@NonNull String studentUid, @NonNull FineListListener listener) {
        if (studentFineRegistration != null) studentFineRegistration.remove();
        studentFineRegistration = firestore.collection(COLLECTION_FINE_CASES)
                .whereEqualTo("sponsorUid", studentUid.trim())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<FineCaseRecord> cases = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            cases.add(FineCaseRecord.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(cases);
                });
    }

    @Override
    public void listenWarningsByStudent(@NonNull String studentUid, @NonNull WarningListListener listener) {
        if (warningRegistration != null) warningRegistration.remove();
        warningRegistration = firestore.collection(COLLECTION_STUDENT_WARNINGS)
                .whereEqualTo("targetUid", studentUid.trim())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<StudentWarning> warnings = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            warnings.add(StudentWarning.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(warnings);
                });
    }

    @Override
    public void removeListeners() {
        if (bansRegistration != null) { bansRegistration.remove(); bansRegistration = null; }
        if (fineRegistration != null) { fineRegistration.remove(); fineRegistration = null; }
        if (studentFineRegistration != null) { studentFineRegistration.remove(); studentFineRegistration = null; }
        if (warningRegistration != null) { warningRegistration.remove(); warningRegistration = null; }
    }

    private void invalidateActivePassesForBannedGuest(@NonNull String normalizedCnic, @NonNull String adminUid) {
        firestore.collection(COLLECTION_ENTRY_REQUESTS)
                .whereEqualTo("guestIdNumber", normalizedCnic)
                .whereIn("status", Arrays.asList("active", "overdue", "reported"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot requestDoc : snapshot.getDocuments()) {
                        Map<String, Object> requestUpdates = new HashMap<>();
                        requestUpdates.put("status", "reported");
                        requestUpdates.put("reportedAt", FieldValue.serverTimestamp());
                        requestUpdates.put("reportedByUid", adminUid.trim());
                        requestUpdates.put("reportReasonCode", "banned_guest");
                        requestUpdates.put("updatedAt", FieldValue.serverTimestamp());
                        requestDoc.getReference().set(requestUpdates, com.google.firebase.firestore.SetOptions.merge());

                        String requestId = requestDoc.getId();
                        firestore.collection(COLLECTION_GUEST_PASSES)
                                .whereEqualTo("entryRequestId", requestId)
                                .whereIn("status", Arrays.asList("active", "used", "overdue", "reported"))
                                .get()
                                .addOnSuccessListener(passSnapshot -> {
                                    for (DocumentSnapshot passDoc : passSnapshot.getDocuments()) {
                                        Map<String, Object> passUpdates = new HashMap<>();
                                        passUpdates.put("status", "reported");
                                        passUpdates.put("updatedAt", FieldValue.serverTimestamp());
                                        passDoc.getReference().set(passUpdates, com.google.firebase.firestore.SetOptions.merge());
                                    }
                                });
                    }
                });
    }

    @NonNull
    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
