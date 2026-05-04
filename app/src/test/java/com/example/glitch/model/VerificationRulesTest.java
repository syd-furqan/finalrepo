package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies mapping behavior for verification policy rules.
 */
public class VerificationRulesTest {

    @Test
    public void fromMap_mapsConfiguredValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("enforceIdExpiry", true);
        map.put("bannedIdentifiersCsv", "AA11,BB22");

        VerificationRules rules = VerificationRules.fromMap(map);

        assertTrue(rules.isEnforceIdExpiry());
        assertEquals("AA11,BB22", rules.getBannedIdentifiersCsv());
    }

    @Test
    public void fromMap_usesDefaultsWhenNull() {
        VerificationRules rules = VerificationRules.fromMap(null);
        assertTrue(rules.isEnforceIdExpiry());
        assertEquals("", rules.getBannedIdentifiersCsv());
    }
}
