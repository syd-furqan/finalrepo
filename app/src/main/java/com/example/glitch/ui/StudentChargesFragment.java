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
import com.example.glitch.model.UserProfile;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class StudentChargesFragment extends Fragment {
    private static final String ARG_TARGET_CHARGE_ID = "target_charge_id";

    private InterventionRepository interventionRepository;
    private StudentChargeAdapter adapter;
    private TextView textEmpty;
    private RecyclerView recyclerView;
    private String targetChargeId = "";

    @NonNull
    public static StudentChargesFragment newInstance() {
        return new StudentChargesFragment();
    }

    @NonNull
    public static StudentChargesFragment newInstance(@NonNull String targetChargeId) {
        StudentChargesFragment fragment = new StudentChargesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TARGET_CHARGE_ID, targetChargeId.trim());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_charges, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        interventionRepository = RepositoryProvider.getInterventionRepository();
        Bundle args = getArguments();
        targetChargeId = args == null ? "" : safe(args.getString(ARG_TARGET_CHARGE_ID));
        textEmpty = view.findViewById(R.id.text_student_charges_empty);

        recyclerView = view.findViewById(R.id.recycler_student_charges);
        adapter = new StudentChargeAdapter();
        adapter.setListener(this::requestRemoval);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.STUDENT_CHARGES);

        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) return;

        // Clear any stale global listener (e.g. from AdminChargesFragment) before
        // registering the student-scoped query on the shared repository instance.
        interventionRepository.removeListeners();

        interventionRepository.listenChargesByStudent(profile.getUid(), new InterventionRepository.FineListListener() {
            @Override
            public void onData(@NonNull List<FineCaseRecord> fineCases) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(fineCases);
                    textEmpty.setVisibility(fineCases.isEmpty() ? View.VISIBLE : View.GONE);
                    scrollToTargetCharge();
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), "Failed to load charges.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void requestRemoval(@NonNull FineCaseRecord charge, @NonNull String paymentNote) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) return;
        interventionRepository.requestChargeRemoval(charge.getId(), paymentNote, profile.getUid(),
                (success, message, exception) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
                }
        );
    }

    private void scrollToTargetCharge() {
        if (targetChargeId.isEmpty() || recyclerView == null) {
            return;
        }
        int position = adapter.indexOfChargeId(targetChargeId);
        if (position == RecyclerView.NO_POSITION) {
            Snackbar.make(requireView(), "Linked charge was not found.", Snackbar.LENGTH_LONG).show();
            targetChargeId = "";
            return;
        }
        recyclerView.post(() -> recyclerView.smoothScrollToPosition(position));
        Snackbar.make(requireView(), "Linked charge is in your charges list.", Snackbar.LENGTH_LONG).show();
        targetChargeId = "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (interventionRepository != null) interventionRepository.removeListeners();
    }

    @NonNull
    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
