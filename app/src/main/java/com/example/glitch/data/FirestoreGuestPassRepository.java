package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestPassTimePolicy;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Firestore-backed guest pass repository.
 * Updated with a pendingIssuances lock to prevent duplicate creation race conditions.
 */
public class FirestoreGuestPassRepository implements GuestPassRepository {
    private static final String COLLECTION_GUEST_PASSES = "guest_passes";
    private static final String COLLECTION_ENTRY_REQUESTS = "entry_requests";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_USED = "used";
    private static final String STATUS_EXPIRED = "expired";
    private static final String STATUS_DENIED = "denied";
    private static final String STATUS_OVERDUE = "overdue";
    private static final String STATUS_EXITED = "exited";
    private static final String REQUEST_STATUS_PENDING = "pending";
    private static final String REQUEST_STATUS_DENIED = "denied";
    private static final String REQUEST_STATUS_FIELD = "status";
    private static final String REQUEST_ENTERED_AT_FIELD = "enteredAt";
    private static final String ISSUE_WINDOW_CLOSED_MESSAGE =
            "Guest passes can only be created between 8:30 AM and 10:30 PM.";
    private final FirebaseFirestore firestore;
    private final CollectionReference collection;
    private final CollectionReference entryRequestCollection;
    
    // In-memory lock to prevent rapid-click duplicates for the same user
    private final Set<String> pendingIssuances = new HashSet<>();

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
            @NonNull OperationCallback callback
    ) {
        issueGuestPassWithEntryRequest(
                sponsorUid,
                sponsorRole,
                sponsorName,
                sponsorEmail,
                guestName,
                guestIdNumber,
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
            @NonNull IssueCallback callback
    ) {
        if (!GuestPassTimePolicy.canIssueNow()) {
            callback.onComplete(false, ISSUE_WINDOW_CLOSED_MESSAGE, null, null);
            return;
        }

        // 1. Immediate in-memory check
        if ("student".equalsIgnoreCase(sponsorRole) && pendingIssuances.contains(sponsorUid)) {
            callback.onComplete(false, "An issuance request is already in progress.", null, null);
            return;
        }

        if ("student".equalsIgnoreCase(sponsorRole)) {
            pendingIssuances.add(sponsorUid);
            
            // 2. Database check
            collection.whereEqualTo("sponsorUid", sponsorUid)
                    .whereIn("status", Arrays.asList(STATUS_ACTIVE, STATUS_USED, STATUS_OVERDUE))
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            pendingIssuances.remove(sponsorUid);
                            callback.onComplete(false, "You already have an active or pending guest pass.", null, null);
                        } else {
                            // 3. Perform write
                            performIssueGuestPass(sponsorUid, sponsorRole, sponsorName, sponsorEmail, guestName, guestIdNumber, (success, message, issuedPass, exception) -> {
                                pendingIssuances.remove(sponsorUid);
                                callback.onComplete(success, message, issuedPass, exception);
                            });
                        }
                    })
                    .addOnFailureListener(error -> {
                        pendingIssuances.remove(sponsorUid);
                        callback.onComplete(false, "Failed to verify existing passes", null, error);
                    });
        } else {
            performIssueGuestPass(sponsorUid, sponsorRole, sponsorName, sponsorEmail, guestName, guestIdNumber, callback);
        }
    }

    private void performIssueGuestPass(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String sponsorEmail,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull IssueCallback callback
    ) {
        Timestamp expiresAt = GuestPassTimePolicy.expiryAtToday2230();
        String normalizedGate = GatePolicy.STORED_VALUE;
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
                    List<GuestPass> passesToExpire = new ArrayList<>();
                    Set<String> requestIdsToCleanup = new HashSet<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            GuestPass rawPass = GuestPass.fromMap(doc.getId(), doc.getData());
                            if (GuestPassStatusRules.isTimeExpiredActive(rawPass)
                                    || GuestPassTimePolicy.isActivePassOutOfPolicy(rawPass)) {
                                passesToExpire.add(rawPass);
                            }
                            String status = rawPass.getStatus().trim().toLowerCase();
                            if ((STATUS_CANCELLED.equals(status) || STATUS_EXPIRED.equals(status))
                                    && !rawPass.getEntryRequestId().trim().isEmpty()) {
                                requestIdsToCleanup.add(rawPass.getEntryRequestId().trim());
                            }
                            passes.add(normalizeExpiryForRead(rawPass));
                        }
                    }
                    persistExpiredStatuses(passesToExpire);
                    deleteUnaccessedEntryRequests(new ArrayList<>(requestIdsToCleanup));
                    reconcileExitedPasses(passes);
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
        DocumentReference passRef = collection.document(passId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot passSnapshot = transaction.get(passRef);
                    if (!passSnapshot.exists()) {
                        throw new IllegalStateException("Pass not found.");
                    }
                    GuestPass pass = GuestPass.fromMap(passSnapshot.getId(), passSnapshot.getData());
                    if (!STATUS_ACTIVE.equalsIgnoreCase(pass.getStatus())) {
                        throw new IllegalStateException("Only active passes can be cancelled.");
                    }
                    DocumentReference requestToDelete = findLinkedUnaccessedEntryRequestForDelete(
                            transaction,
                            pass.getEntryRequestId()
                    );

                    Map<String, Object> passUpdates = new HashMap<>();
                    passUpdates.put("status", STATUS_CANCELLED);
                    passUpdates.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.update(passRef, passUpdates);
                    if (requestToDelete != null) {
                        transaction.delete(requestToDelete);
                    }
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onComplete(true, "Guest pass cancelled", null))
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
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
                    boolean shouldExpire = GuestPassStatusRules.isTimeExpiredActive(rawPass)
                            || GuestPassTimePolicy.isActivePassOutOfPolicy(rawPass);
                    GuestPass pass = normalizeExpiryForRead(rawPass);
                    if (shouldExpire) {
                        persistExpiredStatus(pass);
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
                    if (GuestPassStatusRules.isTimeExpiredActive(pass)
                            || GuestPassTimePolicy.isActivePassOutOfPolicy(pass)) {
                        DocumentReference requestToDelete = findLinkedUnaccessedEntryRequestForDelete(
                                transaction,
                                pass.getEntryRequestId()
                        );
                        Map<String, Object> expiredUpdates = new HashMap<>();
                        expiredUpdates.put("status", STATUS_EXPIRED);
                        expiredUpdates.put("updatedAt", FieldValue.serverTimestamp());
                        transaction.update(passRef, expiredUpdates);
                        if (requestToDelete != null) {
                            transaction.delete(requestToDelete);
                        }
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

    @Override
    public void markPassExitedByEntryRequestId(@NonNull String entryRequestId, @NonNull OperationCallback callback) {
        collection.whereEqualTo("entryRequestId", entryRequestId.trim())
                .whereIn("status", Arrays.asList(STATUS_USED, STATUS_OVERDUE))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(true, "No active pass to mark exited.", null);
                        return;
                    }
                    String passId = snapshot.getDocuments().get(0).getId();
                    collection.document(passId)
                            .update("status", STATUS_EXITED, "updatedAt", FieldValue.serverTimestamp())
                            .addOnSuccessListener(unused -> callback.onComplete(true, "Pass marked as exited", null))
                            .addOnFailureListener(error -> callback.onComplete(false, "Failed to update pass to exited", error));
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to find pass for exit", error));
    }

    private void reconcileExitedPasses(@NonNull List<GuestPass> passes) {
        List<GuestPass> candidates = new ArrayList<>();
        for (GuestPass pass : passes) {
            String status = pass.getStatus().trim().toLowerCase();
            if (("used".equals(status) || "overdue".equals(status))
                    && !pass.getEntryRequestId().trim().isEmpty()) {
                candidates.add(pass);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        Map<String, String> requestIdToPassId = new HashMap<>();
        List<String> requestIds = new ArrayList<>();
        for (GuestPass pass : candidates) {
            String requestId = pass.getEntryRequestId().trim();
            if (!requestIdToPassId.containsKey(requestId)) {
                requestIds.add(requestId);
            }
            requestIdToPassId.put(requestId, pass.getId());
        }

        for (int i = 0; i < requestIds.size(); i += 10) {
            int end = Math.min(i + 10, requestIds.size());
            List<String> chunk = requestIds.subList(i, end);
            entryRequestCollection
                    .whereIn(FieldPath.documentId(), chunk)
                    .whereEqualTo("status", STATUS_EXITED)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            return;
                        }
                        WriteBatch batch = firestore.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String passId = requestIdToPassId.get(doc.getId());
                            if (passId == null || passId.trim().isEmpty()) {
                                continue;
                            }
                            batch.update(
                                    collection.document(passId),
                                    "status", STATUS_EXITED,
                                    "updatedAt", FieldValue.serverTimestamp()
                            );
                        }
                        batch.commit();
                    });
        }
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
        if (!GuestPassStatusRules.isTimeExpiredActive(pass)
                && !GuestPassTimePolicy.isActivePassOutOfPolicy(pass)) {
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

    private void persistExpiredStatuses(@NonNull List<GuestPass> passesToExpire) {
        if (passesToExpire.isEmpty()) {
            return;
        }
        Set<String> linkedRequestIds = new HashSet<>();
        WriteBatch batch = firestore.batch();
        for (GuestPass pass : passesToExpire) {
            String passId = pass.getId().trim();
            if (passId.isEmpty()) {
                continue;
            }
            batch.update(
                    collection.document(passId),
                    "status", STATUS_EXPIRED,
                    "updatedAt", FieldValue.serverTimestamp()
            );
            String entryRequestId = pass.getEntryRequestId().trim();
            if (!entryRequestId.isEmpty()) {
                linkedRequestIds.add(entryRequestId);
            }
        }
        batch.commit().addOnSuccessListener(unused -> deleteUnaccessedEntryRequests(new ArrayList<>(linkedRequestIds)));
    }

    private void persistExpiredStatus(@NonNull GuestPass pass) {
        DocumentReference passRef = collection.document(pass.getId());
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(passRef);
                    if (!snapshot.exists()) {
                        return null;
                    }
                    GuestPass latest = GuestPass.fromMap(snapshot.getId(), snapshot.getData());
                    if (!GuestPassStatusRules.isTimeExpiredActive(latest)
                            && !GuestPassTimePolicy.isActivePassOutOfPolicy(latest)) {
                        return null;
                    }
                    DocumentReference requestToDelete = findLinkedUnaccessedEntryRequestForDelete(
                            transaction,
                            latest.getEntryRequestId()
                    );
                    transaction.update(
                            passRef,
                            "status", STATUS_EXPIRED,
                            "updatedAt", FieldValue.serverTimestamp()
                    );
                    if (requestToDelete != null) {
                        transaction.delete(requestToDelete);
                    }
                    return null;
                })
                .addOnFailureListener(error -> {
                    // Best-effort normalization path: intentionally no-op on failure.
                });
    }

    private void deleteUnaccessedEntryRequests(@NonNull List<String> entryRequestIds) {
        if (entryRequestIds.isEmpty()) {
            return;
        }
        for (int i = 0; i < entryRequestIds.size(); i += 10) {
            int end = Math.min(i + 10, entryRequestIds.size());
            List<String> chunk = entryRequestIds.subList(i, end);
            entryRequestCollection
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            return;
                        }
                        WriteBatch batch = firestore.batch();
                        boolean hasDelete = false;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (shouldDeleteUnaccessedRequest(doc)) {
                                batch.delete(doc.getReference());
                                hasDelete = true;
                            }
                        }
                        if (hasDelete) {
                            batch.commit();
                        }
                    });
        }
    }

    @Nullable
    private DocumentReference findLinkedUnaccessedEntryRequestForDelete(
            @NonNull com.google.firebase.firestore.Transaction transaction,
            @Nullable String entryRequestId
    ) throws FirebaseFirestoreException {
        String trimmedId = entryRequestId == null ? "" : entryRequestId.trim();
        if (trimmedId.isEmpty()) {
            return null;
        }
        DocumentReference requestRef = entryRequestCollection.document(trimmedId);
        DocumentSnapshot requestSnapshot = transaction.get(requestRef);
        if (!requestSnapshot.exists()) {
            return null;
        }
        if (shouldDeleteUnaccessedRequest(requestSnapshot)) {
            return requestRef;
        }
        return null;
    }

    private boolean shouldDeleteUnaccessedRequest(@NonNull DocumentSnapshot snapshot) {
        String requestStatus = safeString(snapshot.get(REQUEST_STATUS_FIELD));
        boolean unaccessed = snapshot.get(REQUEST_ENTERED_AT_FIELD) == null;
        if (!unaccessed) {
            return false;
        }
        return REQUEST_STATUS_PENDING.equalsIgnoreCase(requestStatus)
                || REQUEST_STATUS_DENIED.equalsIgnoreCase(requestStatus)
                || requestStatus.isEmpty();
    }

    @NonNull
    private String safeMessage(@NonNull Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "Operation failed." : message;
    }

    @NonNull
    private String safeString(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }
}
