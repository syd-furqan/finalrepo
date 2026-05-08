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
import com.example.glitch.model.NotificationItem;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Faculty notifications inbox screen (US-07).
 * Pattern: Realtime RecyclerView feed bound to NotificationRepository.
 * Known issue: read acknowledgement currently updates one item at a time.
 */
public class FacultyNotificationsFragment extends Fragment implements NotificationAdapter.NotificationActionListener {
    private NotificationRepository repository;
    private NotificationAdapter adapter;
    private TextView textEmpty;
    private MaterialButton buttonMarkAllRead;
    private final List<NotificationItem> currentNotifications = new ArrayList<>();
    private String currentUserUid;

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
        buttonMarkAllRead = view.findViewById(R.id.button_mark_all_read);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_notifications);
        adapter = new NotificationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.FACULTY_NOTIFICATIONS);
        buttonMarkAllRead.setOnClickListener(v -> markAllNotificationsRead());

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }
        currentUserUid = profile.getUid();

        repository.listenNotifications(profile.getUid(), new NotificationRepository.NotificationListener() {
            @Override
            public void onData(@NonNull java.util.List<com.example.glitch.model.NotificationItem> notifications) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    currentNotifications.clear();
                    currentNotifications.addAll(notifications);
                    adapter.submitList(notifications);
                    textEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    buttonMarkAllRead.setVisibility(hasUnreadNotifications(notifications) ? View.VISIBLE : View.GONE);
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
    public void onNotificationSelected(@NonNull com.example.glitch.model.NotificationItem item) {
        if (item.isRead() || currentUserUid == null || currentUserUid.trim().isEmpty()) {
            return;
        }
        repository.markNotificationRead(currentUserUid, item.getId(), (success, message, exception) -> {
            if (!isAdded() || success) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
        currentNotifications.clear();
        currentUserUid = null;
    }

    private void markAllNotificationsRead() {
        if (currentUserUid == null || currentUserUid.trim().isEmpty() || !hasUnreadNotifications(currentNotifications)) {
            return;
        }
        repository.markAllNotificationsRead(currentUserUid, (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show());
        });
    }

    private boolean hasUnreadNotifications(@NonNull List<NotificationItem> notifications) {
        for (NotificationItem item : notifications) {
            if (!item.isRead()) {
                return true;
            }
        }
        return false;
    }
}
