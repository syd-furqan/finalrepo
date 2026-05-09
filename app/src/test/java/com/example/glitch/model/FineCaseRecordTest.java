package com.example.glitch.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class FineCaseRecordTest {

    @Test
    public void fromMap_parsesStudentIdForStudentPeggedFine() {
        Map<String, Object> map = new HashMap<>();
        map.put("sponsorUid", "student-uid");
        map.put("studentId", "STU-789");
        map.put("studentName", "Student Name");
        map.put("violationReportId", "report-1");
        map.put("amount", 2500.0);

        FineCaseRecord record = FineCaseRecord.fromMap("fine-1", map);

        assertEquals("student-uid", record.getSponsorUid());
        assertEquals("STU-789", record.getStudentId());
        assertEquals("report-1", record.getViolationReportId());
        assertEquals(2500.0, record.getAmount(), 0.0);
    }
}
