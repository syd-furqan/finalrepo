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
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Faculty access-request form (US-06) for sponsoring guest entry.
 * Updated to manage Firestore listener lifecycle explicitly.
 */
public class FacultyAccessRequestFragment extends Fragment implements GuestPassAdapter.GuestPassActionListener {
    private GuestPassRepository repository;
    private GuestPassAdapter adapter;
    private TextView textEmpty;
    private ListenerRegistration passListener;

    @NonNull
    public static FacultyAccessRequestFragment newInstance() {
        return new FacultyAccessRequestFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_faculty_access_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getGuestPassRepository();

        TextInputEditText inputGuestName = view.findViewById(R.id.input_guest_name);
        TextInputEditText inputGuestId = view.findViewById(R.id.input_guest_id);
        MaterialButton buttonSubmit = view.findViewById(R.id.button_submit_request);
        MaterialButton buttonArchived = view.findViewById(R.id.button_view_archived_passes);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_guest_passes);
        textEmpty = view.findViewById(R.id.text_guest_pass_empty);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DASHBOARD);

        adapter = new GuestPassAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile != null) {
            passListener = repository.listenGuestPasses(profile.getUid(), new GuestPassRepository.PassListListener() {
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

        buttonSubmit.setOnClickListener(v -> {
            String guestName = read(inputGuestName);
            String guestId = read(inputGuestId);
            UserProfile userProfile = AuthUiGuard.requireProfile(this);

            if (guestName.isEmpty() || guestId.isEmpty() || userProfile == null) {
                Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
                return;
            }

            repository.issueGuestPassWithEntryRequest(
                    userProfile.getUid(),
                    userProfile.getRole(),
                    userProfile.getDisplayName(),
                    userProfile.getEmail(),
                    guestName,
                    guestId,
                    (success, message, issuedPass, exception) -> {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() ->
                                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
                    }
            );
        });
        buttonArchived.setOnClickListener(v -> openArchivedPasses());
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
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
    public void onSharePass(@NonNull GuestPass pass) {
        if (!isAdded()) {
            return;
        }
        try {
            PassShareHelper.share(this, pass);
        } catch (Exception exception) {
            Snackbar.make(requireView(), R.string.error_export_logs, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onViewPassDetails(@NonNull GuestPass pass) {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(GuestPassDetailsFragment.newInstance(pass), true);
    }

    private void openArchivedPasses() {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(GuestPassArchiveFragment.newInstance(), true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (passListener != null) {
            passListener.remove();
            passListener = null;
        }
    }
}
