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
 * Known issue: editing submitted requests is currently unsupported.
 */
public class StaffVehicleRequestFragment extends Fragment {
    private VehicleRequestRepository repository;
    private VehicleRequestAdapter adapter;
    private TextView textEmpty;

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
        TextInputEditText inputMake = view.findViewById(R.id.input_vehicle_make);
        TextInputEditText inputPlate = view.findViewById(R.id.input_plate_number);
        TextInputEditText inputModel = view.findViewById(R.id.input_vehicle_model);
        MaterialButton buttonSubmit = view.findViewById(R.id.button_submit_vehicle_request);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_vehicle_requests);
        textEmpty = view.findViewById(R.id.text_vehicle_empty);

        adapter = new VehicleRequestAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile != null) {
            repository.listenVehicleRequests(profile.getUid(), new VehicleRequestRepository.RequestListListener() {
                @Override
                public void onData(@NonNull java.util.List<com.example.glitch.model.VehicleRequestRecord> requests) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        adapter.submitList(requests);
                        textEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
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

        buttonSubmit.setOnClickListener(v -> {
            String plate = read(inputPlate);
            String model = read(inputModel);
            String make = read(inputMake);
            UserProfile current = AuthUiGuard.requireProfile(this);
            if (plate.isEmpty() || model.isEmpty() || current == null) {
                Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
                return;
            }
            String fullModel = make.isEmpty() ? model : (make + " " + model);
            repository.submitVehicleRequest(current.getUid(), plate, fullModel, (success, message, exception) -> {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
}