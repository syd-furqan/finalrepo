package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.VerificationRules;

/**
 * Contract for loading and updating global credential verification policy.
 * Pattern: Single-document repository for verification_rules/current.
 * Known issue: no versioned policy history in v1.
 */
public interface VerificationRulesRepository {
	void listenRules(@NonNull RulesListener listener);

	void saveRules(@NonNull VerificationRules rules, @NonNull SaveCallback callback);

	void removeListeners();

	interface RulesListener {
		void onData(@NonNull VerificationRules rules);

		void onError(@NonNull Exception exception);
	}

	interface SaveCallback {
		void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
	}
}