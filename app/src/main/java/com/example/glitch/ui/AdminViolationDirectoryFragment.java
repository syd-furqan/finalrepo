package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.ViolationReportRepository;
import com.example.glitch.model.ViolationReport;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AdminViolationDirectoryFragment extends Fragment {
    private ViolationReportRepository repository;
    private ViolationReportAdapter adapter;
    private TextView textEmpty;

    @NonNull
    public static AdminViolationDirectoryFragment newInstance() {
        return new AdminViolationDirectoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_violation_directory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getViolationReportRepository();
        textEmpty = view.findViewById(R.id.text_reports_empty);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_violation_reports);
        adapter = new ViolationReportAdapter();
        adapter.setListener(this::openViolationDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        ImageButton buttonRefresh = view.findViewById(R.id.button_refresh_reports);
        buttonRefresh.setOnClickListener(v -> loadReports());

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VIOLATION_DIRECTORY);

        getParentFragmentManager().setFragmentResultListener(
                AdminViolationDetailBottomSheetFragment.RESULT_KEY,
                getViewLifecycleOwner(),
                (key, result) -> {
                    // Reload to reflect any status changes
                    loadReports();
                }
        );

        loadReports();
    }

    private void loadReports() {
        repository.listenAllReports(new ViolationReportRepository.ReportListListener() {
            @Override
            public void onData(@NonNull List<ViolationReport> reports) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(reports);
                    textEmpty.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), "Failed to load reports.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void openViolationDetail(@NonNull ViolationReport report) {
        if (!isAdded()) return;
        AdminViolationDetailBottomSheetFragment.newInstance(report)
                .show(getParentFragmentManager(), AdminViolationDetailBottomSheetFragment.TAG);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (repository != null) repository.removeListeners();
    }
}
