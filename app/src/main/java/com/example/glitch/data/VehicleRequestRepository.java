package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.VehicleRequestRecord;

import java.util.List;

/**
 * Contract for staff vehicle request submissions, status tracking, and admin review.
 * Pattern: Repository abstraction for create/list/review vehicle requests.
 */
public interface VehicleRequestRepository {

    void submitVehicleRequest(
            @NonNull String requesterUid,
            @NonNull String plateNumber,
            @NonNull String vehicleMake,
            @NonNull String vehicleModel,
            @NonNull String vehicleColor,
            @NonNull OperationCallback callback
    );

    void listenVehicleRequests(@NonNull String requesterUid, @NonNull RequestListListener listener);

    void listenAllVehicleRequests(@NonNull RequestListListener listener);

    void updateVehicleRequest(
            @NonNull String requestId,
            @NonNull String plateNumber,
            @NonNull String vehicleMake,
            @NonNull String vehicleModel,
            @NonNull String vehicleColor,
            @NonNull OperationCallback callback
    );

    void reviewVehicleRequest(
            @NonNull String requestId,
            @NonNull String reviewerUid,
            boolean approved,
            @NonNull String reviewNote,
            @NonNull OperationCallback callback
    );

    void removeListeners();

    interface RequestListListener {
        void onData(@NonNull List<VehicleRequestRecord> requests);

        void onError(@NonNull Exception exception);
    }

    interface OperationCallback {
        void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
    }
}
