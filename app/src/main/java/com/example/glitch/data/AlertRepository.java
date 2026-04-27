package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.SecurityAlert;

import java.util.List;

/**
 * Contract for admin monitoring of automated security alerts.
 * Pattern: Read-focused repository for alerts collection.
 * Known issue: alert acknowledgment workflow is not implemented in v1.
 */
public interface AlertRepository {
	void listenAlerts(@NonNull AlertListener listener);

	void removeListeners();

	interface AlertListener {
		void onData(@NonNull List<SecurityAlert> alerts);

		void onError(@NonNull Exception exception);
	}
}