package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.CredentialVerificationResult;
import com.example.glitch.model.EntryRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Guard search and credential verification screen (US-02 and US-03).
 * Pattern: Form + RecyclerView fragment backed by one-shot repository queries.
 * Known issue: search currently queries latest 50 requests then filters client-side.
 */
public class GuardSearchFragment extends Fragment implements EntryRequestAdapter.EntryActionListener {
    private EntryRequestRepository repository;
    private EntryRequestAdapter adapter;
    private TextInputEditText inputSearch;
    private TextInputEditText inputCredential;
    private TextView textVerificationResult;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm a", Locale.getDefault());

    @NonNull
    public static GuardSearchFragment newInstance() {
        return new GuardSearchFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_guard_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getRepository();
        inputSearch = view.findViewById(R.id.input_search_query);
        inputCredential = view.findViewById(R.id.input_credential_identifier);
        textVerificationResult = view.findViewById(R.id.text_verification_result);
        MaterialButton buttonSearch = view.findViewById(R.id.button_search);
        MaterialButton buttonVerify = view.findViewById(R.id.button_verify_credential);
        MaterialButton buttonScanQr = view.findViewById(R.id.button_scan_qr_prompt);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_search_results);

        adapter = new EntryRequestAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        buttonSearch.setOnClickListener(v -> runSearch());
        buttonVerify.setOnClickListener(v -> runVerification());
        if (buttonScanQr != null) {
            buttonScanQr.setOnClickListener(v -> RoleNavRouter.route(this, RoleDestination.SCAN));
        }
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.PASSES);
    }

    @Override
    public void onLogEntryClicked(@NonNull EntryRequest request) {
        if (!isAdded()) {
            return;
        }
        Snackbar.make(requireView(), R.string.route_to_dashboard_for_admission, Snackbar.LENGTH_LONG).show();
        if (requireActivity() instanceof NavigationHost) {
            ((NavigationHost) requireActivity()).showFragment(DashboardFragment.newInstance(), true);
        }
    }

    private void runSearch() {
        String query = read(inputSearch);
        repository.searchRequests(query, new EntryRequestRepository.RequestListListener() {
            @Override
            public void onData(@NonNull java.util.List<EntryRequest> requests) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(requests);
                    if (requests.isEmpty()) {
                        Snackbar.make(requireView(), R.string.no_search_results, Snackbar.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_requests, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void runVerification() {
        String identifier = read(inputCredential);
        repository.verifyCredential(identifier, new EntryRequestRepository.CredentialListener() {
            @Override
            public void onData(@NonNull CredentialVerificationResult result) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    String text = result.getMessage();
                    if (!result.getHolderName().isEmpty()) {
                        text += " (" + result.getHolderName() + ")";
                    }
                    textVerificationResult.setText(text);
                    textVerificationResult.setTextColor(requireContext().getColor(
                            result.isValid() ? R.color.primary_navy : android.R.color.holo_red_dark
                    ));
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> textVerificationResult.setText(R.string.error_verify_credential));
            }
        });
    }

    @Override
    public void onDetailsClicked(@NonNull EntryRequest request) {
        openVerificationProfile(request);
    }

    @Override
    public void onLogExitClicked(@NonNull EntryRequest request) {
        repository.logExit(request.getId(), (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    private void openVerificationProfile(@NonNull EntryRequest request) {
        String entered = request.getEnteredAt() == null ? "--:--" : timeFormat.format(request.getEnteredAt().toDate());
        VerificationResultFragment fragment = VerificationResultFragment.newInstance(
                request.getId(),
                request.getFullName(),
                request.getRoleTag(),
                request.getHostName(),
                request.getGateLabel(),
                entered
        );
        if (requireActivity() instanceof NavigationHost) {
            ((NavigationHost) requireActivity()).showFragment(fragment, true);
        }
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
}
