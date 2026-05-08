package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin data-driven analytics dashboard for traffic and security signals.
 */
public class AdminTrafficAnalyticsFragment extends Fragment {
    private FirebaseFirestore firestore;

    private TextView textIncoming;
    private TextView textAdmitted;
    private TextView textDenied;
    private TextView textExited;
    private TextView textOverdue;
    private TextView textReported;
    private TextView textAlerts;

    private LinearLayout containerTrends;
    private LinearLayout containerGuardBreakdown;
    private LinearLayout containerRoleBreakdown;

    private int rangeDays = 7;

    @NonNull
    public static AdminTrafficAnalyticsFragment newInstance() {
        return new AdminTrafficAnalyticsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_traffic_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (AuthUiGuard.requireProfile(this) == null) {
            return;
        }
        firestore = FirebaseFirestore.getInstance();

        textIncoming = view.findViewById(R.id.text_kpi_incoming);
        textAdmitted = view.findViewById(R.id.text_kpi_admitted);
        textDenied = view.findViewById(R.id.text_kpi_denied);
        textExited = view.findViewById(R.id.text_kpi_exited);
        textOverdue = view.findViewById(R.id.text_kpi_overdue);
        textReported = view.findViewById(R.id.text_kpi_reported);
        textAlerts = view.findViewById(R.id.text_kpi_alerts);
        containerTrends = view.findViewById(R.id.container_trends);
        containerGuardBreakdown = view.findViewById(R.id.container_guard_breakdown);
        containerRoleBreakdown = view.findViewById(R.id.container_role_breakdown);

        Chip chip7d = view.findViewById(R.id.chip_analytics_7d);
        Chip chip30d = view.findViewById(R.id.chip_analytics_30d);
        Chip chip90d = view.findViewById(R.id.chip_analytics_90d);
        MaterialButton buttonDrilldown = view.findViewById(R.id.button_open_audit_drilldown);

        chip7d.setOnClickListener(v -> {
            rangeDays = 7;
            loadAnalytics();
        });
        chip30d.setOnClickListener(v -> {
            rangeDays = 30;
            loadAnalytics();
        });
        chip90d.setOnClickListener(v -> {
            rangeDays = 90;
            loadAnalytics();
        });
        buttonDrilldown.setOnClickListener(v -> RoleNavRouter.route(this, RoleDestination.AUDIT));

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.ADMIN_ANALYTICS);
        loadAnalytics();
    }

    private void loadAnalytics() {
        long now = System.currentTimeMillis();
        long from = now - (rangeDays * 24L * 60L * 60L * 1000L);
        Timestamp fromTs = new Timestamp(new java.util.Date(from));
        Timestamp toTs = new Timestamp(new java.util.Date(now));

        firestore.collection("entry_requests")
                .whereGreaterThanOrEqualTo("createdAt", fromTs)
                .whereLessThanOrEqualTo("createdAt", toTs)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int incoming = snapshot.size();
                    int admitted = 0;
                    int denied = 0;
                    int exited = 0;
                    int overdue = 0;
                    int reported = 0;
                    Map<String, Integer> byRole = new HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = safeLower(doc.get("status"));
                        String requesterRole = safeLower(doc.get("requesterRole"));
                        byRole.put(requesterRole, byRole.getOrDefault(requesterRole, 0) + 1);
                        if ("active".equals(status) || "overdue".equals(status) || "reported".equals(status) || "exited".equals(status)) {
                            admitted++;
                        }
                        if ("denied".equals(status)) denied++;
                        if ("exited".equals(status)) exited++;
                        if ("overdue".equals(status)) overdue++;
                        if ("reported".equals(status)) reported++;
                    }
                    textIncoming.setText(String.valueOf(incoming));
                    textAdmitted.setText(String.valueOf(admitted));
                    textDenied.setText(String.valueOf(denied));
                    textExited.setText(String.valueOf(exited));
                    textOverdue.setText(String.valueOf(overdue));
                    textReported.setText(String.valueOf(reported));
                    renderRoleBreakdown(byRole);
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) return;
                    Snackbar.make(requireView(), "Failed to load entry analytics.", Snackbar.LENGTH_LONG).show();
                });

        firestore.collection("alerts")
                .whereEqualTo("alertType", "entry_report")
                .whereGreaterThanOrEqualTo("createdAt", fromTs)
                .whereLessThanOrEqualTo("createdAt", toTs)
                .get()
                .addOnSuccessListener(snapshot -> textAlerts.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(error -> textAlerts.setText("0"));

        loadTrends(from, now);
        loadGuardBreakdown(fromTs, toTs);
    }

    private void loadTrends(long fromMillis, long toMillis) {
        firestore.collection("access_events")
                .whereGreaterThanOrEqualTo("createdAt", new Timestamp(new java.util.Date(fromMillis)))
                .whereLessThanOrEqualTo("createdAt", new Timestamp(new java.util.Date(toMillis)))
                .get()
                .addOnSuccessListener(snapshot -> {
                    int buckets = Math.min(rangeDays, 14);
                    long bucketSpan = Math.max(1L, (toMillis - fromMillis) / buckets);
                    int[] counts = new int[buckets];
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("createdAt");
                        if (ts == null) continue;
                        long ms = ts.toDate().getTime();
                        int idx = (int) ((ms - fromMillis) / bucketSpan);
                        if (idx < 0) idx = 0;
                        if (idx >= buckets) idx = buckets - 1;
                        counts[idx]++;
                    }
                    renderTrendBars(counts);
                });
    }

    private void renderTrendBars(int[] counts) {
        if (!isAdded()) return;
        containerTrends.removeAllViews();
        int max = 1;
        for (int count : counts) {
            if (count > max) max = count;
        }
        for (int i = 0; i < counts.length; i++) {
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_analytics_bar, containerTrends, false);
            TextView label = row.findViewById(R.id.text_bar_label);
            View fill = row.findViewById(R.id.view_bar_fill);
            TextView value = row.findViewById(R.id.text_bar_value);
            label.setText("P" + (i + 1));
            value.setText(String.valueOf(counts[i]));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) fill.getLayoutParams();
            params.weight = (float) counts[i] / (float) max;
            if (params.weight <= 0f) params.weight = 0.02f;
            fill.setLayoutParams(params);
            containerTrends.addView(row);
        }
    }

    private void loadGuardBreakdown(Timestamp fromTs, Timestamp toTs) {
        firestore.collection("access_events")
                .whereGreaterThanOrEqualTo("createdAt", fromTs)
                .whereLessThanOrEqualTo("createdAt", toTs)
                .whereEqualTo("actorRole", "guard")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Integer> byGuard = new HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String guard = safe(doc.get("actorUid"));
                        byGuard.put(guard, byGuard.getOrDefault(guard, 0) + 1);
                    }
                    renderSimpleBreakdown(containerGuardBreakdown, byGuard, "No guard events in range.");
                });
    }

    private void renderRoleBreakdown(@NonNull Map<String, Integer> byRole) {
        renderSimpleBreakdown(containerRoleBreakdown, byRole, "No role breakdown data.");
    }

    private void renderSimpleBreakdown(@NonNull LinearLayout container, @NonNull Map<String, Integer> map, @NonNull String emptyMessage) {
        if (!isAdded()) return;
        container.removeAllViews();
        if (map.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(emptyMessage);
            container.addView(empty);
            return;
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (Map.Entry<String, Integer> entry : entries) {
            TextView row = new TextView(requireContext());
            row.setText(entry.getKey() + " : " + entry.getValue());
            row.setTextSize(14f);
            container.addView(row);
        }
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
