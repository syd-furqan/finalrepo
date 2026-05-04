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
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.InterventionRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.FineCaseRecord;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Admin page for charge history and charge resolution actions.
 */
public class AdminChargesFragment extends Fragment {
    private InterventionRepository interventionRepository;
    private AdminChargeAdapter adapter;
    private TextView textEmpty;
    private final List<FineCaseRecord> allCharges = new ArrayList<>();

    @NonNull
    public static AdminChargesFragment newInstance() {
        return new AdminChargesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_charges, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        interventionRepository = RepositoryProvider.getInterventionRepository();
        textEmpty = view.findViewById(R.id.text_charges_empty);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_charges);

        adapter = new AdminChargeAdapter();
        adapter.setListener(this::openChargeDetails);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DIRECTORY);

        getParentFragmentManager().setFragmentResultListener(
                AdminChargeDetailsBottomSheetFragment.RESULT_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> handleChargeAction(result)
        );

        interventionRepository.listenFineCases(new InterventionRepository.FineListListener() {
            @Override
            public void onData(@NonNull List<FineCaseRecord> fineCases) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    allCharges.clear();
                    allCharges.addAll(fineCases);
                    allCharges.sort(Comparator.comparing(
                                    FineCaseRecord::getCreatedAt,
                                    Comparator.nullsLast(Comparator.naturalOrder())
                            )
                            .reversed());
                    adapter.submitList(allCharges);
                    textEmpty.setVisibility(allCharges.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_charges, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (interventionRepository != null) {
            interventionRepository.removeListeners();
        }
    }

    private void openChargeDetails(@NonNull FineCaseRecord record) {
        if (!isAdded()) {
            return;
        }
        String guest = valueOr(record.getGuestName(), "Unknown")
                + " (" + valueOr(record.getGuestIdNumber(), "N/A") + ")";
        AdminChargeDetailsBottomSheetFragment.newInstance(
                        record.getId(),
                        capitalize(record.getChargeDisplayStatus()),
                        valueOr(record.getRequestId(), "N/A"),
                        valueOr(record.getAlertId(), "N/A"),
                        guest,
                        valueOr(record.getSponsorUid(), "N/A")
                )
                .show(getParentFragmentManager(), AdminChargeDetailsBottomSheetFragment.TAG);
    }

    private void handleChargeAction(@NonNull Bundle result) {
        String action = valueOr(result.getString(AdminChargeDetailsBottomSheetFragment.RESULT_ACTION), "");
        String chargeId = valueOr(result.getString(AdminChargeDetailsBottomSheetFragment.RESULT_CHARGE_ID), "");
        if (chargeId.isEmpty()) {
            return;
        }
        if (AdminChargeDetailsBottomSheetFragment.ACTION_MARK_PAID.equals(action)) {
            interventionRepository.resolveChargePaid(chargeId, currentAdminUid(), (success, message, exception) -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
            });
            return;
        }
        if (AdminChargeDetailsBottomSheetFragment.ACTION_REMOVE_CHARGE.equals(action)) {
            interventionRepository.resolveChargeRemoved(chargeId, currentAdminUid(), (success, message, exception) -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
            });
        }
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

    @NonNull
    private String capitalize(@NonNull String text) {
        if (text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
