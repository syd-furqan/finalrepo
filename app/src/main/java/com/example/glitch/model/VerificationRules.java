package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Represents admin-configurable credential verification policy rules.
 * Pattern: Immutable policy model consumed by guard verification workflow.
 * Known issue: banned list uses comma-separated string for easier editing in v1.
 */
public class VerificationRules {
	private final boolean enforceIdExpiry;
	private final int alertThreshold;
	private final String bannedIdentifiersCsv;

	/**
	 * Creates immutable verification policy settings.
	 */
	public VerificationRules(
			boolean enforceIdExpiry,
			int alertThreshold,
			@NonNull String bannedIdentifiersCsv
	) {
		this.enforceIdExpiry = enforceIdExpiry;
		this.alertThreshold = alertThreshold;
		this.bannedIdentifiersCsv = bannedIdentifiersCsv;
	}

	/**
	 * Maps Firestore rules document into a {@link VerificationRules} object.
	 *
	 * @param map Firestore field map.
	 * @return parsed rules object or sensible defaults when map is null.
	 */
	@NonNull
	public static VerificationRules fromMap(@Nullable Map<String, Object> map) {
		if (map == null) {
			return defaultRules();
		}
		return new VerificationRules(
				asBoolean(map.get("enforceIdExpiry")),
				asInt(map.get("alertThreshold"), 3),
				asString(map.get("bannedIdentifiersCsv"))
		);
	}

	/**
	 * @return application default verification rules for first run / missing config.
	 */
	@NonNull
	public static VerificationRules defaultRules() {
		return new VerificationRules(true, 3, "");
	}

	/**
	 * @return true when credential expiry validation must be enforced.
	 */
	public boolean isEnforceIdExpiry() {
		return enforceIdExpiry;
	}

	/**
	 * @return failed-verification threshold used for alert generation.
	 */
	public int getAlertThreshold() {
		return alertThreshold;
	}

	/**
	 * @return comma-separated banned identifier list.
	 */
	@NonNull
	public String getBannedIdentifiersCsv() {
		return bannedIdentifiersCsv;
	}

	private static boolean asBoolean(@Nullable Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return false;
	}

	private static int asInt(@Nullable Object value, int fallback) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return fallback;
	}

	@NonNull
	private static String asString(@Nullable Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}