package com.example.glitch.model;

import androidx.annotation.NonNull;

/**
 * In-memory export payload used by admin audit sharing actions.
 */
public class AuditExportFile {
    private final String fileName;
    private final String mimeType;
    private final byte[] content;

    public AuditExportFile(
            @NonNull String fileName,
            @NonNull String mimeType,
            @NonNull byte[] content
    ) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.content = content;
    }

    @NonNull
    public String getFileName() {
        return fileName;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    @NonNull
    public byte[] getContent() {
        return content;
    }
}
