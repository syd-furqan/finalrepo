package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.VehicleRequestRecord;

import java.util.List;

/**
 * Contract for staff vehicle request submissions and status tracking.
 * Pattern: Repository abstraction for create/list vehicle requests.
 * Known issue: admin review action is currently managed in separate user-management workflows.
 */
public interface VehicleRequestRepository {

	void submitVehicleRequest(
			@NonNull String requesterUid,
			@NonNull String plateNumber,
			@NonNull String vehicleModel,
			@NonNull OperationCallback callback
	);

	void listenVehicleRequests(@NonNull String requesterUid, @NonNull RequestListListener listener);

	void updateVehicleRequest(
			@NonNull String requestId,
			@NonNull String plateNumber,
			@NonNull String vehicleModel,
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