package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.AnalyticsPeriod;
import com.example.glitch.model.AuditAnalyticsSnapshot;

public interface AuditAnalyticsRepository {
    void loadSnapshot(@NonNull AnalyticsPeriod period, @NonNull SnapshotListener listener);

    interface SnapshotListener {
        void onData(@NonNull AuditAnalyticsSnapshot snapshot);

        void onError(@NonNull Exception exception);
    }
}
