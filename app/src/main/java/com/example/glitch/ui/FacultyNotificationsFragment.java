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
import com.example.glitch.data.NotificationRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.UserProfile;
import com.google.android.material.snackbar.Snackbar;

/**
 * Faculty notifications inbox screen (US-07).
 * Pattern: Realtime RecyclerView feed bound to NotificationRepository.
 * Known issue: notifications read state acknowledgement is deferred to future iteration.
 */
public class FacultyNotificationsFragment extends Fragment {
    private NotificationRepository repository;
    private NotificationAdapter adapter;
    private TextView textEmpty;

    @NonNull
    public static FacultyNotificationsFragment newInstance() {
        return new FacultyNotificationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_faculty_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getNotificationRepository();
        textEmpty = view.findViewById(R.id.text_notifications_empty);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_notifications);
        adapter = new NotificationAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.PASSES);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        repository.listenNotifications(profile.getUid(), new NotificationRepository.NotificationListener() {
            @Override
            public void onData(@NonNull java.util.List<com.example.glitch.model.NotificationItem> notifications) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(notifications);
                    textEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_notifications, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }
}