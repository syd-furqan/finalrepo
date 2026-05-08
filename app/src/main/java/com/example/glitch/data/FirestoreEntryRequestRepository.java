package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.auth.SessionManager;
import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.CredentialVerificationResult;
import com.example.glitch.model.DashboardState;
import com.example.glitch.model.EntryRequest;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.UserProfile;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Firestore-backed implementation of entry request repository.
 * Pattern: Data source adapter from Firebase snapshot APIs to domain models.
 * Updated to handle "overdue" status for guests past their expiry time.
 */
public class FirestoreEntryRequestRepository implements EntryRequestRepository {
    static final String COLLECTION_ENTRY_REQUESTS = "entry_requests";
    static final String COLLECTION_GUEST_PASSES = "guest_passes";
    static final String COLLECTION_CREDENTIALS = "credentials";
    static final String COLLECTION_ACCESS_EVENTS = "access_events";
    static final String COLLECTION_ALERTS = "alerts";
    static final String DASHBOARD_STATE_DOC = "dashboard_state";
    static final String TYPE_FIELD = "type";
    static final String TYPE_REQUEST = "request";
    static final String TYPE_STATE = "state";
    static final String STATUS_FIELD = "status";
    static final String STATUS_ACTIVE = "active";
    static final String STATUS_PENDING = "pending";
    static final String STATUS_EXITED = "exited";
    static final String STATUS_DENIED = "denied";
    static final String STATUS_OVERDUE = "overdue";
    static final String STATUS_REPORTED = "reported";
    static final String ENTERED_AT_FIELD = "enteredAt";
    static final String EXITED_AT_FIELD = "exitedAt";
    static final String UPDATED_AT_FIELD = "updatedAt";
    static final String CREATED_AT_FIELD = "createdAt";
    static final String GUEST_NAME_FIELD = "fullName";
    static final String GUEST_ID_FIELD = "guestIdNumber";
    static final String HOST_NAME_FIELD = "hostName";
    static final String GATE_LABEL_FIELD = "gateLabel";
    static final String REQUESTER_UID_FIELD = "requesterUid";
    static final String REQUESTER_ROLE_FIELD = "requesterRole";
    static final String CREDENTIAL_IDENTIFIER_FIELD = "identifier";
    static final String CREDENTIAL_VALID_FIELD = "isValid";
    static final String CREDENTIAL_NAME_FIELD = "holderName";
    static final String DENIAL_REASON_FIELD = "denialReason";
    static final String FAIL_COUNT_FIELD = "failCount";
    static final String SEVERITY_FIELD = "severity";
    static final String MESSAGE_FIELD = "message";
    static final String IDENTIFIER_FIELD = "identifier";
    static final String LAST_FAILED_AT_FIELD = "lastFailedAt";
    static final String EXPIRY_AT_FIELD = "expiresAt";
    static final String ADMITTED_BY_UID_FIELD = "admittedByUid";
    static final String ADMITTED_BY_ROLE_FIELD = "admittedByRole";
    static final String ADMITTED_BY_NAME_FIELD = "admittedByName";
    static final String ALERT_TYPE_FIELD = "alertType";
    static final String ENTRY_REQUEST_ID_FIELD = "entryRequestId";
    static final String REPORTED_BY_UID_FIELD = "reportedByUid";
    static final String REPORTED_BY_ROLE_FIELD = "reportedByRole";
    static final String REPORTED_BY_NAME_FIELD = "reportedByName";
    static final String REPORTED_AT_FIELD = "reportedAt";
    static final String REPORT_REASON_CODE_FIELD = "reportReasonCode";
    static final String REPORT_SOURCE_FIELD = "reportSource";
    static final String REPORT_REASON_GUARD_VIOLATION = "guard_violation";
    static final String REPORT_REASON_OVERDUE_GRACE = "overdue_grace_elapsed";
    static final String REPORT_SOURCE_GUARD_MANUAL = "guard_manual";
    static final String REPORT_SOURCE_SYSTEM_OVERDUE = "system_overdue_grace";
    static final String ALERT_TYPE_ENTRY_REPORT = "entry_report";
    static final long OVERDUE_GRACE_MILLIS = 30L * 60L * 1000L;
    static final String GUEST_PASS_STATUS_ACTIVE = "active";
    static final String GUEST_PASS_STATUS_USED = "used";
    static final String GUEST_PASS_STATUS_OVERDUE = "overdue";
    static final String GUEST_PASS_STATUS_REPORTED = "reported";

