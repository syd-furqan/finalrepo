package com.example.glitch.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for single-gate backfill helper logic.
 */
public class SingleGateMigrationRunnerTest {

    @Test
    public void safeString_trimsAndDefaultsToEmpty() {
        assertEquals("", SingleGateMigrationRunner.safeString(null));
        assertEquals("in-gate", SingleGateMigrationRunner.safeString("  in-gate  "));
        assertEquals("123", SingleGateMigrationRunner.safeString(123));
    }

    @Test
    public void shouldBackfillGateLabel_onlyForNonCanonicalValues() {
        assertFalse(SingleGateMigrationRunner.shouldBackfillGateLabel("in-gate"));
        assertFalse(SingleGateMigrationRunner.shouldBackfillGateLabel(" IN-GATE "));
        assertTrue(SingleGateMigrationRunner.shouldBackfillGateLabel(""));
        assertTrue(SingleGateMigrationRunner.shouldBackfillGateLabel("Main Gate"));
        assertTrue(SingleGateMigrationRunner.shouldBackfillGateLabel(null));
    }
}
