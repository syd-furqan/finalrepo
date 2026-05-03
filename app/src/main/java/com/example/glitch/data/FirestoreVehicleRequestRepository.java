package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.VehicleRequestRecord;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Firestore implementation of vehicle request repository.
 * Pattern: Collection adapter with realtime updates; transactions guard concurrent review actions.
 */
public class FirestoreVehicleRequestRepository implements VehicleRequestRepository {
    private static final String COLLECTION_VEHICLE_REQUESTS = "vehicle_requests";
    private static final String COLLECTION_CREDENTIALS = "credentials";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String SUBCOLLECTION_NOTIFICATION_ITEMS = "items";
    private static final String COLLECTION_ACCESS_EVENTS = "access_events";

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_DENIED = "denied";

    private static final String REQUESTER_UID_FIELD = "requesterUid";
    private static final String PLATE_NUMBER_FIELD = "plateNumber";
    private static final String PLATE_KEY_FIELD = "plateKey";
    private static final String VEHICLE_MAKE_FIELD = "vehicleMake";
    private static final String VEHICLE_MODEL_FIELD = "vehicleModel";
    private static final String VEHICLE_COLOR_FIELD = "vehicleColor";
    private static final String STATUS_FIELD = "status";
    private static final String CREATED_AT_FIELD = "createdAt";
    private static final String UPDATED_AT_FIELD = "updatedAt";
    private static final String REVIEWED_AT_FIELD = "reviewedAt";
    private static final String REVIEWER_UID_FIELD = "reviewerUid";
    private static final String REVIEW_NOTE_FIELD = "reviewNote";

    private final FirebaseFirestore firestore;
    private final CollectionReference collection;
    private ListenerRegistration registration;

    public FirestoreVehicleRequestRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreVehicleRequestRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.collection = firestore.collection(COLLECTION_VEHICLE_REQUESTS);
    }

    @Override
    public void submitVehicleRequest(
            @NonNull String requesterUid,
            @NonNull String plateNumber,
            @NonNull String vehicleMake,
            @NonNull String vehicleModel,
            @NonNull String vehicleColor,
            @NonNull OperationCallback callback
    ) {
        String uid = requesterUid.trim();
        String plate = normalizePlate(plateNumber);
        String make = vehicleMake.trim();
        String model = vehicleModel.trim();
        String color = vehicleColor.trim();
        if (uid.isEmpty() || plate.isEmpty() || model.isEmpty()) {
            callback.onComplete(false, "Plate number and vehicle model are required", null);
            return;
        }
        collection.whereEqualTo(REQUESTER_UID_FIELD, uid)
                .whereEqualTo(STATUS_FIELD, STATUS_PENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        VehicleRequestRecord existing = VehicleRequestRecord.fromMap(doc.getId(), doc.getData());
                        if (plate.equals(existing.getPlateKey())) {
                            callback.onComplete(false, "A pending request already exists for this plate", null);
                            return;
                        }
                    }
                    addVehicleRequest(uid, plate, make, model, color, callback);
                })
                .addOnFailureListener(e -> callback.onComplete(false, "Failed to check existing requests", e));
    }

    private void addVehicleRequest(
            @NonNull String requesterUid,
            @NonNull String plateKey,
            @NonNull String vehicleMake,
            @NonNull String vehicleModel,
            @NonNull String vehicleColor,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put(REQUESTER_UID_FIELD, requesterUid);
        data.put(PLATE_NUMBER_FIELD, plateKey);
        data.put(PLATE_KEY_FIELD, plateKey);
        data.put(VEHICLE_MAKE_FIELD, vehicleMake);
        data.put(VEHICLE_MODEL_FIELD, vehicleModel);
        data.put(VEHICLE_COLOR_FIELD, vehicleColor);
        data.put(STATUS_FIELD, STATUS_PENDING);
        data.put(CREATED_AT_FIELD, FieldValue.serverTimestamp());
        data.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
        collection.add(data)
                .addOnSuccessListener(ref -> {
                    appendVehicleEvent("VEHICLE_REQUEST_CREATED", ref.getId(), requesterUid, "Vehicle request submitted");
                    callback.onComplete(true, "Vehicle request submitted", null);
                })
                .addOnFailureListener(e -> callback.onComplete(false, "Failed to submit request", e));
    }

    @Override
    public void listenVehicleRequests(@NonNull String requesterUid, @NonNull RequestListListener listener) {
        removeListeners();
        registration = collection
                .whereEqualTo(REQUESTER_UID_FIELD, requesterUid)
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
    }

    @Override
    public void listenAllVehicleRequests(@NonNull RequestListListener listener) {
        removeListeners();
        registration = collection.addSnapshotListener((snapshot, error) -> {
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
    }

    @Override
    public void updateVehicleRequest(
            @NonNull String requestId,
            @NonNull String plateNumber,
            @NonNull String vehicleMake,
            @NonNull String vehicleModel,
            @NonNull String vehicleColor,
            @NonNull OperationCallback callback
    ) {
        String plate = normalizePlate(plateNumber);
        String make = vehicleMake.trim();
        String model = vehicleModel.trim();
        String color = vehicleColor.trim();
        if (requestId.trim().isEmpty() || plate.isEmpty() || model.isEmpty()) {
            callback.onComplete(false, "Plate number and vehicle model are required", null);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put(PLATE_NUMBER_FIELD, plate);
        updates.put(PLATE_KEY_FIELD, plate);
        updates.put(VEHICLE_MAKE_FIELD, make);
        updates.put(VEHICLE_MODEL_FIELD, model);
        updates.put(VEHICLE_COLOR_FIELD, color);
        updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());

        firestore.runTransaction(tx -> {
                    DocumentSnapshot snap = tx.get(collection.document(requestId));
                    if (!snap.exists()) {
                        throw new IllegalStateException("Vehicle request was not found");
                    }
                    VehicleRequestRecord record = VehicleRequestRecord.fromMap(snap.getId(), snap.getData());
                    if (!record.isPending()) {
                        throw new IllegalStateException("Only pending vehicle requests can be updated");
                    }
                    tx.update(collection.document(requestId), updates);
                    return null;
                })
                .addOnSuccessListener(unused -> callback.onComplete(true, "Vehicle request updated", null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage() != null ? e.getMessage() : "Failed to update request", e));
    }

    @Override
    public void reviewVehicleRequest(
            @NonNull String requestId,
            @NonNull String reviewerUid,
            boolean approved,
            @NonNull String reviewNote,
            @NonNull OperationCallback callback
    ) {
        String reviewer = reviewerUid.trim();
        String nextStatus = approved ? STATUS_APPROVED : STATUS_DENIED;
        firestore.runTransaction(tx -> {
                    DocumentSnapshot snap = tx.get(collection.document(requestId));
                    if (!snap.exists()) {
                        throw new IllegalStateException("Vehicle request was not found");
                    }
                    VehicleRequestRecord record = VehicleRequestRecord.fromMap(snap.getId(), snap.getData());
                    if (!record.isPending()) {
                        throw new IllegalStateException("Vehicle request is already reviewed");
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(STATUS_FIELD, nextStatus);
                    updates.put(REVIEWER_UID_FIELD, reviewer);
                    updates.put(REVIEW_NOTE_FIELD, reviewNote.trim());
                    updates.put(REVIEWED_AT_FIELD, FieldValue.serverTimestamp());
                    updates.put(UPDATED_AT_FIELD, FieldValue.serverTimestamp());
                    tx.update(collection.document(requestId), updates);
                    return record;
                })
                .addOnSuccessListener(record -> {
                    appendVehicleEvent(
                            approved ? "VEHICLE_REQUEST_APPROVED" : "VEHICLE_REQUEST_DENIED",
                            requestId,
                            reviewer,
                            "Vehicle request " + nextStatus
                    );
                    if (approved) {
                        upsertVehicleCredential(requestId, record);
                    }
                    notifyRequester(requestId, record.getRequesterUid(), approved, reviewNote);
                    callback.onComplete(true, approved ? "Vehicle request approved" : "Vehicle request denied", null);
                })
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage() != null ? e.getMessage() : "Failed to review vehicle request", e));
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void upsertVehicleCredential(@NonNull String requestId, @NonNull VehicleRequestRecord record) {
        String plateKey = record.getPlateKey();
        if (plateKey.isEmpty()) {
            return;
        }
        Map<String, Object> credential = new HashMap<>();
        credential.put("identifier", plateKey);
        credential.put("isValid", true);
        credential.put("holderName", record.getVehicleDescription().isEmpty() ? plateKey : record.getVehicleDescription());
        credential.put("credentialType", "vehicle");
        credential.put("requestId", requestId);
        credential.put("requesterUid", record.getRequesterUid());
        credential.put("updatedAt", FieldValue.serverTimestamp());
        String docId = "vehicle_" + plateKey.replaceAll("[^A-Z0-9]", "_");
        firestore.collection(COLLECTION_CREDENTIALS).document(docId).set(credential);
    }

    private void notifyRequester(
            @NonNull String requestId,
            @NonNull String requesterUid,
            boolean approved,
            @NonNull String reviewNote
    ) {
        if (requesterUid.trim().isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", approved ? "Vehicle request approved" : "Vehicle request denied");
        payload.put("message", buildReviewMessage(approved, reviewNote));
        payload.put("type", approved ? "vehicle_approval" : "vehicle_denial");
        payload.put("isRead", false);
        payload.put("requestId", requestId);
        payload.put("createdAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(requesterUid)
                .collection(SUBCOLLECTION_NOTIFICATION_ITEMS)
                .add(payload);
    }

    @NonNull
    private String buildReviewMessage(boolean approved, @NonNull String reviewNote) {
        String note = reviewNote.trim();
        if (approved) {
            return note.isEmpty()
                    ? "Your vehicle registration has been approved."
                    : "Your vehicle registration has been approved. Note: " + note;
        }
        return note.isEmpty()
                ? "Your vehicle registration has been denied."
                : "Your vehicle registration has been denied. Reason: " + note;
    }

    private void appendVehicleEvent(
            @NonNull String eventType,
            @NonNull String requestId,
            @NonNull String actorUid,
            @NonNull String description
    ) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("requestId", requestId);
        event.put("actorUid", actorUid);
        event.put("actorRole", "vehicle-reviewer");
        event.put("description", description);
        event.put("createdAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_ACCESS_EVENTS).add(event);
    }

    @NonNull
    private String normalizePlate(@NonNull String plateNumber) {
        return plateNumber.trim().toUpperCase(Locale.getDefault()).replaceAll("\\s+", "");
    }
}
