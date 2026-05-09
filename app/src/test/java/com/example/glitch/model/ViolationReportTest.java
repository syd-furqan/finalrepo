package com.example.glitch.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ViolationReportTest {

    @Test
    public void fromMap_parsesSubjectStudentId() {
        Map<String, Object> map = new HashMap<>();
        map.put("subjectType", ViolationReport.SUBJECT_STUDENT);
        map.put("subjectStudentUid", "student-uid");
        map.put("subjectStudentName", "Student Name");
        map.put("subjectStudentEmail", "student@example.edu");
        map.put("subjectStudentId", "STU-123");

        ViolationReport report = ViolationReport.fromMap("report-1", map);

        assertEquals(ViolationReport.SUBJECT_STUDENT, report.getSubjectType());
        assertEquals("STU-123", report.getSubjectStudentId());
    }

    @Test
    public void fromMap_parsesGuestPassReviewContext() {
        Map<String, Object> map = new HashMap<>();
        map.put("subjectType", ViolationReport.SUBJECT_GUEST);
        map.put("guestPassId", "pass-1");
        map.put("guestPhone", "+923001234567");
        map.put("entryRequestId", "entry-1");
        map.put("previousGuestPassStatus", "overdue");
        map.put("previousEntryRequestStatus", "active");
        map.put("sponsorStudentId", "STU-456");
        map.put("source", "entry_report");
        map.put("status", ViolationReport.STATUS_WARNING_ISSUED);

        ViolationReport report = ViolationReport.fromMap("report-2", map);

        assertEquals("pass-1", report.getGuestPassId());
        assertEquals("+923001234567", report.getGuestPhone());
        assertEquals("entry-1", report.getEntryRequestId());
        assertEquals("overdue", report.getPreviousGuestPassStatus());
        assertEquals("active", report.getPreviousEntryRequestStatus());
        assertEquals("STU-456", report.getSponsorStudentId());
        assertEquals("entry_report", report.getSource());
        assertEquals(ViolationReport.STATUS_WARNING_ISSUED, report.getStatus());
    }
}
