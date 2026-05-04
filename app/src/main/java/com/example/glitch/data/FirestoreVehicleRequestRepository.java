package com.example.glitch.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.RegisteredVehicleRecord;
import com.example.glitch.model.VehicleDocumentInput;
import com.example.glitch.model.VehicleDocumentRef;
import com.example.glitch.model.VehicleRemovalDraft;
import com.example.glitch.model.VehicleRequestRecord;
import com.example.glitch.model.VehicleRegistrationDraft;
import com.example.glitch.model.VehicleStickerPolicy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Firestore + Storage implementation for sponsor vehicle-program workflows.
 */
public class FirestoreVehicleRequestRepository implements VehicleRequestRepository {
    private static final String COLLECTION_VEHICLE_REQUESTS = "vehicle_requests";
    private static final String COLLECTION_REGISTERED_VEHICLES = "registered_vehicles";
    private static final String COLLECTION_ACCESS_EVENTS = "access_events";

    private static final String STATUS_SUBMITTED = VehicleRequestRecord.STATUS_SUBMITTED;
    private static final String STATUS_RECEIVED = VehicleRequestRecord.STATUS_RECEIVED;
    private static final String STATUS_APPROVED = VehicleRequestRecord.STATUS_APPROVED;
    private static final String STATUS_DENIED = VehicleRequestRecord.STATUS_DENIED;
    private static final String STATUS_CANCELLED = VehicleRequestRecord.STATUS_CANCELLED;
    private static final int MAX_UPLOAD_BYTES = 12 * 1024 * 1024;
    private static final int STORAGE_ERROR_OBJECT_NOT_FOUND = -13010;
    private static final int STORAGE_ERROR_BUCKET_NOT_FOUND = -13011;
    private static final int STORAGE_ERROR_PROJECT_NOT_FOUND = -13012;

    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final CollectionReference requestCollection;
    private final CollectionReference registeredVehicleCollection;
    private final ContentResolver contentResolver;
    private final List<ListenerRegistration> registrations = new ArrayList<>();

    public FirestoreVehicleRequestRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance(), FirebaseApp.getInstance().getApplicationContext());
    }

    FirestoreVehicleRequestRepository(
            @NonNull FirebaseFirestore firestore,
            @NonNull FirebaseStorage storage,
            @NonNull Context appContext
    ) {
        this.firestore = firestore;
        this.storage = storage;
        this.requestCollection = firestore.collection(COLLECTION_VEHICLE_REQUESTS);
        this.registeredVehicleCollection = firestore.collection(COLLECTION_REGISTERED_VEHICLES);
        this.contentResolver = appContext.getContentResolver();
    }

    @Override
    public void submitRegistrationApplication(
            @NonNull VehicleRegistrationDraft draft,
            @NonNull OperationCallback callback
    ) {
        submitRegistrationApplication(draft, callback, null);
    }

    @Override
    public void submitRegistrationApplication(
            @NonNull VehicleRegistrationDraft draft,
            @NonNull OperationCallback callback,
            @Nullable UploadProgressListener progressListener
    ) {
        String requesterUid = draft.getRequesterUid().trim();
        String requesterRole = draft.getRequesterRole().trim().toLowerCase(Locale.getDefault());
        String studentCategory = VehicleStickerPolicy.normalizeStudentCategory(draft.getStudentCategory());
        String stickerType = VehicleStickerPolicy.resolveStickerType(requesterRole, studentCategory);
        String plate = GuestIdentityPolicy.normalizeVehiclePlate(draft.getPlateNumber());
        String make = draft.getVehicleMake().trim();
        String model = draft.getVehicleModel().trim();
        String variant = draft.getVehicleVariant().trim();

        if (requesterUid.isEmpty()) {
            callback.onComplete(false, "Unable to identify user session.", null);
            return;
        }
        if (!("student".equals(requesterRole) || "faculty".equals(requesterRole))) {
            callback.onComplete(false, "Only student/faculty can submit vehicle applications.", null);
            return;
        }
        if (plate == null || make.isEmpty() || model.isEmpty() || variant.isEmpty()) {
            callback.onComplete(false, "Plate, make, model, and variant are required.", null);
            return;
        }
        if (stickerType.isEmpty()) {
            callback.onComplete(false, "Student category is required for sticker eligibility.", null);
            return;
        }
        if (!isAllowedType(draft.getApplicantCnicDoc())
                || !isAllowedType(draft.getRegistrationDoc())
                || (!draft.isOwner() && (draft.getOwnerCnicDoc() == null || !isAllowedType(draft.getOwnerCnicDoc())))) {
            callback.onComplete(false, "Documents must be PDF, PNG, or JPEG.", null);
            return;
        }

        ensureCanSubmitNewApplication(requesterUid, requesterRole, true, canSubmitError -> {
            if (canSubmitError != null) {
                callback.onComplete(false, canSubmitError, null);
                return;
            }

            DocumentReference requestRef = requestCollection.document();
            String requestId = requestRef.getId();
            UploadAggregateTracker tracker = progressListener == null
                    ? null
                    : new UploadAggregateTracker(progressListener);

            Task<VehicleDocumentRef> applicantTask = uploadDocument(requestId, "applicant_cnic", draft.getApplicantCnicDoc(), tracker);
            Task<VehicleDocumentRef> registrationTask = uploadDocument(requestId, "registration", draft.getRegistrationDoc(), tracker);
            Task<VehicleDocumentRef> ownerTask;
            if (draft.isOwner()) {
                ownerTask = Tasks.forResult(new VehicleDocumentRef("", "", "", ""));
            } else {
                ownerTask = uploadDocument(requestId, "owner_cnic", draft.getOwnerCnicDoc(), tracker);
            }

            Tasks.whenAllSuccess(applicantTask, registrationTask, ownerTask)
                    .addOnSuccessListener(results -> {
                        if (tracker != null) {
                            tracker.markComplete();
                        }
                        VehicleDocumentRef applicantDoc = (VehicleDocumentRef) results.get(0);
                        VehicleDocumentRef registrationDoc = (VehicleDocumentRef) results.get(1);
                        VehicleDocumentRef ownerDoc = (VehicleDocumentRef) results.get(2);

                        Map<String, Object> payload = new HashMap<>();
                        payload.put("requestKind", VehicleRequestRecord.KIND_REGISTER);
                        payload.put("status", STATUS_SUBMITTED);
                        payload.put("requesterUid", requesterUid);
                        payload.put("requesterRole", requesterRole);
                        payload.put("studentCategory", studentCategory);
                        payload.put("stickerType", stickerType);
                        payload.put("plateNumber", plate);
                        payload.put("plateKey", plate);
                        payload.put("vehicleMake", make);
                        payload.put("vehicleModel", model);
                        payload.put("vehicleVariant", variant);
                        payload.put("vehicleColor", "");
                        payload.put("isOwner", draft.isOwner());
                        payload.put("linkedVehicleId", "");
                        payload.put("removalReason", "");
                        payload.put("reviewerUid", "");
                        payload.put("receivedByUid", "");
                        payload.put("reviewNote", "");
                        payload.put("receivedAt", null);
                        payload.put("reviewedAt", null);
                        payload.put("applicantCnicDoc", applicantDoc.toMap());
                        payload.put("registrationDoc", registrationDoc.toMap());
                        payload.put("ownerCnicDoc", ownerDoc.toMap());
                        payload.put("evidenceDocs", new ArrayList<>());
                        payload.put("createdAt", FieldValue.serverTimestamp());
                        payload.put("updatedAt", FieldValue.serverTimestamp());

                        requestRef.set(payload)
                                .addOnSuccessListener(unused -> {
                                    appendVehicleEvent(
                                            "VEHICLE_APPLICATION_SUBMITTED",
                                            requestId,
                                            requesterUid,
                                            requesterRole,
                                            "Vehicle registration application submitted"
                                    );
                                    callback.onComplete(true, "Vehicle registration application submitted", null);
                                })
                                .addOnFailureListener(error -> callback.onComplete(false, "Failed to submit application", error));
                    })
                    .addOnFailureListener(error -> callback.onComplete(false, "Failed to upload required documents: " + safeMessage(asException(error)), asException(error)));
        });
    }

    @Override
    public void submitRemovalApplication(@NonNull VehicleRemovalDraft draft, @NonNull OperationCallback callback) {
        String requesterUid = draft.getRequesterUid().trim();
        String requesterRole = draft.getRequesterRole().trim().toLowerCase(Locale.getDefault());
        String studentCategory = VehicleStickerPolicy.normalizeStudentCategory(draft.getStudentCategory());
        String linkedVehicleId = draft.getLinkedVehicleId().trim();
        String reason = draft.getRemovalReason().trim();

        if (requesterUid.isEmpty() || linkedVehicleId.isEmpty()) {
            callback.onComplete(false, "Vehicle removal request is missing required identifiers.", null);
            return;
        }
        if (reason.isEmpty()) {
            callback.onComplete(false, "Removal reason is required.", null);
            return;
        }
        for (VehicleDocumentInput input : draft.getEvidenceDocs()) {
            if (!isAllowedType(input)) {
                callback.onComplete(false, "Evidence documents must be PDF, PNG, or JPEG.", null);
                return;
            }
        }

        ensureCanSubmitNewApplication(requesterUid, requesterRole, false, canSubmitError -> {
            if (canSubmitError != null) {
                callback.onComplete(false, canSubmitError, null);
                return;
            }

            registeredVehicleCollection.document(linkedVehicleId)
                    .get()
                    .addOnSuccessListener(vehicleDoc -> {
                        if (!vehicleDoc.exists()) {
                            callback.onComplete(false, "Registered vehicle not found.", null);
                            return;
                        }
                        RegisteredVehicleRecord vehicle = RegisteredVehicleRecord.fromMap(vehicleDoc.getId(), vehicleDoc.getData());
                        if (!requesterUid.equals(vehicle.getRequesterUid())) {
                            callback.onComplete(false, "You can only request removal for your own registered vehicle.", null);
                            return;
                        }
                        if (!"active".equalsIgnoreCase(vehicle.getStatus())) {
                            callback.onComplete(false, "Only active registered vehicles can be removed.", null);
                            return;
                        }

                        DocumentReference requestRef = requestCollection.document();
                        String requestId = requestRef.getId();

                        List<Task<VehicleDocumentRef>> uploadTasks = new ArrayList<>();
                        List<VehicleDocumentInput> evidenceInputs = draft.getEvidenceDocs();
                        for (int i = 0; i < evidenceInputs.size(); i++) {
                            uploadTasks.add(uploadDocument(requestId, "evidence_" + i, evidenceInputs.get(i), null));
                        }

                        Task<List<VehicleDocumentRef>> evidenceTask;
                        if (uploadTasks.isEmpty()) {
                            evidenceTask = Tasks.forResult(new ArrayList<>());
                        } else {
                            evidenceTask = Tasks.whenAllSuccess(uploadTasks)
                                    .continueWith(task -> {
                                        List<VehicleDocumentRef> docs = new ArrayList<>();
                                        for (Object item : task.getResult()) {
                                            docs.add((VehicleDocumentRef) item);
                                        }
                                        return docs;
                                    });
                        }

                        evidenceTask
                                .addOnSuccessListener(evidenceDocs -> {
                                    Map<String, Object> payload = new HashMap<>();
                                    payload.put("requestKind", VehicleRequestRecord.KIND_REMOVE);
                                    payload.put("status", STATUS_SUBMITTED);
                                    payload.put("requesterUid", requesterUid);
                                    payload.put("requesterRole", requesterRole);
                                    payload.put("studentCategory", studentCategory);
                                    payload.put("stickerType", vehicle.getStickerType());
                                    payload.put("plateNumber", vehicle.getPlateNumber());
                                    payload.put("plateKey", vehicle.getPlateNumber());
                                    payload.put("vehicleMake", vehicle.getMake());
                                    payload.put("vehicleModel", vehicle.getModel());
                                    payload.put("vehicleVariant", vehicle.getVariant());
                                    payload.put("vehicleColor", "");
                                    payload.put("isOwner", vehicle.isOwner());
                                    payload.put("linkedVehicleId", linkedVehicleId);
                                    payload.put("removalReason", reason);
                                    payload.put("reviewerUid", "");
                                    payload.put("receivedByUid", "");
                                    payload.put("reviewNote", "");
                                    payload.put("receivedAt", null);
                                    payload.put("reviewedAt", null);
                                    payload.put("applicantCnicDoc", new VehicleDocumentRef("", "", "", "").toMap());
                                    payload.put("registrationDoc", new VehicleDocumentRef("", "", "", "").toMap());
                                    payload.put("ownerCnicDoc", new VehicleDocumentRef("", "", "", "").toMap());

                                    List<Map<String, Object>> evidenceMaps = new ArrayList<>();
                                    for (VehicleDocumentRef doc : evidenceDocs) {
                                        evidenceMaps.add(doc.toMap());
                                    }
                                    payload.put("evidenceDocs", evidenceMaps);
                                    payload.put("createdAt", FieldValue.serverTimestamp());
                                    payload.put("updatedAt", FieldValue.serverTimestamp());

                                    requestRef.set(payload)
                                            .addOnSuccessListener(unused -> {
                                                appendVehicleEvent(
                                                        "VEHICLE_REMOVAL_SUBMITTED",
                                                        requestId,
                                                        requesterUid,
                                                        requesterRole,
                                                        "Vehicle removal application submitted"
                                                );
                                                callback.onComplete(true, "Vehicle removal application submitted", null);
                                            })
                                            .addOnFailureListener(error -> callback.onComplete(false, "Failed to submit removal application", error));
                                })
                                .addOnFailureListener(error -> callback.onComplete(false, "Failed to upload evidence documents: " + safeMessage(asException(error)), asException(error)));
                    })
                    .addOnFailureListener(error -> callback.onComplete(false, "Failed to load registered vehicle", error));
        });
    }

    @Override
    public void listenVehicleRequests(@NonNull String requesterUid, @NonNull RequestListListener listener) {
        ListenerRegistration registration = requestCollection
                .whereEqualTo("requesterUid", requesterUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<VehicleRequestRecord> result = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            result.add(VehicleRequestRecord.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    result.sort(Comparator.comparing(
                            VehicleRequestRecord::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());
                    listener.onData(result);
                });
        registrations.add(registration);
    }

    @Override
    public void listenOpenVehicleRequest(@NonNull String requesterUid, @NonNull SingleRequestListener listener) {
        ListenerRegistration registration = requestCollection
                .whereEqualTo("requesterUid", requesterUid)
                .whereIn("status", Arrays.asList(STATUS_SUBMITTED, STATUS_RECEIVED, "pending"))
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    VehicleRequestRecord best = null;
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            VehicleRequestRecord current = VehicleRequestRecord.fromMap(doc.getId(), doc.getData());
                            if (best == null) {
                                best = current;
                                continue;
                            }
                            if (compareTimestamp(current.getCreatedAt(), best.getCreatedAt()) > 0) {
                                best = current;
                            }
                        }
                    }
                    listener.onData(best);
                });
        registrations.add(registration);
    }

    @Override
    public void listenAllVehicleRequests(@NonNull RequestListListener listener) {
        ListenerRegistration registration = requestCollection
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<VehicleRequestRecord> result = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            result.add(VehicleRequestRecord.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    result.sort(Comparator.comparing(
                            VehicleRequestRecord::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());
                    listener.onData(result);
                });
        registrations.add(registration);
    }

    @Override
    public void listenRegisteredVehicles(@NonNull String requesterUid, @NonNull RegisteredVehicleListListener listener) {
        ListenerRegistration registration = registeredVehicleCollection
                .whereEqualTo("requesterUid", requesterUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<RegisteredVehicleRecord> result = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            result.add(RegisteredVehicleRecord.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    result.sort(Comparator.comparing(
                            RegisteredVehicleRecord::getApprovedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());
                    listener.onData(result);
                });
        registrations.add(registration);
    }

    @Override
    public void cancelVehicleRequest(@NonNull String requestId, @NonNull OperationCallback callback) {
        DocumentReference ref = requestCollection.document(requestId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(ref);
                    if (!snap.exists()) {
                        throw new IllegalStateException("Vehicle request was not found");
                    }
                    VehicleRequestRecord record = VehicleRequestRecord.fromMap(snap.getId(), snap.getData());
                    if (!record.canCancelByApplicant()) {
                        throw new IllegalStateException("Only submitted applications can be cancelled");
                    }
                    transaction.update(ref,
                            "status", STATUS_CANCELLED,
                            "updatedAt", FieldValue.serverTimestamp());
                    return record;
                })
                .addOnSuccessListener(record -> {
                    appendVehicleEvent(
                            "VEHICLE_APPLICATION_CANCELLED",
                            requestId,
                            record == null ? "" : record.getRequesterUid(),
                            record == null ? "" : record.getRequesterRole(),
                            "Vehicle application cancelled"
                    );
                    callback.onComplete(true, "Vehicle application cancelled", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    @Override
    public void markVehicleRequestReceived(
            @NonNull String requestId,
            @NonNull String reviewerUid,
            @NonNull OperationCallback callback
    ) {
        DocumentReference ref = requestCollection.document(requestId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(ref);
                    if (!snap.exists()) {
                        throw new IllegalStateException("Vehicle request was not found");
                    }
                    VehicleRequestRecord record = VehicleRequestRecord.fromMap(snap.getId(), snap.getData());
                    if (!record.isSubmitted()) {
                        throw new IllegalStateException("Only submitted applications can be marked as received");
                    }
                    transaction.update(ref,
                            "status", STATUS_RECEIVED,
                            "receivedByUid", reviewerUid.trim(),
                            "receivedAt", FieldValue.serverTimestamp(),
                            "updatedAt", FieldValue.serverTimestamp());
                    return record;
                })
                .addOnSuccessListener(record -> {
                    appendVehicleEvent(
                            "VEHICLE_APPLICATION_RECEIVED",
                            requestId,
                            reviewerUid,
                            "admin",
                            "Vehicle application marked as received"
                    );
                    callback.onComplete(true, "Application marked as received", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    @Override
    public void reviewVehicleRequest(
            @NonNull String requestId,
            @NonNull String reviewerUid,
            boolean approved,
            @NonNull String reviewNote,
            @NonNull OperationCallback callback
    ) {
        String nextStatus = approved ? STATUS_APPROVED : STATUS_DENIED;
        DocumentReference requestRef = requestCollection.document(requestId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(requestRef);
                    if (!snap.exists()) {
                        throw new IllegalStateException("Vehicle request was not found");
                    }
                    VehicleRequestRecord record = VehicleRequestRecord.fromMap(snap.getId(), snap.getData());
                    if (!(record.isSubmitted() || record.isReceived())) {
                        throw new IllegalStateException("Vehicle request is already finalized");
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", nextStatus);
                    updates.put("reviewerUid", reviewerUid.trim());
                    updates.put("reviewNote", reviewNote.trim());
                    updates.put("reviewedAt", FieldValue.serverTimestamp());
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    if (record.isSubmitted()) {
                        updates.put("receivedByUid", reviewerUid.trim());
                        updates.put("receivedAt", FieldValue.serverTimestamp());
                    }

                    if (approved) {
                        if (record.isRegisterRequest()) {
                            DocumentReference vehicleRef = registeredVehicleCollection.document();
                            Map<String, Object> vehiclePayload = new HashMap<>();
                            vehiclePayload.put("requesterUid", record.getRequesterUid());
                            vehiclePayload.put("requesterRole", record.getRequesterRole());
                            vehiclePayload.put("studentCategory", record.getStudentCategory());
                            vehiclePayload.put("stickerType", record.getStickerType());
                            vehiclePayload.put("plateNumber", record.getPlateNumber());
                            vehiclePayload.put("vehicleMake", record.getVehicleMake());
                            vehiclePayload.put("vehicleModel", record.getVehicleModel());
                            vehiclePayload.put("vehicleVariant", record.getVehicleVariant());
                            vehiclePayload.put("isOwner", record.isOwner());
                            vehiclePayload.put("status", "active");
                            vehiclePayload.put("sourceRequestId", requestId);
                            vehiclePayload.put("approvedAt", FieldValue.serverTimestamp());
                            vehiclePayload.put("removedAt", null);
                            vehiclePayload.put("updatedAt", FieldValue.serverTimestamp());
                            transaction.set(vehicleRef, vehiclePayload);
                            updates.put("linkedVehicleId", vehicleRef.getId());
                        } else {
                            String linkedVehicleId = record.getLinkedVehicleId().trim();
                            if (linkedVehicleId.isEmpty()) {
                                throw new IllegalStateException("Removal request is missing linked vehicle id");
                            }
                            DocumentReference vehicleRef = registeredVehicleCollection.document(linkedVehicleId);
                            DocumentSnapshot vehicleSnap = transaction.get(vehicleRef);
                            if (!vehicleSnap.exists()) {
                                throw new IllegalStateException("Linked vehicle not found");
                            }
                            Map<String, Object> vehicleUpdates = new HashMap<>();
                            vehicleUpdates.put("status", "removed");
                            vehicleUpdates.put("removedAt", FieldValue.serverTimestamp());
                            vehicleUpdates.put("updatedAt", FieldValue.serverTimestamp());
                            transaction.set(vehicleRef, vehicleUpdates, SetOptions.merge());
                        }
                    }

                    transaction.update(requestRef, updates);
                    return record;
                })
                .addOnSuccessListener(record -> {
                    String eventType;
                    String message;
                    if (approved && record != null && record.isRemovalRequest()) {
                        eventType = "VEHICLE_REMOVAL_APPROVED";
                        message = "Vehicle removal approved";
                    } else if (approved) {
                        eventType = "VEHICLE_APPLICATION_APPROVED";
                        message = "Vehicle application approved";
                    } else {
                        eventType = "VEHICLE_APPLICATION_DENIED";
                        message = "Vehicle application denied";
                    }
                    appendVehicleEvent(
                            eventType,
                            requestId,
                            reviewerUid,
                            "admin",
                            message
                    );
                    callback.onComplete(true, message, null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, safeMessage(error), error));
    }

    @Override
    public void removeListeners() {
        for (ListenerRegistration registration : registrations) {
            registration.remove();
        }
        registrations.clear();
    }

    private void ensureCanSubmitNewApplication(
            @NonNull String requesterUid,
            @NonNull String requesterRole,
            boolean enforceStudentVehicleCap,
            @NonNull CanSubmitCallback callback
    ) {
        requestCollection
                .whereEqualTo("requesterUid", requesterUid)
                .whereIn("status", Arrays.asList(STATUS_SUBMITTED, STATUS_RECEIVED, "pending"))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        callback.onResult("You already have an open vehicle application.");
                        return;
                    }
                    if (!"student".equals(requesterRole) || !enforceStudentVehicleCap) {
                        callback.onResult(null);
                        return;
                    }
                    registeredVehicleCollection
                            .whereEqualTo("requesterUid", requesterUid)
                            .whereEqualTo("status", "active")
                            .get()
                            .addOnSuccessListener(vehicleSnap -> {
                                if (vehicleSnap.size() >= 2) {
                                    callback.onResult("Students can have at most two active registered vehicles.");
                                    return;
                                }
                                callback.onResult(null);
                            })
                            .addOnFailureListener(error -> callback.onResult("Failed to validate active vehicle limit."));
                })
                .addOnFailureListener(error -> callback.onResult("Failed to validate existing applications."));
    }

    private boolean isAllowedType(@NonNull VehicleDocumentInput input) {
        String mime = safeLower(contentResolver.getType(input.getContentUri()));
        if ("application/pdf".equals(mime)
                || "image/png".equals(mime)
                || "image/jpeg".equals(mime)
                || "image/jpg".equals(mime)) {
            return true;
        }
        String name = input.getDisplayName().trim().toLowerCase(Locale.getDefault());
        return name.endsWith(".pdf") || name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".jpg");
    }

    @NonNull
    private Task<VehicleDocumentRef> uploadDocument(
            @NonNull String requestId,
            @NonNull String key,
            @NonNull VehicleDocumentInput input,
            @Nullable UploadAggregateTracker tracker
    ) {
        String mimeType = contentResolver.getType(input.getContentUri());
        String safeName = sanitizeName(input.getDisplayName());
        String ext = extensionFor(mimeType, safeName);
        String storagePath = "vehicle_requests/" + requestId + "/" + key + "." + ext;

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(mimeType == null ? defaultMimeForExtension(ext) : mimeType)
                .build();

        Uri uri = input.getContentUri();
        byte[] payload;
        try {
            payload = readBytesFromUri(uri);
        } catch (Exception e) {
            return Tasks.forException(e);
        }
        if (tracker != null) {
            tracker.onFileSizeKnown(key, payload.length);
        }
        List<String> bucketCandidates = resolveBucketCandidates();
        return uploadDocumentAcrossBuckets(
                bucketCandidates,
                0,
                storagePath,
                payload,
                metadata,
                safeName,
                mimeType == null ? defaultMimeForExtension(ext) : mimeType,
                key,
                tracker,
                null
        );
    }

    @NonNull
    private Task<VehicleDocumentRef> uploadDocumentAcrossBuckets(
            @NonNull List<String> bucketCandidates,
            int index,
            @NonNull String storagePath,
            @NonNull byte[] payload,
            @NonNull StorageMetadata metadata,
            @NonNull String safeName,
            @NonNull String resolvedMimeType,
            @NonNull String fileKey,
            @Nullable UploadAggregateTracker tracker,
            @Nullable Exception previousError
    ) {
        if (index >= bucketCandidates.size()) {
            String detail = "Upload failed at path '" + storagePath + "' using buckets " + bucketCandidates;
            if (previousError == null) {
                return Tasks.forException(new IllegalStateException(detail + ". No storage bucket candidate succeeded."));
            }
            return Tasks.forException(new IllegalStateException(detail + ". Last error: " + safeMessage(previousError), previousError));
        }

        String bucket = bucketCandidates.get(index);
        StorageReference bucketReference = buildStorageReference(bucket, storagePath);
        UploadTask uploadTask = bucketReference.putBytes(payload, metadata);
        if (tracker != null) {
            uploadTask.addOnProgressListener(snapshot ->
                    tracker.onFileProgress(fileKey, snapshot.getBytesTransferred(), snapshot.getTotalByteCount()));
        }
        return uploadTask.continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        if (tracker != null) {
                            tracker.onFileProgress(fileKey, payload.length, payload.length);
                        }
                        return Tasks.forResult(new VehicleDocumentRef(
                                safeName,
                                resolvedMimeType,
                                storagePath,
                                "",
                                bucket
                        ));
                    }

                    Exception error = task.getException();
                    if (error == null) {
                        error = new IllegalStateException("Upload failed");
                    }
                    if (shouldTryPutStreamFallback(error)) {
                        return tryPutStreamThenMaybeNextBucket(
                                bucketCandidates,
                                index,
                                storagePath,
                                payload,
                                metadata,
                                safeName,
                                resolvedMimeType,
                                fileKey,
                                tracker,
                                error
                        );
                    }
                    if (shouldTryNextBucket(error, index, bucketCandidates)) {
                        return uploadDocumentAcrossBuckets(
                                bucketCandidates,
                                index + 1,
                                storagePath,
                                payload,
                                metadata,
                                safeName,
                                resolvedMimeType,
                                fileKey,
                                tracker,
                                error
                        );
                    }
                    return Tasks.forException(error);
                });
    }

    @NonNull
    private Task<VehicleDocumentRef> tryPutStreamThenMaybeNextBucket(
            @NonNull List<String> bucketCandidates,
            int index,
            @NonNull String storagePath,
            @NonNull byte[] payload,
            @NonNull StorageMetadata metadata,
            @NonNull String safeName,
            @NonNull String resolvedMimeType,
            @NonNull String fileKey,
            @Nullable UploadAggregateTracker tracker,
            @NonNull Exception previousError
    ) {
        String bucket = bucketCandidates.get(index);
        StorageReference bucketReference = buildStorageReference(bucket, storagePath);
        InputStream stream = new java.io.ByteArrayInputStream(payload);
        UploadTask uploadTask = bucketReference.putStream(stream, metadata);
        if (tracker != null) {
            uploadTask.addOnProgressListener(snapshot ->
                    tracker.onFileProgress(fileKey, snapshot.getBytesTransferred(), snapshot.getTotalByteCount()));
        }
        return uploadTask.continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        if (tracker != null) {
                            tracker.onFileProgress(fileKey, payload.length, payload.length);
                        }
                        return Tasks.forResult(new VehicleDocumentRef(
                                safeName,
                                resolvedMimeType,
                                storagePath,
                                "",
                                bucket
                        ));
                    }
                    Exception streamError = task.getException();
                    if (streamError == null) {
                        streamError = previousError;
                    }
                    if (shouldTryNextBucket(streamError, index, bucketCandidates)) {
                        return uploadDocumentAcrossBuckets(
                                bucketCandidates,
                                index + 1,
                                storagePath,
                                payload,
                                metadata,
                                safeName,
                                resolvedMimeType,
                                fileKey,
                                tracker,
                                streamError
                        );
                    }
                    return Tasks.forException(streamError);
                });
    }

    @NonNull
    private StorageReference buildStorageReference(@NonNull String bucket, @NonNull String storagePath) {
        if (bucket.trim().isEmpty()) {
            return storage.getReference().child(storagePath);
        }
        return FirebaseStorage.getInstance("gs://" + bucket).getReference().child(storagePath);
    }

    @NonNull
    private List<String> resolveBucketCandidates() {
        LinkedHashSet<String> buckets = new LinkedHashSet<>();
        addBucketCandidate(buckets, storage.getReference().getBucket());
        addBucketCandidate(buckets, FirebaseApp.getInstance().getOptions().getStorageBucket());

        List<String> existing = new ArrayList<>(buckets);
        for (String bucket : existing) {
            addBucketCandidate(buckets, alternateBucketFor(bucket));
        }

        if (buckets.isEmpty()) {
            buckets.add("");
        }
        return new ArrayList<>(buckets);
    }

    private void addBucketCandidate(
            @NonNull LinkedHashSet<String> buckets,
            @Nullable String candidate
    ) {
        String normalized = normalizeBucket(candidate);
        if (!normalized.isEmpty()) {
            buckets.add(normalized);
        }
    }

    @NonNull
    private String normalizeBucket(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.startsWith("gs://")) {
            value = value.substring(5);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    @NonNull
    private String alternateBucketFor(@NonNull String bucket) {
        if (bucket.endsWith(".firebasestorage.app")) {
            return bucket.replace(".firebasestorage.app", ".appspot.com");
        }
        if (bucket.endsWith(".appspot.com")) {
            return bucket.replace(".appspot.com", ".firebasestorage.app");
        }
        return "";
    }

    private boolean shouldTryNextBucket(
            @NonNull Exception error,
            int currentIndex,
            @NonNull List<String> bucketCandidates
    ) {
        if (currentIndex >= bucketCandidates.size() - 1) {
            return false;
        }
        int errorCode = storageErrorCode(error);
        return errorCode == STORAGE_ERROR_OBJECT_NOT_FOUND
                || errorCode == STORAGE_ERROR_BUCKET_NOT_FOUND
                || errorCode == STORAGE_ERROR_PROJECT_NOT_FOUND;
    }

    private boolean shouldTryPutStreamFallback(@NonNull Exception error) {
        int errorCode = storageErrorCode(error);
        return errorCode == STORAGE_ERROR_OBJECT_NOT_FOUND;
    }

    private int storageErrorCode(@NonNull Exception error) {
        if (error instanceof StorageException) {
            return ((StorageException) error).getErrorCode();
        }
        return Integer.MIN_VALUE;
    }

    @NonNull
    private byte[] readBytesFromUri(@NonNull Uri uri) throws Exception {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                throw new IllegalStateException("Unable to read selected document");
            }
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                if (outputStream.size() > MAX_UPLOAD_BYTES) {
                    throw new IllegalStateException("Selected file is too large. Maximum supported size is 12 MB.");
                }
            }
            return outputStream.toByteArray();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @NonNull
    private String extensionFor(@Nullable String mimeType, @NonNull String fileName) {
        String lowerMime = safeLower(mimeType);
        if ("application/pdf".equals(lowerMime)) {
            return "pdf";
        }
        if ("image/png".equals(lowerMime)) {
            return "png";
        }
        if ("image/jpeg".equals(lowerMime) || "image/jpg".equals(lowerMime)) {
            return "jpg";
        }
        String lower = fileName.toLowerCase(Locale.getDefault());
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".png")) return "png";
        if (lower.endsWith(".jpeg") || lower.endsWith(".jpg")) return "jpg";
        return "bin";
    }

    @NonNull
    private String defaultMimeForExtension(@NonNull String ext) {
        if ("pdf".equals(ext)) {
            return "application/pdf";
        }
        if ("png".equals(ext)) {
            return "image/png";
        }
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    @NonNull
    private String sanitizeName(@NonNull String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "document";
        }
        return trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void appendVehicleEvent(
            @NonNull String eventType,
            @NonNull String requestId,
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String description
    ) {
        Map<String, Object> event = AuditEventPayloadFactory.build(
                eventType,
                actorUid,
                actorRole,
                "vehicle_request",
                requestId,
                requestId,
                description,
                "vehicle_program",
                "success",
                "",
                "in-gate",
                new HashMap<>()
        );
        firestore.collection(COLLECTION_ACCESS_EVENTS).add(event);
    }

    private int compareTimestamp(@Nullable com.google.firebase.Timestamp a, @Nullable com.google.firebase.Timestamp b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    @NonNull
    private String safeLower(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.getDefault());
    }

    @NonNull
    private String safeMessage(@NonNull Exception exception) {
        if (exception instanceof StorageException) {
            StorageException se = (StorageException) exception;
            String storageMessage = se.getMessage();
            int errorCode = se.getErrorCode();
            int httpCode = se.getHttpResultCode();
            StringBuilder builder = new StringBuilder();
            builder.append("Storage error (")
                    .append(errorCode)
                    .append(", HTTP ")
                    .append(httpCode)
                    .append(")");
            String codeHint = storageCodeHint(errorCode);
            if (!codeHint.isEmpty()) {
                builder.append(" [").append(codeHint).append("]");
            }
            if (storageMessage != null && !storageMessage.trim().isEmpty()) {
                builder.append(": ").append(storageMessage.trim());
            }
            return builder.toString();
        }
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "Operation failed." : message;
    }

    @NonNull
    private String storageCodeHint(int code) {
        if (code == STORAGE_ERROR_OBJECT_NOT_FOUND) {
            return "OBJECT_NOT_FOUND";
        }
        if (code == STORAGE_ERROR_BUCKET_NOT_FOUND) {
            return "BUCKET_NOT_FOUND";
        }
        if (code == STORAGE_ERROR_PROJECT_NOT_FOUND) {
            return "PROJECT_NOT_FOUND";
        }
        if (code == StorageException.ERROR_NOT_AUTHORIZED) {
            return "NOT_AUTHORIZED (check Storage Rules)";
        }
        return "";
    }

    @NonNull
    private Exception asException(@NonNull Exception exception) {
        return exception;
    }

    @NonNull
    private Exception asException(@NonNull Throwable throwable) {
        if (throwable instanceof Exception) {
            return (Exception) throwable;
        }
        return new Exception(throwable);
    }

    private interface CanSubmitCallback {
        void onResult(@Nullable String errorMessage);
    }

    private static final class UploadAggregateTracker {
        private final UploadProgressListener listener;
        private final Map<String, Long> totalBytesByFile = new HashMap<>();
        private final Map<String, Long> transferredBytesByFile = new HashMap<>();
        private int lastPercent = -1;

        UploadAggregateTracker(@NonNull UploadProgressListener listener) {
            this.listener = listener;
        }

        synchronized void onFileSizeKnown(@NonNull String fileKey, long totalBytes) {
            if (totalBytes > 0) {
                totalBytesByFile.put(fileKey, totalBytes);
                emitProgressLocked();
            }
        }

        synchronized void onFileProgress(@NonNull String fileKey, long transferred, long snapshotTotal) {
            long knownTotal = totalBytesByFile.getOrDefault(fileKey, 0L);
            if (snapshotTotal > 0 && snapshotTotal > knownTotal) {
                knownTotal = snapshotTotal;
                totalBytesByFile.put(fileKey, knownTotal);
            }
            if (knownTotal <= 0) {
                knownTotal = transferred;
                totalBytesByFile.put(fileKey, knownTotal);
            }
            long clampedTransferred = Math.max(0L, Math.min(transferred, knownTotal));
            long previousTransferred = transferredBytesByFile.getOrDefault(fileKey, 0L);
            if (clampedTransferred < previousTransferred) {
                clampedTransferred = previousTransferred;
            }
            transferredBytesByFile.put(fileKey, clampedTransferred);
            emitProgressLocked();
        }

        synchronized void markComplete() {
            for (Map.Entry<String, Long> entry : totalBytesByFile.entrySet()) {
                transferredBytesByFile.put(entry.getKey(), entry.getValue());
            }
            emitProgressLocked();
        }

        private void emitProgressLocked() {
            long total = 0L;
            long transferred = 0L;
            for (Map.Entry<String, Long> entry : totalBytesByFile.entrySet()) {
                long fileTotal = Math.max(0L, entry.getValue());
                long fileTransferred = Math.max(0L, transferredBytesByFile.getOrDefault(entry.getKey(), 0L));
                total += fileTotal;
                transferred += Math.min(fileTransferred, fileTotal);
            }
            if (total <= 0L) {
                return;
            }
            int percent = (int) Math.min(100L, (transferred * 100L) / total);
            if (percent == lastPercent) {
                return;
            }
            lastPercent = percent;
            listener.onProgress(percent, transferred, total);
        }
    }
}
