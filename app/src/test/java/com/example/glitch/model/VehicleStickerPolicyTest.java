package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VehicleStickerPolicyTest {

    @Test
    public void facultyAlwaysMapsToFc() {
        assertEquals("FC", VehicleStickerPolicy.resolveStickerType("faculty", ""));
    }

    @Test
    public void studentCategoriesMapToExpectedStickers() {
        assertEquals("MP", VehicleStickerPolicy.resolveStickerType("student", "day_scholar"));
        assertEquals("RS", VehicleStickerPolicy.resolveStickerType("student", "hostelite"));
        assertEquals("REDC", VehicleStickerPolicy.resolveStickerType("student", "redc"));
    }

    @Test
    public void unknownStudentCategoryReturnsEmptySticker() {
        assertEquals("", VehicleStickerPolicy.resolveStickerType("student", ""));
        assertFalse(VehicleStickerPolicy.isSupportedStudentCategory(""));
    }

    @Test
    public void normalizationSupportsCommonAliases() {
        assertEquals("day_scholar", VehicleStickerPolicy.normalizeStudentCategory("Day Scholar"));
        assertEquals("hostelite", VehicleStickerPolicy.normalizeStudentCategory("hostelite student"));
        assertTrue(VehicleStickerPolicy.isSupportedStudentCategory("redc program"));
    }
}
