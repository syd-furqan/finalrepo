package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.AnalyticsPeriod;
import com.example.glitch.model.AuditAnalyticsSnapshot;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreAuditAnalyticsRepository implements AuditAnalyticsRepository {
    private final FirebaseFirestore firestore;

    public FirestoreAuditAnalyticsRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreAuditAnalyticsRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void loadSnapshot(@NonNull AnalyticsPeriod period, @NonNull SnapshotListener listener) {
        long now = System.currentTimeMillis();
        long from = period.getFromMillis(now);
        Timestamp fromTs = new Timestamp(new Date(from));
        Timestamp toTs = new Timestamp(new Date(now));

        Task<QuerySnapshot> entryTask = firestore.collection("entry_requests")
                .whereGreaterThanOrEqualTo("createdAt", fromTs)
                .whereLessThanOrEqualTo("createdAt", toTs)
                .get();

        Task<QuerySnapshot> accessTask = firestore.collection("access_events")
                .whereGreaterThanOrEqualTo("createdAt", fromTs)
                .whereLessThanOrEqualTo("createdAt", toTs)
                .get();

        Task<QuerySnapshot> alertTask = firestore.collection("alerts")
                .whereGreaterThanOrEqualTo("createdAt", fromTs)
                .whereLessThanOrEqualTo("createdAt", toTs)
                .get();

        Tasks.whenAllComplete(entryTask, accessTask, alertTask)
                .addOnSuccessListener(ignored -> {
                    try {
                        if (!entryTask.isSuccessful()) {
                            throw entryTask.getException() == null
                                    ? new IllegalStateException("Unable to load entry analytics.")
                                    : entryTask.getException();
                        }
                        if (!accessTask.isSuccessful()) {
                            throw accessTask.getException() == null
                                    ? new IllegalStateException("Unable to load access analytics.")
                                    : accessTask.getException();
                        }
                        if (!alertTask.isSuccessful()) {
                            throw alertTask.getException() == null
                                    ? new IllegalStateException("Unable to load alert analytics.")
                                    : alertTask.getException();
                        }
                        QuerySnapshot entries = entryTask.getResult();
                        QuerySnapshot access = accessTask.getResult();
                        QuerySnapshot alerts = alertTask.getResult();
                        if (entries == null || access == null || alerts == null) {
                            throw new IllegalStateException("Analytics query returned empty result.");
                        }
                        listener.onData(buildSnapshot(period, from, now, entries, access, alerts));
                    } catch (Exception exception) {
                        listener.onError(exception);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    @NonNull
    private AuditAnalyticsSnapshot buildSnapshot(
            @NonNull AnalyticsPeriod period,
            long fromMillis,
            long toMillis,
            @NonNull QuerySnapshot entries,
            @NonNull QuerySnapshot accessEvents,
            @NonNull QuerySnapshot alerts
    ) {
        int incoming = entries.size();
        int admitted = 0;
        int denied = 0;
        int exited = 0;
        int overdue = 0;
        int reported = 0;

        Map<String, Integer> byRole = new HashMap<>();
        Map<String, Integer> outcomeMixMap = new HashMap<>();

        for (DocumentSnapshot doc : entries.getDocuments()) {
            String status = safeLower(doc.get("status"));
            String requesterRole = safeLower(doc.get("requesterRole"));
            increment(byRole, requesterRole);
            increment(outcomeMixMap, status);
            if ("active".equals(status) || "overdue".equals(status) || "reported".equals(status) || "exited".equals(status)) {
                admitted++;
            }
            if ("denied".equals(status)) {
                denied++;
            }
            if ("exited".equals(status)) {
                exited++;
            }
            if ("overdue".equals(status)) {
                overdue++;
            }
            if ("reported".equals(status)) {
                reported++;
            }
        }

        int bucketCount = period.getBucketCount();
        long span = Math.max(1L, (toMillis - fromMillis) / bucketCount);
        int[] trendCounts = new int[bucketCount];
        Map<String, Integer> byGuard = new HashMap<>();
        Map<String, Integer> byReason = new HashMap<>();

        for (DocumentSnapshot doc : accessEvents.getDocuments()) {
            Timestamp createdAt = doc.getTimestamp("createdAt");
            if (createdAt != null) {
                long ms = createdAt.toDate().getTime();
                int idx = (int) ((ms - fromMillis) / span);
                if (idx < 0) {
                    idx = 0;
                } else if (idx >= bucketCount) {
                    idx = bucketCount - 1;
                }
                trendCounts[idx]++;
            }
            String actorRole = safeLower(doc.get("actorRole"));
            if ("guard".equals(actorRole)) {
                String guardUid = safe(doc.get("actorUid"));
                if (!"unknown".equalsIgnoreCase(guardUid)) {
                    increment(byGuard, guardUid);
                }
            }
            String reasonCode = safeLower(doc.get("reasonCode"));
            if (!"unknown".equals(reasonCode)) {
                increment(byReason, reasonCode);
            }
        }

        List<AuditAnalyticsSnapshot.TrendBucket> trendBuckets = buildTrendBuckets(
                period,
                fromMillis,
                span,
                trendCounts
        );

        double denyRate = incoming == 0 ? 0d : ((double) denied / (double) incoming) * 100d;
        double reportRate = incoming == 0 ? 0d : ((double) reported / (double) incoming) * 100d;

        return new AuditAnalyticsSnapshot(
                period,
                incoming,
                admitted,
                denied,
                exited,
                overdue,
                reported,
                alerts.size(),
                accessEvents.size(),
                denyRate,
                reportRate,
                trendBuckets,
                sortNamedCounts(byRole, 8, true),
                sortNamedCounts(outcomeMixMap, 8, false),
                sortNamedCounts(byGuard, 5, true),
                sortNamedCounts(byReason, 5, false)
        );
    }

    @NonNull
    private List<AuditAnalyticsSnapshot.TrendBucket> buildTrendBuckets(
            @NonNull AnalyticsPeriod period,
            long fromMillis,
            long spanMillis,
            @NonNull int[] counts
    ) {
        List<AuditAnalyticsSnapshot.TrendBucket> buckets = new ArrayList<>();
        SimpleDateFormat format;
        if (period == AnalyticsPeriod.DAILY) {
            format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else if (period == AnalyticsPeriod.WEEKLY) {
            format = new SimpleDateFormat("EEE", Locale.getDefault());
        } else {
            format = new SimpleDateFormat("dd MMM", Locale.getDefault());
        }
        for (int i = 0; i < counts.length; i++) {
            long pointMillis = fromMillis + (i * spanMillis);
            String label = format.format(new Date(pointMillis));
            buckets.add(new AuditAnalyticsSnapshot.TrendBucket(label, counts[i]));
        }
        return buckets;
    }

    @NonNull
    private List<AuditAnalyticsSnapshot.NamedCount> sortNamedCounts(
            @NonNull Map<String, Integer> source,
            int limit,
            boolean unknownAsUid
    ) {
        if (source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(source.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<AuditAnalyticsSnapshot.NamedCount> out = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            if (out.size() >= limit) {
                break;
            }
            String key = entry.getKey();
            if (key.trim().isEmpty()) {
                key = unknownAsUid ? "unknown_guard" : "unknown";
            }
            out.add(new AuditAnalyticsSnapshot.NamedCount(key, entry.getValue()));
        }
        return out;
    }

    private void increment(@NonNull Map<String, Integer> map, @NonNull String key) {
        String normalizedKey = key.trim().isEmpty() ? "unknown" : key.trim();
        map.put(normalizedKey, map.getOrDefault(normalizedKey, 0) + 1);
    }

    @NonNull
    private String safe(Object value) {
        String parsed = value == null ? "" : String.valueOf(value).trim();
        return parsed.isEmpty() ? "unknown" : parsed;
    }

    @NonNull
    private String safeLower(Object value) {
        return safe(value).toLowerCase(Locale.getDefault());
    }
}
