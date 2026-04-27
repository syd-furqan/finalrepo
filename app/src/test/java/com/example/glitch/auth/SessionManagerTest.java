package com.example.glitch.auth;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.example.glitch.model.UserProfile;

import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for in-memory session state behavior.
 */
public class SessionManagerTest {

    @After
    public void tearDown() {
        SessionManager.clear();
    }

    @Test
    public void setCurrentProfile_storesProfile() {
        UserProfile profile = new UserProfile(
                "uid-1",
                "guard@lums.edu.pk",
                "guard",
                true,
                "Gate Guard",
                null,
                null
        );

        SessionManager.setCurrentProfile(profile);

        assertSame(profile, SessionManager.getCurrentProfile());
    }

    @Test
    public void clear_removesProfile() {
        UserProfile profile = new UserProfile(
                "uid-2",
                "student@lums.edu.pk",
                "student",
                true,
                "Student User",
                null,
                null
        );
        SessionManager.setCurrentProfile(profile);

        SessionManager.clear();

        assertNull(SessionManager.getCurrentProfile());
    }
}
