package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.GuestPass;
import com.example.glitch.model.ViolationReport;
import com.example.glitch.model.VehicleRequestRecord;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds Firestore payloads for admin-facing alerts.
 */
public final class AdminAlertPayloadFactory {
    public static final String TYPE_ENTRY_REPORT = "entry_report";
    public static final String TYPE_MANUAL_VIOLATION = "manual_violation";
    public static final String TYPE_SCAN_RISK = "scan_risk";
    public static final String TYPE_VEHICLE_REVIEW = "vehicle_review";
    public static final String TYPE_CHARGE_REVIEW = "charge_review";

    public static final String SOURCE_MANUAL_VIOLATION = "manual_violation_report";
    public static final String SOURCE_BANNED_SCAN = "banned_guest_scan";
    public static final String SOURCE_VEHICLE_APPLICATION = "vehicle_application_submitted";
    public static final String SOURCE_VEHICLE_REMOVAL = "vehicle_removal_submitted";
    public static final String SOURCE_CHARGE_REMOVAL = "charge_removal_requested";

    private AdminAlertPayloadFactory() {
    }

    @NonNull
    public static Map<String, Object> manualViolation(
            @NonNull String reportId,
            @NonNull String reporterUid,
            @NonNull String reporterRole,
            @NonNull String reporterName,
            @NonNull String detail,
            @NonNull String violationLevel,
            @NonNull String subjectType,
            @NonNull String guestCnic,
            @NonNull String guestName,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String subjectStudentUid,
            @NonNull String subjectStudentName,
            @NonNull String subjectStudentEmail,
            @NonNull String subjectStudentId
    ) {
        boolean studentViolation = ViolationReport.SUBJECT_STUDENT.equalsIgnoreCase(subjectType.trim());
        String subjectName = studentViolation ? subjectStudentName.trim() : guestName.trim();
        String subjectIdentifier = studentViolation ? subjectStudentId.trim() : guestCnic.trim();
        String requesterUid = studentViolation ? subjectStudentUid.trim() : sponsorUid.trim();
        String requesterRole = studentViolation ? "student" : sponsorRole.trim();
        String hostName = studentViolation ? "Student violation" : sponsorName.trim();

        Map<String, Object> extra = extra("violationReportId", reportId);
        if (studentViolation) {
            extra.put("subjectStudentId", subjectStudentId.trim());
            extra.put("subjectStudentEmail", subjectStudentEmail.trim());
        }

        return base(
                TYPE_MANUAL_VIOLATION,
                reportId,
                severityForViolationLevel(violationLevel),
                buildViolationMessage(subjectName, detail),
                "",
                reporterUid,
                reporterRole,
                reporterName,
                violationLevel,
                SOURCE_MANUAL_VIOLATION,
                "Review in Violations",
                subjectName,
                subjectIdentifier,
                hostName,
                requesterUid,
                requesterRole,
                "",
                extra
        );
    }

    @NonNull
    public static Map<String, Object> vehicleReview(
            @NonNull String requestId,
            @NonNull String requesterUid,
            @NonNull String requesterRole,
            @NonNull String requestKind,
            @NonNull String plateNumber
    ) {
        boolean removal = VehicleRequestRecord.KIND_REMOVE.equalsIgnoreCase(requestKind.trim());
        String source = removal ? SOURCE_VEHICLE_REMOVAL : SOURCE_VEHICLE_APPLICATION;
        String message = removal
                ? "Vehicle removal request submitted for " + valueOr(plateNumber, "unknown plate") + "."
                : "Vehicle registration request submitted for " + valueOr(plateNumber, "unknown plate") + ".";
        return base(
                TYPE_VEHICLE_REVIEW,
                requestId,
                "MEDIUM",
                message,
                "",
                requesterUid,
                requesterRole,
                "",
                requestKind,
                source,
                "Review in Vehicle Review",
                valueOr(plateNumber, "Vehicle request"),
                requestKind,
                "Vehicle Review",
                requesterUid,
                requesterRole,
                "",
                extra("vehicleRequestId", requestId)
        );
    }

