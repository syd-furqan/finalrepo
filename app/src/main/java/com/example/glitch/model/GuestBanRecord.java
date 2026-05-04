package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.Map;

/**
 * Structured admin ban record for guest CNIC governance.
 */
public class GuestBanRecord {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";

    private final String id;
    private final String cnic;
    private final String status;
    private final String reasonCode;
    private final String sourceAlertId;
    private final String sourceRequestId;
    private final String bannedByUid;
    private final Timestamp startAt;
    private final Timestamp endAt;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;

    public GuestBanRecord(
            @NonNull String id,
            @NonNull String cnic,
            @NonNull String status,
            @NonNull String reasonCode,
            @NonNull String sourceAlertId,
            @NonNull String sourceRequestId,
            @NonNull String bannedByUid,
            @Nullable Timestamp startAt,
            @Nullable Timestamp endAt,
            @Nullable Timestamp createdAt,
            @Nullable Timestamp updatedAt
    ) {
        this.id = id;
        this.cnic = cnic;
        this.status = status;
        this.reasonCode = reasonCode;
        this.sourceAlertId = sourceAlertId;
        this.sourceRequestId = sourceRequestId;
        this.bannedByUid = bannedByUid;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public static GuestBanRecord fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
        if (map == null) {
            return new GuestBanRecord(id, "", STATUS_INACTIVE, "", "", "", "", null, null, null, null);
        }
        return new GuestBanRecord(
                id,
                asString(map.get("cnic")),
                asStringOr(map.get("status"), STATUS_INACTIVE),
                asString(map.get("reasonCode")),
                asString(map.get("sourceAlertId")),
                asString(map.get("sourceRequestId")),
                asString(map.get("bannedByUid")),
                asTimestamp(map.get("startAt")),
                asTimestamp(map.get("endAt")),
                asTimestamp(map.get("createdAt")),
                asTimestamp(map.get("updatedAt"))
        );
    }

    public boolean isActiveNow() {
        if (!STATUS_ACTIVE.equalsIgnoreCase(status)) {
            return false;
        }
        Date now = new Date();
        if (startAt != null && startAt.toDate().after(now)) {
            return false;
        }
        return endAt == null || endAt.toDate().after(now);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getCnic() {
        return cnic;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getReasonCode() {
        return reasonCode;
    }

    @NonNull
    public String getSourceAlertId() {
        return sourceAlertId;
    }

    @NonNull
    public String getSourceRequestId() {
        return sourceRequestId;
    }

    @NonNull
    public String getBannedByUid() {
        return bannedByUid;
    }

    @Nullable
    public Timestamp getStartAt() {
        return startAt;
    }

    @Nullable
    public Timestamp getEndAt() {
        return endAt;
    }

    @Nullable
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @NonNull
    private static String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @NonNull
    private static String asStringOr(@Nullable Object value, @NonNull String fallback) {
        String parsed = asString(value);
        return parsed.isEmpty() ? fallback : parsed;
    }

    @Nullable
    private static Timestamp asTimestamp(@Nullable Object value) {
        return value instanceof Timestamp ? (Timestamp) value : null;
    }
}
