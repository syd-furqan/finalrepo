package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Represents a staff vehicle registration request and current review status.
 * Pattern: Immutable POJO for staff submission and status tracking screens.
 * Known issue: vehicle details are intentionally compact in v1.
 */
public class VehicleRequestRecord {
	private final String id;
	private final String requesterUid;
	private final String plateNumber;
	private final String vehicleModel;
	private final String status;
	private final Timestamp createdAt;

	/**
	 * Creates an immutable vehicle request record.
	 */
	public VehicleRequestRecord(
			@NonNull String id,
			@NonNull String requesterUid,
			@NonNull String plateNumber,
			@NonNull String vehicleModel,
			@NonNull String status,
			@Nullable Timestamp createdAt
	) {
		this.id = id;
		this.requesterUid = requesterUid;
		this.plateNumber = plateNumber;
		this.vehicleModel = vehicleModel;
		this.status = status;
		this.createdAt = createdAt;
	}

	/**
	 * Maps Firestore data into a {@link VehicleRequestRecord}.
	 *
	 * @param id Firestore document id.
	 * @param map Firestore field map.
	 * @return parsed vehicle request object or empty fallback when data is missing.
	 */
	@NonNull
	public static VehicleRequestRecord fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
		if (map == null) {
			return new VehicleRequestRecord(id, "", "", "", "", null);
		}
		return new VehicleRequestRecord(
				id,
				asString(map.get("requesterUid")),
				asString(map.get("plateNumber")),
				asString(map.get("vehicleModel")),
				asString(map.get("status")),
				asTimestamp(map.get("createdAt"))
		);
	}

	/**
	 * @return Firestore document id.
	 */
	@NonNull
	public String getId() {
		return id;
	}

	/**
	 * @return uid of the requester who submitted this vehicle request.
	 */
	@NonNull
	public String getRequesterUid() {
		return requesterUid;
	}

	/**
	 * @return requested vehicle plate number.
	 */
	@NonNull
	public String getPlateNumber() {
		return plateNumber;
	}

	/**
	 * @return vehicle model/description submitted by the requester.
	 */
	@NonNull
	public String getVehicleModel() {
		return vehicleModel;
	}

	/**
	 * @return workflow status (pending/approved/denied).
	 */
	@NonNull
	public String getStatus() {
		return status;
	}

	/**
	 * @return creation timestamp from Firestore.
	 */
	@Nullable
	public Timestamp getCreatedAt() {
		return createdAt;
	}

	@NonNull
	private static String asString(@Nullable Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	@Nullable
	private static Timestamp asTimestamp(@Nullable Object value) {
		if (value instanceof Timestamp) {
			return (Timestamp) value;
		}
		return null;
	}
}