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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Staff vehicle registration request and status history screen (US-08/US-09).
 * Pattern: Form + realtime list fragment backed by VehicleRequestRepository.
 */
public class StaffVehicleRequestFragment extends Fragment implements VehicleRequestAdapter.VehicleRequestActionListener {
    private VehicleRequestRepository repository;
    private VehicleRequestAdapter adapter;
    private TextView textEmpty;
    private TextView textStatusSummary;
    private TextInputEditText inputMake;
    private TextInputEditText inputModel;
    private TextInputEditText inputPlate;
    private TextInputEditText inputColor;
    private MaterialButton buttonSubmit;
    @Nullable
    private String editingRequestId;

    @NonNull
    public static StaffVehicleRequestFragment newInstance() {
        return new StaffVehicleRequestFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_staff_vehicle_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getVehicleRequestRepository();
        inputMake = view.findViewById(R.id.input_vehicle_make);
        inputModel = view.findViewById(R.id.input_vehicle_model);
        inputPlate = view.findViewById(R.id.input_plate_number);
        inputColor = view.findViewById(R.id.input_vehicle_color);
        buttonSubmit = view.findViewById(R.id.button_submit_vehicle_request);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_vehicle_requests);
        textEmpty = view.findViewById(R.id.text_vehicle_empty);
        textStatusSummary = view.findViewById(R.id.text_vehicle_status_summary);

        adapter = new VehicleRequestAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile != null) {
            repository.listenVehicleRequests(profile.getUid(), new VehicleRequestRepository.RequestListListener() {
                @Override
                public void onData(@NonNull java.util.List<com.example.glitch.model.VehicleRequestRecord> requests) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        adapter.submitList(requests);
                        textEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                        bindStatusSummary(requests);
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

        buttonSubmit.setOnClickListener(v -> onSubmitRequest());
    }

    @Override
    public void onVehicleRequestSelected(@NonNull com.example.glitch.model.VehicleRequestRecord record) {
        if (!"pending".equalsIgnoreCase(record.getStatus())) {
            Snackbar.make(requireView(), R.string.vehicle_edit_not_allowed, Snackbar.LENGTH_SHORT).show();
            return;
        }
        editingRequestId = record.getId();
        inputPlate.setText(record.getPlateNumber());
        inputMake.setText(record.getVehicleMake());
        inputModel.setText(record.getVehicleModel());
        inputColor.setText(record.getVehicleColor());
        buttonSubmit.setText(R.string.update_vehicle_request);
        Snackbar.make(requireView(), R.string.vehicle_edit_mode_enabled, Snackbar.LENGTH_SHORT).show();
    }

    private void onSubmitRequest() {
        String plate = read(inputPlate);
        String make = read(inputMake);
        String model = read(inputModel);
        String color = read(inputColor);
        UserProfile current = AuthUiGuard.requireProfile(this);
        if (plate.isEmpty() || model.isEmpty() || current == null) {
            Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (editingRequestId == null) {
            repository.submitVehicleRequest(current.getUid(), plate, make, model, color, (success, message, exception) -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (success) clearForm();
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                });
            });
            return;
        }
        repository.updateVehicleRequest(editingRequestId, plate, make, model, color, (success, message, exception) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    clearForm();
                    exitEditMode();
                }
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
            });
        });
    }

    private void clearForm() {
        inputMake.setText("");
        inputModel.setText("");
        inputPlate.setText("");
        inputColor.setText("");
    }

    private void exitEditMode() {
        editingRequestId = null;
        buttonSubmit.setText(R.string.submit_vehicle_request);
    }

    private void bindStatusSummary(@NonNull java.util.List<com.example.glitch.model.VehicleRequestRecord> requests) {
        if (requests.isEmpty()) {
            textStatusSummary.setVisibility(View.GONE);
            return;
        }
        int pending = 0, approved = 0, denied = 0;
        for (com.example.glitch.model.VehicleRequestRecord record : requests) {
            String status = record.getStatus().trim().toLowerCase();
            if ("approved".equals(status)) approved++;
            else if ("denied".equals(status) || "rejected".equals(status)) denied++;
            else pending++;
        }
        textStatusSummary.setVisibility(View.VISIBLE);
        textStatusSummary.setText(getString(R.string.vehicle_status_summary, pending, approved, denied));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
        editingRequestId = null;
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
}
