package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for single-gate normalization policy.
 */
public class GatePolicyTest {

    @Test
    public void normalizeStoredValue_alwaysReturnsCanonicalValue() {
        assertEquals(GatePolicy.STORED_VALUE, GatePolicy.normalizeStoredValue(null));
        assertEquals(GatePolicy.STORED_VALUE, GatePolicy.normalizeStoredValue(""));
        assertEquals(GatePolicy.STORED_VALUE, GatePolicy.normalizeStoredValue("Main Gate"));
        assertEquals(GatePolicy.STORED_VALUE, GatePolicy.normalizeStoredValue("Gate North"));
    }

    @Test
    public void isCanonicalStoredValue_matchesOnlyCanonicalForm() {
        assertTrue(GatePolicy.isCanonicalStoredValue("in-gate"));
        assertTrue(GatePolicy.isCanonicalStoredValue(" IN-GATE "));
        assertFalse(GatePolicy.isCanonicalStoredValue("main gate"));
        assertFalse(GatePolicy.isCanonicalStoredValue(""));
        assertFalse(GatePolicy.isCanonicalStoredValue(null));
    }

    @Test
    public void toDisplayLabel_alwaysReturnsInGate() {
        assertEquals(GatePolicy.DISPLAY_LABEL, GatePolicy.toDisplayLabel(null));
        assertEquals(GatePolicy.DISPLAY_LABEL, GatePolicy.toDisplayLabel("in-gate"));
        assertEquals(GatePolicy.DISPLAY_LABEL, GatePolicy.toDisplayLabel("gate 2"));
    }
}
