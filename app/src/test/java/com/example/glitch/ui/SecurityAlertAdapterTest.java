package com.example.glitch.ui;

import static org.junit.Assert.assertEquals;

import com.example.glitch.R;

import org.junit.Test;

/**
 * Unit tests for security alert severity chip mapping.
 */
public class SecurityAlertAdapterTest {

    @Test
    public void criticalSeverityUsesCriticalStyle() {
        SecurityAlertAdapter.ChipStyle style = SecurityAlertAdapter.resolveSeverityStyle("critical");
        assertEquals(R.drawable.bg_chip_alert_critical, style.backgroundRes);
        assertEquals(R.color.danger_red, style.textColorRes);
    }

    @Test
    public void warningSeverityUsesWarningStyle() {
        SecurityAlertAdapter.ChipStyle style = SecurityAlertAdapter.resolveSeverityStyle("warning");
        assertEquals(R.drawable.bg_chip_alert_warning, style.backgroundRes);
        assertEquals(R.color.primary_navy, style.textColorRes);
    }

    @Test
    public void lowSeverityUsesDefaultStyle() {
        SecurityAlertAdapter.ChipStyle style = SecurityAlertAdapter.resolveSeverityStyle("low");
        assertEquals(R.drawable.bg_chip_role, style.backgroundRes);
        assertEquals(R.color.primary_navy, style.textColorRes);
    }
}
