package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Active or removed registered vehicle managed through admin-reviewed applications.
 */
public class RegisteredVehicleRecord {
    private final String id;
    private final String requesterUid;
    private final String requesterRole;
    private final String studentCategory;
    private final String stickerType;
    private final String plateNumber;
    private final String make;
    private final String model;
    private final String variant;
    private final boolean owner;
    private final String status;
    private final String sourceRequestId;
    private final Timestamp approvedAt;
    private final Timestamp removedAt;

    public RegisteredVehicleRecord(
            @NonNull String id,
            @NonNull String requesterUid,
            @NonNull String requesterRole,
            @NonNull String studentCategory,
            @NonNull String stickerType,
            @NonNull String plateNumber,
            @NonNull String make,
            @NonNull String model,
            @NonNull String variant,
            boolean owner,
            @NonNull String status,
            @NonNull String sourceRequestId,
            @Nullable Timestamp approvedAt,
            @Nullable Timestamp removedAt
    ) {
        this.id = id;
        this.requesterUid = requesterUid;
        this.requesterRole = requesterRole;
        this.studentCategory = studentCategory;
        this.stickerType = stickerType;
        this.plateNumber = plateNumber;
        this.make = make;
        this.model = model;
        this.variant = variant;
        this.owner = owner;
        this.status = status;
        this.sourceRequestId = sourceRequestId;
        this.approvedAt = approvedAt;
        this.removedAt = removedAt;
    }

    @NonNull
    public static RegisteredVehicleRecord fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
        if (map == null) {
            return new RegisteredVehicleRecord(id, "", "", "", "", "", "", "", "", false, "", "", null, null);
        }
        return new RegisteredVehicleRecord(
                id,
                asString(map.get("requesterUid")),
                asString(map.get("requesterRole")),
                asString(map.get("studentCategory")),
                asString(map.get("stickerType")),
                asString(map.get("plateNumber")),
                asString(map.get("vehicleMake")),
                asString(map.get("vehicleModel")),
                asString(map.get("vehicleVariant")),
                asBoolean(map.get("isOwner")),
                asString(map.get("status")),
                asString(map.get("sourceRequestId")),
                asTimestamp(map.get("approvedAt")),
                asTimestamp(map.get("removedAt"))
        );
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
    public String getRequesterRole() {
        return requesterRole;
    }

    @NonNull
    public String getStudentCategory() {
        return studentCategory;
    }

    @NonNull
    public String getStickerType() {
        return stickerType;
    }

    @NonNull
    public String getPlateNumber() {
        return plateNumber;
    }

    @NonNull
    public String getMake() {
        return make;
    }

    @NonNull
    public String getModel() {
        return model;
    }

    @NonNull
    public String getVariant() {
        return variant;
    }

    public boolean isOwner() {
        return owner;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getSourceRequestId() {
        return sourceRequestId;
    }

    @Nullable
    public Timestamp getApprovedAt() {
        return approvedAt;
    }

    @Nullable
    public Timestamp getRemovedAt() {
        return removedAt;
    }

    @NonNull
    private static String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean asBoolean(@Nullable Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            return "true".equalsIgnoreCase(String.valueOf(value).trim());
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
