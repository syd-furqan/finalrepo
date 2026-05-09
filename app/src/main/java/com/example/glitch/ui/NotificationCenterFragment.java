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
import com.example.glitch.data.NotificationRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.ViolationReportRepository;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.NotificationItem;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.ViolationReport;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationCenterFragment extends Fragment implements NotificationAdapter.NotificationActionListener {
    private NotificationRepository repository;
    private GuestPassRepository guestPassRepository;
    private ViolationReportRepository violationReportRepository;
    private NotificationAdapter adapter;
    private TextView textEmpty;
    private MaterialButton buttonMarkAllRead;
    private final List<NotificationItem> currentNotifications = new ArrayList<>();
    private String currentUserUid;
    private String currentRole = "";

    @NonNull
    public static NotificationCenterFragment newInstance() {
        return new NotificationCenterFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_faculty_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getNotificationRepository();
        guestPassRepository = RepositoryProvider.getGuestPassRepository();
        violationReportRepository = RepositoryProvider.getViolationReportRepository();
        textEmpty = view.findViewById(R.id.text_notifications_empty);
        buttonMarkAllRead = view.findViewById(R.id.button_mark_all_read);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_notifications);
        adapter = new NotificationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.NOTIFICATIONS);
        buttonMarkAllRead.setOnClickListener(v -> markAllNotificationsRead());
        getParentFragmentManager().setFragmentResultListener(
                NotificationDetailsBottomSheetFragment.RESULT_KEY,
                getViewLifecycleOwner(),
                (key, result) -> openRelatedSource(
                        safe(result.getString(NotificationDetailsBottomSheetFragment.RESULT_SOURCE_COLLECTION)),
                        safe(result.getString(NotificationDetailsBottomSheetFragment.RESULT_SOURCE_ID))
                )
        );

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }
        currentUserUid = profile.getUid();
        currentRole = profile.getRole().trim().toLowerCase(Locale.US);
        repository.listenNotifications(currentUserUid, new NotificationRepository.NotificationListener() {
            @Override
            public void onData(@NonNull List<NotificationItem> notifications) {
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
    public void onNotificationSelected(@NonNull NotificationItem item) {
        if (!isAdded()) return;
        NotificationDetailsBottomSheetFragment.newInstance(item)
                .show(getParentFragmentManager(), NotificationDetailsBottomSheetFragment.TAG);
        if (!item.isRead() && currentUserUid != null && !currentUserUid.trim().isEmpty()) {
            repository.markNotificationRead(currentUserUid, item.getId(), (success, message, exception) -> {
                if (!isAdded() || success) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show());
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (repository != null) {
            repository.removeListeners();
        }
        currentNotifications.clear();
        currentUserUid = null;
        currentRole = "";
    }

    private void openRelatedSource(@NonNull String sourceCollection, @NonNull String sourceId) {
        if (!isAdded() || sourceCollection.isEmpty() || sourceId.isEmpty()) {
            return;
        }
        String collection = sourceCollection.toLowerCase(Locale.US);
        if ("guest_passes".equals(collection)) {
            openGuestPassSource(sourceId);
            return;
        }
        if ("vehicle_requests".equals(collection)) {
            routeToFragment(SponsorVehicleRegistrationFragment.newInstance(sourceId));
            return;
        }
        if ("fine_cases".equals(collection)) {
            if ("student".equals(currentRole)) {
                routeToFragment(StudentChargesFragment.newInstance(sourceId));
            } else {
                Snackbar.make(requireView(), "Linked charge page is unavailable for this role.", Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        if ("violation_reports".equals(collection)) {
            openViolationReportSource(sourceId);
            return;
        }
        Snackbar.make(requireView(), "Linked record is unavailable.", Snackbar.LENGTH_LONG).show();
    }

    private void openGuestPassSource(@NonNull String passId) {
        guestPassRepository.findPassById(passId, new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (pass == null) {
                        Snackbar.make(requireView(), "Linked guest pass was not found.", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    routeToFragment(GuestPassDetailsFragment.newInstance(pass));
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), "Failed to open linked guest pass.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void openViolationReportSource(@NonNull String reportId) {
        violationReportRepository.getReportById(reportId, new ViolationReportRepository.SingleReportListener() {
            @Override
            public void onData(@Nullable ViolationReport report) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (report == null) {
                        Snackbar.make(requireView(), "Linked violation report was not found.", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    ViolationReportReadOnlyBottomSheetFragment.newInstance(report)
                            .show(getParentFragmentManager(), ViolationReportReadOnlyBottomSheetFragment.TAG);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), "Failed to open linked violation report.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void routeToFragment(@NonNull Fragment fragment) {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(fragment, true);
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

    @NonNull
    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
