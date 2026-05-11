package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
    private ViewGroup animContent;
    private static final String ARG_ANNOUNCEMENTS_ONLY = "arg_announcements_only";
    private static final String ARG_START_WITH_ANNOUNCEMENTS = "arg_start_with_announcements";

    private NotificationRepository repository;
    private GuestPassRepository guestPassRepository;
    private ViolationReportRepository violationReportRepository;
    private NotificationAdapter adapter;
    private TextView textEmpty;
    private MaterialButton buttonMarkAllRead;
    private MaterialButton buttonFilterAll;
    private MaterialButton buttonFilterAnnouncements;
    private TextView textBannerChannel;
    private TextView textBannerTitle;
    private TextView textBannerSubtitle;

    private final List<NotificationItem> currentNotifications = new ArrayList<>();
    private String currentUserUid;
    private String currentRole = "";
    private boolean announcementsOnlyMode;
    private boolean announcementsFilterSelected;
    private boolean hasShownLoadErrorForCurrentFeed;

    @NonNull
    public static NotificationCenterFragment newInstance() {
        return new NotificationCenterFragment();
    }

    @NonNull
    public static NotificationCenterFragment newInstanceAnnouncementsOnly() {
        NotificationCenterFragment fragment = new NotificationCenterFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_ANNOUNCEMENTS_ONLY, true);
        args.putBoolean(ARG_START_WITH_ANNOUNCEMENTS, true);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static NotificationCenterFragment newInstanceWithAnnouncementsCategory() {
        NotificationCenterFragment fragment = new NotificationCenterFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_START_WITH_ANNOUNCEMENTS, true);
        fragment.setArguments(args);
        return fragment;
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

        Bundle args = getArguments();
        announcementsOnlyMode = this instanceof SecurityAnnouncementsFragment
                || (args != null && args.getBoolean(ARG_ANNOUNCEMENTS_ONLY, false));
        announcementsFilterSelected = announcementsOnlyMode
                || (args != null && args.getBoolean(ARG_START_WITH_ANNOUNCEMENTS, false));

        textEmpty = view.findViewById(R.id.text_notifications_empty);
        buttonMarkAllRead = view.findViewById(R.id.button_mark_all_read);
        buttonFilterAll = view.findViewById(R.id.button_filter_all_notifications);
        buttonFilterAnnouncements = view.findViewById(R.id.button_filter_announcements);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_notifications);
        adapter = new NotificationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        RoleDestination navDestination = announcementsOnlyMode ? RoleDestination.ANNOUNCEMENTS : RoleDestination.NOTIFICATIONS;
        RoleNavRouter.bindBottomNav(view, this, navDestination);
        configureBanner();
        configureFilterButtons();

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
        startNotificationFeed();

        animContent = view.findViewById(R.id.anim_content);
    }

    public boolean isShowingAnnouncementsOnly() {
        return announcementsOnlyMode || announcementsFilterSelected;
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
        hasShownLoadErrorForCurrentFeed = false;
    }

    private void configureBanner() {
        if (textBannerChannel == null || textBannerTitle == null || textBannerSubtitle == null) {
            return;
        }
        if (announcementsOnlyMode) {
            textBannerChannel.setText(R.string.announcements_banner_channel);
            textBannerTitle.setText(R.string.announcements_banner_title);
            textBannerSubtitle.setText(R.string.announcements_banner_subtitle_security);
            return;
        }
        if (announcementsFilterSelected) {
            textBannerChannel.setText(R.string.announcements_banner_channel);
            textBannerTitle.setText(R.string.announcements_banner_title);
            textBannerSubtitle.setText(R.string.announcements_banner_subtitle_users);
        } else {
            textBannerChannel.setText(R.string.notifications_banner_channel);
            textBannerTitle.setText(R.string.notifications_banner_title);
            textBannerSubtitle.setText(R.string.notifications_banner_subtitle);
        }
    }

    private void configureFilterButtons() {
        if (buttonFilterAll == null || buttonFilterAnnouncements == null) {
            return;
        }
        if (announcementsOnlyMode) {
            buttonFilterAll.setVisibility(View.GONE);
            buttonFilterAnnouncements.setVisibility(View.GONE);
            return;
        }
        buttonFilterAll.setVisibility(View.VISIBLE);
        buttonFilterAnnouncements.setVisibility(View.VISIBLE);
        buttonFilterAll.setOnClickListener(v -> {
            if (!announcementsFilterSelected) {
                return;
            }
            announcementsFilterSelected = false;
            configureBanner();
            applyFilterButtonStyles();
            startNotificationFeed();
        });
        buttonFilterAnnouncements.setOnClickListener(v -> {
            if (announcementsFilterSelected) {
                return;
            }
            announcementsFilterSelected = true;
            configureBanner();
            applyFilterButtonStyles();
            startNotificationFeed();
        });
        applyFilterButtonStyles();
    }

    private void applyFilterButtonStyles() {
        if (buttonFilterAll == null || buttonFilterAnnouncements == null || !isAdded()) {
            return;
        }
        buttonFilterAll.setAlpha(announcementsFilterSelected ? 0.45f : 1f);
        buttonFilterAnnouncements.setAlpha(announcementsFilterSelected ? 1f : 0.45f);
    }

    private void startNotificationFeed() {
        if (currentUserUid == null || currentUserUid.trim().isEmpty()) {
            return;
        }
        currentNotifications.clear();
        hasShownLoadErrorForCurrentFeed = false;

        NotificationRepository.NotificationListener listener = new NotificationRepository.NotificationListener() {
            @Override
            public void onData(@NonNull List<NotificationItem> notifications) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    hasShownLoadErrorForCurrentFeed = false;
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
                if (hasShownLoadErrorForCurrentFeed) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_notifications, Snackbar.LENGTH_LONG).show());
                hasShownLoadErrorForCurrentFeed = true;
            }
        };

        if (announcementsOnlyMode || announcementsFilterSelected) {
            repository.listenAnnouncements(currentUserUid, listener);
            return;
        }
        repository.listenNotifications(currentUserUid, listener);
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

    @Override
    public void onResume() {
        super.onResume();
        if (animContent != null) UiAnimations.animateFallIn(animContent);
    }
}
