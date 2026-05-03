package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Firestore-backed guest pass repository.
 * Updated to return ListenerRegistration to avoid cross-fragment listener collisions.
 */
public class FirestoreGuestPassRepository implements GuestPassRepository {
    private static final String COLLECTION_GUEST_PASSES = "guest_passes";
    private static final String COLLECTION_ENTRY_REQUESTS = "entry_requests";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_USED = "used";
    private static final String STATUS_EXPIRED = "expired";
    private static final String STATUS_DENIED = "denied";
    private static final String DEFAULT_GATE = "Main Gate";

    private final FirebaseFirestore firestore;
    private final CollectionReference collection;
    private final CollectionReference entryRequestCollection;

    public FirestoreGuestPassRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreGuestPassRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.collection = firestore.collection(COLLECTION_GUEST_PASSES);
        this.entryRequestCollection = firestore.collection(COLLECTION_ENTRY_REQUESTS);
    }

    @Override
    public void createGuestPass(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String sponsorEmail,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            int expiryHours,
            @NonNull OperationCallback callback
    ) {
        issueGuestPassWithEntryRequest(
                sponsorUid,
                sponsorRole,
                sponsorName,
                sponsorEmail,
                guestName,
                guestIdNumber,
                DEFAULT_GATE,
                expiryHours,
                (success, message, issuedPass, exception) -> callback.onComplete(success, message, exception)
        );
    }

    @Override
    public void issueGuestPassWithEntryRequest(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String sponsorEmail,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String gateLabel,
            int expiryHours,
            @NonNull IssueCallback callback
    ) {
        int safeHours = Math.max(1, expiryHours);
        Timestamp expiresAt = new Timestamp(
                new Date(System.currentTimeMillis() + safeHours * 60L * 60L * 1000L)
        );
        String normalizedGate = gateLabel.trim().isEmpty() ? DEFAULT_GATE : gateLabel.trim();
        String passCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        DocumentReference requestRef = entryRequestCollection.document();
        DocumentReference passRef = collection.document();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("type", "request");
        requestData.put("status", "pending");
        requestData.put("fullName", guestName);
        requestData.put("roleTag", "Guest");
        requestData.put("guestIdNumber", guestIdNumber);
        requestData.put("hostName", sponsorName);
        requestData.put("gateLabel", normalizedGate);
        requestData.put("iconType", "guest");
        requestData.put("expiresAt", expiresAt);
        requestData.put("requesterUid", sponsorUid);
        requestData.put("requesterRole", sponsorRole);
        requestData.put("createdAt", FieldValue.serverTimestamp());
        requestData.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> data = new HashMap<>();
        data.put("sponsorUid", sponsorUid);
        data.put("sponsorRole", sponsorRole);
        data.put("sponsorName", sponsorName);
        data.put("sponsorEmail", sponsorEmail);
        data.put("guestName", guestName);
        data.put("guestIdNumber", guestIdNumber);
        data.put("passCode", passCode);
        data.put("entryRequestId", requestRef.getId());
        data.put("gateLabel", normalizedGate);
        data.put("status", STATUS_ACTIVE);
        data.put("expiresAt", expiresAt);
        data.put("admittedAt", null);
        data.put("admittedByUid", "");
        data.put("admissionMethod", "");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(requestRef, requestData);
        batch.set(passRef, data);
        batch.commit()
                .addOnSuccessListener(unused -> callback.onComplete(
                        true,
                        "Guest pass issued",
                        new GuestPass(
                                passRef.getId(),
                                sponsorUid,
                                sponsorRole,
                                sponsorName,
                                sponsorEmail,
                                guestName,
                                guestIdNumber,
                                passCode,
                                requestRef.getId(),
                                normalizedGate,
                                STATUS_ACTIVE,
                                expiresAt,
                                null,
                                "",
                                "",
                                null
                        ),
                        null
                ))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to issue guest pass", null, error));
    }

    @Nullable
    @Override
    public ListenerRegistration listenGuestPasses(@NonNull String sponsorUid, @NonNull PassListListener listener) {
        return listenPassesByArchiveMode(sponsorUid, false, listener);
    }

    @Nullable
    @Override
    public ListenerRegistration listenArchivedGuestPasses(@NonNull String sponsorUid, @NonNull PassListListener listener) {
        return listenPassesByArchiveMode(sponsorUid, true, listener);
    }

    private ListenerRegistration listenPassesByArchiveMode(
            @NonNull String sponsorUid,
            boolean archivedOnly,
            @NonNull PassListListener listener
    ) {
        return collection
                .whereEqualTo("sponsorUid", sponsorUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<GuestPass> passes = new ArrayList<>();
                    List<String> passIdsToExpire = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            GuestPass rawPass = GuestPass.fromMap(doc.getId(), doc.getData());
                            if (GuestPassStatusRules.isTimeExpiredActive(rawPass)) {
                                passIdsToExpire.add(rawPass.getId());
                            }
                            passes.add(normalizeExpiryForRead(rawPass));
                        }
                    }
                    persistExpiredStatuses(passIdsToExpire);
                    passes.sort(Comparator.comparing(
                            GuestPass::getCreatedAt,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                    ).reversed());
                    List<GuestPass> filtered = new ArrayList<>();
                    for (GuestPass pass : passes) {
                        boolean archived = GuestPassStatusRules.isArchivedStatus(pass.getStatus());
                        if (archivedOnly == archived) {
                            filtered.add(pass);
                        }
                    }
                    listener.onData(filtered);
                });
    }

    @Override
    public void cancelGuestPass(@NonNull String passId, @NonNull OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_CANCELLED);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        collection.document(passId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onComplete(true, "Guest pass cancelled", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to cancel guest pass", error));
    }

    @Override
    public void findPassByCode(@NonNull String passCode, @NonNull PassLookupListener listener) {
        collection.whereEqualTo("passCode", passCode.trim().toUpperCase())
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        listener.onData(null);
                        return;
                    }
                    DocumentSnapshot document = snapshot.getDocuments().get(0);
                    GuestPass rawPass = GuestPass.fromMap(document.getId(), document.getData());
                    boolean shouldExpire = GuestPassStatusRules.isTimeExpiredActive(rawPass);
                    GuestPass pass = normalizeExpiryForRead(rawPass);
                    if (shouldExpire) {
                        persistExpiredStatus(pass.getId());
                    }
                    listener.onData(pass);
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void markPassAdmitted(
            @NonNull String passId,
            @NonNull String admittedByUid,
            @NonNull String admissionMethod,
            @NonNull OperationCallback callback
    ) {
        DocumentReference passRef = collection.document(passId);
        String normalizedMethod = admissionMethod.trim().isEmpty() ? "PASS_CODE" : admissionMethod.trim();
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(passRef);
                    if (!snapshot.exists()) {
                        throw new IllegalStateException("Pass not found.");
                    }
                    GuestPass pass = GuestPass.fromMap(snapshot.getId(), snapshot.getData());
                    if (!STATUS_ACTIVE.equalsIgnoreCase(pass.getStatus())) {
                        throw new IllegalStateException("Pass is not active.");
                    }
                    if (pass.getExpiresAt() != null && pass.getExpiresAt().toDate().before(new Date())) {
                        Map<String, Object> expiredUpdates = new HashMap<>();
                        expiredUpdates.put("status", STATUS_EXPIRED);
                        expiredUpdates.put("updatedAt", FieldValue.serverTimestamp());
                        transaction.update(passRef, expiredUpdates);
                        return STATUS_EXPIRED;
                    }
                    if (pass.getEntryRequestId().trim().isEmpty()) {
                        throw new IllegalStateException("Pass is missing entry request linkage.");
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", STATUS_USED);
                    updates.put("admittedAt", FieldValue.serverTimestamp());
                    updates.put("admittedByUid", admittedByUid);
                    updates.put("admissionMethod", normalizedMethod);
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.update(passRef, updates);
                    return STATUS_USED;
                })
                .addOnSuccessListener(result -> {
                    if (STATUS_EXPIRED.equalsIgnoreCase(result)) {
                        callback.onComplete(false, "Pass has expired.", null);
                        return;
                    }
                    callback.onComplete(true, "Pass admitted", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    @Override
    public void markPassAdmittedByEntryRequestId(
            @NonNull String entryRequestId,
            @NonNull String admittedByUid,
            @NonNull String admissionMethod,
            @NonNull OperationCallback callback
    ) {
        collection.whereEqualTo("entryRequestId", entryRequestId.trim())
                .whereEqualTo("status", STATUS_ACTIVE)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(true, "No linked guest pass.", null);
                        return;
                    }
                    String passId = snapshot.getDocuments().get(0).getId();
                    markPassAdmitted(passId, admittedByUid, admissionMethod, callback);
                })
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    @Override
    public void markPassDeniedByEntryRequestId(
            @NonNull String entryRequestId,
            @NonNull String deniedByUid,
            @NonNull OperationCallback callback
    ) {
        collection.whereEqualTo("entryRequestId", entryRequestId.trim())
                .whereEqualTo("status", STATUS_ACTIVE)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(true, "No linked active guest pass.", null);
                        return;
                    }
                    String passId = snapshot.getDocuments().get(0).getId();
                    markPassDenied(passId, deniedByUid, callback);
                })
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    private void markPassDenied(
            @NonNull String passId,
            @NonNull String deniedByUid,
            @NonNull OperationCallback callback
    ) {
        DocumentReference passRef = collection.document(passId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(passRef);
                    if (!snapshot.exists()) {
                        throw new IllegalStateException("Pass not found.");
                    }
                    GuestPass pass = GuestPass.fromMap(snapshot.getId(), snapshot.getData());
                    if (!STATUS_ACTIVE.equalsIgnoreCase(pass.getStatus())) {
                        throw new IllegalStateException("Pass is not active.");
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", STATUS_DENIED);
                    updates.put("admittedByUid", deniedByUid);
                    updates.put("admissionMethod", "DENIED");
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.update(passRef, updates);
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onComplete(true, "Pass denied", null))
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    @NonNull
    private GuestPass normalizeExpiryForRead(@NonNull GuestPass pass) {
        if (!GuestPassStatusRules.isTimeExpiredActive(pass)) {
            return pass;
        }
        return new GuestPass(
                pass.getId(),
                pass.getSponsorUid(),
                pass.getSponsorRole(),
                pass.getSponsorName(),
                pass.getSponsorEmail(),
                pass.getGuestName(),
                pass.getGuestIdNumber(),
                pass.getPassCode(),
                pass.getEntryRequestId(),
                pass.getGateLabel(),
                STATUS_EXPIRED,
                pass.getExpiresAt(),
                pass.getAdmittedAt(),
                pass.getAdmittedByUid(),
                pass.getAdmissionMethod(),
                pass.getCreatedAt()
        );
    }

    private void persistExpiredStatuses(@NonNull List<String> passIdsToExpire) {
        if (passIdsToExpire.isEmpty()) {
            return;
        }
        WriteBatch batch = firestore.batch();
        for (String passId : passIdsToExpire) {
            batch.update(
                    collection.document(passId),
                    "status", STATUS_EXPIRED,
                    "updatedAt", FieldValue.serverTimestamp()
            );
        }
        batch.commit();
    }

    private void persistExpiredStatus(@NonNull String passId) {
        collection.document(passId).update(
                "status", STATUS_EXPIRED,
                "updatedAt", FieldValue.serverTimestamp()
        );
    }

    @NonNull
    private String safeMessage(@NonNull Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "Unable to admit pass." : message;
    }
}
