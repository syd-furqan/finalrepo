package com.example.glitch.ui;

import android.content.Intent;
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
import com.example.glitch.data.AuditLogRepository;
import com.example.glitch.data.RepositoryProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

/**
 * Admin audit log screen with CSV export capability (US-13, US-15).
 * Pattern: Realtime timeline list + export action fragment.
 * Known issue: export shares raw CSV text instead of persisted file URI in v1.
 */
public class AdminAuditLogFragment extends Fragment {
    private AuditLogRepository repository;
    private AccessEventAdapter adapter;
    private TextView textEmpty;

    @NonNull
    public static AdminAuditLogFragment newInstance() {
        return new AdminAuditLogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_audit_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getAuditLogRepository();
        textEmpty = view.findViewById(R.id.text_audit_empty);
        MaterialButton buttonExportCsv = view.findViewById(R.id.button_export_csv);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_audit_logs);

        adapter = new AccessEventAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DASHBOARD);

        repository.listenAuditLogs(new AuditLogRepository.AuditLogListener() {
            @Override
            public void onData(@NonNull java.util.List<com.example.glitch.model.AccessEvent> events) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(events);
                    textEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_audit_logs, Snackbar.LENGTH_LONG).show());
            }
        });

        buttonExportCsv.setOnClickListener(v -> repository.exportCsv((success, csvContent, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!success) {
                    Snackbar.make(requireView(), R.string.error_export_logs, Snackbar.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/csv");
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.audit_export_subject));
                intent.putExtra(Intent.EXTRA_TEXT, csvContent);
                startActivity(Intent.createChooser(intent, getString(R.string.export_action)));
            });
        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }
}
