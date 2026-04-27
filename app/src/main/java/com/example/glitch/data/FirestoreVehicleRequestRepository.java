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
import java.util.Map;

/**
 * Firestore implementation of vehicle request repository.
 * Pattern: Collection adapter with realtime updates for user-specific requests.
 * Known issue: submission validates only basic non-empty plate and model values.
 */
public class FirestoreVehicleRequestRepository implements VehicleRequestRepository {
    private static final String COLLECTION_VEHICLE_REQUESTS = "vehicle_requests";
    private final CollectionReference collection;
    private ListenerRegistration registration;

    public FirestoreVehicleRequestRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreVehicleRequestRepository(@NonNull FirebaseFirestore firestore) {
        this.collection = firestore.collection(COLLECTION_VEHICLE_REQUESTS);
    }

    @Override
    public void submitVehicleRequest(
            @NonNull String requesterUid,
            @NonNull String plateNumber,
            @NonNull String vehicleModel,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("requesterUid", requesterUid);
        data.put("plateNumber", plateNumber.trim().toUpperCase());
        data.put("vehicleModel", vehicleModel.trim());
        data.put("status", "pending");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());
        collection.add(data)
                .addOnSuccessListener(reference -> callback.onComplete(true, "Vehicle request submitted", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to submit request", error));
    }

    @Override
    public void listenVehicleRequests(@NonNull String requesterUid, @NonNull RequestListListener listener) {
        removeListeners();
        registration = collection
                .whereEqualTo("requesterUid", requesterUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<VehicleRequestRecord> result = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            result.add(VehicleRequestRecord.fromMap(document.getId(), document.getData()));
                        }
                    }
                    // Keep list recency order client-side to avoid composite index dependency.
                    result.sort(Comparator.comparing(
                            VehicleRequestRecord::getCreatedAt,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                    ).reversed());
                    listener.onData(result);
                });
    }

    @Override
    public void updateVehicleRequest(
            @NonNull String requestId,
            @NonNull String plateNumber,
            @NonNull String vehicleModel,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("plateNumber", plateNumber.trim().toUpperCase());
        updates.put("vehicleModel", vehicleModel.trim());
        updates.put("updatedAt", FieldValue.serverTimestamp());
        collection.document(requestId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onComplete(true, "Vehicle request updated", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to update request", error));
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
