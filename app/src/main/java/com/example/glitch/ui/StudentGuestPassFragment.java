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
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Student guest pass generation, expiry selection, and cancellation screen (US-10/11/12).
 * Pattern: Form + list fragment backed by GuestPassRepository.
 * Known issue: expiry unit is limited to whole hours in v1.
 */
public class StudentGuestPassFragment extends Fragment implements GuestPassAdapter.GuestPassActionListener {
    private GuestPassRepository repository;
    private GuestPassAdapter adapter;
    private TextInputEditText inputGuestName;
    private TextInputEditText inputGuestId;
    private TextInputEditText inputExpiryHours;
    private TextView textEmpty;

    @NonNull
    public static StudentGuestPassFragment newInstance() {
        return new StudentGuestPassFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_student_guest_pass, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getGuestPassRepository();
        inputGuestName = view.findViewById(R.id.input_pass_guest_name);
        inputGuestId = view.findViewById(R.id.input_pass_guest_id);
        inputExpiryHours = view.findViewById(R.id.input_pass_expiry_hours);
        textEmpty = view.findViewById(R.id.text_guest_pass_empty);
        MaterialButton buttonCreate = view.findViewById(R.id.button_create_pass);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_guest_passes);

        adapter = new GuestPassAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.PASSES);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile != null) {
            repository.listenGuestPasses(profile.getUid(), new GuestPassRepository.PassListListener() {
                @Override
                public void onData(@NonNull java.util.List<GuestPass> passes) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        adapter.submitList(passes);
                        textEmpty.setVisibility(passes.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                }

                @Override
                public void onError(@NonNull Exception exception) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(), R.string.error_guest_pass_load, Snackbar.LENGTH_LONG).show());
                }
            });
        }

        buttonCreate.setOnClickListener(v -> createGuestPass());
    }

    private void createGuestPass() {
        String guestName = read(inputGuestName);
        String guestId = read(inputGuestId);
        String expiryRaw = read(inputExpiryHours);
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (guestName.isEmpty() || guestId.isEmpty() || expiryRaw.isEmpty() || profile == null) {
            Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
            return;
        }
        int expiryHours;
        try {
            expiryHours = Integer.parseInt(expiryRaw);
        } catch (NumberFormatException e) {
            Snackbar.make(requireView(), R.string.error_invalid_expiry, Snackbar.LENGTH_SHORT).show();
            return;
        }
        repository.createGuestPass(profile.getUid(), profile.getRole(),profile.getDisplayName(),profile.getEmail(), guestName, guestId, expiryHours, (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    @Override
    public void onCancelPass(@NonNull GuestPass pass) {
        repository.cancelGuestPass(pass.getId(), (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
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