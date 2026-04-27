package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Represents the dashboard-level state ribbon and protocol information.
 * Pattern: Immutable POJO used for presentation mapping.
 * Known issue: only one document is currently supported (`dashboard_state`).
 */
public class DashboardState {
	private final String systemStatusTitle;
	private final String systemStatusMessage;
	private final String protocolLevel;
	private final String protocolDescription;
	private final Timestamp updatedAt;

	/**
	 * Creates an immutable dashboard state model.
	 */
	public DashboardState(
			@NonNull String systemStatusTitle,
			@NonNull String systemStatusMessage,
			@NonNull String protocolLevel,
			@NonNull String protocolDescription,
			@Nullable Timestamp updatedAt
	) {
		this.systemStatusTitle = systemStatusTitle;
		this.systemStatusMessage = systemStatusMessage;
		this.protocolLevel = protocolLevel;
		this.protocolDescription = protocolDescription;
		this.updatedAt = updatedAt;
	}

	/**
	 * Converts Firestore map data into a dashboard state object.
	 */
	@NonNull
	public static DashboardState fromMap(@Nullable Map<String, Object> map) {
		if (map == null) {
			return defaultState();
		}

		return new DashboardState(
				asStringOrDefault(map.get("systemStatusTitle"), "System Status: Active"),
				asStringOrDefault(map.get("systemStatusMessage"), "All gates reporting normal activity"),
				asStringOrDefault(map.get("protocolLevel"), "Level 1 - Open"),
				asStringOrDefault(map.get("protocolDescription"), "Standard visitor verification required."),
				asTimestamp(map.get("updatedAt"))
		);
	}

	/**
	 * Returns a UI-safe fallback state for first launch or missing docs.
	 */
	@NonNull
	public static DashboardState defaultState() {
		return new DashboardState(
				"System Status: Active",
				"All gates reporting normal activity",
				"Level 1 - Open",
				"Standard visitor verification required. All student/staff IDs must be swiped.",
				null
		);
	}

	/**
	 * Returns system status heading text.
	 */
	@NonNull
	public String getSystemStatusTitle() {
		return systemStatusTitle;
	}

	/**
	 * Returns supporting status message text.
	 */
	@NonNull
	public String getSystemStatusMessage() {
		return systemStatusMessage;
	}

	/**
	 * Returns protocol level heading.
	 */
	@NonNull
	public String getProtocolLevel() {
		return protocolLevel;
	}

	/**
	 * Returns protocol long description.
	 */
	@NonNull
	public String getProtocolDescription() {
		return protocolDescription;
	}

	/**
	 * Returns the update timestamp for this dashboard state.
	 */
	@Nullable
	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	@NonNull
	private static String asStringOrDefault(@Nullable Object value, @NonNull String fallback) {
		if (value == null) {
			return fallback;
		}
		String parsed = String.valueOf(value);
		return parsed.isEmpty() ? fallback : parsed;
	}

	@Nullable
	private static Timestamp asTimestamp(@Nullable Object value) {
		if (value instanceof Timestamp) {
			return (Timestamp) value;
		}
		return null;
	}
}