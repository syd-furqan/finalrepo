package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.AdminAlertPayloadFactory;
import com.example.glitch.data.AlertRepository;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.InterventionRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.SecurityAlert;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.example.glitch.ui.UiAnimations;

/**
 * Admin incident queue for reported entry alerts.
 */
public class AdminAlertsFragment extends Fragment {
    private android.view.ViewGroup animContent;
    private AlertRepository repository;
    private EntryRequestRepository entryRequestRepository;
    private GuestPassRepository guestPassRepository;
    private InterventionRepository interventionRepository;

    private SecurityAlertAdapter adapter;
    private TextView textEmpty;
    private TextView textFilterSummary;
    private MaterialButton buttonOpenFilters;
    private AutoCompleteTextView inputStatusFilter;
    private AutoCompleteTextView inputSeverityFilter;
    private AutoCompleteTextView inputTypeFilter;
    private AutoCompleteTextView inputSourceFilter;

    private final List<SecurityAlert> allAlerts = new ArrayList<>();
    private final Map<String, String> statusDisplayToCanonical = new LinkedHashMap<>();
    private final Map<String, String> severityDisplayToCanonical = new LinkedHashMap<>();
    private final Map<String, String> typeDisplayToCanonical = new LinkedHashMap<>();
    private final Map<String, String> sourceDisplayToCanonical = new LinkedHashMap<>();

    private static final List<String> STATUS_OPTIONS = Arrays.asList(
            "all", "new", "in_review", "actioned", "closed"
    );
    private static final List<String> SEVERITY_OPTIONS = Arrays.asList(
            "all", "high", "medium", "low", "critical"
    );
    private static final List<String> TYPE_OPTIONS = Arrays.asList(
            "all",
            AdminAlertPayloadFactory.TYPE_ENTRY_REPORT,
            AdminAlertPayloadFactory.TYPE_MANUAL_VIOLATION,
            AdminAlertPayloadFactory.TYPE_SCAN_RISK,
            AdminAlertPayloadFactory.TYPE_VEHICLE_REVIEW,
            AdminAlertPayloadFactory.TYPE_CHARGE_REVIEW
    );
    private static final List<String> SOURCE_OPTIONS = Arrays.asList(
            "all",
            "guard_manual",
            AdminAlertPayloadFactory.SOURCE_MANUAL_VIOLATION,
            "system_overdue_grace",
            AdminAlertPayloadFactory.SOURCE_BANNED_SCAN,
            AdminAlertPayloadFactory.SOURCE_VEHICLE_APPLICATION,
            AdminAlertPayloadFactory.SOURCE_VEHICLE_REMOVAL,
            AdminAlertPayloadFactory.SOURCE_CHARGE_REMOVAL
    );

    @NonNull
    public static AdminAlertsFragment newInstance() {
        return new AdminAlertsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animContent = view.findViewById(R.id.anim_content);
        repository = RepositoryProvider.getAlertRepository();
        entryRequestRepository = RepositoryProvider.getRepository();
        guestPassRepository = RepositoryProvider.getGuestPassRepository();
        interventionRepository = RepositoryProvider.getInterventionRepository();

        textEmpty = view.findViewById(R.id.text_alerts_empty);
        textFilterSummary = view.findViewById(R.id.text_alert_filter_summary);
        buttonOpenFilters = view.findViewById(R.id.button_open_alert_filters);
        inputStatusFilter = view.findViewById(R.id.input_alert_status_filter);
        inputSeverityFilter = view.findViewById(R.id.input_alert_severity_filter);
        inputTypeFilter = view.findViewById(R.id.input_alert_type_filter);
        inputSourceFilter = view.findViewById(R.id.input_alert_source_filter);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_alerts);

