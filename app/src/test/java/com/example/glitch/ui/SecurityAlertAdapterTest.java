package com.example.glitch.ui;

import static org.junit.Assert.assertEquals;

import com.example.glitch.R;
import com.example.glitch.model.SecurityAlert;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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

    @Test
    public void labelsImportantAlertTypes() {
        assertEquals("Entry Report", SecurityAlertAdapter.labelForAlert(alert("entry_report")));
        assertEquals("Violation Report", SecurityAlertAdapter.labelForAlert(alert("manual_violation")));
        assertEquals("Vehicle Review", SecurityAlertAdapter.labelForAlert(alert("vehicle_review")));
        assertEquals("Charge Review", SecurityAlertAdapter.labelForAlert(alert("charge_review")));
        assertEquals("Scan Risk", SecurityAlertAdapter.labelForAlert(alert("scan_risk")));
    }

    @Test
    public void alertParsesSubjectStudentId() {
        Map<String, Object> map = new HashMap<>();
        map.put("alertType", "manual_violation");
        map.put("subjectStudentId", "STU-123");

        SecurityAlert alert = SecurityAlert.fromMap("alert-1", map);

        assertEquals("STU-123", alert.getSubjectStudentId());
    }

    private SecurityAlert alert(String type) {
        Map<String, Object> map = new HashMap<>();
        map.put("alertType", type);
        return SecurityAlert.fromMap(type + "-id", map);
    }
}
