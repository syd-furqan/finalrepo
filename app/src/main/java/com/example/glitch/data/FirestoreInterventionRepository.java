package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.FineCaseRecord;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestBanRecord;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.IncidentInterventionRecord;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore-backed intervention repository for bans and sponsor fines.
 */
public class FirestoreInterventionRepository implements InterventionRepository {
    private static final String COLLECTION_GUEST_BANS = "guest_bans";
    private static final String COLLECTION_FINE_CASES = "fine_cases";
    private static final String COLLECTION_INTERVENTIONS = "interventions";
    private static final String COLLECTION_ALERTS = "alerts";
    private static final String COLLECTION_ENTRY_REQUESTS = "entry_requests";
    private static final String COLLECTION_GUEST_PASSES = "guest_passes";
    private static final String COLLECTION_ACCESS_EVENTS = "access_events";
    private static final String STATUS_ISSUED = "issued";
    private static final String STATUS_WAIVED = "waived";
    private static final String STATUS_SETTLED = "settled";

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_REPORTED = "reported";

    private final FirebaseFirestore firestore;
    private final AuditEventLogger auditEventLogger;
    private ListenerRegistration bansRegistration;
    private ListenerRegistration fineRegistration;
    private ListenerRegistration interventionsRegistration;

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
            @NonNull String sourceAlertId,
            @NonNull String sourceRequestId,
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
            payload.put("sourceAlertId", sourceAlertId.trim());
            payload.put("sourceRequestId", sourceRequestId.trim());
            payload.put("bannedByUid", adminUid.trim());
            payload.put("startAt", FieldValue.serverTimestamp());
            payload.put("endAt", null);
            payload.put("createdAt", FieldValue.serverTimestamp());
            payload.put("updatedAt", FieldValue.serverTimestamp());
            banRef.set(payload)
                    .addOnSuccessListener(unused -> {
                        updateAlertInterventionState(sourceAlertId, sourceRequestId, "actioned", "Guest banned", banRef.getId(), "");
                        invalidateActiveRecordsForBannedGuest(normalizedCnic, adminUid, sourceAlertId, sourceRequestId);
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("cnic", normalizedCnic);
                        metadata.put("sourceAlertId", sourceAlertId.trim());
                        metadata.put("sourceRequestId", sourceRequestId.trim());
                        auditEventLogger.log(
                                AuditEventType.GUEST_BANNED,
                                "guest_ban",
                                banRef.getId(),
                                sourceRequestId.trim().isEmpty() ? banRef.getId() : sourceRequestId.trim(),
                                adminUid.trim(),
                                "admin",
                                "Guest CNIC added to banned list",
                                "admin_intervention",
                                "success",
                                reasonCode.trim().isEmpty() ? "ban_guest" : reasonCode.trim(),
                                GatePolicy.STORED_VALUE,
                                metadata
                        );
                        callback.onComplete(true, "Guest banned and active entries invalidated.", null);
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
    public void issueFine(
            @NonNull String sponsorUid,
            @NonNull String requestId,
            @NonNull String alertId,
            double amount,
            @NonNull String currency,
            @NonNull String reasonCode,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        if (sponsorUid.trim().isEmpty()) {
            callback.onComplete(false, "Sponsor UID is missing for this incident.", null);
            return;
        }
        if (amount <= 0.0) {
            callback.onComplete(false, "Fine amount must be greater than zero.", null);
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("sponsorUid", sponsorUid.trim());
        payload.put("requestId", requestId.trim());
        payload.put("alertId", alertId.trim());
        payload.put("amount", amount);
        payload.put("currency", currency.trim().isEmpty() ? "PKR" : currency.trim().toUpperCase());
        payload.put("reasonCode", reasonCode.trim().isEmpty() ? "security_violation" : reasonCode.trim());
        payload.put("status", FineCaseRecord.STATUS_ISSUED);
        payload.put("issuedByUid", adminUid.trim());
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("updatedAt", FieldValue.serverTimestamp());
        payload.put("resolvedAt", null);

        firestore.collection(COLLECTION_FINE_CASES)
                .add(payload)
                .addOnSuccessListener(reference -> {
                    updateAlertInterventionState(alertId, requestId, "actioned", "Fine issued", "", reference.getId());
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("sponsorUid", sponsorUid.trim());
                    metadata.put("amount", amount);
                    metadata.put("currency", currency.trim().isEmpty() ? "PKR" : currency.trim().toUpperCase());
                    auditEventLogger.log(
                            AuditEventType.FINE_ISSUED,
                            "fine_case",
                            reference.getId(),
                            requestId.trim().isEmpty() ? reference.getId() : requestId.trim(),
                            adminUid.trim(),
                            "admin",
                            "Fine issued to sponsor",
                            "admin_intervention",
                            "success",
                            reasonCode.trim().isEmpty() ? "security_violation" : reasonCode.trim(),
                            GatePolicy.STORED_VALUE,
                            metadata
                    );
                    callback.onComplete(true, "Fine issued.", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to issue fine.", error));
    }

    @Override
    public void createChargeForAlert(
            @NonNull String alertId,
            @NonNull String requestId,
            @NonNull String sponsorUid,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        String normalizedAlertId = alertId.trim();
        String normalizedRequestId = requestId.trim();
        String normalizedSponsorUid = sponsorUid.trim();
        if (normalizedAlertId.isEmpty() || normalizedRequestId.isEmpty() || normalizedSponsorUid.isEmpty()) {
            callback.onComplete(false, "Alert, request, and sponsor IDs are required.", null);
            return;
        }

        String chargeId = "charge_" + normalizedAlertId;
        DocumentReference chargeRef = firestore.collection(COLLECTION_FINE_CASES).document(chargeId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot existing = transaction.get(chargeRef);
                    if (existing.exists()) {
                        String status = asString(existing.get("status"));
                        if (STATUS_ISSUED.equalsIgnoreCase(status)) {
                            DocumentReference alertRef = firestore.collection(COLLECTION_ALERTS).document(normalizedAlertId);
                            Map<String, Object> alertUpdates = new HashMap<>();
                            alertUpdates.put("incidentStatus", "in_review");
                            alertUpdates.put("interventionSummary", "Charge created");
                            alertUpdates.put("updatedAt", FieldValue.serverTimestamp());
                            transaction.set(alertRef, alertUpdates, com.google.firebase.firestore.SetOptions.merge());

                            String interventionId = normalizedRequestId.isEmpty() ? normalizedAlertId : normalizedRequestId;
                            DocumentReference interventionRef = firestore.collection(COLLECTION_INTERVENTIONS).document(interventionId);
                            Map<String, Object> interventionUpdates = new HashMap<>();
                            interventionUpdates.put("alertId", normalizedAlertId);
                            interventionUpdates.put("requestId", normalizedRequestId);
                            interventionUpdates.put("fineId", chargeId);
                            interventionUpdates.put("status", "in_review");
                            interventionUpdates.put("summary", "Charge created");
                            interventionUpdates.put("updatedAt", FieldValue.serverTimestamp());
                            transaction.set(interventionRef, interventionUpdates, com.google.firebase.firestore.SetOptions.merge());
                        }
                        return new ChargeMutationResult(
                                chargeId,
                                normalizedRequestId,
                                normalizedAlertId,
                                status.isEmpty() ? STATUS_ISSUED : status,
                                asString(existing.get("guestIdNumber")),
                                true
                        );
                    }

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("sponsorUid", normalizedSponsorUid);
                    payload.put("requestId", normalizedRequestId);
                    payload.put("alertId", normalizedAlertId);
                    payload.put("guestName", guestName.trim());
                    payload.put("guestIdNumber", guestIdNumber.trim());
                    payload.put("amount", 0.0);
                    payload.put("currency", "PKR");
                    payload.put("reasonCode", "security_violation_charge");
                    payload.put("status", STATUS_ISSUED);
                    payload.put("issuedByUid", adminUid.trim());
                    payload.put("createdAt", FieldValue.serverTimestamp());
                    payload.put("updatedAt", FieldValue.serverTimestamp());
                    payload.put("resolvedAt", null);
                    transaction.set(chargeRef, payload);

                    DocumentReference alertRef = firestore.collection(COLLECTION_ALERTS).document(normalizedAlertId);
                    Map<String, Object> alertUpdates = new HashMap<>();
                    alertUpdates.put("incidentStatus", "in_review");
                    alertUpdates.put("interventionSummary", "Charge created");
                    alertUpdates.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.set(alertRef, alertUpdates, com.google.firebase.firestore.SetOptions.merge());

                    String interventionId = normalizedRequestId.isEmpty() ? normalizedAlertId : normalizedRequestId;
                    DocumentReference interventionRef = firestore.collection(COLLECTION_INTERVENTIONS).document(interventionId);
                    Map<String, Object> interventionUpdates = new HashMap<>();
                    interventionUpdates.put("alertId", normalizedAlertId);
                    interventionUpdates.put("requestId", normalizedRequestId);
                    interventionUpdates.put("fineId", chargeId);
                    interventionUpdates.put("status", "in_review");
                    interventionUpdates.put("summary", "Charge created");
                    interventionUpdates.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.set(interventionRef, interventionUpdates, com.google.firebase.firestore.SetOptions.merge());

                    return new ChargeMutationResult(
                            chargeId,
                            normalizedRequestId,
                            normalizedAlertId,
                            STATUS_ISSUED,
                            guestIdNumber.trim(),
                            false
                    );
                })
                .addOnSuccessListener(result -> {
                    if (result == null) {
                        callback.onComplete(false, "Failed to create charge.", null);
                        return;
                    }
                    if (result.existed) {
                        callback.onComplete(true, "Charge already exists for this alert.", null);
                        return;
                    }
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("chargeId", result.chargeId);
                    metadata.put("alertId", result.alertId);
                    metadata.put("status", result.status);
                    auditEventLogger.log(
                            AuditEventType.CHARGE_CREATED,
                            "fine_case",
                            result.chargeId,
                            result.requestId,
                            adminUid.trim(),
                            "admin",
                            "Charge created for security alert",
                            "admin_alerts",
                            "success",
                            "charge_created",
                            GatePolicy.STORED_VALUE,
                            metadata
                    );
                    callback.onComplete(true, "Charge created.", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to create charge.", error));
    }

    @Override
    public void resolveChargePaid(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        resolveCharge(
                chargeId,
                adminUid,
                STATUS_SETTLED,
                true,
                AuditEventType.CHARGE_PAID,
                "Charge paid and incident closed.",
                callback
        );
    }

    @Override
    public void resolveChargeRemoved(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        resolveCharge(
                chargeId,
                adminUid,
                STATUS_WAIVED,
                false,
                AuditEventType.CHARGE_REMOVED,
                "Charge removed and incident closed.",
                callback
        );
    }

    @Override
    public void waiveFineByRequestId(
            @NonNull String requestId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        updateOpenFineStatus(requestId, adminUid, FineCaseRecord.STATUS_WAIVED, AuditEventType.FINE_WAIVED, callback);
    }

    @Override
    public void settleFineByRequestId(
            @NonNull String requestId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    ) {
        updateOpenFineStatus(requestId, adminUid, FineCaseRecord.STATUS_SETTLED, AuditEventType.FINE_SETTLED, callback);
    }

    @Override
    public void closeIncident(
            @NonNull String alertId,
            @NonNull String requestId,
            @NonNull String adminUid,
            @NonNull String summary,
            @NonNull OperationCallback callback
    ) {
        String normalizedAlertId = alertId.trim();
        String normalizedRequestId = requestId.trim();
        if (normalizedAlertId.isEmpty() && normalizedRequestId.isEmpty()) {
            callback.onComplete(false, "Incident identifiers are missing.", null);
            return;
        }
        String normalizedSummary = summary.trim().isEmpty()
                ? "Incident closed by admin."
                : summary.trim();

        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        if (!normalizedAlertId.isEmpty()) {
            Map<String, Object> alertUpdates = new HashMap<>();
            alertUpdates.put("incidentStatus", "closed");
            alertUpdates.put("interventionSummary", normalizedSummary);
            alertUpdates.put("closedByUid", adminUid.trim());
            alertUpdates.put("closedAt", FieldValue.serverTimestamp());
            alertUpdates.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(
                    firestore.collection(COLLECTION_ALERTS).document(normalizedAlertId),
                    alertUpdates,
                    com.google.firebase.firestore.SetOptions.merge()
            );
        }

        String interventionId = normalizedRequestId.isEmpty() ? normalizedAlertId : normalizedRequestId;
        if (!interventionId.isEmpty()) {
            Map<String, Object> interventionUpdates = new HashMap<>();
            interventionUpdates.put("alertId", normalizedAlertId);
            interventionUpdates.put("requestId", normalizedRequestId);
            interventionUpdates.put("status", "closed");
            interventionUpdates.put("summary", normalizedSummary);
            interventionUpdates.put("closedByUid", adminUid.trim());
            interventionUpdates.put("closedAt", FieldValue.serverTimestamp());
            interventionUpdates.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(
                    firestore.collection(COLLECTION_INTERVENTIONS).document(interventionId),
                    interventionUpdates,
                    com.google.firebase.firestore.SetOptions.merge()
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("alertId", normalizedAlertId);
                    metadata.put("summary", normalizedSummary);
                    auditEventLogger.log(
                            AuditEventType.INCIDENT_CLOSED,
                            "security_alert",
                            normalizedAlertId.isEmpty() ? interventionId : normalizedAlertId,
                            normalizedRequestId.isEmpty()
                                    ? (normalizedAlertId.isEmpty() ? interventionId : normalizedAlertId)
                                    : normalizedRequestId,
                            adminUid.trim(),
                            "admin",
                            "Incident closed",
                            "admin_alerts",
                            "success",
                            "incident_closed",
                            GatePolicy.STORED_VALUE,
                            metadata
                    );
                    callback.onComplete(true, "Incident marked as closed.", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to close incident.", error));
    }

    private void updateOpenFineStatus(
            @NonNull String requestId,
            @NonNull String adminUid,
            @NonNull String targetStatus,
            @NonNull String auditEventType,
            @NonNull OperationCallback callback
    ) {
        String normalizedRequestId = requestId.trim();
        if (normalizedRequestId.isEmpty()) {
            callback.onComplete(false, "Request ID is required.", null);
            return;
        }
        firestore.collection(COLLECTION_FINE_CASES)
                .whereEqualTo("requestId", normalizedRequestId)
                .whereEqualTo("status", FineCaseRecord.STATUS_ISSUED)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(false, "No open fine case found.", null);
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", targetStatus);
                    updates.put("resolvedAt", FieldValue.serverTimestamp());
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    doc.getReference().set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("fineCaseId", doc.getId());
                                auditEventLogger.log(
                                        auditEventType,
                                        "fine_case",
                                        doc.getId(),
                                        normalizedRequestId,
                                        adminUid.trim(),
                                        "admin",
                                        "Fine status updated to " + targetStatus,
                                        "admin_intervention",
                                        "success",
                                        targetStatus,
                                        GatePolicy.STORED_VALUE,
                                        metadata
                                );
                                callback.onComplete(true, "Fine updated to " + targetStatus + ".", null);
                            })
                            .addOnFailureListener(error -> callback.onComplete(false, "Failed to update fine.", error));
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to load fine cases.", error));
    }

    private void resolveCharge(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull String targetStatus,
            boolean shouldBanGuest,
            @NonNull String auditType,
            @NonNull String closeSummary,
            @NonNull OperationCallback callback
    ) {
        String normalizedChargeId = chargeId.trim();
        if (normalizedChargeId.isEmpty()) {
            callback.onComplete(false, "Charge ID is required.", null);
            return;
        }
        DocumentReference chargeRef = firestore.collection(COLLECTION_FINE_CASES).document(normalizedChargeId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot chargeSnapshot = transaction.get(chargeRef);
                    if (!chargeSnapshot.exists()) {
                        throw new IllegalStateException("Charge not found.");
                    }
                    String currentStatus = asString(chargeSnapshot.get("status"));
                    if (STATUS_WAIVED.equalsIgnoreCase(currentStatus) || STATUS_SETTLED.equalsIgnoreCase(currentStatus)) {
                        return ChargeMutationResult.fromSnapshot(chargeSnapshot, true);
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", targetStatus);
                    updates.put("resolvedAt", FieldValue.serverTimestamp());
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.set(chargeRef, updates, com.google.firebase.firestore.SetOptions.merge());
                    return ChargeMutationResult.fromSnapshot(chargeSnapshot, false);
                })
                .addOnSuccessListener(result -> {
                    if (result == null) {
                        callback.onComplete(false, "Failed to resolve charge.", null);
                        return;
                    }
                    if (result.existed) {
                        callback.onComplete(true, "Charge already resolved.", null);
                        return;
                    }

                    // Keep entry request reported by lock; only pass exits on charge resolution.
                    markPassExitedForRequest(result.requestId, adminUid, shouldBanGuest ? "charge_paid" : "charge_removed");
                    closeIncident(
                            result.alertId,
                            result.requestId,
                            adminUid,
                            closeSummary,
                            (closeSuccess, closeMessage, closeError) -> {
                                if (!closeSuccess) {
                                    callback.onComplete(false, closeMessage, closeError);
                                    return;
                                }
                                if (shouldBanGuest) {
                                    if (!result.guestIdNumber.trim().isEmpty()) {
                                        createOrActivateBan(
                                                result.guestIdNumber,
                                                adminUid,
                                                result.alertId,
                                                result.requestId
                                        );
                                    } else {
                                        firestore.collection(COLLECTION_ALERTS)
                                                .document(result.alertId)
                                                .get()
                                                .addOnSuccessListener(alertSnapshot -> createOrActivateBan(
                                                        asString(alertSnapshot.get("guestIdNumber")),
                                                        adminUid,
                                                        result.alertId,
                                                        result.requestId
                                                ));
                                    }
                                }
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("chargeId", result.chargeId);
                                metadata.put("alertId", result.alertId);
                                metadata.put("requestId", result.requestId);
                                metadata.put("status", targetStatus);
                                auditEventLogger.log(
                                        auditType,
                                        "fine_case",
                                        result.chargeId,
                                        result.requestId,
                                        adminUid.trim(),
                                        "admin",
                                        shouldBanGuest ? "Charge marked paid." : "Charge removed.",
                                        "admin_charges",
                                        "success",
                                        shouldBanGuest ? "charge_paid" : "charge_removed",
                                        GatePolicy.STORED_VALUE,
                                        metadata
                                );
                                callback.onComplete(true, shouldBanGuest ? "Charge marked paid." : "Charge removed.", null);
                            }
                    );
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to resolve charge.", error));
    }

    @Override
    public void listenBans(@NonNull BanListListener listener) {
        if (bansRegistration != null) {
            bansRegistration.remove();
        }
        bansRegistration = firestore.collection(COLLECTION_GUEST_BANS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
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
        if (fineRegistration != null) {
            fineRegistration.remove();
        }
        fineRegistration = firestore.collection(COLLECTION_FINE_CASES)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<FineCaseRecord> fineCases = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            fineCases.add(FineCaseRecord.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(fineCases);
                });
    }

    @Override
    public void listenInterventions(@NonNull InterventionListListener listener) {
        if (interventionsRegistration != null) {
            interventionsRegistration.remove();
        }
        interventionsRegistration = firestore.collection(COLLECTION_INTERVENTIONS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<IncidentInterventionRecord> records = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            records.add(IncidentInterventionRecord.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(records);
                });
    }

    @Override
    public void removeListeners() {
        if (bansRegistration != null) {
            bansRegistration.remove();
            bansRegistration = null;
        }
        if (fineRegistration != null) {
            fineRegistration.remove();
            fineRegistration = null;
        }
        if (interventionsRegistration != null) {
            interventionsRegistration.remove();
            interventionsRegistration = null;
        }
    }

    private void invalidateActiveRecordsForBannedGuest(
            @NonNull String normalizedCnic,
            @NonNull String adminUid,
            @NonNull String sourceAlertId,
            @NonNull String sourceRequestId
    ) {
        firestore.collection(COLLECTION_ENTRY_REQUESTS)
                .whereEqualTo("guestIdNumber", normalizedCnic)
                .whereIn("status", Arrays.asList("active", "overdue", "reported"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot requestDoc : snapshot.getDocuments()) {
                        String requestId = requestDoc.getId();
                        Map<String, Object> requestUpdates = new HashMap<>();
                        requestUpdates.put("status", STATUS_REPORTED);
                        requestUpdates.put("reportedAt", FieldValue.serverTimestamp());
                        requestUpdates.put("reportedByUid", adminUid.trim());
                        requestUpdates.put("reportedByRole", "admin");
                        requestUpdates.put("reportReasonCode", "banned_guest");
                        requestUpdates.put("reportSource", "admin_ban");
                        requestUpdates.put("updatedAt", FieldValue.serverTimestamp());
                        requestDoc.getReference().set(requestUpdates, com.google.firebase.firestore.SetOptions.merge());

                        firestore.collection(COLLECTION_GUEST_PASSES)
                                .whereEqualTo("entryRequestId", requestId)
                                .whereIn("status", Arrays.asList("active", "used", "overdue", "reported"))
                                .get()
                                .addOnSuccessListener(passSnapshot -> {
                                    for (DocumentSnapshot passDoc : passSnapshot.getDocuments()) {
                                        Map<String, Object> passUpdates = new HashMap<>();
                                        passUpdates.put("status", STATUS_REPORTED);
                                        passUpdates.put("admittedByUid", adminUid.trim());
                                        passUpdates.put("admissionMethod", "INVALIDATED_BAN");
                                        passUpdates.put("updatedAt", FieldValue.serverTimestamp());
                                        passDoc.getReference().set(passUpdates, com.google.firebase.firestore.SetOptions.merge());
                                    }
                                });

                        createBanEntryReportAlert(requestDoc, adminUid, sourceAlertId);
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("guestIdNumber", normalizedCnic);
                        metadata.put("sourceAlertId", sourceAlertId.trim());
                        metadata.put("sourceRequestId", sourceRequestId.trim());
                        auditEventLogger.log(
                                AuditEventType.ENTRY_INVALIDATED_BAN,
                                "entry_request",
                                requestId,
                                requestId,
                                adminUid.trim(),
                                "admin",
                                "Entry invalidated due to banned guest",
                                "admin_intervention",
                                "failure",
                                "banned_guest",
                                GatePolicy.STORED_VALUE,
                                metadata
                        );
                    }
                });
    }

    private void createBanEntryReportAlert(@NonNull DocumentSnapshot requestDoc, @NonNull String adminUid, @NonNull String sourceAlertId) {
        String requestId = requestDoc.getId();
        Map<String, Object> alert = new HashMap<>();
        alert.put("alertType", "entry_report");
        alert.put("entryRequestId", requestId);
        alert.put("identifier", requestId);
        alert.put("severity", "HIGH");
        alert.put("message", "Entry invalidated because guest is on banned list.");
        alert.put("reportedByUid", adminUid.trim());
        alert.put("reportedByRole", "admin");
        alert.put("reportedByName", adminUid.trim());
        alert.put("reportReasonCode", "banned_guest");
        alert.put("reportSource", "admin_ban");
        alert.put("sourceAlertId", sourceAlertId.trim());
        alert.put("guestName", asString(requestDoc.get("fullName")));
        alert.put("guestIdNumber", asString(requestDoc.get("guestIdNumber")));
        alert.put("hostName", asString(requestDoc.get("hostName")));
        alert.put("requesterUid", asString(requestDoc.get("requesterUid")));
        alert.put("requesterRole", asString(requestDoc.get("requesterRole")));
        alert.put("gateLabel", GatePolicy.normalizeStoredValue(asString(requestDoc.get("gateLabel"))));
        alert.put("incidentStatus", "actioned");
        alert.put("interventionSummary", "Guest banned and entry invalidated");
        alert.put("createdAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_ALERTS)
                .document("entry_report_ban_" + requestId)
                .set(alert, com.google.firebase.firestore.SetOptions.merge());
    }

    private void markPassExitedForRequest(
            @NonNull String requestId,
            @NonNull String adminUid,
            @NonNull String source
    ) {
        String normalizedRequestId = requestId.trim();
        if (normalizedRequestId.isEmpty()) {
            return;
        }
        firestore.collection(COLLECTION_GUEST_PASSES)
                .whereEqualTo("entryRequestId", normalizedRequestId)
                .whereIn("status", Arrays.asList("active", "used", "overdue", "reported"))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        return;
                    }
                    DocumentSnapshot passDoc = snapshot.getDocuments().get(0);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "exited");
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    passDoc.getReference()
                            .set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("passCode", asString(passDoc.get("passCode")));
                                metadata.put("source", source);
                                auditEventLogger.log(
                                        AuditEventType.PASS_EXITED,
                                        "guest_pass",
                                        passDoc.getId(),
                                        normalizedRequestId,
                                        adminUid.trim(),
                                        "admin",
                                        "Guest pass marked exited from charge workflow",
                                        "admin_charges",
                                        "success",
                                        "pass_exited",
                                        GatePolicy.normalizeStoredValue(asString(passDoc.get("gateLabel"))),
                                        metadata
                                );
                            });
                });
    }

    private void createOrActivateBan(
            @NonNull String cnic,
            @NonNull String adminUid,
            @NonNull String alertId,
            @NonNull String requestId
    ) {
        String normalizedCnic = GuestIdentityPolicy.normalizeCnic(cnic);
        if (normalizedCnic == null) {
            return;
        }
        firestore.collection(COLLECTION_GUEST_BANS)
                .whereEqualTo("cnic", normalizedCnic)
                .whereEqualTo("status", GuestBanRecord.STATUS_ACTIVE)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        return;
                    }
                    DocumentReference banRef = firestore.collection(COLLECTION_GUEST_BANS).document();
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("cnic", normalizedCnic);
                    payload.put("status", GuestBanRecord.STATUS_ACTIVE);
                    payload.put("reasonCode", "charge_paid");
                    payload.put("sourceAlertId", alertId.trim());
                    payload.put("sourceRequestId", requestId.trim());
                    payload.put("bannedByUid", adminUid.trim());
                    payload.put("startAt", FieldValue.serverTimestamp());
                    payload.put("endAt", null);
                    payload.put("createdAt", FieldValue.serverTimestamp());
                    payload.put("updatedAt", FieldValue.serverTimestamp());
                    banRef.set(payload)
                            .addOnSuccessListener(unused -> {
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("cnic", normalizedCnic);
                                metadata.put("sourceAlertId", alertId.trim());
                                metadata.put("sourceRequestId", requestId.trim());
                                auditEventLogger.log(
                                        AuditEventType.GUEST_BANNED,
                                        "guest_ban",
                                        banRef.getId(),
                                        requestId.trim().isEmpty() ? banRef.getId() : requestId.trim(),
                                        adminUid.trim(),
                                        "admin",
                                        "Guest CNIC added to banned list from paid charge",
                                        "admin_charges",
                                        "success",
                                        "charge_paid_ban",
                                        GatePolicy.STORED_VALUE,
                                        metadata
                                );
                            });
                });
    }

    private void updateAlertInterventionState(
            @NonNull String alertId,
            @NonNull String requestId,
            @NonNull String incidentStatus,
            @NonNull String summary,
            @NonNull String banId,
            @NonNull String fineId
    ) {
        String normalizedAlertId = alertId.trim();
        if (!normalizedAlertId.isEmpty()) {
            Map<String, Object> alertUpdates = new HashMap<>();
            alertUpdates.put("incidentStatus", incidentStatus.trim());
            alertUpdates.put("interventionSummary", summary.trim());
            alertUpdates.put("updatedAt", FieldValue.serverTimestamp());
            firestore.collection(COLLECTION_ALERTS)
                    .document(normalizedAlertId)
                    .set(alertUpdates, com.google.firebase.firestore.SetOptions.merge());
        }

        String interventionId = requestId.trim().isEmpty() ? normalizedAlertId : requestId.trim();
        if (interventionId.isEmpty()) {
            return;
        }
        Map<String, Object> intervention = new HashMap<>();
        intervention.put("alertId", normalizedAlertId);
        intervention.put("requestId", requestId.trim());
        if (!banId.trim().isEmpty()) {
            intervention.put("banId", banId.trim());
        }
        if (!fineId.trim().isEmpty()) {
            intervention.put("fineId", fineId.trim());
        }
        intervention.put("status", incidentStatus.trim());
        intervention.put("summary", summary.trim());
        intervention.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_INTERVENTIONS)
                .document(interventionId)
                .set(intervention, com.google.firebase.firestore.SetOptions.merge());
    }

    @NonNull
    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static final class ChargeMutationResult {
        private final String chargeId;
        private final String requestId;
        private final String alertId;
        private final String status;
        private final String guestIdNumber;
        private final boolean existed;

        private ChargeMutationResult(
                @NonNull String chargeId,
                @NonNull String requestId,
                @NonNull String alertId,
                @NonNull String status,
                @NonNull String guestIdNumber,
                boolean existed
        ) {
            this.chargeId = chargeId;
            this.requestId = requestId;
            this.alertId = alertId;
            this.status = status;
            this.guestIdNumber = guestIdNumber;
            this.existed = existed;
        }

        @NonNull
        private static ChargeMutationResult fromSnapshot(@NonNull DocumentSnapshot snapshot, boolean existed) {
            return new ChargeMutationResult(
                    snapshot.getId(),
                    asStringStatic(snapshot.get("requestId")),
                    asStringStatic(snapshot.get("alertId")),
                    asStringStatic(snapshot.get("status")),
                    asStringStatic(snapshot.get("guestIdNumber")),
                    existed
            );
        }

        @NonNull
        private static String asStringStatic(@Nullable Object value) {
            return value == null ? "" : String.valueOf(value).trim();
        }
    }
}
