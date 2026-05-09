package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.SecurityAlert;

import java.util.List;
import java.util.Map;

/**
 * Contract for admin monitoring and review of important alerts.
 */
public interface AlertRepository {
	void listenAlerts(@NonNull AlertListener listener);

	void createAlert(
			@Nullable String alertId,
			@NonNull Map<String, Object> payload,
			@NonNull OperationCallback callback
	);

	void updateAlertStatus(
			@NonNull String alertId,
			@NonNull String status,
			@NonNull String interventionSummary,
			@NonNull String reviewedByUid,
			@NonNull OperationCallback callback
	);

	void updateLinkedAlertStatus(
			@NonNull String linkedField,
			@NonNull String linkedId,
			@NonNull String status,
			@NonNull String interventionSummary,
			@NonNull String reviewedByUid,
			@NonNull OperationCallback callback
	);

	void removeListeners();

	interface AlertListener {
		void onData(@NonNull List<SecurityAlert> alerts);

		void onError(@NonNull Exception exception);
	}

	interface OperationCallback {
		void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
	}
}
