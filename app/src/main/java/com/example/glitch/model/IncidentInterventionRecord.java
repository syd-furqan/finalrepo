package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Links one incident alert to intervention artifacts (ban/fine).
 */
public class IncidentInterventionRecord {
    private final String id;
    private final String alertId;
    private final String requestId;
    private final String banId;
    private final String fineId;
    private final String status;
    private final String summary;
    private final Timestamp updatedAt;

    public IncidentInterventionRecord(
            @NonNull String id,
            @NonNull String alertId,
            @NonNull String requestId,
            @NonNull String banId,
            @NonNull String fineId,
            @NonNull String status,
            @NonNull String summary,
            @Nullable Timestamp updatedAt
    ) {
        this.id = id;
        this.alertId = alertId;
        this.requestId = requestId;
        this.banId = banId;
        this.fineId = fineId;
        this.status = status;
        this.summary = summary;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public static IncidentInterventionRecord fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
        if (map == null) {
            return new IncidentInterventionRecord(id, "", "", "", "", "", "", null);
        }
        return new IncidentInterventionRecord(
                id,
                asString(map.get("alertId")),
                asString(map.get("requestId")),
                asString(map.get("banId")),
                asString(map.get("fineId")),
                asString(map.get("status")),
                asString(map.get("summary")),
                asTimestamp(map.get("updatedAt"))
        );
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getAlertId() {
        return alertId;
    }

    @NonNull
    public String getRequestId() {
        return requestId;
    }

    @NonNull
    public String getBanId() {
        return banId;
    }

    @NonNull
    public String getFineId() {
        return fineId;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getSummary() {
        return summary;
    }

    @Nullable
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @NonNull
    private static String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @Nullable
    private static Timestamp asTimestamp(@Nullable Object value) {
        return value instanceof Timestamp ? (Timestamp) value : null;
    }
}
