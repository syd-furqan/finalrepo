package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.VehicleRequestRepository;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.VehicleRequestRecord;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * Admin screen for reviewing all staff vehicle registration requests (US-10).
 * Pattern: Realtime list fragment; approve/deny actions run Firestore transactions.
 */
public class AdminVehicleReviewFragment extends Fragment implements AdminVehicleReviewAdapter.ActionListener {

    private VehicleRequestRepository repository;
    private AdminVehicleReviewAdapter adapter;
    private TextView textEmpty;
    private TextView textSummary;

    @NonNull
    public static AdminVehicleReviewFragment newInstance() {
        return new AdminVehicleReviewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_vehicle_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }
        if (!"admin".equalsIgnoreCase(profile.getRole())) {
            RoleNavRouter.route(this, RoleDestination.DIRECTORY);
            return;
        }

        repository = RepositoryProvider.getVehicleRequestRepository();
        textEmpty = view.findViewById(R.id.text_vehicle_review_empty);
        textSummary = view.findViewById(R.id.text_vehicle_review_summary);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_vehicle_review);
        adapter = new AdminVehicleReviewAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        repository.listenAllVehicleRequests(new VehicleRequestRepository.RequestListListener() {
            @Override
            public void onData(@NonNull List<VehicleRequestRecord> requests) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(requests);
                    textEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                    bindSummary(requests);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_vehicle_load, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onApprove(@NonNull VehicleRequestRecord record) {
        showReviewDialog(record, true);
    }

    @Override
    public void onDeny(@NonNull VehicleRequestRecord record) {
        showReviewDialog(record, false);
    }

    private void showReviewDialog(@NonNull VehicleRequestRecord record, boolean approved) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_vehicle_review_note, null, false);
        TextInputEditText inputNote = dialogView.findViewById(R.id.input_review_note);

        String title = approved ? getString(R.string.vehicle_approve_title) : getString(R.string.vehicle_deny_title);
        String positiveLabel = approved ? getString(R.string.vehicle_approve_action) : getString(R.string.vehicle_deny_action);

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(positiveLabel, (dialog, which) -> {
                    CharSequence raw = inputNote.getText();
                    String note = raw == null ? "" : raw.toString().trim();
                    submitReview(record, approved, note);
                })
                .setNegativeButton(R.string.cancel_action, null)
                .show();
    }

    private void submitReview(@NonNull VehicleRequestRecord record, boolean approved, @NonNull String note) {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) return;

        repository.reviewVehicleRequest(
                record.getId(),
                profile.getUid(),
                approved,
                note,
                (success, message, exception) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
                }
        );
    }

    private void bindSummary(@NonNull List<VehicleRequestRecord> requests) {
        if (requests.isEmpty()) {
            textSummary.setVisibility(View.GONE);
            return;
        }
        int pending = 0, approved = 0, denied = 0;
        for (VehicleRequestRecord r : requests) {
            String s = r.getStatus().trim().toLowerCase();
            if ("approved".equals(s)) approved++;
            else if ("denied".equals(s)) denied++;
            else pending++;
        }
        textSummary.setVisibility(View.VISIBLE);
        textSummary.setText(getString(R.string.vehicle_status_summary, pending, approved, denied));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }
}
