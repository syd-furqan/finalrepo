package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Locale;
import java.util.Map;

/**
 * Represents one active or exited entry request shown on the guard dashboard.
 * Pattern: Plain Old Java Object used by repository and UI layers.
 * Known issue: iconType currently maps to local drawables and is not remotely configurable.
 */
public class EntryRequest {
	private final String id;
	private final String fullName;
	private final String roleTag;
	private final String hostName;
	private final String gateLabel;
	private final String guestIDNumber;
	private final boolean hasVehicle;
	private final String vehiclePlate;
	private final String guestType;
	private final String requesterRole;
	private final Timestamp enteredAt;
	private final String status;
	private final Timestamp expiresAt;
	private final String iconType;

	/**
	 * Creates an immutable entry request model.
	 */
	public EntryRequest(
			@NonNull String id,
			@NonNull String fullName,
			@NonNull String roleTag,
			@NonNull String hostName,
			@NonNull String gateLabel,
			@NonNull String guestIdNumber,
			boolean hasVehicle,
			@NonNull String vehiclePlate,
			@NonNull String guestType,
			@NonNull String requesterRole,
			@Nullable Timestamp enteredAt,
			@NonNull String status,
			@Nullable Timestamp expiresAt,
			@NonNull String iconType
	) {
		this.id = id;
		this.fullName = fullName;
		this.roleTag = roleTag;
		this.hostName = hostName;
		this.gateLabel = GatePolicy.normalizeStoredValue(gateLabel);
		String normalizedGuestId = GuestIdentityPolicy.normalizeCnic(guestIdNumber);
		this.guestIDNumber = normalizedGuestId == null ? guestIdNumber : normalizedGuestId;
		this.hasVehicle = hasVehicle;
		String normalizedPlate = GuestIdentityPolicy.normalizeVehiclePlate(vehiclePlate);
		this.vehiclePlate = normalizedPlate == null ? vehiclePlate : normalizedPlate;
		this.guestType = guestType.trim().isEmpty() ? GuestIdentityPolicy.guestTypeFor(hasVehicle) : guestType;
		this.requesterRole = requesterRole.trim().toLowerCase(Locale.getDefault());
		this.enteredAt = enteredAt;
		this.status = status;
		this.expiresAt = expiresAt;
		this.iconType = iconType;
	}

	/**
	 * Converts Firestore map data into a strongly typed entry request.
	 */
	@NonNull
	public static EntryRequest fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
		if (map == null) {
			return new EntryRequest(id, "", "", "", GatePolicy.STORED_VALUE, "",
					false, "", "non_vehicle", "", null, "active", null, "");
		}

		return new EntryRequest(
				id,
				asString(map.get("fullName")),
				asString(map.get("roleTag")),
				asString(map.get("hostName")),
				GatePolicy.normalizeStoredValue(asString(map.get("gateLabel"))),
				asString(map.get("guestIdNumber")),
				asBoolean(map.get("hasVehicle")),
				asString(map.get("vehiclePlate")),
				asString(map.get("guestType")),
				asString(map.get("requesterRole")),
				asTimestamp(map.get("enteredAt")),
				asStringOrDefault(map.get("status"), "active"),
				asTimestamp(map.get("expiresAt")),
				asString(map.get("iconType"))
		);
	}

	/**
	 * Returns Firestore document id.
	 */
	@NonNull
	public String getId() {
		return id;
	}

	/**
	 * Returns display name of the requester.
	 */
	@NonNull
	public String getFullName() {
		return fullName;
	}

	/**
	 * Returns short role label (for example Guest, Service, Contractor).
	 */
	@NonNull
	public String getRoleTag() {
		return roleTag;
	}

	/**
	 * Returns host detail shown under role chip.
	 */
	@NonNull
	public String getHostName() {
		return hostName;
	}

	/**
	 * Returns gate label assigned to this request.
	 */
	@NonNull
	public String getGateLabel() {
		return gateLabel;
	}

	/**
	 * Returns guest identifier value used by pass/entry lookup.
	 */
	@NonNull
	public String getGuestIdNumber() {
		return guestIDNumber;
	}

	public boolean hasVehicle() {
		return hasVehicle;
	}

	@NonNull
	public String getVehiclePlate() {
		return vehiclePlate;
	}

	@NonNull
	public String getGuestType() {
		return guestType;
	}

	@NonNull
	public String getRequesterRole() {
		return requesterRole;
	}

	/**
	 * Returns entry timestamp.
	 */
	@Nullable
	public Timestamp getEnteredAt() {
		return enteredAt;
	}

	/**
	 * Returns current request status.
	 */
	@NonNull
	public String getStatus() {
		return status;
	}

	/**
	 * Returns pass/request expiry timestamp when available.
	 */
	@Nullable
	public Timestamp getExpiresAt() {
		return expiresAt;
	}

	/**
	 * Returns icon type key used for local UI icon mapping.
	 */
	@NonNull
	public String getIconType() {
		return iconType;
	}

	@NonNull
	private static String asString(@Nullable Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	@NonNull
	private static String asStringOrDefault(@Nullable Object value, @NonNull String fallback) {
		String parsed = asString(value);
		return parsed.isEmpty() ? fallback : parsed;
	}

	private static boolean asBoolean(@Nullable Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		}
		if (value instanceof String) {
			String normalized = ((String) value).trim().toLowerCase(Locale.getDefault());
			return "true".equals(normalized) || "1".equals(normalized);
		}
		return false;
	}

	@Nullable
	private static Timestamp asTimestamp(@Nullable Object value) {
		if (value instanceof Timestamp) {
			return (Timestamp) value;
		}
		return null;
	}
}
