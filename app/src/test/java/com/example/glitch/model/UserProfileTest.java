package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies Firestore map-to-model mapping and role support checks for user profiles.
 */
public class UserProfileTest {

    @Test
    public void fromMap_mapsCoreFields() {
        Timestamp createdAt = new Timestamp(100, 0);
        Timestamp updatedAt = new Timestamp(200, 0);
        Map<String, Object> map = new HashMap<>();
        map.put("email", "guard@example.com");
        map.put("role", "GuArD");
        map.put("isActive", true);
        map.put("displayName", "Gate Guard");
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);

        UserProfile profile = UserProfile.fromMap("uid-1", map);

        assertEquals("uid-1", profile.getUid());
        assertEquals("guard@example.com", profile.getEmail());
        assertEquals("guard", profile.getRole());
        assertEquals("Gate Guard", profile.getDisplayName());
        assertTrue(profile.isActive());
        assertTrue(profile.hasSupportedRole());
    }

    @Test
    public void hasSupportedRole_returnsFalseForUnknownRole() {
        UserProfile profile = new UserProfile("u", "e@x.com", "unknown", true, "Name", null, null);
        assertFalse(profile.hasSupportedRole());
    }

    @Test
    public void hasSupportedRole_returnsFalseForStaffRole() {
        UserProfile profile = new UserProfile("u", "e@x.com", "staff", true, "Name", null, null);
        assertFalse(profile.hasSupportedRole());
    }

    @Test
    public void fromMap_trimsRoleAndParsesStringActiveFlag() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", "student@lums.edu.pk");
        map.put("role", " Student ");
        map.put("isActive", "true");
        map.put("displayName", "Student User");

        UserProfile profile = UserProfile.fromMap("uid-2", map);

        assertEquals("student", profile.getRole());
        assertTrue(profile.isActive());
        assertTrue(profile.hasSupportedRole());
    }
}
