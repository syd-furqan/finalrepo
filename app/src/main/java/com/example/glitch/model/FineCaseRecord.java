package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Structured sponsor fine lifecycle record.
 */
public class FineCaseRecord {
    public static final String STATUS_ISSUED = "issued";
    public static final String STATUS_WAIVED = "waived";
    public static final String STATUS_SETTLED = "settled";

    private final String id;
    private final String sponsorUid;
    private final String requestId;
    private final String alertId;
    private final String guestName;
    private final String guestIdNumber;
    private final double amount;
    private final String currency;
    private final String reasonCode;
    private final String status;
    private final String issuedByUid;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;
    private final Timestamp resolvedAt;

    public FineCaseRecord(
            @NonNull String id,
            @NonNull String sponsorUid,
            @NonNull String requestId,
            @NonNull String alertId,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            double amount,
            @NonNull String currency,
            @NonNull String reasonCode,
            @NonNull String status,
            @NonNull String issuedByUid,
            @Nullable Timestamp createdAt,
            @Nullable Timestamp updatedAt,
            @Nullable Timestamp resolvedAt
    ) {
        this.id = id;
        this.sponsorUid = sponsorUid;
        this.requestId = requestId;
        this.alertId = alertId;
        this.guestName = guestName;
        this.guestIdNumber = guestIdNumber;
        this.amount = amount;
        this.currency = currency;
        this.reasonCode = reasonCode;
        this.status = status;
        this.issuedByUid = issuedByUid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
    }

    @NonNull
    public static FineCaseRecord fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
        if (map == null) {
            return new FineCaseRecord(id, "", "", "", "", "", 0.0, "PKR", "", STATUS_ISSUED, "", null, null, null);
        }
        return new FineCaseRecord(
                id,
                asString(map.get("sponsorUid")),
                asString(map.get("requestId")),
                asString(map.get("alertId")),
                asString(map.get("guestName")),
                asString(map.get("guestIdNumber")),
                asDouble(map.get("amount")),
                asStringOr(map.get("currency"), "PKR"),
                asString(map.get("reasonCode")),
                asStringOr(map.get("status"), STATUS_ISSUED),
                asString(map.get("issuedByUid")),
                asTimestamp(map.get("createdAt")),
                asTimestamp(map.get("updatedAt")),
                asTimestamp(map.get("resolvedAt"))
        );
    }

    public boolean isOpen() {
        return STATUS_ISSUED.equalsIgnoreCase(status);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getSponsorUid() {
        return sponsorUid;
    }

    @NonNull
    public String getRequestId() {
        return requestId;
    }

    @NonNull
    public String getAlertId() {
        return alertId;
    }

    @NonNull
    public String getGuestName() {
        return guestName;
    }

    @NonNull
    public String getGuestIdNumber() {
        return guestIdNumber;
    }

    public double getAmount() {
        return amount;
    }

    @NonNull
    public String getCurrency() {
        return currency;
    }

    @NonNull
    public String getReasonCode() {
        return reasonCode;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getChargeDisplayStatus() {
        if (STATUS_SETTLED.equalsIgnoreCase(status)) {
            return "paid";
        }
        if (STATUS_WAIVED.equalsIgnoreCase(status)) {
            return "removed";
        }
        return "charged";
    }

    @NonNull
    public String getIssuedByUid() {
        return issuedByUid;
    }

    @Nullable
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @Nullable
    public Timestamp getResolvedAt() {
        return resolvedAt;
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

    private static double asDouble(@Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    @Nullable
    private static Timestamp asTimestamp(@Nullable Object value) {
        return value instanceof Timestamp ? (Timestamp) value : null;
    }
}
