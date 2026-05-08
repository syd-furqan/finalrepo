package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

public class StudentWarning {
    private final String id;
    private final String targetUid;
    private final String targetName;
    private final String targetRole;
    private final String violationReportId;
    private final String violationLevel;
    private final String detail;
    private final String issuedByUid;
    private final Timestamp createdAt;

    public StudentWarning(
            @NonNull String id,
            @NonNull String targetUid,
            @NonNull String targetName,
            @NonNull String targetRole,
            @NonNull String violationReportId,
            @NonNull String violationLevel,
            @NonNull String detail,
            @NonNull String issuedByUid,
            @Nullable Timestamp createdAt
    ) {
        this.id = id;
        this.targetUid = targetUid;
        this.targetName = targetName;
        this.targetRole = targetRole;
        this.violationReportId = violationReportId;
        this.violationLevel = violationLevel;
        this.detail = detail;
        this.issuedByUid = issuedByUid;
        this.createdAt = createdAt;
    }

    @NonNull
    public static StudentWarning fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
        if (map == null) {
            return new StudentWarning(id, "", "", "", "", "", "", "", null);
        }
        return new StudentWarning(
                id,
                asString(map.get("targetUid")),
                asString(map.get("targetName")),
                asString(map.get("targetRole")),
                asString(map.get("violationReportId")),
                asString(map.get("violationLevel")),
                asString(map.get("detail")),
                asString(map.get("issuedByUid")),
                asTimestamp(map.get("createdAt"))
        );
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getTargetUid() { return targetUid; }
    @NonNull public String getTargetName() { return targetName; }
    @NonNull public String getTargetRole() { return targetRole; }
    @NonNull public String getViolationReportId() { return violationReportId; }
    @NonNull public String getViolationLevel() { return violationLevel; }
    @NonNull public String getDetail() { return detail; }
    @NonNull public String getIssuedByUid() { return issuedByUid; }
    @Nullable public Timestamp getCreatedAt() { return createdAt; }

    @NonNull
    private static String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @Nullable
    private static Timestamp asTimestamp(@Nullable Object value) {
        return value instanceof Timestamp ? (Timestamp) value : null;
    }
}
