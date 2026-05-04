package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.AlertRepository;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.InterventionRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.SecurityAlert;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Admin incident queue for reported entry alerts.
 */
public class AdminAlertsFragment extends Fragment {
    private AlertRepository repository;
    private EntryRequestRepository entryRequestRepository;
    private GuestPassRepository guestPassRepository;
    private InterventionRepository interventionRepository;

    private SecurityAlertAdapter adapter;
    private TextView textEmpty;
    private AutoCompleteTextView inputStatusFilter;
    private AutoCompleteTextView inputSeverityFilter;
    private AutoCompleteTextView inputSourceFilter;

    private final List<SecurityAlert> allAlerts = new ArrayList<>();

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
        repository = RepositoryProvider.getAlertRepository();
        entryRequestRepository = RepositoryProvider.getRepository();
        guestPassRepository = RepositoryProvider.getGuestPassRepository();
        interventionRepository = RepositoryProvider.getInterventionRepository();

        textEmpty = view.findViewById(R.id.text_alerts_empty);
        inputStatusFilter = view.findViewById(R.id.input_alert_status_filter);
        inputSeverityFilter = view.findViewById(R.id.input_alert_severity_filter);
        inputSourceFilter = view.findViewById(R.id.input_alert_source_filter);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_alerts);

        adapter = new SecurityAlertAdapter();
        adapter.setActionListener(this::openAlertDetailsSheet);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        getParentFragmentManager().setFragmentResultListener(
                AdminAlertDetailsBottomSheetFragment.RESULT_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> handleAlertDecision(result)
        );

        setupFilters();

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
                Arrays.asList("all", "new", "in_review", "actioned", "closed"),
                "all"
        );
        configureDropdown(
                inputSeverityFilter,
                Arrays.asList("all", "HIGH", "MEDIUM", "LOW", "CRITICAL"),
                "all"
        );
        configureDropdown(
                inputSourceFilter,
                Arrays.asList("all", "guard_manual", "system_overdue_grace", "admin_ban"),
                "all"
        );

        inputStatusFilter.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        inputSeverityFilter.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        inputSourceFilter.setOnItemClickListener((parent, view, position, id) -> applyFilters());
    }

    private void configureDropdown(@NonNull AutoCompleteTextView input, @NonNull List<String> options, @NonNull String defaultValue) {
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options
        );
        input.setAdapter(dropdownAdapter);
        input.setText(defaultValue, false);
    }

    private void applyFilters() {
        String status = readFilter(inputStatusFilter);
        String severity = readFilter(inputSeverityFilter);
        String source = readFilter(inputSourceFilter);

        List<SecurityAlert> filtered = new ArrayList<>();
        for (SecurityAlert alert : allAlerts) {
            String incidentStatus = alert.getIncidentStatus().trim().isEmpty()
                    ? "new"
                    : alert.getIncidentStatus().trim().toLowerCase(Locale.getDefault());
            String alertSeverity = alert.getSeverity().trim().toUpperCase(Locale.getDefault());
            String alertSource = alert.getSource().trim().toLowerCase(Locale.getDefault());

            if (!"all".equals(status) && !status.equals(incidentStatus)) {
                continue;
            }
            if (!"all".equals(severity) && !severity.equals(alertSeverity)) {
                continue;
            }
            if (!"all".equals(source) && !source.equals(alertSource)) {
                continue;
            }
            filtered.add(alert);
        }

        adapter.submitList(filtered);
        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private String readFilter(@NonNull AutoCompleteTextView input) {
        CharSequence value = input.getText();
        return value == null ? "all" : value.toString().trim().toLowerCase(Locale.getDefault());
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
        String sponsor = valueOr(alert.getHostName(), "N/A");
        if (!alert.getRequesterUid().trim().isEmpty()) {
            sponsor = sponsor + " / " + alert.getRequesterUid().trim();
        }
        if (!alert.getRequesterRole().trim().isEmpty()) {
            sponsor = sponsor + " (" + alert.getRequesterRole().trim() + ")";
        }

        AdminAlertDetailsBottomSheetFragment.newInstance(
                        alert.getId(),
                        alert.getEntryRequestId(),
                        valueOr(alert.getIncidentStatus(), "new"),
                        valueOr(alert.getInterventionSummary(), "N/A"),
                        guard,
                        visitor,
                        sponsor,
                        valueOr(alert.getGateLabel(), "In-Gate"),
                        valueOr(alert.getReasonCode(), "N/A"),
                        valueOr(alert.getSource(), "N/A"),
                        valueOr(alert.getMessage(), "N/A"),
                        valueOr(alert.getRequesterUid(), ""),
                        valueOr(alert.getGuestName(), ""),
                        valueOr(alert.getGuestIdNumber(), "")
                )
                .show(getParentFragmentManager(), AdminAlertDetailsBottomSheetFragment.TAG);
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
        interventionRepository.createChargeForAlert(
                alertId,
                requestId,
                sponsorUid,
                guestName,
                guestId,
                currentAdminUid(),
                (success, message, exception) -> {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
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
                    interventionRepository.closeIncident(
                            alertId,
                            requestId,
                            currentAdminUid(),
                            "Visitor exited and incident closed by admin.",
                            (closeSuccess, closeMessage, closeError) -> {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    if (closeSuccess) {
                                        Snackbar.make(requireView(), R.string.alert_log_exit_success, Snackbar.LENGTH_SHORT).show();
                                    } else {
                                        Snackbar.make(requireView(), closeMessage, Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            }
                    );
                });
            });
        });
    }
}