    private final FirebaseFirestore firestore;
    private final CollectionReference entryRequestCollection;
    private ListenerRegistration activeRequestsRegistration;
    private ListenerRegistration dashboardStateRegistration;
    private ListenerRegistration requesterRequestsRegistration;
    private final Set<String> overdueTransitionInFlight = new HashSet<>();
    private final Set<String> autoReportInFlight = new HashSet<>();

    public FirestoreEntryRequestRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Visible for testing so fake Firestore instances can be injected.
     */
    FirestoreEntryRequestRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.entryRequestCollection = firestore.collection(COLLECTION_ENTRY_REQUESTS);
    }

    @Override
    public void listenActiveRequests(@NonNull RequestListListener listener) {
        removeActiveRequestsListener();
        activeRequestsRegistration = entryRequestCollection
                .whereEqualTo(TYPE_FIELD, TYPE_REQUEST)
                .whereIn(STATUS_FIELD, Arrays.asList(STATUS_ACTIVE, STATUS_OVERDUE))
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    List<EntryRequest> requests = new ArrayList<>();
                    Date now = new Date();

                    if (snapshots != null) {
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            EntryRequest request = EntryRequest.fromMap(document.getId(), document.getData());
                            boolean studentRequest = isStudentRequest(request);
                            String normalizedStatus = safeLower(request.getStatus());
                            boolean timeExpired = isTimeExpired(request.getExpiresAt(), now);
                            boolean overdueGraceElapsed = isOverdueGraceElapsed(request.getExpiresAt(), now);

                            if (studentRequest
                                    && STATUS_ACTIVE.equals(normalizedStatus)
                                    && timeExpired) {
                                markRequestAsOverdue(request);
                                requests.add(asStatus(request, STATUS_OVERDUE));
                                continue;
                            }

                            if (studentRequest
                                    && STATUS_OVERDUE.equals(normalizedStatus)
                                    && overdueGraceElapsed) {
                                autoReportOverdueRequest(request);
                                continue;
                            }

                            if (STATUS_OVERDUE.equals(normalizedStatus) && !studentRequest) {
                                continue;
                            }

                            requests.add(request);
                        }
                    }

                    listener.onData(requests);
                });
    }

    private void markRequestAsOverdue(@NonNull EntryRequest request) {
        final String requestId = request.getId().trim();
        if (requestId.isEmpty() || !acquireInFlight(overdueTransitionInFlight, requestId)) {
            return;
        }

        DocumentReference requestRef = entryRequestCollection.document(requestId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(requestRef);
                    if (!snapshot.exists()) {
                        return null;
                    }
                    String currentStatus = safeLower(snapshot.get(STATUS_FIELD));
                    if (!STATUS_ACTIVE.equals(currentStatus)) {
                        return null;
                    }
                    String requesterRole = safeLower(snapshot.get(REQUESTER_ROLE_FIELD));
                    if (!"student".equals(requesterRole)) {
                        return null;
                    }
                    Object expiryValue = snapshot.get(EXPIRY_AT_FIELD);
                    if (!(expiryValue instanceof Timestamp)
                            || ((Timestamp) expiryValue).toDate().after(new Date())) {
                        return null;
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(STATUS_FIELD, STATUS_OVERDUE);
                    updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
                    transaction.update(requestRef, updates);
                    return snapshot;
                })
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null) {
                        return;
                    }
                    updateLinkedPassStatusForRequest(
                            requestId,
                            Arrays.asList(GUEST_PASS_STATUS_ACTIVE, GUEST_PASS_STATUS_USED),
                            GUEST_PASS_STATUS_OVERDUE,
                            currentActorUid("system"),
                            "system",
                            "Guest pass moved to overdue with entry request",
                            "status_normalizer",
                            "request_overdue",
                            null
                    );
                    appendAccessEvent(
                            AuditEventType.REQUEST_OVERDUE,
                            "entry_request",
                            requestId,
                            requestId,
                            "system",
                            "system",
                            "Request flagged as overdue by system",
                            "status_normalizer",
                            "failure",
                            "request_overdue",
                            GatePolicy.STORED_VALUE,
                            buildRequestMetadata(snapshot)
                    );
                })
                .addOnCompleteListener(task -> releaseInFlight(overdueTransitionInFlight, requestId));
    }

    private void autoReportOverdueRequest(@NonNull EntryRequest request) {
        final String requestId = request.getId().trim();
        if (requestId.isEmpty() || !acquireInFlight(autoReportInFlight, requestId)) {
            return;
        }

        DocumentReference requestRef = entryRequestCollection.document(requestId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(requestRef);
                    if (!snapshot.exists()) {
                        return null;
                    }
                    String currentStatus = safeLower(snapshot.get(STATUS_FIELD));
                    if (!STATUS_OVERDUE.equals(currentStatus)) {
                        return null;
                    }
                    String requesterRole = safeLower(snapshot.get(REQUESTER_ROLE_FIELD));
                    if (!"student".equals(requesterRole)) {
                        return null;
                    }
                    Object expiryValue = snapshot.get(EXPIRY_AT_FIELD);
                    if (!(expiryValue instanceof Timestamp)) {
                        return null;
                    }
                    Date expiryAt = ((Timestamp) expiryValue).toDate();
                    if (new Date().getTime() < (expiryAt.getTime() + OVERDUE_GRACE_MILLIS)) {
                        return null;
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(STATUS_FIELD, STATUS_REPORTED);
                    updates.put(REPORTED_AT_FIELD, FieldValue.serverTimestamp());
                    updates.put(REPORTED_BY_UID_FIELD, "system");
                    updates.put(REPORTED_BY_ROLE_FIELD, "system");
                    updates.put(REPORT_REASON_CODE_FIELD, REPORT_REASON_OVERDUE_GRACE);
                    updates.put(REPORT_SOURCE_FIELD, REPORT_SOURCE_SYSTEM_OVERDUE);
                    updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
                    transaction.update(requestRef, updates);
                    return snapshot;
                })
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null) {
                        return;
                    }
                    updateLinkedPassStatusForRequest(
                            requestId,
                            Arrays.asList(
                                    GUEST_PASS_STATUS_ACTIVE,
                                    GUEST_PASS_STATUS_USED,
                                    GUEST_PASS_STATUS_OVERDUE
                            ),
                            GUEST_PASS_STATUS_REPORTED,
                            "system",
                            "system",
                            "Guest pass moved to reported after overdue grace elapsed",
                            "system_overdue",
                            "pass_reported",
                            AuditEventType.PASS_REPORTED
                    );
                    createEntryReportAlert(
                            requestId,
                            "Guest automatically reported after overdue grace period elapsed.",
                            "HIGH",
                            "system",
                            "system",
                            "System",
                            REPORT_REASON_OVERDUE_GRACE,
                            REPORT_SOURCE_SYSTEM_OVERDUE,
                            "entry_report_overdue_" + requestId,
                            buildRequestMetadata(snapshot)
                    );
                    appendAccessEvent(
                            AuditEventType.ENTRY_REPORTED_OVERDUE,
                            "entry_request",
                            requestId,
                            requestId,
                            "system",
                            "system",
                            "Entry request auto-reported after overdue grace period",
                            REPORT_SOURCE_SYSTEM_OVERDUE,
                            "failure",
                            REPORT_REASON_OVERDUE_GRACE,
                            GatePolicy.STORED_VALUE,
                            buildRequestMetadata(snapshot)
                    );
                })
                .addOnCompleteListener(task -> releaseInFlight(autoReportInFlight, requestId));
    }

    @Override
    public void listenDashboardState(@NonNull DashboardStateListener listener) {
        removeDashboardStateListener();
        dashboardStateRegistration = entryRequestCollection
                .document(DASHBOARD_STATE_DOC)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        listener.onData(DashboardState.defaultState());
                        return;
                    }
                    Map<String, Object> data = snapshot.getData();
                    if (data == null) {
                        data = new HashMap<>();
                    }
                    Object type = data.get(TYPE_FIELD);
                    if (type == null || TYPE_STATE.equals(String.valueOf(type))) {
                        listener.onData(DashboardState.fromMap(data));
                    } else {
                        listener.onData(DashboardState.defaultState());
                    }
                });
    }

    @Override
    public void searchRequests(@NonNull String query, @NonNull RequestListListener listener) {
        final String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        entryRequestCollection
                .whereEqualTo(TYPE_FIELD, TYPE_REQUEST)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<EntryRequest> result = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Map<String, Object> data = document.getData();
                        EntryRequest request = EntryRequest.fromMap(document.getId(), data);
                        if (normalizedQuery.isEmpty() || matchesQuery(request, data, normalizedQuery)) {
                            result.add(request);
                        }
                    }
                    listener.onData(result);
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void verifyCredential(@NonNull String identifier, @NonNull CredentialListener listener) {
        final String normalizedIdentifier = identifier.trim();
        if (normalizedIdentifier.isEmpty()) {
            listener.onData(new CredentialVerificationResult(false, "", "", "Enter a credential identifier"));
            return;
        }
        firestore.collection(COLLECTION_CREDENTIALS)
                .whereEqualTo(CREDENTIAL_IDENTIFIER_FIELD, normalizedIdentifier)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        listener.onData(new CredentialVerificationResult(false, normalizedIdentifier, "", "Credential not found."));
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    boolean isValid = false;
                    Object isValidField = doc.get(CREDENTIAL_VALID_FIELD);
                    if (isValidField instanceof Boolean) {
                        isValid = (Boolean) isValidField;
                    }
                    String holderName = doc.getString(CREDENTIAL_NAME_FIELD);
                    if (holderName == null) holderName = "";
                    String failureMessage = "Credential is not valid.";
                    if (isExpired(doc.get(EXPIRY_AT_FIELD))) {
                        isValid = false;
                        failureMessage = "Credential has expired.";
                    }
                    if (isValid) {
                        listener.onData(new CredentialVerificationResult(true, normalizedIdentifier, holderName, "Credential verified successfully."));
                    } else {
                        listener.onData(new CredentialVerificationResult(false, normalizedIdentifier, holderName, failureMessage));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void createEntryRequest(
            @NonNull String requesterUid,
            @NonNull String requesterRole,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String hostName,
            @Nullable Timestamp expiresAt,
            @NonNull CompletionCallback callback
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(TYPE_FIELD, TYPE_REQUEST);
        payload.put(STATUS_FIELD, "pending");
        payload.put(GUEST_NAME_FIELD, guestName);
        payload.put("roleTag", "Guest");
        payload.put(GUEST_ID_FIELD, guestIdNumber);
        payload.put(HOST_NAME_FIELD, hostName);
        payload.put(GATE_LABEL_FIELD, GatePolicy.STORED_VALUE);
        payload.put("iconType", "guest");
        payload.put("expiresAt", expiresAt);
        payload.put(REQUESTER_UID_FIELD, requesterUid);
        payload.put(REQUESTER_ROLE_FIELD, requesterRole);
        // payload.put(ENTERED_AT_FIELD, FieldValue.serverTimestamp());
        payload.put(CREATED_AT_FIELD, FieldValue.serverTimestamp());
        payload.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());

        entryRequestCollection
                .add(payload)
                .addOnSuccessListener(reference -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("guestName", guestName);
                    metadata.put("guestIdNumber", guestIdNumber);
                    appendAccessEvent(
                            AuditEventType.REQUEST_CREATED,
                            "entry_request",
                            reference.getId(),
                            reference.getId(),
                            requesterUid,
                            requesterRole,
                            "Entry request created",
                            "requester_form",
                            "success",
                            "",
                            GatePolicy.STORED_VALUE,
                            metadata
                    );
                    callback.onComplete(true, "Entry request submitted", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to create entry request", error));
    }

    @Override
    public void logEntry(@NonNull String requestId, @NonNull CompletionCallback callback) {
        enforceGuardShiftOrRun("allow_entry", requestId, callback, () -> {
            String actorUid = currentActorUid("guard");
            String actorRole = currentActorRole("guard");
            String actorName = currentActorName(actorUid, actorRole);
            Map<String, Object> updates = new HashMap<>();
            updates.put(STATUS_FIELD, STATUS_ACTIVE);
            updates.put(GATE_LABEL_FIELD, GatePolicy.STORED_VALUE);
            updates.put(ADMITTED_BY_UID_FIELD, actorUid);
            updates.put(ADMITTED_BY_ROLE_FIELD, actorRole);
            updates.put(ADMITTED_BY_NAME_FIELD, actorName);
            updates.put(ENTERED_AT_FIELD, FieldValue.serverTimestamp());
            updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
            entryRequestCollection
                    .document(requestId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        appendAccessEvent(
                                AuditEventType.ENTRY_ALLOWED,
                                "entry_request",
                                requestId,
                                requestId,
                                actorUid,
                                actorRole,
                                "Entry logged",
                                "guard_decision",
                                "success",
                                "entry_allowed",
                                GatePolicy.STORED_VALUE,
                                new HashMap<>()
                        );
                        callback.onComplete(true, "Entry logged successfully", null);
                    })
                    .addOnFailureListener(error -> callback.onComplete(false, "Failed to log entry", error));
        });
    }

    @Override
    public void logExit(@NonNull String requestId, @NonNull CompletionCallback callback) {
        enforceGuardShiftOrRun("log_exit", requestId, callback, () -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put(STATUS_FIELD, STATUS_EXITED);
            updates.put(EXITED_AT_FIELD, FieldValue.serverTimestamp());
            updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
            entryRequestCollection
                    .document(requestId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        appendAccessEvent(
                                AuditEventType.EXIT_LOGGED,
                                "entry_request",
                                requestId,
                                requestId,
                                currentActorUid("guard"),
                                currentActorRole("guard"),
                                "Exit logged",
                                "guard_details",
                                "success",
                                "exit_logged",
                                GatePolicy.STORED_VALUE,
                                new HashMap<>()
                        );
                        callback.onComplete(true, "Request marked as exited", null);
                    })
                    .addOnFailureListener(error -> callback.onComplete(false, "Failed to log exit", error));
        });
    }

    @Override
    public void denyRequest(@NonNull String requestId, @NonNull String reason, @NonNull CompletionCallback callback) {
        enforceGuardShiftOrRun("deny_entry", requestId, callback, () -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put(STATUS_FIELD, STATUS_DENIED);
            updates.put(DENIAL_REASON_FIELD, reason.trim());
            updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
            entryRequestCollection
                    .document(requestId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("reason", reason.trim());
                        appendAccessEvent(
                                AuditEventType.ENTRY_DENIED,
                                "entry_request",
                                requestId,
                                requestId,
                                currentActorUid("guard"),
                                currentActorRole("guard"),
                                "Request denied: " + reason,
                                "guard_decision",
                                "failure",
                                "entry_denied",
                                GatePolicy.STORED_VALUE,
                                metadata
                        );
                        callback.onComplete(true, "Request denied", null);
                    })
                    .addOnFailureListener(error -> callback.onComplete(false, "Failed to deny request", error));
        });
    }

    @Override
    public void reportViolation(@NonNull String requestId, @NonNull CompletionCallback callback) {
        String normalizedRequestId = requestId.trim();
        if (normalizedRequestId.isEmpty()) {
            callback.onComplete(false, "Request ID is required.", null);
            return;
        }
        enforceGuardShiftOrRun("report_violation", normalizedRequestId, callback, () -> {
            String actorUid = currentActorUid("guard");
            String actorRole = currentActorRole("guard");
            String actorName = currentActorName(actorUid, actorRole);
            DocumentReference requestRef = entryRequestCollection.document(normalizedRequestId);
            firestore.runTransaction(transaction -> {
                        DocumentSnapshot snapshot = transaction.get(requestRef);
                        if (!snapshot.exists()) {
                            throw new IllegalStateException("Request not found.");
                        }
                        String currentStatus = safeLower(snapshot.get(STATUS_FIELD));
                        if (STATUS_REPORTED.equals(currentStatus)) {
                            return ReportMutationResult.alreadyReported(snapshot);
                        }
                        if (!STATUS_ACTIVE.equals(currentStatus) && !STATUS_OVERDUE.equals(currentStatus)) {
                            throw new IllegalStateException("Only active or overdue requests can be reported.");
                        }
                        Map<String, Object> updates = new HashMap<>();
                        updates.put(STATUS_FIELD, STATUS_REPORTED);
                        updates.put(REPORTED_AT_FIELD, FieldValue.serverTimestamp());
                        updates.put(REPORTED_BY_UID_FIELD, actorUid);
                        updates.put(REPORTED_BY_ROLE_FIELD, actorRole);
                        updates.put(REPORT_REASON_CODE_FIELD, REPORT_REASON_GUARD_VIOLATION);
                        updates.put(REPORT_SOURCE_FIELD, REPORT_SOURCE_GUARD_MANUAL);
                        updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
                        transaction.update(requestRef, updates);
                        return ReportMutationResult.updated(snapshot);
                    })
                    .addOnSuccessListener(result -> {
                        if (result == null) {
                            callback.onComplete(false, "Unable to report request.", null);
                            return;
                        }
                        if (result.alreadyReported) {
                            callback.onComplete(true, "Request already reported.", null);
                            return;
                        }
                        updateLinkedPassStatusForRequest(
                                normalizedRequestId,
                                Arrays.asList(
                                        GUEST_PASS_STATUS_ACTIVE,
                                        GUEST_PASS_STATUS_USED,
                                        GUEST_PASS_STATUS_OVERDUE
                                ),
                                GUEST_PASS_STATUS_REPORTED,
                                actorUid,
                                actorRole,
                                "Guest pass moved to reported due to guard violation",
                                REPORT_SOURCE_GUARD_MANUAL,
                                "pass_reported",
                                AuditEventType.PASS_REPORTED
                        );
                        createEntryReportAlert(
                                normalizedRequestId,
                                "Guard reported an active visitor entry as a violation.",
                                "HIGH",
                                actorUid,
                                actorRole,
                                actorName,
                                REPORT_REASON_GUARD_VIOLATION,
                                REPORT_SOURCE_GUARD_MANUAL,
                                "entry_report_manual_" + normalizedRequestId,
                                buildRequestMetadata(result.snapshot)
                        );
                        appendAccessEvent(
                                AuditEventType.ENTRY_REPORTED_MANUAL,
                                "entry_request",
                                normalizedRequestId,
                                normalizedRequestId,
                                actorUid,
                                actorRole,
                                "Entry request reported by guard as violation",
                                REPORT_SOURCE_GUARD_MANUAL,
                                "failure",
                                REPORT_REASON_GUARD_VIOLATION,
                                GatePolicy.STORED_VALUE,
                                buildRequestMetadata(result.snapshot)
                        );
                        callback.onComplete(true, "Request reported successfully.", null);
                    })
                    .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
        });
    }

    @Override
    public void listenRequestsByRequester(@NonNull String requesterUid, @NonNull RequestListListener listener) {
        removeRequesterRequestsListener();
        requesterRequestsRegistration = entryRequestCollection
                .whereEqualTo(TYPE_FIELD, TYPE_REQUEST)
                .whereEqualTo(REQUESTER_UID_FIELD, requesterUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<EntryRequest> requests = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            requests.add(EntryRequest.fromMap(document.getId(), document.getData()));
                        }
                    }
                    // Sort newest first using document id (lexicographic desc approximates creation order).
                    requests.sort((a, b) -> b.getId().compareTo(a.getId()));
                    listener.onData(requests);
                });
    }

    @Override
    public void removeListeners() {
        removeActiveRequestsListener();
        removeDashboardStateListener();
        removeRequesterRequestsListener();
    }

    private boolean matchesQuery(
            @NonNull EntryRequest request,
            @Nullable Map<String, Object> rawData,
            @NonNull String normalizedQuery
    ) {
        return request.getFullName().toLowerCase(Locale.getDefault()).contains(normalizedQuery)
                || request.getHostName().toLowerCase(Locale.getDefault()).contains(normalizedQuery)
                || request.getGateLabel().toLowerCase(Locale.getDefault()).contains(normalizedQuery)
                || safeLower(rawData == null ? null : rawData.get(GUEST_ID_FIELD)).contains(normalizedQuery)
                || safeLower(rawData == null ? null : rawData.get("vehicleNumber")).contains(normalizedQuery)
                || safeLower(rawData == null ? null : rawData.get("plateNumber")).contains(normalizedQuery)
                || request.getId().toLowerCase(Locale.getDefault()).contains(normalizedQuery);
    }

    private boolean isStudentRequest(@NonNull EntryRequest request) {
        return "student".equals(safeLower(request.getRequesterRole()));
    }

    private boolean isTimeExpired(@Nullable Timestamp expiresAt, @NonNull Date now) {
        return expiresAt != null && !expiresAt.toDate().after(now);
    }

    private boolean isOverdueGraceElapsed(@Nullable Timestamp expiresAt, @NonNull Date now) {
        return expiresAt != null
                && now.getTime() >= (expiresAt.toDate().getTime() + OVERDUE_GRACE_MILLIS);
    }

    @NonNull
    private EntryRequest asStatus(@NonNull EntryRequest request, @NonNull String status) {
        return new EntryRequest(
                request.getId(),
                request.getFullName(),
                request.getRoleTag(),
                request.getHostName(),
                request.getGateLabel(),
                request.getGuestIdNumber(),
                request.hasVehicle(),
                request.getVehiclePlate(),
                request.getGuestType(),
                request.getRequesterRole(),
                request.getAdmittedByUid(),
                request.getAdmittedByRole(),
                request.getAdmittedByName(),
                request.getEnteredAt(),
                status,
                request.getExpiresAt(),
                request.getIconType()
        );
    }

    private boolean acquireInFlight(@NonNull Set<String> inFlightSet, @NonNull String requestId) {
        synchronized (inFlightSet) {
            if (inFlightSet.contains(requestId)) {
                return false;
            }
            inFlightSet.add(requestId);
            return true;
        }
    }

    private void releaseInFlight(@NonNull Set<String> inFlightSet, @NonNull String requestId) {
        synchronized (inFlightSet) {
            inFlightSet.remove(requestId);
        }
    }

    private void updateLinkedPassStatusForRequest(
            @NonNull String requestId,
            @NonNull List<String> allowedStatuses,
            @NonNull String targetStatus,
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String description,
            @NonNull String source,
            @NonNull String reasonCode,
            @Nullable String auditEventType
    ) {
        firestore.collection(COLLECTION_GUEST_PASSES)
                .whereEqualTo(ENTRY_REQUEST_ID_FIELD, requestId)
                .whereIn(STATUS_FIELD, allowedStatuses)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        return;
                    }
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot passDoc : snapshot.getDocuments()) {
                        batch.update(
                                passDoc.getReference(),
                                STATUS_FIELD, targetStatus,
                                UPDATED_AT_FIELD, FieldValue.serverTimestamp(),
                                "admittedByUid", actorUid,
                                "admissionMethod", "REPORTED".equals(targetStatus) ? "REPORTED" : targetStatus.toUpperCase(Locale.getDefault())
                        );
                    }
                    batch.commit();

                    if (auditEventType == null || auditEventType.trim().isEmpty()) {
                        return;
                    }
                    for (DocumentSnapshot passDoc : snapshot.getDocuments()) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("passCode", safeString(passDoc.get("passCode")));
                        metadata.put("targetStatus", targetStatus);
                        appendAccessEvent(
                                auditEventType,
                                "guest_pass",
                                passDoc.getId(),
                                requestId,
                                actorUid,
                                actorRole,
                                description,
                                source,
                                "failure",
                                reasonCode,
                                GatePolicy.normalizeStoredValue(safeLower(passDoc.get(GATE_LABEL_FIELD))),
                                metadata
                        );
                    }
                });
    }

    private void createEntryReportAlert(
            @NonNull String requestId,
            @NonNull String message,
            @NonNull String severity,
            @NonNull String reportedByUid,
            @NonNull String reportedByRole,
            @NonNull String reportedByName,
            @NonNull String reasonCode,
            @NonNull String source,
            @NonNull String alertDocId,
            @Nullable Map<String, Object> requestMetadata
    ) {
        Map<String, Object> alert = new HashMap<>();
        alert.put(ALERT_TYPE_FIELD, ALERT_TYPE_ENTRY_REPORT);
        alert.put(ENTRY_REQUEST_ID_FIELD, requestId);
        alert.put(IDENTIFIER_FIELD, requestId);
        alert.put(FAIL_COUNT_FIELD, 1);
        alert.put(SEVERITY_FIELD, severity);
        alert.put(MESSAGE_FIELD, message);
        alert.put(REPORTED_BY_UID_FIELD, reportedByUid);
        alert.put(REPORTED_BY_ROLE_FIELD, reportedByRole);
        alert.put(REPORTED_BY_NAME_FIELD, reportedByName);
        alert.put(REPORT_REASON_CODE_FIELD, reasonCode);
        alert.put(REPORT_SOURCE_FIELD, source);
        alert.put("incidentStatus", "new");
        alert.put("interventionSummary", "");
        if (requestMetadata != null) {
            if (requestMetadata.containsKey("guestName")) {
                alert.put("guestName", requestMetadata.get("guestName"));
            }
            if (requestMetadata.containsKey("guestIdNumber")) {
                alert.put("guestIdNumber", requestMetadata.get("guestIdNumber"));
            }
            if (requestMetadata.containsKey("hostName")) {
                alert.put("hostName", requestMetadata.get("hostName"));
            }
            if (requestMetadata.containsKey("requesterUid")) {
                alert.put("requesterUid", requestMetadata.get("requesterUid"));
            }
            if (requestMetadata.containsKey("requesterRole")) {
                alert.put("requesterRole", requestMetadata.get("requesterRole"));
            }
            if (requestMetadata.containsKey("gateLabel")) {
                alert.put("gateLabel", requestMetadata.get("gateLabel"));
            }
        }
        alert.put(CREATED_AT_FIELD, FieldValue.serverTimestamp());
        alert.put(LAST_FAILED_AT_FIELD, FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_ALERTS)
                .document(alertDocId)
                .set(alert);
    }

    @NonNull
    private Map<String, Object> buildRequestMetadata(@NonNull DocumentSnapshot snapshot) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("guestName", safeString(snapshot.get(GUEST_NAME_FIELD)));
        metadata.put("guestIdNumber", safeString(snapshot.get(GUEST_ID_FIELD)));
        metadata.put("hostName", safeString(snapshot.get(HOST_NAME_FIELD)));
        metadata.put("requesterUid", safeString(snapshot.get(REQUESTER_UID_FIELD)));
        metadata.put("requesterRole", safeString(snapshot.get(REQUESTER_ROLE_FIELD)));
        metadata.put("gateLabel", GatePolicy.normalizeStoredValue(safeString(snapshot.get(GATE_LABEL_FIELD))));
        return metadata;
    }

    @NonNull
    private String safeMessage(@NonNull Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "Operation failed." : message;
    }

    @NonNull
    private String safeString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private void appendAccessEvent(
            @NonNull String eventType,
            @NonNull String entityType,
            @NonNull String entityId,
            @NonNull String requestId,
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String description,
            @NonNull String source,
            @NonNull String outcome,
            @NonNull String reasonCode,
            @NonNull String gateLabel,
            @Nullable Map<String, Object> metadata
    ) {
        Map<String, Object> event = AuditEventPayloadFactory.build(
                eventType,
                actorUid,
                actorRole,
                entityType,
                entityId,
                requestId,
                description,
                source,
                outcome,
                reasonCode,
                gateLabel,
                metadata
        );
        firestore.collection(COLLECTION_ACCESS_EVENTS).add(event);
    }

    @NonNull
    private String currentActorUid(@NonNull String fallbackRole) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) {
            return "unknown_" + fallbackRole;
        }
        String uid = profile.getUid().trim();
        return uid.isEmpty() ? "unknown_" + fallbackRole : uid;
    }

    @NonNull
    private String currentActorRole(@NonNull String fallbackRole) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) {
            return fallbackRole;
        }
        String role = profile.getRole().trim();
        return role.isEmpty() ? fallbackRole : role;
    }

    @NonNull
    private String currentActorName(@NonNull String fallbackUid, @NonNull String fallbackRole) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) {
            return fallbackUid.trim().isEmpty() ? fallbackRole : fallbackUid;
        }
        String name = profile.getDisplayName().trim();
        if (!name.isEmpty()) {
            return name;
        }
        String uid = profile.getUid().trim();
        return uid.isEmpty() ? fallbackRole : uid;
    }

    private void enforceGuardShiftOrRun(
            @NonNull String actionCode,
            @NonNull String requestId,
            @NonNull CompletionCallback callback,
            @NonNull GuardAction guardAction
    ) {
        guardAction.run();
    }

    private boolean isExpired(@Nullable Object expiryValue) {
        if (!(expiryValue instanceof Timestamp)) {
            return false;
        }
        Timestamp expiry = (Timestamp) expiryValue;
        return expiry.toDate().before(new Date());
    }

    @NonNull
    private String safeLower(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).toLowerCase(Locale.getDefault());
    }

    private interface GuardAction {
        void run();
    }

    private static final class ReportMutationResult {
        private final boolean alreadyReported;
        private final DocumentSnapshot snapshot;

        private ReportMutationResult(boolean alreadyReported, @NonNull DocumentSnapshot snapshot) {
            this.alreadyReported = alreadyReported;
            this.snapshot = snapshot;
        }

        @NonNull
        private static ReportMutationResult alreadyReported(@NonNull DocumentSnapshot snapshot) {
            return new ReportMutationResult(true, snapshot);
        }

        @NonNull
        private static ReportMutationResult updated(@NonNull DocumentSnapshot snapshot) {
            return new ReportMutationResult(false, snapshot);
        }
    }

    private void removeActiveRequestsListener() {
        if (activeRequestsRegistration != null) {
            activeRequestsRegistration.remove();
            activeRequestsRegistration = null;
        }
    }

    private void removeDashboardStateListener() {
        if (dashboardStateRegistration != null) {
            dashboardStateRegistration.remove();
            dashboardStateRegistration = null;
        }
    }

    private void removeRequesterRequestsListener() {
        if (requesterRequestsRegistration != null) {
            requesterRequestsRegistration.remove();
            requesterRequestsRegistration = null;
        }
    }
}
