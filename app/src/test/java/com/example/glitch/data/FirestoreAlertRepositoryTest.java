package com.example.glitch.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FirestoreAlertRepositoryTest {

    @Test
    public void importantAlertTypesIncludeAllAdminQueueTypes() {
        assertTrue(FirestoreAlertRepository.isImportantAlertType(AdminAlertPayloadFactory.TYPE_ENTRY_REPORT));
        assertTrue(FirestoreAlertRepository.isImportantAlertType(AdminAlertPayloadFactory.TYPE_MANUAL_VIOLATION));
        assertTrue(FirestoreAlertRepository.isImportantAlertType(AdminAlertPayloadFactory.TYPE_SCAN_RISK));
        assertTrue(FirestoreAlertRepository.isImportantAlertType(AdminAlertPayloadFactory.TYPE_VEHICLE_REVIEW));
        assertTrue(FirestoreAlertRepository.isImportantAlertType(AdminAlertPayloadFactory.TYPE_CHARGE_REVIEW));
        assertFalse(FirestoreAlertRepository.isImportantAlertType("routine_audit"));
    }
}
