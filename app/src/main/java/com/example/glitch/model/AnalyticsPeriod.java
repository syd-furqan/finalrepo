package com.example.glitch.model;

import androidx.annotation.NonNull;

/**
 * Rolling analytics periods used by audit and traffic dashboards.
 */
public enum AnalyticsPeriod {
    DAILY(24, 24L * 60L * 60L * 1000L),
    WEEKLY(7, 7L * 24L * 60L * 60L * 1000L),
    MONTHLY(30, 30L * 24L * 60L * 60L * 1000L);

    private final int bucketCount;
    private final long rangeMillis;

    AnalyticsPeriod(int bucketCount, long rangeMillis) {
        this.bucketCount = bucketCount;
        this.rangeMillis = rangeMillis;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public long getRangeMillis() {
        return rangeMillis;
    }

    public long getFromMillis(long nowMillis) {
        return nowMillis - rangeMillis;
    }

    @NonNull
    public String getLabel() {
        switch (this) {
            case DAILY:
                return "Daily";
            case WEEKLY:
                return "Weekly";
            case MONTHLY:
            default:
                return "Monthly";
        }
    }
}
