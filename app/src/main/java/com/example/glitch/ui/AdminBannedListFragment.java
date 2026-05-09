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
import com.example.glitch.data.InterventionRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.GuestBanRecord;
import com.example.glitch.model.UserProfile;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AdminBannedListFragment extends Fragment {
    private static final String ARG_TARGET_CNIC = "target_cnic";

    private InterventionRepository interventionRepository;
    private BannedGuestAdapter adapter;
    private TextView textEmpty;
    private RecyclerView recyclerView;
    private String targetCnic = "";

    @NonNull
    public static AdminBannedListFragment newInstance() {
        return new AdminBannedListFragment();
    }

    @NonNull
    public static AdminBannedListFragment newInstance(@NonNull String targetCnic) {
        AdminBannedListFragment fragment = new AdminBannedListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TARGET_CNIC, targetCnic.trim());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_banned_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        interventionRepository = RepositoryProvider.getInterventionRepository();
        textEmpty = view.findViewById(R.id.text_banned_empty);
        Bundle args = getArguments();
        targetCnic = args == null ? "" : safe(args.getString(ARG_TARGET_CNIC));

        recyclerView = view.findViewById(R.id.recycler_banned_guests);
        adapter = new BannedGuestAdapter();
        adapter.setListener(this::confirmUnban);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.BANNED_LIST);

        interventionRepository.listenBans(new InterventionRepository.BanListListener() {
            @Override
            public void onData(@NonNull List<GuestBanRecord> bans) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(bans);
                    textEmpty.setVisibility(bans.isEmpty() ? View.VISIBLE : View.GONE);
                    scrollToTargetBan();
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), "Failed to load banned list.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void confirmUnban(@NonNull GuestBanRecord record) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) return;
        interventionRepository.unbanGuest(record.getId(), profile.getUid(), (success, message, exception) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    private void scrollToTargetBan() {
        if (targetCnic.trim().isEmpty() || recyclerView == null) {
            return;
        }
        int position = adapter.indexOfCnic(targetCnic);
        if (position == RecyclerView.NO_POSITION) {
            Snackbar.make(requireView(), "Linked banned guest was not found.", Snackbar.LENGTH_LONG).show();
            targetCnic = "";
            return;
        }
        recyclerView.post(() -> recyclerView.smoothScrollToPosition(position));
        targetCnic = "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (interventionRepository != null) interventionRepository.removeListeners();
    }

    @NonNull
    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
