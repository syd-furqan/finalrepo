package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Represents a staff vehicle registration request and current review status.
 * Pattern: Immutable POJO for staff submission and admin review screens.
 */
public class VehicleRequestRecord {
    private final String id;
    private final String requesterUid;
    private final String plateNumber;
    private final String plateKey;
    private final String vehicleMake;
    private final String vehicleModel;
    private final String vehicleColor;
    private final String status;
    private final String reviewerUid;
    private final String reviewNote;
    private final Timestamp createdAt;
    private final Timestamp reviewedAt;

    public VehicleRequestRecord(
            @NonNull String id,
            @NonNull String requesterUid,
            @NonNull String plateNumber,
            @NonNull String plateKey,
            @NonNull String vehicleMake,
            @NonNull String vehicleModel,
            @NonNull String vehicleColor,
            @NonNull String status,
            @NonNull String reviewerUid,
            @NonNull String reviewNote,
            @Nullable Timestamp createdAt,
            @Nullable Timestamp reviewedAt
    ) {
        this.id = id;
        this.requesterUid = requesterUid;
        this.plateNumber = plateNumber;
        this.plateKey = plateKey;
        this.vehicleMake = vehicleMake;
        this.vehicleModel = vehicleModel;
        this.vehicleColor = vehicleColor;
        this.status = status;
        this.reviewerUid = reviewerUid;
        this.reviewNote = reviewNote;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    @NonNull
    public static VehicleRequestRecord fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
        if (map == null) {
            return new VehicleRequestRecord(id, "", "", "", "", "", "", "", "", "", null, null);
        }
        String plateNumber = asString(map.get("plateNumber"));
        String plateKey = asString(map.get("plateKey"));
        if (plateKey.isEmpty()) {
            plateKey = plateNumber;
        }
        return new VehicleRequestRecord(
                id,
                asString(map.get("requesterUid")),
                plateNumber,
                plateKey,
                asString(map.get("vehicleMake")),
                asString(map.get("vehicleModel")),
                asString(map.get("vehicleColor")),
                asString(map.get("status")),
                asString(map.get("reviewerUid")),
                asString(map.get("reviewNote")),
                asTimestamp(map.get("createdAt")),
                asTimestamp(map.get("reviewedAt"))
        );
    }

    public boolean isPending() {
        return "pending".equalsIgnoreCase(status.trim());
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getRequesterUid() {
        return requesterUid;
    }

    @NonNull
    public String getPlateNumber() {
        return plateNumber;
    }

    @NonNull
    public String getPlateKey() {
        return plateKey.isEmpty() ? plateNumber : plateKey;
    }

    @NonNull
    public String getVehicleMake() {
        return vehicleMake;
    }

    @NonNull
    public String getVehicleModel() {
        return vehicleModel;
    }

    @NonNull
    public String getVehicleColor() {
        return vehicleColor;
    }

    /** Full make + model string for display; falls back to whichever part is non-empty. */
    @NonNull
    public String getVehicleDescription() {
        String make = vehicleMake.trim();
        String model = vehicleModel.trim();
        if (make.isEmpty()) return model;
        if (model.isEmpty()) return make;
        return make + " " + model;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getReviewerUid() {
        return reviewerUid;
    }

    @NonNull
    public String getReviewNote() {
        return reviewNote;
    }

    @Nullable
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public Timestamp getReviewedAt() {
        return reviewedAt;
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
