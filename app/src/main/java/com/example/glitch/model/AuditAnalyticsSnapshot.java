package com.example.glitch.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated analytics payload for admin traffic and audit insight surfaces.
 */
public class AuditAnalyticsSnapshot {
    private final AnalyticsPeriod period;
    private final int incoming;
    private final int admitted;
    private final int denied;
    private final int exited;
    private final int overdue;
    private final int reported;
    private final int alerts;
    private final int totalEvents;
    private final double denyRate;
    private final double reportRate;
    private final List<TrendBucket> trendBuckets;
    private final List<NamedCount> sponsorRoleMix;
    private final List<NamedCount> outcomeMix;
    private final List<NamedCount> topGuards;
    private final List<NamedCount> topReasonCodes;

    public AuditAnalyticsSnapshot(
            @NonNull AnalyticsPeriod period,
            int incoming,
            int admitted,
            int denied,
            int exited,
            int overdue,
            int reported,
            int alerts,
            int totalEvents,
            double denyRate,
            double reportRate,
            @NonNull List<TrendBucket> trendBuckets,
            @NonNull List<NamedCount> sponsorRoleMix,
            @NonNull List<NamedCount> outcomeMix,
            @NonNull List<NamedCount> topGuards,
            @NonNull List<NamedCount> topReasonCodes
    ) {
        this.period = period;
        this.incoming = incoming;
        this.admitted = admitted;
        this.denied = denied;
        this.exited = exited;
        this.overdue = overdue;
        this.reported = reported;
        this.alerts = alerts;
        this.totalEvents = totalEvents;
        this.denyRate = denyRate;
        this.reportRate = reportRate;
        this.trendBuckets = Collections.unmodifiableList(new ArrayList<>(trendBuckets));
        this.sponsorRoleMix = Collections.unmodifiableList(new ArrayList<>(sponsorRoleMix));
        this.outcomeMix = Collections.unmodifiableList(new ArrayList<>(outcomeMix));
        this.topGuards = Collections.unmodifiableList(new ArrayList<>(topGuards));
        this.topReasonCodes = Collections.unmodifiableList(new ArrayList<>(topReasonCodes));
    }

    @NonNull
    public AnalyticsPeriod getPeriod() {
        return period;
    }

    public int getIncoming() {
        return incoming;
    }

    public int getAdmitted() {
        return admitted;
    }

    public int getDenied() {
        return denied;
    }

    public int getExited() {
        return exited;
    }

    public int getOverdue() {
        return overdue;
    }

    public int getReported() {
        return reported;
    }

    public int getAlerts() {
        return alerts;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public double getDenyRate() {
        return denyRate;
    }

    public double getReportRate() {
        return reportRate;
    }

    @NonNull
    public List<TrendBucket> getTrendBuckets() {
        return trendBuckets;
    }

    @NonNull
    public List<NamedCount> getSponsorRoleMix() {
        return sponsorRoleMix;
    }

    @NonNull
    public List<NamedCount> getOutcomeMix() {
        return outcomeMix;
    }

    @NonNull
    public List<NamedCount> getTopGuards() {
        return topGuards;
    }

    @NonNull
    public List<NamedCount> getTopReasonCodes() {
        return topReasonCodes;
    }

    public static class TrendBucket {
        private final String label;
        private final int count;

        public TrendBucket(@NonNull String label, int count) {
            this.label = label;
            this.count = count;
        }

        @NonNull
        public String getLabel() {
            return label;
        }

        public int getCount() {
            return count;
        }
    }

    public static class NamedCount {
        private final String name;
        private final int count;

        public NamedCount(@NonNull String name, int count) {
            this.name = name;
            this.count = count;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }
    }
}
