package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.CredentialVerificationResult;
import com.example.glitch.model.DashboardState;
import com.example.glitch.model.EntryRequest;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.VerificationRules;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
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
    static final String COLLECTION_VERIFICATION_RULES = "verification_rules";
    static final String DOC_CURRENT_RULES = "current";
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
    static final String EVENT_TYPE_FIELD = "eventType";
    static final String EVENT_DESCRIPTION_FIELD = "description";
    static final String EVENT_REQUEST_ID_FIELD = "requestId";
    static final String EVENT_ACTOR_UID_FIELD = "actorUid";
    static final String EVENT_ACTOR_ROLE_FIELD = "actorRole";
    static final String FAIL_COUNT_FIELD = "failCount";
    static final String SEVERITY_FIELD = "severity";
    static final String MESSAGE_FIELD = "message";
    static final String IDENTIFIER_FIELD = "identifier";
    static final String LAST_FAILED_AT_FIELD = "lastFailedAt";
    static final String EXPIRY_AT_FIELD = "expiresAt";

    private final FirebaseFirestore firestore;
    private final CollectionReference entryRequestCollection;
    private ListenerRegistration activeRequestsRegistration;
    private ListenerRegistration dashboardStateRegistration;
    private ListenerRegistration requesterRequestsRegistration;

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
                    List<String> overdueRequestIds = new ArrayList<>();
                    Date now = new Date();

                    if (snapshots != null) {
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            EntryRequest request = EntryRequest.fromMap(document.getId(), document.getData());
                            
                            // Check for overdue status (entered but past expiry)
                            if (STATUS_ACTIVE.equals(request.getStatus()) && 
                                request.getExpiresAt() != null && 
                                request.getExpiresAt().toDate().before(now)) {
                                overdueRequestIds.add(request.getId());
                            }
                            
                            requests.add(request);
                        }
                    }
                    
                    if (!overdueRequestIds.isEmpty()) {
                        markRequestsAsOverdue(overdueRequestIds);
                    }
                    
                    listener.onData(requests);
                });
    }

    /**
     * Updates requests and their linked guest passes to "overdue" status.
     */
    private void markRequestsAsOverdue(@NonNull List<String> requestIds) {
        for (String requestId : requestIds) {
            WriteBatch batch = firestore.batch();
            
            // Update Entry Request
            batch.update(entryRequestCollection.document(requestId), 
                    STATUS_FIELD, STATUS_OVERDUE,
                    UPDATED_AT_FIELD, FieldValue.serverTimestamp());
            
            // Update linked Guest Passes
            firestore.collection(COLLECTION_GUEST_PASSES)
                    .whereEqualTo("entryRequestId", requestId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        WriteBatch passBatch = firestore.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            passBatch.update(doc.getReference(), 
                                    STATUS_FIELD, STATUS_OVERDUE,
                                    UPDATED_AT_FIELD, FieldValue.serverTimestamp());
                        }
                        passBatch.commit();
                        
                        // Append event for the overdue status
                        appendAccessEvent("OVERDUE", requestId, "", "system", "Request flagged as overdue by system");
                    });
            
            batch.commit();
        }
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

        firestore.collection(COLLECTION_VERIFICATION_RULES)
                .document(DOC_CURRENT_RULES)
                .get()
                .addOnSuccessListener(ruleSnapshot -> verifyCredentialWithRules(
                        normalizedIdentifier,
                        VerificationRules.fromMap(ruleSnapshot.getData()),
                        listener
                ))
                .addOnFailureListener(error -> verifyCredentialWithRules(
                        normalizedIdentifier,
                        VerificationRules.defaultRules(),
                        listener
                ));
    }

    private void verifyCredentialWithRules(
            @NonNull String identifier,
            @NonNull VerificationRules rules,
            @NonNull CredentialListener listener
    ) {
        Set<String> bannedIdentifiers = parseBannedIdentifiers(rules.getBannedIdentifiersCsv());
        if (bannedIdentifiers.contains(identifier.toLowerCase(Locale.getDefault()))) {
            recordFailedVerification(identifier, "Identifier blocked by verification rule", rules.getAlertThreshold());
            listener.onData(new CredentialVerificationResult(
                    false,
                    identifier,
                    "",
                    "Credential is blocked by policy."
            ));
            return;
        }

        firestore.collection(COLLECTION_CREDENTIALS)
                .whereEqualTo(CREDENTIAL_IDENTIFIER_FIELD, identifier)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        recordFailedVerification(identifier, "Identifier not found", rules.getAlertThreshold());
                        listener.onData(new CredentialVerificationResult(
                                false,
                                identifier,
                                "",
                                "Credential not found."
                        ));
                        return;
                    }

                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    boolean isValid = false;
                    Object isValidField = doc.get(CREDENTIAL_VALID_FIELD);
                    if (isValidField instanceof Boolean) {
                        isValid = (Boolean) isValidField;
                    }
                    String holderName = doc.getString(CREDENTIAL_NAME_FIELD);
                    if (holderName == null) {
                        holderName = "";
                    }

                    String failureMessage = "Credential is not valid.";
                    if (rules.isEnforceIdExpiry() && isExpired(doc.get(EXPIRY_AT_FIELD))) {
                        isValid = false;
                        failureMessage = "Credential has expired.";
                    }

                    if (isValid) {
                        listener.onData(new CredentialVerificationResult(
                                true,
                                identifier,
                                holderName,
                                "Credential verified successfully."
                        ));
                    } else {
                        recordFailedVerification(identifier, failureMessage, rules.getAlertThreshold());
                        listener.onData(new CredentialVerificationResult(
                                false,
                                identifier,
                                holderName,
                                failureMessage
                        ));
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
                    appendAccessEvent("REQUEST_CREATED", reference.getId(), requesterUid, requesterRole, "Entry request created");
                    callback.onComplete(true, "Entry request submitted", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to create entry request", error));
    }

    @Override
    public void logEntry(@NonNull String requestId, @NonNull CompletionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(STATUS_FIELD, STATUS_ACTIVE);
        updates.put(GATE_LABEL_FIELD, GatePolicy.STORED_VALUE);
        updates.put(ENTERED_AT_FIELD, FieldValue.serverTimestamp());
        updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
        entryRequestCollection
                .document(requestId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    appendAccessEvent("ENTRY", requestId, "", "guard", "Entry logged");
                    callback.onComplete(true, "Entry logged successfully", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to log entry", error));
    }

    @Override
    public void logExit(@NonNull String requestId, @NonNull CompletionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(STATUS_FIELD, STATUS_EXITED);
        updates.put(EXITED_AT_FIELD, FieldValue.serverTimestamp());
        updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
        entryRequestCollection
                .document(requestId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    appendAccessEvent("EXIT", requestId, "", "guard", "Exit logged");
                    callback.onComplete(true, "Request marked as exited", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to log exit", error));
    }

    @Override
    public void denyRequest(@NonNull String requestId, @NonNull String reason, @NonNull CompletionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(STATUS_FIELD, STATUS_DENIED);
        updates.put(DENIAL_REASON_FIELD, reason.trim());
        updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
        entryRequestCollection
                .document(requestId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    appendAccessEvent("DENY", requestId, "", "guard", "Request denied: " + reason);
                    callback.onComplete(true, "Request denied", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to deny request", error));
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

    private void appendAccessEvent(
            @NonNull String eventType,
            @NonNull String requestId,
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String description
    ) {
        Map<String, Object> event = new HashMap<>();
        event.put(EVENT_TYPE_FIELD, eventType);
        event.put(EVENT_REQUEST_ID_FIELD, requestId);
        event.put(EVENT_ACTOR_UID_FIELD, actorUid);
        event.put(EVENT_ACTOR_ROLE_FIELD, actorRole);
        event.put(EVENT_DESCRIPTION_FIELD, description);
        event.put(CREATED_AT_FIELD, FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_ACCESS_EVENTS).add(event);
    }

    private void recordFailedVerification(@NonNull String identifier, @NonNull String message, int alertThreshold) {
        String alertDocId = "credential_" + identifier.replaceAll("[^a-zA-Z0-9]", "_");
        firestore.collection(COLLECTION_ALERTS)
                .document(alertDocId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int currentCount = 0;
                    if (snapshot.exists()) {
                        Object count = snapshot.get(FAIL_COUNT_FIELD);
                        if (count instanceof Number) {
                            currentCount = ((Number) count).intValue();
                        }
                    }
                    int nextCount = currentCount + 1;
                    Map<String, Object> payload = new HashMap<>();
                    payload.put(IDENTIFIER_FIELD, identifier);
                    payload.put(FAIL_COUNT_FIELD, nextCount);
                    payload.put(SEVERITY_FIELD, nextCount >= Math.max(1, alertThreshold) ? "HIGH" : "MEDIUM");
                    payload.put(MESSAGE_FIELD, message);
                    payload.put(LAST_FAILED_AT_FIELD, FieldValue.serverTimestamp());
                    payload.put(CREATED_AT_FIELD, FieldValue.serverTimestamp());
                    firestore.collection(COLLECTION_ALERTS).document(alertDocId).set(payload);
                });
    }

    private boolean isExpired(@Nullable Object expiryValue) {
        if (!(expiryValue instanceof Timestamp)) {
            return false;
        }
        Timestamp expiry = (Timestamp) expiryValue;
        return expiry.toDate().before(new Date());
    }

    @NonNull
    private Set<String> parseBannedIdentifiers(@NonNull String csv) {
        if (csv.trim().isEmpty()) {
            return new HashSet<>();
        }
        String[] tokens = csv.split(",");
        Set<String> banned = new HashSet<>();
        for (String token : Arrays.asList(tokens)) {
            String value = token.trim().toLowerCase(Locale.getDefault());
            if (!value.isEmpty()) {
                banned.add(value);
            }
        }
        return banned;
    }

    @NonNull
    private String safeLower(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).toLowerCase(Locale.getDefault());
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
