package com.example.glitch.notification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NotificationLocalAlertCoordinatorTest {

    @Test
    public void supportedRolesAreStudentAndFaculty() {
        assertTrue(NotificationLocalAlertCoordinator.isSupportedRole("student"));
        assertTrue(NotificationLocalAlertCoordinator.isSupportedRole("FACULTY"));
        assertFalse(NotificationLocalAlertCoordinator.isSupportedRole("admin"));
        assertFalse(NotificationLocalAlertCoordinator.isSupportedRole("guard"));
        assertFalse(NotificationLocalAlertCoordinator.isSupportedRole(null));
    }
}
