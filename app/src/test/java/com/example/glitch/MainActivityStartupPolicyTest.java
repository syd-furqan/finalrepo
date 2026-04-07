package com.example.glitch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for startup/login routing rules used by MainActivity.
 */
public class MainActivityStartupPolicyTest {

    @Test
    public void coldStartForcesFreshLogin() {
        assertTrue(MainActivityStartupPolicy.shouldForceFreshLogin(false));
        assertFalse(MainActivityStartupPolicy.shouldForceFreshLogin(true));
    }

    @Test
    public void restoredProtectedScreenWithoutSessionRedirectsToLogin() {
        assertTrue(MainActivityStartupPolicy.shouldRedirectToLogin(false, false));
        assertFalse(MainActivityStartupPolicy.shouldRedirectToLogin(true, false));
        assertFalse(MainActivityStartupPolicy.shouldRedirectToLogin(false, true));
    }
}
