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
 * Updated to support overdue guest status.
 */
public class DashboardFragment extends Fragment implements EntryRequestAdapter.EntryActionListener {

    private EntryRequestRepository repository;
    private GuestPassRepository guestPassRepository;
    private EntryRequestAdapter adapter;
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
                            List<EntryRequest> visibleRequests = filterVisibleRequests(requests);

                            adapter.submitList(visibleRequests);
                            emptyStateCard.setVisibility(visibleRequests.isEmpty() ? View.VISIBLE : View.GONE);
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
                    if (requestId != null && isAdded()) {
                        repository.logExit(requestId, (success, message, error) -> {
                            if (!isAdded()) {
                                return;
                            }
                            if (!success) {
                                requireActivity().runOnUiThread(() ->
                                        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show());
                                return;
                            }
                            guestPassRepository.markPassExitedByEntryRequestId(
                                    requestId,
                                    (passSuccess, passMessage, passError) -> {
                                        if (!isAdded()) {
                                            return;
                                        }
                                        requireActivity().runOnUiThread(() -> {
                                            if (passSuccess) {
                                                Snackbar.make(requireView(), "Guest exit logged.", Snackbar.LENGTH_SHORT).show();
                                            } else {
                                                Snackbar.make(
                                                        requireView(),
                                                        "Exit logged, but guest pass update failed: " + passMessage,
                                                        Snackbar.LENGTH_LONG
                                                ).show();
                                            }
                                        });
                                    }
                            );
                        });
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
                List<EntryRequest> visibleRequests = filterVisibleRequests(requests);
                adapter.submitList(visibleRequests);
                emptyStateCard.setVisibility(visibleRequests.isEmpty() ? View.VISIBLE : View.GONE);
                refreshLoadingState();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requestsLoaded = true;
                adapter.submitList(new ArrayList<>());
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

    @NonNull
    private List<EntryRequest> filterVisibleRequests(@NonNull List<EntryRequest> requests) {
        List<EntryRequest> visibleRequests = new ArrayList<>();
        for (EntryRequest request : requests) {
            String status = request.getStatus() == null ? "" : request.getStatus().trim().toLowerCase(Locale.getDefault());
            if ("active".equals(status) || "overdue".equals(status)) {
                visibleRequests.add(request);
            }
        }
        return visibleRequests;
    }

    @Override
    public void onDetailsClicked(@NonNull EntryRequest request) {
        openEntryDetails(request, false);
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
                request.getStatus(),
                promptExit
        );
        sheet.show(getParentFragmentManager(), EntryDetailsBottomSheetDialogFragment.TAG);
    }

    private String formatTimestamp(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return "--:--";
        }
        return timeFormat.format(timestamp.toDate());
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
