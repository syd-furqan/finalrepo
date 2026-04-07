package com.example.glitch.ui;

import static org.junit.Assert.assertEquals;

import com.example.glitch.R;

import org.junit.Test;

/**
 * Unit tests for vehicle request status-chip style mapping.
 */
public class VehicleRequestAdapterTest {

    @Test
    public void approvedStatusUsesSuccessChip() {
        VehicleRequestAdapter.ChipStyle style = VehicleRequestAdapter.resolveStatusStyle("approved");
        assertEquals(R.drawable.bg_chip_success, style.backgroundRes);
        assertEquals(R.color.success_green, style.textColorRes);
    }

    @Test
    public void deniedStatusUsesCriticalChip() {
        VehicleRequestAdapter.ChipStyle style = VehicleRequestAdapter.resolveStatusStyle("denied");
        assertEquals(R.drawable.bg_chip_alert_critical, style.backgroundRes);
        assertEquals(R.color.danger_red, style.textColorRes);
    }

    @Test
    public void unknownStatusUsesDefaultChip() {
        VehicleRequestAdapter.ChipStyle style = VehicleRequestAdapter.resolveStatusStyle("processing");
        assertEquals(R.drawable.bg_chip_role, style.backgroundRes);
        assertEquals(R.color.primary_navy, style.textColorRes);
    }
}
