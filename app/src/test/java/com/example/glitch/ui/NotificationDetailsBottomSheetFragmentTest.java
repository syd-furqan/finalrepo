package com.example.glitch.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NotificationDetailsBottomSheetFragmentTest {
    @Test
    public void isSupportedSource_acceptsKnownSourceCollections() {
        assertTrue(NotificationDetailsBottomSheetFragment.isSupportedSource("guest_passes"));
        assertTrue(NotificationDetailsBottomSheetFragment.isSupportedSource("vehicle_requests"));
        assertTrue(NotificationDetailsBottomSheetFragment.isSupportedSource("fine_cases"));
        assertTrue(NotificationDetailsBottomSheetFragment.isSupportedSource("violation_reports"));
    }

    @Test
    public void isSupportedSource_rejectsUnknownOrBlankSources() {
        assertFalse(NotificationDetailsBottomSheetFragment.isSupportedSource(""));
        assertFalse(NotificationDetailsBottomSheetFragment.isSupportedSource("alerts"));
    }
}
