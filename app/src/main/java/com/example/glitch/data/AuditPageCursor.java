package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Opaque cursor token for paged audit-list reads.
 */
public final class AuditPageCursor {
    @Nullable
    private final DocumentSnapshot lastDocument;
    private final boolean exhausted;

    private AuditPageCursor(@Nullable DocumentSnapshot lastDocument, boolean exhausted) {
        this.lastDocument = lastDocument;
        this.exhausted = exhausted;
    }

    @NonNull
    public static AuditPageCursor initial() {
        return new AuditPageCursor(null, false);
    }

    @NonNull
    static AuditPageCursor of(@Nullable DocumentSnapshot lastDocument, boolean exhausted) {
        return new AuditPageCursor(lastDocument, exhausted);
    }

    @Nullable
    DocumentSnapshot getLastDocument() {
        return lastDocument;
    }

    public boolean isExhausted() {
        return exhausted;
    }
}
