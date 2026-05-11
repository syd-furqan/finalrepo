package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.auth.SessionManager;
import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.InGateServiceControl;
import com.example.glitch.model.GuestPassTimePolicy;
import com.example.glitch.model.PhoneValidationResult;
import com.example.glitch.model.UserProfile;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Firestore-backed guest pass repository.
 * Updated with a pendingIssuances lock to prevent duplicate creation race conditions.
 */
public class FirestoreGuestPassRepository implements GuestPassRepository {
    private static final String COLLECTION_GUEST_PASSES = "guest_passes";
    private static final String COLLECTION_ENTRY_REQUESTS = "entry_requests";
    private static final String COLLECTION_ACCESS_EVENTS = "access_events";
    private static final String COLLECTION_SERVICE_CONTROLS = "service_controls";
    private static final String DOC_IN_GATE = "in_gate";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_USED = "used";
    private static final String STATUS_EXPIRED = "expired";
    private static final String STATUS_DENIED = "denied";
    private static final String STATUS_OVERDUE = "overdue";
    private static final String STATUS_EXITED = "exited";
    private static final String STATUS_REPORTED = "reported";
    private static final String REQUEST_STATUS_PENDING = "pending";
    private static final String REQUEST_STATUS_DENIED = "denied";
    private static final String REQUEST_STATUS_FIELD = "status";
    private static final String REQUEST_ENTERED_AT_FIELD = "enteredAt";
    private static final String ISSUE_WINDOW_CLOSED_MESSAGE =
            "Guest passes can only be created between 8:30 AM and 10:00 PM.";
    private static final String INVALID_CNIC_MESSAGE = "Enter a valid CNIC in xxxxx-xxxxxxx-x format.";
    private static final String INVALID_PLATE_MESSAGE = "Enter vehicle plate as AAA-xxx(x).";
    private final FirebaseFirestore firestore;
    private final CollectionReference collection;
    private final CollectionReference entryRequestCollection;
    private final InterventionRepository interventionRepository;
    private final UserNotificationWriter notificationWriter;
    
