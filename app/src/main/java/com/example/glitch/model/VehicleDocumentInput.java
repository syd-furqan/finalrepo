package com.example.glitch.model;

import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * Client-side selection for a document that should be uploaded with a vehicle application.
 */
public class VehicleDocumentInput {
    private final Uri contentUri;
    private final String displayName;

    public VehicleDocumentInput(@NonNull Uri contentUri, @NonNull String displayName) {
        this.contentUri = contentUri;
        this.displayName = displayName;
    }

    @NonNull
    public Uri getContentUri() {
        return contentUri;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }
}
