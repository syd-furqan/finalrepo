package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GuestPass;

import java.util.List;

/**
 * Contract for creating, listing and cancelling guest passes.
 * Pattern: Repository interface for student and faculty pass lifecycle flows.
 * Known issue: v1 uses one flat guest_passes collection without archival split.
 */
public interface GuestPassRepository {

	void createGuestPass(
			@NonNull String sponsorUid,
			@NonNull String sponsorRole,
			@NonNull String sponsorName,
			@NonNull String sponsorEmail,
			@NonNull String guestName,
			@NonNull String guestIdNumber,
			int expiryHours,
			@NonNull OperationCallback callback
	);

	void listenGuestPasses(@NonNull String sponsorUid, @NonNull PassListListener listener);

	void cancelGuestPass(@NonNull String passId, @NonNull OperationCallback callback);

	void findPassByCode(@NonNull String passCode, @NonNull PassLookupListener listener);

	void removeListeners();

	interface PassListListener {
		void onData(@NonNull List<GuestPass> passes);

		void onError(@NonNull Exception exception);
	}

	interface PassLookupListener {
		void onData(@Nullable GuestPass pass);

		void onError(@NonNull Exception exception);
	}

	interface OperationCallback {
		void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
	}
}