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
import com.example.glitch.data.AlertRepository;
import com.example.glitch.data.RepositoryProvider;
import com.google.android.material.snackbar.Snackbar;

/**
 * Admin alert monitoring screen for repeated verification failures (US-18).
 * Pattern: Realtime list fragment bound to AlertRepository.
 * Known issue: acknowledge/snooze actions are deferred beyond this milestone.
 */
public class AdminAlertsFragment extends Fragment {
    private AlertRepository repository;
    private SecurityAlertAdapter adapter;
    private TextView textEmpty;

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
        textEmpty = view.findViewById(R.id.text_alerts_empty);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_alerts);

        adapter = new SecurityAlertAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        repository.listenAlerts(new AlertRepository.AlertListener() {
            @Override
            public void onData(@NonNull java.util.List<com.example.glitch.model.SecurityAlert> alerts) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(alerts);
                    textEmpty.setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);
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
}
