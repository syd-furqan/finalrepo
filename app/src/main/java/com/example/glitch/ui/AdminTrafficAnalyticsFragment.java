package com.example.glitch.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.data.AuditAnalyticsRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.AnalyticsPeriod;
import com.example.glitch.model.AuditAnalyticsSnapshot;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Admin data-driven analytics dashboard for traffic and security signals.
 */
public class AdminTrafficAnalyticsFragment extends Fragment {
    private AuditAnalyticsRepository analyticsRepository;

    private TextView textIncoming;
    private TextView textAdmitted;
    private TextView textDenied;
    private TextView textExited;
    private TextView textOverdue;
    private TextView textReported;
    private TextView textAlerts;
    private TextView textRates;
    private LinearLayout containerTopReasonCodes;

    private LineChart chartTrend;
    private BarChart chartOutcomes;
    private PieChart chartRoleMix;
    private HorizontalBarChart chartTopGuards;

    private Chip chipDaily;
    private Chip chipWeekly;
    private Chip chipMonthly;

    private AnalyticsPeriod period = AnalyticsPeriod.WEEKLY;

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
        analyticsRepository = RepositoryProvider.getAuditAnalyticsRepository();

        textIncoming = view.findViewById(R.id.text_kpi_incoming);
        textAdmitted = view.findViewById(R.id.text_kpi_admitted);
        textDenied = view.findViewById(R.id.text_kpi_denied);
        textExited = view.findViewById(R.id.text_kpi_exited);
        textOverdue = view.findViewById(R.id.text_kpi_overdue);
        textReported = view.findViewById(R.id.text_kpi_reported);
        textAlerts = view.findViewById(R.id.text_kpi_alerts);
        textRates = view.findViewById(R.id.text_kpi_rates);
        containerTopReasonCodes = view.findViewById(R.id.container_top_reason_codes);
        chartTrend = view.findViewById(R.id.chart_trend);
        chartOutcomes = view.findViewById(R.id.chart_outcomes);
        chartRoleMix = view.findViewById(R.id.chart_role_mix);
        chartTopGuards = view.findViewById(R.id.chart_top_guards);

        chipDaily = view.findViewById(R.id.chip_analytics_daily);
        chipWeekly = view.findViewById(R.id.chip_analytics_weekly);
        chipMonthly = view.findViewById(R.id.chip_analytics_monthly);
        MaterialButton buttonDrilldown = view.findViewById(R.id.button_open_audit_drilldown);

        chipDaily.setOnClickListener(v -> switchPeriod(AnalyticsPeriod.DAILY));
        chipWeekly.setOnClickListener(v -> switchPeriod(AnalyticsPeriod.WEEKLY));
        chipMonthly.setOnClickListener(v -> switchPeriod(AnalyticsPeriod.MONTHLY));
        buttonDrilldown.setOnClickListener(v -> RoleNavRouter.route(this, RoleDestination.AUDIT));

