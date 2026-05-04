package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.UserProfile;

import java.util.List;

/**
 * Contract for security-admin user lifecycle operations.
 * Pattern: Repository interface supporting realtime list and update actions.
 * Known issue: password management remains in Firebase Console for v1.
 */
public interface UserManagementRepository {

	void listenUsers(@NonNull UserListListener listener);

	void setUserActive(@NonNull String uid, boolean isActive, @NonNull OperationCallback callback);

	void upsertUser(
			@NonNull String uid,
			@NonNull String email,
			@NonNull String role,
			@NonNull String displayName,
			@NonNull String studentCategory,
			boolean isActive,
			@NonNull OperationCallback callback
	);

	void removeListeners();

	interface UserListListener {
		void onData(@NonNull List<UserProfile> users);

		void onError(@NonNull Exception exception);
	}

	interface OperationCallback {
		void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
	}
}
