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
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Archived guest passes screen (expired + cancelled).
 */
public class GuestPassArchiveFragment extends Fragment implements GuestPassAdapter.GuestPassActionListener {
    private GuestPassRepository repository;
    private GuestPassAdapter adapter;
    private TextView textEmpty;

    @NonNull
    public static GuestPassArchiveFragment newInstance() {
        return new GuestPassArchiveFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_guest_pass_archive, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getGuestPassRepository();
        textEmpty = view.findViewById(R.id.text_archived_guest_pass_empty);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_archived_guest_passes);
        adapter = new GuestPassAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.PASSES);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }
        repository.listenArchivedGuestPasses(profile.getUid(), new GuestPassRepository.PassListListener() {
            @Override
            public void onData(@NonNull List<GuestPass> passes) {
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

    @Override
    public void onCancelPass(@NonNull GuestPass pass) {
        Snackbar.make(requireView(), R.string.archived_pass_read_only, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onSharePass(@NonNull GuestPass pass) {
        Snackbar.make(requireView(), R.string.archived_pass_not_shareable, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onViewPassDetails(@NonNull GuestPass pass) {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(GuestPassDetailsFragment.newInstance(pass), true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }
}