    @NonNull
    public static Map<String, Object> chargeReview(
            @NonNull String chargeId,
            @NonNull String studentUid,
            @NonNull String paymentNote
    ) {
        String note = paymentNote.trim();
        String message = note.isEmpty()
                ? "Charge removal requested."
                : "Charge removal requested: " + note;
        return base(
                TYPE_CHARGE_REVIEW,
                chargeId,
                "MEDIUM",
                message,
                "",
                studentUid,
                "student",
                "",
                "charge_removal",
                SOURCE_CHARGE_REMOVAL,
                "Review in Charges",
                chargeId,
                "Charge",
                "Charge Review",
                studentUid,
                "student",
                "",
                extra("chargeId", chargeId)
        );
    }

    @NonNull
    public static Map<String, Object> bannedGuestScan(
            @NonNull GuestPass pass,
            @NonNull String guardUid,
            @NonNull String guardName
    ) {
        return base(
                TYPE_SCAN_RISK,
                pass.getId(),
                "CRITICAL",
                "Banned guest attempted entry scan.",
                pass.getEntryRequestId(),
                guardUid,
                "guard",
                guardName,
                "banned_guest",
                SOURCE_BANNED_SCAN,
                "Review banned guest scan",
                pass.getGuestName(),
                pass.getGuestIdNumber(),
                pass.getSponsorName(),
                pass.getSponsorUid(),
                pass.getSponsorRole(),
                pass.getGateLabel(),
                extra("guestPassId", pass.getId())
        );
    }

    @NonNull
    private static Map<String, Object> base(
            @NonNull String alertType,
            @NonNull String identifier,
            @NonNull String severity,
            @NonNull String message,
            @NonNull String entryRequestId,
            @NonNull String reportedByUid,
            @NonNull String reportedByRole,
            @NonNull String reportedByName,
            @NonNull String reasonCode,
            @NonNull String source,
            @NonNull String interventionSummary,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String hostName,
            @NonNull String requesterUid,
            @NonNull String requesterRole,
            @NonNull String gateLabel,
            @NonNull Map<String, Object> extra
    ) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("alertType", alertType.trim());
        alert.put("identifier", identifier.trim());
        alert.put("failCount", 1);
        alert.put("severity", severity.trim());
        alert.put("message", message.trim());
        alert.put("entryRequestId", entryRequestId.trim());
        alert.put("reportedByUid", reportedByUid.trim());
        alert.put("reportedByRole", reportedByRole.trim());
        alert.put("reportedByName", reportedByName.trim());
        alert.put("reportReasonCode", reasonCode.trim());
        alert.put("reportSource", source.trim());
        alert.put("incidentStatus", "new");
        alert.put("interventionSummary", interventionSummary.trim());
        alert.put("guestName", guestName.trim());
        alert.put("guestIdNumber", guestIdNumber.trim());
        alert.put("hostName", hostName.trim());
        alert.put("requesterUid", requesterUid.trim());
        alert.put("requesterRole", requesterRole.trim());
        alert.put("gateLabel", gateLabel.trim());
        alert.put("reviewedByUid", "");
        alert.put("reviewedAt", null);
        alert.put("createdAt", FieldValue.serverTimestamp());
        alert.put("lastFailedAt", FieldValue.serverTimestamp());
        alert.putAll(extra);
        return alert;
    }

    @NonNull
    private static String severityForViolationLevel(@NonNull String violationLevel) {
        String level = violationLevel.trim().toLowerCase();
        if ("v3".equals(level)) {
            return "HIGH";
        }
        if ("v2".equals(level)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    @NonNull
    private static String buildViolationMessage(@NonNull String subjectName, @NonNull String detail) {
        String subject = valueOr(subjectName, "Unknown subject");
        String trimmedDetail = detail.trim();
        if (trimmedDetail.isEmpty()) {
            return "New violation report submitted for " + subject + ".";
        }
        return "New violation report submitted for " + subject + ": " + trimmedDetail;
    }

    @NonNull
    private static Map<String, Object> extra(@NonNull String key, @NonNull String value) {
        Map<String, Object> extra = new HashMap<>();
        extra.put(key, value.trim());
        return extra;
    }

    @NonNull
    private static String valueOr(@NonNull String value, @NonNull String fallback) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
