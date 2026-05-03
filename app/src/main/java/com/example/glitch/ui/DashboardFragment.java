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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.DashboardState;
import com.example.glitch.model.EntryRequest;
import com.example.glitch.model.UserProfile;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main dashboard screen matching Figma node 1:225 and wiring Firestore data interactions.
 * Pattern: Fragment orchestrating repository data + RecyclerView presentation.
 * Known issue: bottom navigation is currently static and non-routing for milestone 1.
 */
public class DashboardFragment extends Fragment implements EntryRequestAdapter.EntryActionListener {

    private EntryRequestRepository repository;
    private GuestPassRepository guestPassRepository;
    private EntryRequestAdapter adapter;
    private final List<EntryRequest> currentRequests = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm a", Locale.getDefault());

    private TextView textSystemStatusTitle;
    private TextView textSystemStatusMessage;
    private TextView textProtocolLevel;
    private TextView textProtocolDescription;
    private LinearLayout loadingContainer;
    private LinearLayout emptyStateCard;
    private String currentRole = "";

    private boolean requestsLoaded;
    private boolean stateLoaded;

    /**
     * Creates new dashboard fragment.
     */
    @NonNull
    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onLogEntryClicked(@NonNull EntryRequest request) {
        approveEntryFromDashboard(request.getId(), request.getGateLabel(), "Guest Checked In!");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }
        currentRole = profile.getRole();
        repository = RepositoryProvider.getRepository();
        guestPassRepository = RepositoryProvider.getGuestPassRepository();
        bindViews(view);
        setupRecycler(view);
        setupActions(view, currentRole);
        setupBottomSheetResult();
        setupSearch(view);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DASHBOARD);
        startRealtimeListeners();

    }

    private void setupSearch(View view) {
        TextInputEditText editSearch = view.findViewById(R.id.edit_search_requests);

        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                if (query.isEmpty()) {
                    startRealtimeListeners();
                } else {
                    repository.removeListeners();

                    repository.searchRequests(query, new EntryRequestRepository.RequestListListener() {
                        @Override
                        public void onData(@NonNull List<EntryRequest> requests) {
                            if (!isAdded()) return;

                            adapter.submitList(requests);

                            emptyStateCard.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                        }

                        @Override
                        public void onError(@NonNull Exception exception) {
                            if (!isAdded()) return;
                            Snackbar.make(requireView(), "Search failed", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void bindViews(@NonNull View root) {
        textSystemStatusTitle = root.findViewById(R.id.text_system_status_title);
        textSystemStatusMessage = root.findViewById(R.id.text_system_status_message);
        textProtocolLevel = root.findViewById(R.id.text_protocol_level);
        textProtocolDescription = root.findViewById(R.id.text_protocol_description);
        loadingContainer = root.findViewById(R.id.loading_container);
        emptyStateCard = root.findViewById(R.id.empty_state_card);
    }

    private void setupRecycler(@NonNull View root) {
        RecyclerView recyclerView = root.findViewById(R.id.requests_recycler);
        adapter = new EntryRequestAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupActions(@NonNull View root, @NonNull String role) {
        View buttonNotifications = root.findViewById(R.id.button_notifications);
        View buttonScanQr = root.findViewById(R.id.button_scan_qr);
        View buttonManualEntry = root.findViewById(R.id.button_manual_entry);
        View fabAction = root.findViewById(R.id.fab_action);

        buttonScanQr.setOnClickListener(v -> {
            if ("guard".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
                RoleNavRouter.route(this, RoleDestination.SCAN);
            } else {
                Snackbar.make(requireView(), "Access Restricted to Guards/Admins", Snackbar.LENGTH_SHORT).show();
            }
        });

        if (buttonNotifications != null) {
            if ("admin".equalsIgnoreCase(role)) {
                buttonNotifications.setOnClickListener(v -> RoleNavRouter.route(this, RoleDestination.AUDIT));
            } else {
                buttonNotifications.setOnClickListener(view ->
                        Snackbar.make(requireView(), "No new notifications", Snackbar.LENGTH_SHORT).show());
            }
        }

        buttonManualEntry.setOnClickListener(view -> RoleNavRouter.route(this, RoleDestination.SEARCH));

        fabAction.setOnClickListener(v -> {
            if ("student".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
                RoleNavRouter.route(this, RoleDestination.STUDENT_PASSES);
            } else {
                Snackbar.make(requireView(), "Access Restricted to Students/Admins", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
    private void setupBottomSheetResult() {
        getParentFragmentManager().setFragmentResultListener(
                EntryDetailsBottomSheetDialogFragment.RESULT_KEY,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    String requestId = bundle.getString(EntryDetailsBottomSheetDialogFragment.RESULT_REQUEST_ID);
                    boolean isEntry = bundle.getBoolean("is_entry_action", false);

                    if (requestId != null && isAdded()) {
                        if (isEntry) {
                            approveEntryFromDashboard(
                                    requestId,
                                    resolveGateForRequestId(requestId),
                                    "Guest admitted successfully!"
                            );
                        } else {
                            repository.logExit(requestId, (success, message, error) -> {
                                requireActivity().runOnUiThread(() ->
                                        Snackbar.make(requireView(), "Guest exit logged.", Snackbar.LENGTH_SHORT).show()
                                );
                            });
                        }
                    }
                }
        );
    }

    private void startRealtimeListeners() {
        requestsLoaded = false;
        stateLoaded = false;
        showLoading(true);

        repository.listenDashboardState(new EntryRequestRepository.DashboardStateListener() {
            @Override
            public void onData(@NonNull DashboardState state) {
                if (!isAdded()) {
                    return;
                }
                stateLoaded = true;
                bindDashboardState(state);
                refreshLoadingState();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                stateLoaded = true;
                bindDashboardState(DashboardState.defaultState());
                showError(getString(R.string.error_load_state), true);
                refreshLoadingState();
            }
        });

        repository.listenActiveRequests(new EntryRequestRepository.RequestListListener() {
            @Override
            public void onData(@NonNull List<EntryRequest> requests) {
                if (!isAdded()) {
                    return;
                }
                requestsLoaded = true;
                currentRequests.clear();
                currentRequests.addAll(requests);
                adapter.submitList(requests);
                emptyStateCard.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                refreshLoadingState();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requestsLoaded = true;
                currentRequests.clear();
                adapter.submitList(currentRequests);
                emptyStateCard.setVisibility(View.VISIBLE);
                showError(getString(R.string.error_load_requests), true);
                refreshLoadingState();
            }
        });
    }

    private void bindDashboardState(@NonNull DashboardState state) {
        textSystemStatusTitle.setText(state.getSystemStatusTitle());
        textSystemStatusMessage.setText(state.getSystemStatusMessage());
        textProtocolLevel.setText(state.getProtocolLevel());
        textProtocolDescription.setText(state.getProtocolDescription());
    }

    private void refreshLoadingState() {
        showLoading(!(stateLoaded && requestsLoaded));
    }

    private void showLoading(boolean show) {
        loadingContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDetailsClicked(@NonNull EntryRequest request) {
        openEntryDetails(request, false);
    }

    @Override
    public void onLogExitClicked(@NonNull EntryRequest request) {
        openEntryDetails(request, true);
    }

    private void openEntryDetails(@NonNull EntryRequest request, boolean promptExit) {
        EntryDetailsBottomSheetDialogFragment sheet = EntryDetailsBottomSheetDialogFragment.newInstance(
                request.getId(),
                request.getFullName(),
                request.getRoleTag(),
                request.getHostName(),
                request.getGuestIdNumber(),
                request.getGateLabel(),
                formatTimestamp(request.getEnteredAt()),
                formatTimestamp(request.getExpiresAt()),
                promptExit
        );
        sheet.show(getParentFragmentManager(), EntryDetailsBottomSheetDialogFragment.TAG);
    }

    private void approveEntryFromDashboard(
            @NonNull String requestId,
            @Nullable String gateLabelRaw,
            @NonNull String successMessage
    ) {
        String gateLabel = gateLabelRaw == null || gateLabelRaw.trim().isEmpty() ? "Main Gate" : gateLabelRaw.trim();
        repository.logEntry(requestId, gateLabel, (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            if (!success) {
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
                return;
            }
            String guardUid = SessionManager.getCurrentProfile() == null
                    ? ""
                    : SessionManager.getCurrentProfile().getUid();
            guestPassRepository.markPassAdmittedByEntryRequestId(
                    requestId,
                    guardUid,
                    "DASHBOARD_APPROVAL",
                    (passSuccess, passMessage, passException) -> {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (passSuccess) {
                                Snackbar.make(requireView(), successMessage, Snackbar.LENGTH_SHORT).show();
                            } else {
                                Snackbar.make(
                                        requireView(),
                                        successMessage + " (Guest pass update issue: " + passMessage + ")",
                                        Snackbar.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
            );
        });
    }

    @Nullable
    private String resolveGateForRequestId(@NonNull String requestId) {
        for (EntryRequest request : currentRequests) {
            if (requestId.equals(request.getId())) {
                return request.getGateLabel();
            }
        }
        return null;
    }

    private String formatTimestamp(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return "--:--";
        }
        return timeFormat.format(timestamp.toDate());
    }

    private void performLogExit(@NonNull String requestId) {
        repository.logExit(requestId, (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            if (success) {
                Snackbar.make(requireView(), R.string.success_exit_logged, Snackbar.LENGTH_SHORT).show();
            } else {
                String fallback = getString(R.string.generic_write_error);
                String displayMessage = (message == null || message.isEmpty()) ? fallback : message;
                showError(displayMessage, false);
            }
        });
    }

    private void showError(@NonNull String message, boolean withRetry) {
        Snackbar snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG);
        if (withRetry) {
            snackbar.setAction(R.string.retry_action, view -> {
                repository.removeListeners();
                startRealtimeListeners();
            });
        }
        snackbar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
        adapter = null;
    }
}
