package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Result of a Tesseract OCR CNIC scan attempt.
 */
public final class CnicScanResult {

    private final boolean success;
    private final String rawOcrText;
    private final String extractedCnic;
    private final String normalizedCnic;
    private final String failureReason;

    private CnicScanResult(
            boolean success,
            @NonNull String rawOcrText,
            @NonNull String extractedCnic,
            @NonNull String normalizedCnic,
            @NonNull String failureReason
    ) {
        this.success = success;
        this.rawOcrText = rawOcrText;
        this.extractedCnic = extractedCnic;
        this.normalizedCnic = normalizedCnic;
        this.failureReason = failureReason;
    }

    @NonNull
    public static CnicScanResult success(
            @NonNull String rawOcrText,
            @NonNull String extractedCnic,
            @NonNull String normalizedCnic
    ) {
        return new CnicScanResult(true, rawOcrText, extractedCnic, normalizedCnic, "");
    }

    @NonNull
    public static CnicScanResult failure(@NonNull String rawOcrText, @NonNull String reason) {
        return new CnicScanResult(false, rawOcrText, "", "", reason);
    }

    public boolean isSuccess() { return success; }

    @NonNull public String getRawOcrText() { return rawOcrText; }
    @NonNull public String getExtractedCnic() { return extractedCnic; }
    @NonNull public String getNormalizedCnic() { return normalizedCnic; }
    @NonNull public String getFailureReason() { return failureReason; }
}
