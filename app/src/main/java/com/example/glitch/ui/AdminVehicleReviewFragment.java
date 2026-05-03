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
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.VehicleRequestRepository;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.VehicleRequestRecord;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Admin screen for reviewing staff vehicle registration requests.
 * Pattern: Realtime list fragment with approve/deny row actions.
 */
public class AdminVehicleReviewFragment extends Fragment implements AdminVehicleReviewAdapter.VehicleReviewActionListener {
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
            Snackbar.make(requireView(), R.string.admin_vehicle_review_admin_only, Snackbar.LENGTH_LONG).show();
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
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(requests);
                    textEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                    bindSummary(requests);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_vehicle_load, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onApprove(@NonNull VehicleRequestRecord record) {
        review(record, true, "");
    }

    @Override
    public void onDeny(@NonNull VehicleRequestRecord record) {
        review(record, false, "Denied by admin");
    }

    private void review(@NonNull VehicleRequestRecord record, boolean approved, @NonNull String note) {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }
        repository.reviewVehicleRequest(record.getId(), profile.getUid(), approved, note, (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    private void bindSummary(@NonNull List<VehicleRequestRecord> requests) {
        int pending = 0;
        int approved = 0;
        int denied = 0;
        for (VehicleRequestRecord record : requests) {
            String status = record.getStatus().trim().toLowerCase();
            if ("approved".equals(status)) {
                approved++;
            } else if ("denied".equals(status) || "rejected".equals(status)) {
                denied++;
            } else {
                pending++;
            }
        }
        textSummary.setText(getString(R.string.vehicle_status_summary, pending, approved, denied));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (repository != null) {
            repository.removeListeners();
        }
    }
}