        configureCharts();
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.ADMIN_ANALYTICS);
        switchPeriod(period);
    }

    private void switchPeriod(@NonNull AnalyticsPeriod nextPeriod) {
        period = nextPeriod;
        updatePeriodChipState();
        loadAnalytics();
    }

    private void updatePeriodChipState() {
        updateChipSelected(chipDaily, period == AnalyticsPeriod.DAILY);
        updateChipSelected(chipWeekly, period == AnalyticsPeriod.WEEKLY);
        updateChipSelected(chipMonthly, period == AnalyticsPeriod.MONTHLY);
    }

    private void updateChipSelected(@NonNull Chip chip, boolean selected) {
        chip.setChipBackgroundColorResource(selected ? R.color.md_primary_container : R.color.surface_alt);
        chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.primary_navy : R.color.text_muted));
    }

    private void configureCharts() {
        setupLineChart(chartTrend);
        setupBarChart(chartOutcomes);
        setupPieChart(chartRoleMix);
        setupHorizontalChart(chartTopGuards);
    }

    private void loadAnalytics() {
        analyticsRepository.loadSnapshot(period, new AuditAnalyticsRepository.SnapshotListener() {
            @Override
            public void onData(@NonNull AuditAnalyticsSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> renderSnapshot(snapshot));
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.analytics_load_failed, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void renderSnapshot(@NonNull AuditAnalyticsSnapshot snapshot) {
        textIncoming.setText(String.valueOf(snapshot.getIncoming()));
        textAdmitted.setText(String.valueOf(snapshot.getAdmitted()));
        textDenied.setText(String.valueOf(snapshot.getDenied()));
        textExited.setText(String.valueOf(snapshot.getExited()));
        textOverdue.setText(getString(R.string.analytics_overdue_template, snapshot.getOverdue()));
        textReported.setText(getString(R.string.analytics_reported_template, snapshot.getReported()));
        textAlerts.setText(String.valueOf(snapshot.getAlerts()));
        textRates.setText(getString(
                R.string.analytics_rates_template,
                formatPercent(snapshot.getDenyRate()),
                formatPercent(snapshot.getReportRate())
        ));

        renderTrendChart(snapshot.getTrendBuckets());
        renderOutcomeChart(snapshot);
        renderRoleMixChart(snapshot.getSponsorRoleMix());
        renderTopGuardsChart(snapshot.getTopGuards());
        renderTopReasonCodes(snapshot.getTopReasonCodes());
    }

    private void renderTrendChart(@NonNull List<AuditAnalyticsSnapshot.TrendBucket> buckets) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            AuditAnalyticsSnapshot.TrendBucket bucket = buckets.get(i);
            entries.add(new Entry(i, bucket.getCount()));
            labels.add(bucket.getLabel());
        }
        LineDataSet set = new LineDataSet(entries, getString(R.string.analytics_trend_legend));
        set.setColor(ContextCompat.getColor(requireContext(), R.color.primary_navy));
        set.setCircleColor(ContextCompat.getColor(requireContext(), R.color.brand_accent));
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setValueTextSize(10f);
        set.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        set.setDrawFilled(true);
        set.setFillColor(ContextCompat.getColor(requireContext(), R.color.md_primary_container));

        LineData data = new LineData(set);
        chartTrend.setData(data);
        chartTrend.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartTrend.getXAxis().setLabelCount(Math.min(labels.size(), 6), false);
        chartTrend.invalidate();
    }

    private void renderOutcomeChart(@NonNull AuditAnalyticsSnapshot snapshot) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, snapshot.getAdmitted()));
        entries.add(new BarEntry(1, snapshot.getDenied()));
        entries.add(new BarEntry(2, snapshot.getExited()));
        entries.add(new BarEntry(3, snapshot.getOverdue()));
        entries.add(new BarEntry(4, snapshot.getReported()));

        BarDataSet set = new BarDataSet(entries, getString(R.string.analytics_outcome_legend));
        set.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.success_green),
                ContextCompat.getColor(requireContext(), R.color.danger_red),
                ContextCompat.getColor(requireContext(), R.color.md_secondary),
                ContextCompat.getColor(requireContext(), R.color.semantic_warning),
                ContextCompat.getColor(requireContext(), R.color.brand_accent)
        });
        set.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        set.setValueTextSize(10f);
        BarData data = new BarData(set);
        data.setBarWidth(0.6f);
        chartOutcomes.setData(data);
        chartOutcomes.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{
                "Admitted", "Denied", "Exited", "Overdue", "Reported"
        }));
        chartOutcomes.invalidate();
    }

    private void renderRoleMixChart(@NonNull List<AuditAnalyticsSnapshot.NamedCount> roleMix) {
        List<PieEntry> entries = new ArrayList<>();
        for (AuditAnalyticsSnapshot.NamedCount item : roleMix) {
            entries.add(new PieEntry(item.getCount(), item.getName()));
        }
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1f, getString(R.string.analytics_no_data)));
        }
        PieDataSet set = new PieDataSet(entries, "");
        set.setSliceSpace(2f);
        set.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.primary_navy),
                ContextCompat.getColor(requireContext(), R.color.md_secondary),
                ContextCompat.getColor(requireContext(), R.color.md_tertiary),
                ContextCompat.getColor(requireContext(), R.color.brand_accent),
                ContextCompat.getColor(requireContext(), R.color.semantic_warning)
        });
        PieData data = new PieData(set);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(10f);
        chartRoleMix.setData(data);
        chartRoleMix.invalidate();
    }

    private void renderTopGuardsChart(@NonNull List<AuditAnalyticsSnapshot.NamedCount> topGuards) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < topGuards.size(); i++) {
            AuditAnalyticsSnapshot.NamedCount item = topGuards.get(i);
            entries.add(new BarEntry(i, item.getCount()));
            labels.add(item.getName());
        }
        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 0));
            labels.add(getString(R.string.analytics_no_data));
        }
        BarDataSet set = new BarDataSet(entries, getString(R.string.analytics_top_guard_legend));
        set.setColor(ContextCompat.getColor(requireContext(), R.color.md_secondary));
        set.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        set.setValueTextSize(10f);
        BarData data = new BarData(set);
        data.setBarWidth(0.5f);
        chartTopGuards.setData(data);
        chartTopGuards.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartTopGuards.invalidate();
    }

    private void renderTopReasonCodes(@NonNull List<AuditAnalyticsSnapshot.NamedCount> topReasonCodes) {
        containerTopReasonCodes.removeAllViews();
        if (topReasonCodes.isEmpty()) {
            TextView text = new TextView(requireContext());
            text.setText(R.string.analytics_no_data);
            text.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            text.setTextSize(13f);
            containerTopReasonCodes.addView(text);
            return;
        }
        for (AuditAnalyticsSnapshot.NamedCount row : topReasonCodes) {
            TextView text = new TextView(requireContext());
            text.setText(getString(R.string.analytics_reason_row_template, row.getName(), row.getCount()));
            text.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            text.setTextSize(13f);
            text.setPadding(0, 0, 0, dp(4));
            containerTopReasonCodes.addView(text);
        }
    }

    private void setupLineChart(@NonNull LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setForm(Legend.LegendForm.LINE);
        chart.setDrawGridBackground(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        chart.getAxisRight().setEnabled(false);
    }

    private void setupBarChart(@NonNull BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        chart.getAxisRight().setEnabled(false);
    }

    private void setupHorizontalChart(@NonNull HorizontalBarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        chart.getAxisRight().setEnabled(false);
    }

    private void setupPieChart(@NonNull PieChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setUsePercentValues(false);
        chart.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
        chart.setEntryLabelTextSize(11f);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
    }

    @NonNull
    private String formatPercent(double value) {
        return String.format(Locale.getDefault(), "%.1f%%", value);
    }

    private int dp(int value) {
        return (int) (value * requireContext().getResources().getDisplayMetrics().density);
    }
}
