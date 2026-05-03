package com.example.glitch.ui;

import static org.junit.Assert.assertEquals;

import com.example.glitch.R;

import org.junit.Test;

/**
 * Unit tests for guest-pass status chip mapping.
 */
public class GuestPassAdapterTest {

    @Test
    public void activeStatusUsesSuccessChip() {
        GuestPassAdapter.ChipStyle style = GuestPassAdapter.resolveStatusStyle("active");
        assertEquals(R.drawable.bg_chip_success, style.backgroundRes);
        assertEquals(R.color.success_green, style.textColorRes);
    }

    @Test
    public void revokedStatusUsesCriticalChip() {
        GuestPassAdapter.ChipStyle style = GuestPassAdapter.resolveStatusStyle("revoked");
        assertEquals(R.drawable.bg_chip_alert_critical, style.backgroundRes);
        assertEquals(R.color.danger_red, style.textColorRes);
    }

    @Test
    public void expiredStatusUsesCriticalChip() {
        GuestPassAdapter.ChipStyle style = GuestPassAdapter.resolveStatusStyle("expired");
        assertEquals(R.drawable.bg_chip_alert_critical, style.backgroundRes);
        assertEquals(R.color.danger_red, style.textColorRes);
    }

    @Test
    public void deniedStatusUsesCriticalChip() {
        GuestPassAdapter.ChipStyle style = GuestPassAdapter.resolveStatusStyle("denied");
        assertEquals(R.drawable.bg_chip_alert_critical, style.backgroundRes);
        assertEquals(R.color.danger_red, style.textColorRes);
    }

    @Test
    public void fallbackStatusUsesDefaultChip() {
        GuestPassAdapter.ChipStyle style = GuestPassAdapter.resolveStatusStyle("queued");
        assertEquals(R.drawable.bg_chip_role, style.backgroundRes);
        assertEquals(R.color.primary_navy, style.textColorRes);
    }

    @Test
    public void usedStatusUsesMutedChipColor() {
        GuestPassAdapter.ChipStyle style = GuestPassAdapter.resolveStatusStyle("used");
        assertEquals(R.drawable.bg_chip_role, style.backgroundRes);
        assertEquals(R.color.nav_unselected, style.textColorRes);
    }
}
