package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Metadata for a vehicle-application document uploaded to Firebase Storage.
 */
public class VehicleDocumentRef {
    private final String name;
    private final String mimeType;
    private final String storagePath;
    private final String downloadUrl;
    private final String bucket;

    public VehicleDocumentRef(
            @NonNull String name,
            @NonNull String mimeType,
            @NonNull String storagePath,
            @NonNull String downloadUrl
    ) {
        this(name, mimeType, storagePath, downloadUrl, "");
    }

    public VehicleDocumentRef(
            @NonNull String name,
            @NonNull String mimeType,
            @NonNull String storagePath,
            @NonNull String downloadUrl,
            @NonNull String bucket
    ) {
        this.name = name;
        this.mimeType = mimeType;
        this.storagePath = storagePath;
        this.downloadUrl = downloadUrl;
        this.bucket = bucket;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    @NonNull
    public String getStoragePath() {
        return storagePath;
    }

    @NonNull
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @NonNull
    public String getBucket() {
        return bucket;
    }

    @NonNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("mimeType", mimeType);
        map.put("storagePath", storagePath);
        map.put("downloadUrl", downloadUrl);
        map.put("bucket", bucket);
        return map;
    }

    @NonNull
    public static VehicleDocumentRef fromMap(@Nullable Object raw) {
        if (!(raw instanceof Map)) {
            return new VehicleDocumentRef("", "", "", "");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) raw;
        return new VehicleDocumentRef(
                asString(map.get("name")),
                asString(map.get("mimeType")),
                asString(map.get("storagePath")),
                asString(map.get("downloadUrl")),
                asString(map.get("bucket"))
        );
    }

    @NonNull
    private static String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
