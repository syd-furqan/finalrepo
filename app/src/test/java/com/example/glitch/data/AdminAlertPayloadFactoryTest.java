package com.example.glitch.data;

import static org.junit.Assert.assertEquals;

import com.example.glitch.model.GuestPass;
import com.example.glitch.model.ViolationReport;
import com.example.glitch.model.VehicleRequestRecord;

import org.junit.Test;

import java.util.Map;

public class AdminAlertPayloadFactoryTest {

    @Test
    public void manualViolationUsesManualTypeAndMapsV3ToHigh() {
        Map<String, Object> payload = AdminAlertPayloadFactory.manualViolation(
                "report-1",
                "monitor-1",
                "monitor",
                "Monitor User",
                "Serious incident",
                "v3",
                ViolationReport.SUBJECT_GUEST,
                "12345-1234567-1",
                "Guest Name",
                "+923001234567",
                "sponsor-1",
                "Sponsor Name",
                "student",
                "",
                "",
                "",
                ""
        );

        assertEquals(AdminAlertPayloadFactory.TYPE_MANUAL_VIOLATION, payload.get("alertType"));
        assertEquals("HIGH", payload.get("severity"));
        assertEquals(AdminAlertPayloadFactory.SOURCE_MANUAL_VIOLATION, payload.get("reportSource"));
        assertEquals("report-1", payload.get("violationReportId"));
    }

    @Test
    public void manualStudentViolationUsesStudentIdAsSubjectIdentifier() {
        Map<String, Object> payload = AdminAlertPayloadFactory.manualViolation(
                "report-2",
                "monitor-1",
                "monitor",
                "Monitor User",
                "Student incident",
                "v2",
                ViolationReport.SUBJECT_STUDENT,
                "",
                "",
                "",
                "",
                "",
                "",
                "student-uid",
                "Student Name",
                "student@example.com",
                "STU-100"
        );

        assertEquals("STU-100", payload.get("guestIdNumber"));
        assertEquals("STU-100", payload.get("subjectStudentId"));
    }

    @Test
    public void vehicleReviewCreatesMediumReviewAlert() {
        Map<String, Object> payload = AdminAlertPayloadFactory.vehicleReview(
                "vehicle-1",
                "student-1",
                "student",
                VehicleRequestRecord.KIND_REGISTER,
                "ABC-123"
        );

        assertEquals(AdminAlertPayloadFactory.TYPE_VEHICLE_REVIEW, payload.get("alertType"));
        assertEquals("MEDIUM", payload.get("severity"));
        assertEquals(AdminAlertPayloadFactory.SOURCE_VEHICLE_APPLICATION, payload.get("reportSource"));
        assertEquals("vehicle-1", payload.get("vehicleRequestId"));
    }

    @Test
    public void chargeReviewCreatesMediumReviewAlert() {
        Map<String, Object> payload = AdminAlertPayloadFactory.chargeReview(
                "charge-1",
                "student-1",
                "I paid this already"
        );

        assertEquals(AdminAlertPayloadFactory.TYPE_CHARGE_REVIEW, payload.get("alertType"));
        assertEquals("MEDIUM", payload.get("severity"));
        assertEquals(AdminAlertPayloadFactory.SOURCE_CHARGE_REMOVAL, payload.get("reportSource"));
        assertEquals("charge-1", payload.get("chargeId"));
    }

    @Test
    public void bannedGuestScanCreatesCriticalScanRiskAlert() {
        GuestPass pass = new GuestPass(
                "pass-1",
                "sponsor-1",
                "student",
                "Sponsor Name",
                "sponsor@example.com",
                "Guest Name",
                "12345-1234567-1",
                false,
                "",
                "non_vehicle",
                "PASS-1",
                "entry-1",
                "in-gate",
                "active",
                null,
                null,
                "",
                "",
                null
        );

        Map<String, Object> payload = AdminAlertPayloadFactory.bannedGuestScan(pass, "guard-1", "Guard Name");

        assertEquals(AdminAlertPayloadFactory.TYPE_SCAN_RISK, payload.get("alertType"));
        assertEquals("CRITICAL", payload.get("severity"));
        assertEquals(AdminAlertPayloadFactory.SOURCE_BANNED_SCAN, payload.get("reportSource"));
        assertEquals("pass-1", payload.get("guestPassId"));
    }
}