    // In-memory lock to prevent rapid-click duplicates for the same user
    private final Set<String> pendingIssuances = new HashSet<>();
    private final SimpleDateFormat inhibitionDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public FirestoreGuestPassRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreGuestPassRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.collection = firestore.collection(COLLECTION_GUEST_PASSES);
        this.entryRequestCollection = firestore.collection(COLLECTION_ENTRY_REQUESTS);
        this.interventionRepository = new FirestoreInterventionRepository(firestore);
        this.notificationWriter = new UserNotificationWriter(firestore);
    }

    @Override
    public void createGuestPass(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String sponsorEmail,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String guestPhone,
            boolean hasVehicle,
            @NonNull String vehiclePlate,
            @NonNull OperationCallback callback
    ) {
        PhoneValidationResult fallbackValidation = PhoneValidationResult.failure(
                guestPhone, "", "", "Legacy call path — no libphonenumber validation.");
        issueGuestPassWithEntryRequest(
                sponsorUid, sponsorRole, sponsorName, sponsorEmail, "",
                guestName, guestIdNumber, guestPhone, hasVehicle, vehiclePlate,
                fallbackValidation,
                (success, message, issuedPass, exception) -> callback.onComplete(success, message, exception)
        );
    }

    @Override
    public void issueGuestPassWithEntryRequest(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String sponsorEmail,
            @NonNull String sponsorStudentId,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String guestPhone,
            boolean hasVehicle,
            @NonNull String vehiclePlate,
            @NonNull PhoneValidationResult phoneValidation,
            @NonNull IssueCallback callback
    ) {
        // if (!GuestPassTimePolicy.canIssueNow()) {
        //     callback.onComplete(false, ISSUE_WINDOW_CLOSED_MESSAGE, null, null);
        //     return;
        // }

        // Phone already validated by UI via libphonenumber; reject if somehow invalid
        if (!phoneValidation.isValid()) {
            callback.onComplete(false, "Invalid phone number: " + phoneValidation.getFailureReason(), null, null);
            return;
        }

        String normalizedCnic = GuestIdentityPolicy.normalizeCnic(guestIdNumber);
        if (normalizedCnic == null || !GuestIdentityPolicy.isValidCnic(normalizedCnic)) {
            callback.onComplete(false, INVALID_CNIC_MESSAGE, null, null);
            return;
        }
        String normalizedVehiclePlateCandidate = "";
        if (hasVehicle) {
            normalizedVehiclePlateCandidate = GuestIdentityPolicy.normalizeVehiclePlate(vehiclePlate);
            if (normalizedVehiclePlateCandidate == null
                    || !GuestIdentityPolicy.isValidVehiclePlate(normalizedVehiclePlateCandidate)) {
                callback.onComplete(false, INVALID_PLATE_MESSAGE, null, null);
                return;
            }
        }
        final String normalizedVehiclePlate = normalizedVehiclePlateCandidate;
        String trimmedGuestName = guestName.trim();
        if (trimmedGuestName.isEmpty()) {
            callback.onComplete(false, "Please complete all required fields.", null, null);
            return;
        }

        checkInGateServiceAvailability(sponsorRole, new InGateAvailabilityCallback() {
            @Override
            public void onBlocked(@NonNull String message) {
                callback.onComplete(false, message, null, null);
            }

            @Override
            public void onAvailable() {
                interventionRepository.isGuestBanned(normalizedCnic, (banned, record, message) -> {
                    if (banned) {
                        callback.onComplete(false, "This guest is banned from campus entry.", null, null);
                        return;
                    }
                    if (message.startsWith("ERROR_")) {
                        callback.onComplete(false, "Unable to verify banned-list status right now.", null, null);
                        return;
                    }
                    continueIssuanceAfterBanCheck(
                            sponsorUid,
                            sponsorRole,
                            sponsorName,
                            sponsorEmail,
                            sponsorStudentId,
                            trimmedGuestName,
                            normalizedCnic,
                            guestPhone,
                            hasVehicle,
                            normalizedVehiclePlate,
                            phoneValidation,
                            callback
                    );
                });
            }
        });
    }

    private void checkInGateServiceAvailability(
            @NonNull String sponsorRole,
            @NonNull InGateAvailabilityCallback callback
    ) {
        String normalizedRole = sponsorRole.trim().toLowerCase(Locale.US);
        if (!"faculty".equals(normalizedRole) && !"student".equals(normalizedRole)) {
            callback.onAvailable();
            return;
        }
        firestore.collection(COLLECTION_SERVICE_CONTROLS)
                .document(DOC_IN_GATE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onAvailable();
                        return;
                    }
                    InGateServiceControl control = InGateServiceControl.fromMap(snapshot.getData());
                    if (!control.isBlockingRole(normalizedRole)) {
                        callback.onAvailable();
                        return;
                    }
                    String reason = control.getReason().trim().isEmpty()
                            ? "Administrative restriction is active"
                            : control.getReason().trim();
                    String availabilityWindow = "until further notice";
                    if (InGateServiceControl.MODE_TIMED.equals(control.getMode()) && control.getEndsAt() != null) {
                        availabilityWindow = "until " + inhibitionDateFormat.format(new Date(control.getEndsAt().toDate().getTime()));
                    }
                    callback.onBlocked("In-Gate Not Operational. " + reason + ". Services unavailable " + availabilityWindow + ".");
                })
                .addOnFailureListener(error -> callback.onAvailable());
    }

    private void continueIssuanceAfterBanCheck(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String sponsorEmail,
            @NonNull String sponsorStudentId,
            @NonNull String trimmedGuestName,
            @NonNull String normalizedCnic,
            @NonNull String guestPhone,
            boolean hasVehicle,
            @NonNull String normalizedVehiclePlate,
            @NonNull PhoneValidationResult phoneValidation,
            @NonNull IssueCallback callback
    ) {

        // 1. Immediate in-memory check
        if ("student".equalsIgnoreCase(sponsorRole) && pendingIssuances.contains(sponsorUid)) {
            callback.onComplete(false, "An issuance request is already in progress.", null, null);
            return;
        }

        if ("student".equalsIgnoreCase(sponsorRole)) {
            pendingIssuances.add(sponsorUid);
            
            // 2. Database check
            collection.whereEqualTo("sponsorUid", sponsorUid)
                    .whereIn("status", Arrays.asList(STATUS_ACTIVE, STATUS_USED, STATUS_OVERDUE, STATUS_REPORTED))
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            pendingIssuances.remove(sponsorUid);
                            callback.onComplete(false, "You already have an active guest in progress.", null, null);
                        } else {
                            // 3. Perform write
                            performIssueGuestPass(
                                    sponsorUid,
                                    sponsorRole,
                                    sponsorName,
                                    sponsorEmail,
                                    sponsorStudentId,
                                    trimmedGuestName,
                                    normalizedCnic,
                                    guestPhone,
                                    hasVehicle,
                                    normalizedVehiclePlate,
                                    phoneValidation,
                                    (success, message, issuedPass, exception) -> {
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
            performIssueGuestPass(
                    sponsorUid,
                    sponsorRole,
                    sponsorName,
                    sponsorEmail,
                    sponsorStudentId,
                    trimmedGuestName,
                    normalizedCnic,
                    guestPhone,
                    hasVehicle,
                    normalizedVehiclePlate,
                    phoneValidation,
                    callback
            );
        }
    }

    private void performIssueGuestPass(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String sponsorEmail,
            @NonNull String sponsorStudentId,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String guestPhone,
            boolean hasVehicle,
            @NonNull String vehiclePlate,
            @NonNull PhoneValidationResult phoneValidation,
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
        requestData.put("roleTag", hasVehicle ? "Guest Vehicle" : "Guest");
        requestData.put("guestIdNumber", guestIdNumber);
        requestData.put("guestPhone", guestPhone);
        requestData.put("hasVehicle", hasVehicle);
        requestData.put("guestType", GuestIdentityPolicy.guestTypeFor(hasVehicle));
        requestData.put("vehiclePlate", vehiclePlate);
        requestData.put("plateNumber", vehiclePlate);
        requestData.put("hostName", sponsorName);
        requestData.put("gateLabel", normalizedGate);
        requestData.put("iconType", "guest");
        requestData.put("expiresAt", expiresAt);
        requestData.put("requesterUid", sponsorUid);
        requestData.put("requesterRole", sponsorRole);
        requestData.put("studentId", sponsorStudentId.trim());
        requestData.put("sponsorStudentId", sponsorStudentId.trim());
        requestData.put("phoneValidation", phoneValidation.toFirestoreMap());
        requestData.put("createdAt", FieldValue.serverTimestamp());
        requestData.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> data = new HashMap<>();
        data.put("sponsorUid", sponsorUid);
        data.put("sponsorRole", sponsorRole);
        data.put("sponsorName", sponsorName);
        data.put("sponsorEmail", sponsorEmail);
        data.put("studentId", sponsorStudentId.trim());
        data.put("sponsorStudentId", sponsorStudentId.trim());
        data.put("guestName", guestName);
        data.put("guestIdNumber", guestIdNumber);
        data.put("guestPhone", guestPhone);
        data.put("hasVehicle", hasVehicle);
        data.put("guestType", GuestIdentityPolicy.guestTypeFor(hasVehicle));
        data.put("vehiclePlate", vehiclePlate);
        data.put("passCode", passCode);
        data.put("entryRequestId", requestRef.getId());
        data.put("gateLabel", normalizedGate);
        data.put("status", STATUS_ACTIVE);
        data.put("expiresAt", expiresAt);
        data.put("admittedAt", null);
        data.put("admittedByUid", "");
        data.put("admissionMethod", "");
        data.put("phoneValidation", phoneValidation.toFirestoreMap());
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(requestRef, requestData);
        batch.set(passRef, data);
        addGuestPassNotificationToBatch(
                batch,
                sponsorUid,
                sponsorRole,
                "guest_pass_created",
                passRef.getId(),
                "Guest Pass Created",
                "Pass " + passCode + " created for " + guestName + "."
        );
        batch.commit()
                .addOnSuccessListener(unused -> {
                    GuestPass issuedPass = new GuestPass(
                            passRef.getId(),
                            sponsorUid,
                            sponsorRole,
                            sponsorName,
                            sponsorEmail,
                            sponsorStudentId,
                            guestName,
                            guestIdNumber,
                            guestPhone,
                            hasVehicle,
                            vehiclePlate,
                            GuestIdentityPolicy.guestTypeFor(hasVehicle),
                            passCode,
                            requestRef.getId(),
                            normalizedGate,
                            STATUS_ACTIVE,
                            expiresAt,
                            null,
                            "",
                            "",
                            null
                    );
                    Map<String, Object> requestMeta = new HashMap<>();
                    requestMeta.put("guestName", guestName);
                    requestMeta.put("guestIdNumber", guestIdNumber);
                    requestMeta.put("guestPhone", guestPhone);
                    requestMeta.put("hasVehicle", hasVehicle);
                    requestMeta.put("vehiclePlate", vehiclePlate);
                    appendAccessEvent(
                            AuditEventType.REQUEST_CREATED,
                            "entry_request",
                            requestRef.getId(),
                            requestRef.getId(),
                            sponsorUid,
                            sponsorRole,
                            "Entry request created for guest pass issuance",
                            "pass_issue",
                            "success",
                            "",
                            normalizedGate,
                            requestMeta
                    );
                    Map<String, Object> passMeta = new HashMap<>();
                    passMeta.put("passCode", passCode);
                    passMeta.put("guestName", guestName);
                    passMeta.put("guestIdNumber", guestIdNumber);
                    passMeta.put("guestPhone", guestPhone);
                    passMeta.put("hasVehicle", hasVehicle);
                    passMeta.put("vehiclePlate", vehiclePlate);
                    appendAccessEvent(
                            AuditEventType.PASS_ISSUED,
                            "guest_pass",
                            passRef.getId(),
                            requestRef.getId(),
                            sponsorUid,
                            sponsorRole,
                            "Guest pass issued",
                            "pass_issue",
                            "success",
                            "",
                            normalizedGate,
                            passMeta
                    );
                    callback.onComplete(true, "Guest pass issued", issuedPass, null);
                })
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
                            // if (GuestPassStatusRules.isTimeExpiredActive(rawPass)
                            //         || GuestPassTimePolicy.isActivePassOutOfPolicy(rawPass)) {
                            //     passesToExpire.add(rawPass);
                            // }
                            String status = rawPass.getStatus().trim().toLowerCase();
                            if ((STATUS_CANCELLED.equals(status) || STATUS_EXPIRED.equals(status))
                                    && !rawPass.getEntryRequestId().trim().isEmpty()) {
                                requestIdsToCleanup.add(rawPass.getEntryRequestId().trim());
                            }
                            passes.add(rawPass);
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
                    return pass;
                })
                .addOnSuccessListener(pass -> {
                    if (pass != null) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("passCode", pass.getPassCode());
                        metadata.put("guestName", pass.getGuestName());
                        appendAccessEvent(
                                AuditEventType.PASS_CANCELLED,
                                "guest_pass",
                                pass.getId(),
                                pass.getEntryRequestId(),
                                currentActorUid(pass.getSponsorUid()),
                                currentActorRole(pass.getSponsorRole()),
                                "Guest pass cancelled",
                                "sponsor_action",
                                "success",
                                "pass_cancelled",
                                pass.getGateLabel(),
                                metadata
                        );
                        writeGuestPassNotification(
                                pass.getSponsorUid(),
                                pass.getSponsorRole(),
                                "guest_pass_cancelled",
                                pass.getId(),
                                "Guest Pass Cancelled",
                                "Pass " + pass.getPassCode() + " for " + pass.getGuestName() + " was cancelled."
                        );
                    }
                    callback.onComplete(true, "Guest pass cancelled", null);
                })
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
                    // boolean shouldExpire = GuestPassStatusRules.isTimeExpiredActive(rawPass)
                    //         || GuestPassTimePolicy.isActivePassOutOfPolicy(rawPass);
                    // if (shouldExpire) {
                    //     persistExpiredStatus(rawPass);
                    // }
                    listener.onData(rawPass);
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void findPassById(@NonNull String passId, @NonNull PassLookupListener listener) {
        String normalizedId = passId.trim();
        if (normalizedId.isEmpty()) {
            listener.onData(null);
            return;
        }
        collection.document(normalizedId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        listener.onData(null);
                        return;
                    }
                    listener.onData(GuestPass.fromMap(snapshot.getId(), snapshot.getData()));
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
                    // if (GuestPassStatusRules.isTimeExpiredActive(pass)
                    //         || GuestPassTimePolicy.isActivePassOutOfPolicy(pass)) {
                    //     DocumentReference requestToDelete = findLinkedUnaccessedEntryRequestForDelete(
                    //             transaction,
                    //             pass.getEntryRequestId()
                    //     );
                    //     Map<String, Object> expiredUpdates = new HashMap<>();
                    //     expiredUpdates.put("status", STATUS_EXPIRED);
                    //     expiredUpdates.put("updatedAt", FieldValue.serverTimestamp());
                    //     transaction.update(passRef, expiredUpdates);
                    //     if (requestToDelete != null) {
                    //         transaction.delete(requestToDelete);
                    //     }
                    //     return new PassMutationResult(STATUS_EXPIRED, pass);
                    // }
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
                    return new PassMutationResult(STATUS_USED, pass);
                })
                .addOnSuccessListener(result -> {
                    if (result == null || result.pass == null) {
                        callback.onComplete(false, "Pass update failed.", null);
                        return;
                    }
                    if (STATUS_EXPIRED.equalsIgnoreCase(result.status)) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("passCode", result.pass.getPassCode());
                        appendAccessEvent(
                                AuditEventType.PASS_EXPIRED,
                                "guest_pass",
                                result.pass.getId(),
                                result.pass.getEntryRequestId(),
                                admittedByUid,
                                "guard",
                                "Guest pass expired before admission",
                                "guard_decision",
                                "failure",
                                "pass_expired",
                                result.pass.getGateLabel(),
                                metadata
                        );
                        callback.onComplete(false, "Pass has expired.", null);
                        return;
                    }
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("passCode", result.pass.getPassCode());
                    metadata.put("admissionMethod", normalizedMethod);
                    appendAccessEvent(
                            AuditEventType.PASS_USED,
                            "guest_pass",
                            result.pass.getId(),
                            result.pass.getEntryRequestId(),
                            admittedByUid,
                            "guard",
                            "Guest pass consumed for admission",
                            "guard_decision",
                            "success",
                            "pass_admitted",
                            result.pass.getGateLabel(),
                            metadata
                    );
                    writeGuestPassNotification(
                            result.pass.getSponsorUid(),
                            result.pass.getSponsorRole(),
                            "guest_pass_used",
                            result.pass.getId(),
                            "Visitor Admitted",
                            result.pass.getGuestName() + " was admitted using pass " + result.pass.getPassCode() + "."
                    );
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
    public void markPassReportedByEntryRequestId(
            @NonNull String entryRequestId,
            @NonNull String reportedByUid,
            @NonNull OperationCallback callback
    ) {
        collection.whereEqualTo("entryRequestId", entryRequestId.trim())
                .whereIn("status", Arrays.asList(STATUS_ACTIVE, STATUS_USED, STATUS_OVERDUE))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(true, "No linked in-progress guest pass.", null);
                        return;
                    }
                    String passId = snapshot.getDocuments().get(0).getId();
                    markPassReported(passId, reportedByUid, callback);
                })
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    @Override
    public void markPassExitedByEntryRequestId(@NonNull String entryRequestId, @NonNull OperationCallback callback) {
        collection.whereEqualTo("entryRequestId", entryRequestId.trim())
                .whereIn("status", Arrays.asList(STATUS_USED, STATUS_OVERDUE, STATUS_REPORTED))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(true, "No active pass to mark exited.", null);
                        return;
                    }
                    GuestPass pass = GuestPass.fromMap(
                            snapshot.getDocuments().get(0).getId(),
                            snapshot.getDocuments().get(0).getData()
                    );
                    String passId = pass.getId();
                    collection.document(passId)
                            .update("status", STATUS_EXITED, "updatedAt", FieldValue.serverTimestamp())
                            .addOnSuccessListener(unused -> {
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("passCode", pass.getPassCode());
                                appendAccessEvent(
                                        AuditEventType.PASS_EXITED,
                                        "guest_pass",
                                        pass.getId(),
                                        pass.getEntryRequestId(),
                                        currentActorUid("system"),
                                        currentActorRole("system"),
                                        "Guest pass marked as exited",
                                        "guard_details",
                                        "success",
                                        "pass_exited",
                                        pass.getGateLabel(),
                                        metadata
                                );
                                writeGuestPassNotification(
                                        pass.getSponsorUid(),
                                        pass.getSponsorRole(),
                                        "guest_pass_exited",
                                        pass.getId(),
                                        "Visitor Exited",
                                        pass.getGuestName() + " has exited. Pass " + pass.getPassCode() + " is now closed."
                                );
                                callback.onComplete(true, "Pass marked as exited", null);
                            })
                            .addOnFailureListener(error -> callback.onComplete(false, "Failed to update pass to exited", error));
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to find pass for exit", error));
    }

    private void reconcileExitedPasses(@NonNull List<GuestPass> passes) {
        List<GuestPass> candidates = new ArrayList<>();
        for (GuestPass pass : passes) {
            String status = pass.getStatus().trim().toLowerCase();
            if (("used".equals(status) || "overdue".equals(status) || STATUS_REPORTED.equals(status))
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
                    return pass;
                })
                .addOnSuccessListener(pass -> {
                    if (pass != null) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("passCode", pass.getPassCode());
                        appendAccessEvent(
                                AuditEventType.PASS_DENIED,
                                "guest_pass",
                                pass.getId(),
                                pass.getEntryRequestId(),
                                deniedByUid,
                                "guard",
                                "Guest pass denied at gate",
                                "guard_decision",
                                "failure",
                                "pass_denied",
                                pass.getGateLabel(),
                                metadata
                        );
                        writeGuestPassNotification(
                                pass.getSponsorUid(),
                                pass.getSponsorRole(),
                                "guest_pass_denied",
                                pass.getId(),
                                "Visitor Denied",
                                pass.getGuestName() + " was denied at the gate."
                        );
                    }
                    callback.onComplete(true, "Pass denied", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    private void markPassReported(
            @NonNull String passId,
            @NonNull String reportedByUid,
            @NonNull OperationCallback callback
    ) {
        DocumentReference passRef = collection.document(passId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(passRef);
                    if (!snapshot.exists()) {
                        throw new IllegalStateException("Pass not found.");
                    }
                    GuestPass pass = GuestPass.fromMap(snapshot.getId(), snapshot.getData());
                    String status = pass.getStatus().trim().toLowerCase();
                    if (STATUS_REPORTED.equals(status)) {
                        return pass;
                    }
                    if (!(STATUS_ACTIVE.equals(status) || STATUS_USED.equals(status) || STATUS_OVERDUE.equals(status))) {
                        throw new IllegalStateException("Only active/admitted/overdue passes can be reported.");
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", STATUS_REPORTED);
                    updates.put("admittedByUid", reportedByUid);
                    updates.put("admissionMethod", "REPORTED");
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.update(passRef, updates);
                    return pass;
                })
                .addOnSuccessListener(pass -> {
                    if (pass != null) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("passCode", pass.getPassCode());
                        appendAccessEvent(
                                AuditEventType.PASS_REPORTED,
                                "guest_pass",
                                pass.getId(),
                                pass.getEntryRequestId(),
                                reportedByUid,
                                "guard",
                                "Guest pass moved to reported state",
                                "guard_violation",
                                "failure",
                                "pass_reported",
                                pass.getGateLabel(),
                                metadata
                        );
                        writeGuestPassNotification(
                                pass.getSponsorUid(),
                                pass.getSponsorRole(),
                                "guest_pass_reported",
                                pass.getId(),
                                "Visitor Reported",
                                pass.getGuestName() + " was moved to reported status."
                        );
                    }
                    callback.onComplete(true, "Pass reported", null);
                })
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
                pass.getSponsorStudentId(),
                pass.getGuestName(),
                pass.getGuestIdNumber(),
                pass.getGuestPhone(),
                pass.hasVehicle(),
                pass.getVehiclePlate(),
                pass.getGuestType(),
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
        batch.commit().addOnSuccessListener(unused -> {
            for (GuestPass pass : passesToExpire) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("passCode", pass.getPassCode());
                appendAccessEvent(
                        AuditEventType.PASS_EXPIRED,
                        "guest_pass",
                        pass.getId(),
                        pass.getEntryRequestId(),
                        "system",
                        "system",
                        "Guest pass expired by policy normalization",
                        "status_normalizer",
                        "failure",
                        "pass_expired",
                        pass.getGateLabel(),
                        metadata
                );
                writeGuestPassNotification(
                        pass.getSponsorUid(),
                        pass.getSponsorRole(),
                        "guest_pass_expired",
                        pass.getId(),
                        "Guest Pass Expired",
                        "Pass " + pass.getPassCode() + " for " + pass.getGuestName() + " expired."
                );
            }
            deleteUnaccessedEntryRequests(new ArrayList<>(linkedRequestIds));
        });
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
                    return latest;
                })
                .addOnSuccessListener(latest -> {
                    if (latest == null) {
                        return;
                    }
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("passCode", latest.getPassCode());
                    appendAccessEvent(
                            AuditEventType.PASS_EXPIRED,
                            "guest_pass",
                            latest.getId(),
                            latest.getEntryRequestId(),
                            "system",
                            "system",
                            "Guest pass expired by lookup normalization",
                            "status_normalizer",
                            "failure",
                            "pass_expired",
                            latest.getGateLabel(),
                            metadata
                    );
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
        Map<String, Object> payload = AuditEventPayloadFactory.build(
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
        firestore.collection(COLLECTION_ACCESS_EVENTS).add(payload);
    }

    private void addGuestPassNotificationToBatch(
            @NonNull WriteBatch batch,
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String type,
            @NonNull String passId,
            @NonNull String title,
            @NonNull String message
    ) {
        if (!UserNotificationWriter.supportsRole(sponsorRole)) {
            return;
        }
        notificationWriter.addToBatch(batch, sponsorUid, type, passId, title, message, COLLECTION_GUEST_PASSES);
    }

    private void writeGuestPassNotification(
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String type,
            @NonNull String passId,
            @NonNull String title,
            @NonNull String message
    ) {
        if (!UserNotificationWriter.supportsRole(sponsorRole)) {
            return;
        }
        notificationWriter.write(sponsorUid, type, passId, title, message, COLLECTION_GUEST_PASSES);
    }

    @NonNull
    private String currentActorUid(@NonNull String fallbackUid) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) {
            return fallbackUid;
        }
        String uid = profile.getUid().trim();
        return uid.isEmpty() ? fallbackUid : uid;
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

    private static final class PassMutationResult {
        private final String status;
        private final GuestPass pass;

        private PassMutationResult(@NonNull String status, @NonNull GuestPass pass) {
            this.status = status;
            this.pass = pass;
        }
    }

    private interface InGateAvailabilityCallback {
        void onBlocked(@NonNull String message);

        void onAvailable();
    }
}
