package com.example.glitch.model;

import androidx.annotation.NonNull;

/**
 * Verification result for credential lookup requests.
 * Pattern: Simple immutable value object used by guard verification flow.
 * Known issue: result carries message text directly for rapid UI feedback in v1.
 */
public class CredentialVerificationResult {
	private final boolean valid;
	private final String identifier;
	private final String holderName;
	private final String message;

	/**
	 * Creates credential verification result.
	 */
	public CredentialVerificationResult(
			boolean valid,
			@NonNull String identifier,
			@NonNull String holderName,
			@NonNull String message
	) {
		this.valid = valid;
		this.identifier = identifier;
		this.holderName = holderName;
		this.message = message;
	}

	/**
	 * @return true when the credential passed policy and lookup checks.
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * @return scanned/entered identifier value.
	 */
	@NonNull
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return resolved holder name from credential registry, if found.
	 */
	@NonNull
	public String getHolderName() {
		return holderName;
	}

	/**
	 * @return user-facing verification message for UI feedback.
	 */
	@NonNull
	public String getMessage() {
		return message;
	}
}