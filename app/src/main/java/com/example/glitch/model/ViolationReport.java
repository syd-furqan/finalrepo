package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

public class ViolationReport {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACTIONED = "actioned";
    public static final String STATUS_IGNORED = "ignored";

    public static final String SUBJECT_GUEST = "guest";
    public static final String SUBJECT_STUDENT = "student";

    private final String id;
    private final String reporterUid;
    private final String reporterRole;
    private final String reporterName;
    private final String detail;
    private final String violationLevel;
    private final String subjectType;
    private final String guestCnic;
    private final String guestName;
    private final String guestPassId;
    private final String sponsorUid;
    private final String sponsorName;
    private final String sponsorRole;
    private final String subjectStudentUid;
    private final String subjectStudentName;
    private final String subjectStudentEmail;
    private final String subjectStudentId;
    private final String status;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;

    public ViolationReport(
            @NonNull String id,
            @NonNull String reporterUid,
            @NonNull String reporterRole,
            @NonNull String reporterName,
            @NonNull String detail,
            @NonNull String violationLevel,
            @NonNull String subjectType,
            @NonNull String guestCnic,
            @NonNull String guestName,
            @NonNull String guestPassId,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String subjectStudentUid,
            @NonNull String subjectStudentName,
            @NonNull String subjectStudentEmail,
            @NonNull String subjectStudentId,
            @NonNull String status,
            @Nullable Timestamp createdAt,
            @Nullable Timestamp updatedAt
    ) {
        this.id = id;
        this.reporterUid = reporterUid;
        this.reporterRole = reporterRole;
        this.reporterName = reporterName;
        this.detail = detail;
        this.violationLevel = violationLevel;
        this.subjectType = subjectType;
        this.guestCnic = guestCnic;
        this.guestName = guestName;
        this.guestPassId = guestPassId;
        this.sponsorUid = sponsorUid;
        this.sponsorName = sponsorName;
        this.sponsorRole = sponsorRole;
        this.subjectStudentUid = subjectStudentUid;
        this.subjectStudentName = subjectStudentName;
        this.subjectStudentEmail = subjectStudentEmail;
        this.subjectStudentId = subjectStudentId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public static ViolationReport fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
        if (map == null) {
            return new ViolationReport(id, "", "", "", "", "", SUBJECT_GUEST, "", "", "", "", "", "", "", "", "", "", STATUS_PENDING, null, null);
        }
        return new ViolationReport(
                id,
                asString(map.get("reporterUid")),
                asString(map.get("reporterRole")),
                asString(map.get("reporterName")),
                asString(map.get("detail")),
                asString(map.get("violationLevel")),
                asStringOr(map.get("subjectType"), SUBJECT_GUEST),
                asString(map.get("guestCnic")),
                asString(map.get("guestName")),
                asString(map.get("guestPassId")),
                asString(map.get("sponsorUid")),
                asString(map.get("sponsorName")),
                asString(map.get("sponsorRole")),
                asString(map.get("subjectStudentUid")),
                asString(map.get("subjectStudentName")),
                asString(map.get("subjectStudentEmail")),
                asString(map.get("subjectStudentId")),
                asStringOr(map.get("status"), STATUS_PENDING),
                asTimestamp(map.get("createdAt")),
                asTimestamp(map.get("updatedAt"))
        );
    }

    public boolean isPending() {
        return STATUS_PENDING.equalsIgnoreCase(status);
    }

    public boolean isGuestViolation() {
        return SUBJECT_GUEST.equalsIgnoreCase(subjectType);
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getReporterUid() { return reporterUid; }
    @NonNull public String getReporterRole() { return reporterRole; }
    @NonNull public String getReporterName() { return reporterName; }
    @NonNull public String getDetail() { return detail; }
    @NonNull public String getViolationLevel() { return violationLevel; }
    @NonNull public String getSubjectType() { return subjectType; }
    @NonNull public String getGuestCnic() { return guestCnic; }
    @NonNull public String getGuestName() { return guestName; }
    @NonNull public String getGuestPassId() { return guestPassId; }
    @NonNull public String getSponsorUid() { return sponsorUid; }
    @NonNull public String getSponsorName() { return sponsorName; }
    @NonNull public String getSponsorRole() { return sponsorRole; }
    @NonNull public String getSubjectStudentUid() { return subjectStudentUid; }
    @NonNull public String getSubjectStudentName() { return subjectStudentName; }
    @NonNull public String getSubjectStudentEmail() { return subjectStudentEmail; }
    @NonNull public String getSubjectStudentId() { return subjectStudentId; }
    @NonNull public String getStatus() { return status; }
    @Nullable public Timestamp getCreatedAt() { return createdAt; }
    @Nullable public Timestamp getUpdatedAt() { return updatedAt; }

    @NonNull
    private static String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @NonNull
    private static String asStringOr(@Nullable Object value, @NonNull String fallback) {
        String s = asString(value);
        return s.isEmpty() ? fallback : s;
    }

    @Nullable
    private static Timestamp asTimestamp(@Nullable Object value) {
        return value instanceof Timestamp ? (Timestamp) value : null;
    }
}