        adapter = new SecurityAlertAdapter();
        adapter.setActionListener(this::routeAlertToOwner);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.ALERTS);

        setupFilters();
        buttonOpenFilters.setOnClickListener(v -> openFilterSheet());

        repository.listenAlerts(new AlertRepository.AlertListener() {
            @Override
            public void onData(@NonNull List<SecurityAlert> alerts) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    allAlerts.clear();
                    allAlerts.addAll(alerts);
                    applyFilters();
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_alerts, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }

    private void setupFilters() {
        configureDropdown(
                inputStatusFilter,
                STATUS_OPTIONS,
                "all"
        );
        configureDropdown(
                inputSeverityFilter,
                SEVERITY_OPTIONS,
                "all"
        );
        configureDropdown(
                inputTypeFilter,
                TYPE_OPTIONS,
                "all"
        );
        configureDropdown(
                inputSourceFilter,
                SOURCE_OPTIONS,
                "all"
        );

        inputStatusFilter.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        inputSeverityFilter.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        inputTypeFilter.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        inputSourceFilter.setOnItemClickListener((parent, view, position, id) -> applyFilters());
    }

    private void configureDropdown(@NonNull AutoCompleteTextView input, @NonNull List<String> options, @NonNull String defaultValue) {
        Map<String, String> lookup = mapForInput(input);
        lookup.clear();
        List<String> displayOptions = new ArrayList<>();
        for (String option : options) {
            String canonical = UiLabelFormatter.normalizeToken(option);
            if (canonical.isEmpty()) {
                continue;
            }
            String label = formatFilterLabel(canonical);
            lookup.put(label, canonical);
            displayOptions.add(label);
        }
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayOptions
        );
        input.setAdapter(dropdownAdapter);
        input.setText(formatFilterLabel(defaultValue), false);
    }

    private void applyFilters() {
        String status = readFilter(inputStatusFilter);
        String severity = readFilter(inputSeverityFilter);
        String type = readFilter(inputTypeFilter);
        String source = readFilter(inputSourceFilter);

        List<SecurityAlert> filtered = new ArrayList<>();
        for (SecurityAlert alert : allAlerts) {
            String incidentStatus = alert.getIncidentStatus().trim().isEmpty()
                    ? "new"
                    : UiLabelFormatter.normalizeToken(alert.getIncidentStatus());
            String alertSeverity = UiLabelFormatter.normalizeToken(alert.getSeverity());
            String alertType = UiLabelFormatter.normalizeToken(alert.getAlertType());
            String alertSource = UiLabelFormatter.normalizeToken(alert.getSource());

            if (!"all".equals(status) && !status.equals(incidentStatus)) {
                continue;
            }
            if (!"all".equals(severity) && !severity.equals(alertSeverity)) {
                continue;
            }
            if (!"all".equals(type) && !type.equals(alertType)) {
                continue;
            }
            if (!"all".equals(source) && !source.equals(alertSource)) {
                continue;
            }
            filtered.add(alert);
        }

        adapter.submitList(filtered);
        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        textFilterSummary.setText(buildFilterSummary(status, severity, type, source));
    }

    private void openFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout content = createSheetContent("Alert Filters");
        AutoCompleteTextView status = addDropdown(
                content,
                "Incident Status",
                STATUS_OPTIONS,
                readFilter(inputStatusFilter)
        );
        AutoCompleteTextView severity = addDropdown(
                content,
                "Severity",
                SEVERITY_OPTIONS,
                readFilter(inputSeverityFilter)
        );
        AutoCompleteTextView type = addDropdown(
                content,
                "Alert Type",
                TYPE_OPTIONS,
                readFilter(inputTypeFilter)
        );
        AutoCompleteTextView source = addDropdown(
                content,
                "Source",
                SOURCE_OPTIONS,
                readFilter(inputSourceFilter)
        );
        addSheetActions(
                content,
                () -> {
                    inputStatusFilter.setText(status.getText(), false);
                    inputSeverityFilter.setText(severity.getText(), false);
                    inputTypeFilter.setText(type.getText(), false);
                    inputSourceFilter.setText(source.getText(), false);
                    applyFilters();
                    dialog.dismiss();
                },
                () -> {
                    inputStatusFilter.setText("All", false);
                    inputSeverityFilter.setText("All", false);
                    inputTypeFilter.setText("All", false);
                    inputSourceFilter.setText("All", false);
                    applyFilters();
                    dialog.dismiss();
                }
        );
        dialog.setContentView(content);
        dialog.show();
    }

    @NonNull
    private String buildFilterSummary(
            @NonNull String status,
            @NonNull String severity,
            @NonNull String type,
            @NonNull String source
    ) {
        List<String> active = new ArrayList<>();
        if (!"all".equals(status)) active.add("Status: " + formatFilterLabel(status));
        if (!"all".equalsIgnoreCase(severity)) active.add("Severity: " + formatFilterLabel(severity));
        if (!"all".equals(type)) active.add("Type: " + formatFilterLabel(type));
        if (!"all".equals(source)) active.add("Source: " + formatFilterLabel(source));
        return active.isEmpty() ? "Showing all important alerts" : "Filters • " + String.join(" • ", active);
    }

    @NonNull
    private LinearLayout createSheetContent(@NonNull String title) {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, padding, padding, padding);
        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(requireContext().getColor(R.color.text_dark));
        titleView.setTextSize(20);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(titleView);
        return content;
    }

    @NonNull
    private AutoCompleteTextView addDropdown(
            @NonNull LinearLayout content,
            @NonNull String hint,
            @NonNull List<String> options,
            @NonNull String selected
    ) {
        TextInputLayout layout = new TextInputLayout(requireContext());
        layout.setHint(hint);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(12);
        layout.setLayoutParams(params);
        AutoCompleteTextView input = new AutoCompleteTextView(requireContext());
        input.setInputType(0);
        List<String> displayOptions = new ArrayList<>();
        for (String option : options) {
            displayOptions.add(formatFilterLabel(option));
        }
        input.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions));
        input.setText(formatFilterLabel(selected), false);
        layout.addView(input);
        content.addView(layout);
        return input;
    }

    private void addSheetActions(@NonNull LinearLayout content, @NonNull Runnable apply, @NonNull Runnable clear) {
        MaterialButton applyButton = new MaterialButton(requireContext());
        applyButton.setText("Apply Filters");
        applyButton.setAllCaps(false);
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        applyParams.topMargin = dp(16);
        applyButton.setLayoutParams(applyParams);
        applyButton.setOnClickListener(v -> apply.run());
        content.addView(applyButton);

        MaterialButton clearButton = new MaterialButton(requireContext());
        clearButton.setText("Clear");
        clearButton.setAllCaps(false);
        clearButton.setOnClickListener(v -> clear.run());
        content.addView(clearButton);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @NonNull
    private String readFilter(@NonNull AutoCompleteTextView input) {
        CharSequence value = input.getText();
        if (value == null) {
            return "all";
        }
        String typed = value.toString().trim();
        if (typed.isEmpty()) {
            return "all";
        }
        String canonical = mapForInput(input).get(typed);
        if (canonical == null) {
            canonical = UiLabelFormatter.normalizeToken(typed);
        }
        return canonical.isEmpty() ? "all" : canonical;
    }

    @NonNull
    private Map<String, String> mapForInput(@NonNull AutoCompleteTextView input) {
        if (input == inputStatusFilter) {
            return statusDisplayToCanonical;
        }
        if (input == inputSeverityFilter) {
            return severityDisplayToCanonical;
        }
        if (input == inputTypeFilter) {
            return typeDisplayToCanonical;
        }
        if (input == inputSourceFilter) {
            return sourceDisplayToCanonical;
        }
        return new LinkedHashMap<>();
    }

    @NonNull
    private String formatFilterLabel(@Nullable String raw) {
        String canonical = UiLabelFormatter.normalizeToken(raw);
        if (canonical.isEmpty() || "all".equals(canonical)) {
            return "All";
        }
        switch (canonical) {
            case "entry_report":
                return "Entry Report";
            case "manual_violation":
                return "Manual Violation";
            case "scan_risk":
                return "Scan Risk";
            case "vehicle_review":
                return "Vehicle Review";
            case "charge_review":
                return "Charge Review";
            case "guard_manual":
                return "Guard Manual";
            case "manual_violation_report":
                return "Manual Violation Report";
            case "system_overdue_grace":
                return "System Overdue Grace";
            case "banned_scan":
                return "Banned Scan";
            case "vehicle_application":
                return "Vehicle Application";
            case "vehicle_removal":
                return "Vehicle Removal";
            case "charge_removal":
                return "Charge Removal";
            default:
                return UiLabelFormatter.humanizeToken(canonical);
        }
    }

    private void openAlertDetailsSheet(@NonNull SecurityAlert alert) {
        if (!isAdded()) {
            return;
        }
        String guard = valueOr(alert.getReportedByName(), "Unknown");
        if (!alert.getReportedByUid().trim().isEmpty()) {
            guard = guard + " [" + alert.getReportedByUid().trim() + "]";
        }
        if (!alert.getReportedByRole().trim().isEmpty()) {
            guard = guard + " (" + alert.getReportedByRole().trim() + ")";
        }

        String visitor = valueOr(alert.getGuestName(), "Unknown")
                + " (" + valueOr(alert.getGuestIdNumber(), "N/A") + ")";
        if (!alert.getGuestPhone().trim().isEmpty()) {
            visitor += " / " + alert.getGuestPhone().trim();
        }
        String sponsor = valueOr(alert.getHostName(), "N/A");
        if (!alert.getRequesterUid().trim().isEmpty()) {
            sponsor = sponsor + " / " + alert.getRequesterUid().trim();
        }
        if (!alert.getRequesterRole().trim().isEmpty()) {
            sponsor = sponsor + " (" + alert.getRequesterRole().trim() + ")";
        }

        AdminAlertDetailsBottomSheetFragment.newInstance(
                        alert.getId(),
                        alert.getAlertType(),
                        alert.getEntryRequestId(),
                        UiLabelFormatter.humanizeToken(valueOr(alert.getIncidentStatus(), "new")),
                        valueOr(alert.getInterventionSummary(), "N/A"),
                        guard,
                        visitor,
                        sponsor,
                        valueOr(alert.getGateLabel(), "In-Gate"),
                        UiLabelFormatter.humanizeToken(valueOr(alert.getReasonCode(), "N/A")),
                        UiLabelFormatter.humanizeToken(valueOr(alert.getSource(), "N/A")),
                        valueOr(alert.getMessage(), "N/A"),
                        valueOr(alert.getRequesterUid(), ""),
                        valueOr(alert.getGuestName(), ""),
                        valueOr(alert.getGuestIdNumber(), "")
                )
                .show(getParentFragmentManager(), AdminAlertDetailsBottomSheetFragment.TAG);
    }

    private void routeAlertToOwner(@NonNull SecurityAlert alert) {
        if (!isAdded()) {
            return;
        }
        String type = UiLabelFormatter.normalizeToken(alert.getAlertType());
        Fragment target = null;
        if (AdminAlertPayloadFactory.TYPE_ENTRY_REPORT.equals(type)) {
            String reportId = firstNonEmpty(
                    alert.getViolationReportId(),
                    alert.getId(),
                    alert.getIdentifier()
            );
            target = AdminViolationDirectoryFragment.newInstance(reportId);
        } else if (AdminAlertPayloadFactory.TYPE_MANUAL_VIOLATION.equals(type)) {
            String reportId = firstNonEmpty(
                    alert.getViolationReportId(),
                    alert.getIdentifier(),
                    alert.getId()
            );
            target = AdminViolationDirectoryFragment.newInstance(reportId);
        } else if (AdminAlertPayloadFactory.TYPE_VEHICLE_REVIEW.equals(type)) {
            String requestId = firstNonEmpty(alert.getVehicleRequestId(), alert.getIdentifier());
            target = AdminVehicleReviewFragment.newInstance(requestId);
        } else if (AdminAlertPayloadFactory.TYPE_CHARGE_REVIEW.equals(type)) {
            String chargeId = firstNonEmpty(alert.getChargeId(), alert.getIdentifier());
            target = AdminChargesFragment.newInstance(chargeId);
        } else if (AdminAlertPayloadFactory.TYPE_SCAN_RISK.equals(type)) {
            String cnic = firstNonEmpty(alert.getGuestIdNumber(), alert.getIdentifier());
            target = AdminBannedListFragment.newInstance(cnic);
        }

        if (target == null) {
            Snackbar.make(requireView(), "No destination is available for this alert.", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (requireActivity() instanceof NavigationHost) {
            ((NavigationHost) requireActivity()).showFragment(target, false);
        }
    }

    @NonNull
    private String firstNonEmpty(@NonNull String... values) {
        for (String value : values) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return "";
    }

    private void handleAlertDecision(@NonNull Bundle result) {
        String action = valueOr(result.getString(AdminAlertDetailsBottomSheetFragment.RESULT_ACTION), "");
        String alertId = valueOr(result.getString(AdminAlertDetailsBottomSheetFragment.RESULT_ALERT_ID), "");
        String requestId = valueOr(result.getString(AdminAlertDetailsBottomSheetFragment.RESULT_REQUEST_ID), "");
        String sponsorUid = valueOr(result.getString(AdminAlertDetailsBottomSheetFragment.RESULT_SPONSOR_UID), "");
        String guestName = valueOr(result.getString(AdminAlertDetailsBottomSheetFragment.RESULT_GUEST_NAME), "");
        String guestId = valueOr(result.getString(AdminAlertDetailsBottomSheetFragment.RESULT_GUEST_ID), "");

        if (AdminAlertDetailsBottomSheetFragment.ACTION_LOG_EXIT.equals(action)) {
            logExitFromAlert(alertId, requestId);
            return;
        }
        if (AdminAlertDetailsBottomSheetFragment.ACTION_CHARGE.equals(action)) {
            createChargeFromAlert(alertId, requestId, sponsorUid, guestName, guestId);
            return;
        }
        if (AdminAlertDetailsBottomSheetFragment.ACTION_MARK_REVIEWED.equals(action)) {
            markAlertReviewed(alertId);
        }
    }

    private void createChargeFromAlert(
            @NonNull String alertId,
            @NonNull String requestId,
            @NonNull String sponsorUid,
            @NonNull String guestName,
            @NonNull String guestId
    ) {
        if (alertId.trim().isEmpty() || requestId.trim().isEmpty() || sponsorUid.trim().isEmpty()) {
            if (isAdded()) {
                Snackbar.make(requireView(), R.string.error_create_charge_missing_context, Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        interventionRepository.createChargeForReport(
                alertId,
                sponsorUid,
                guestName,
                guestId,
                "alert",
                currentAdminUid(),
                (success, message, exception) -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (success) {
                        updateAlertStatus(alertId, "actioned", "Charge issued from alert.");
                    }
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
                }
        );
    }

    private void logExitFromAlert(@NonNull String alertId, @NonNull String requestId) {
        if (requestId.trim().isEmpty()) {
            if (isAdded()) {
                Snackbar.make(requireView(), R.string.alert_log_exit_missing_request, Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        entryRequestRepository.logExit(requestId, (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            if (!success) {
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(
                                requireView(),
                                getString(R.string.alert_log_exit_failed, message),
                                Snackbar.LENGTH_LONG
                        ).show());
                return;
            }
            guestPassRepository.markPassExitedByEntryRequestId(requestId, (passSuccess, passMessage, passError) -> {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (!passSuccess) {
                        Snackbar.make(
                                requireView(),
                                getString(R.string.alert_log_exit_partial, passMessage),
                                Snackbar.LENGTH_LONG
                        ).show();
                        return;
                    }
                    updateAlertStatus(alertId, "closed", "Exit logged from alert.");
                    Snackbar.make(requireView(), R.string.alert_log_exit_success, Snackbar.LENGTH_SHORT).show();
                });
            });
        });
    }

    private void markAlertReviewed(@NonNull String alertId) {
        updateAlertStatus(alertId, "closed", "Reviewed by admin.");
    }

    private void updateAlertStatus(
            @NonNull String alertId,
            @NonNull String status,
            @NonNull String summary
    ) {
        repository.updateAlertStatus(
                alertId,
                status,
                summary,
                currentAdminUid(),
                (success, message, exception) -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (!success) {
                        requireActivity().runOnUiThread(() ->
                                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
                    }
                }
        );
    }

    @NonNull
    private String currentAdminUid() {
        if (SessionManager.getCurrentProfile() == null) {
            return "unknown_admin";
        }
        String uid = SessionManager.getCurrentProfile().getUid().trim();
        return uid.isEmpty() ? "unknown_admin" : uid;
    }

    @NonNull
    private String valueOr(@Nullable String value, @NonNull String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
    @Override
    public void onResume() {
        super.onResume();
        if (animContent != null) UiAnimations.animateFallIn(animContent);
    }
}
