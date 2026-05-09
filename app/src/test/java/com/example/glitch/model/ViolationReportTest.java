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
}
