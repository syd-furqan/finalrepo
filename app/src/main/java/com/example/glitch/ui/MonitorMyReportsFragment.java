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
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.ViolationReportRepository;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.ViolationReport;

import java.util.List;

public class MonitorMyReportsFragment extends Fragment {
    private ViolationReportRepository repository;
    private ViolationReportAdapter adapter;
    private TextView textEmpty;

    @NonNull
    public static MonitorMyReportsFragment newInstance() {
        return new MonitorMyReportsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitor_my_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getViolationReportRepository();
        textEmpty = view.findViewById(R.id.text_my_reports_empty);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_my_reports);
        adapter = new ViolationReportAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.MONITOR_MY_REPORTS);

        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) return;

        repository.listenReportsByReporter(profile.getUid(), new ViolationReportRepository.ReportListListener() {
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
                        textEmpty.setVisibility(View.VISIBLE));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (repository != null) repository.removeListeners();
    }
}
