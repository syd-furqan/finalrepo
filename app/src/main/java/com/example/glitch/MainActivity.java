package com.example.glitch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.glitch.auth.AuthRepository;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.SingleGateMigrationRunner;
import com.example.glitch.data.NotificationRepository;
import com.example.glitch.data.FirestoreNotificationRepository;
import com.example.glitch.model.GuestPassTimePolicy;
import com.example.glitch.model.NotificationItem;
import com.example.glitch.model.UserProfile;
import com.example.glitch.notification.NotificationLocalAlertCoordinator;
import com.example.glitch.ui.AnnouncementInterruptDialogFragment;
import com.example.glitch.ui.LoginFragment;
import com.example.glitch.ui.NavigationHost;
import com.example.glitch.ui.GuardLanguageHelper;
import com.example.glitch.ui.RoleDestination;
import com.example.glitch.ui.RoleHomeFragment;
import com.example.glitch.ui.RoleNavRouter;
import com.example.glitch.ui.NotificationCenterFragment;

import java.util.List;
import java.util.Locale;

/**
 * Host activity that owns the fragment container for the app's primary navigation surface.
 */
public class MainActivity extends AppCompatActivity implements NavigationHost {
    public static final String ACTION_OPEN_NOTIFICATIONS = "com.example.glitch.action.OPEN_NOTIFICATIONS";
    public static final String EXTRA_LAUNCH_DESTINATION = "extra_launch_destination";
    public static final String EXTRA_OPEN_ANNOUNCEMENTS_CATEGORY = "extra_open_announcements_category";
    public static final String DESTINATION_NOTIFICATIONS = "NOTIFICATIONS";
    public static final String DESTINATION_ANNOUNCEMENTS = "ANNOUNCEMENTS";

    private static final int REQUEST_POST_NOTIFICATIONS = 1201;
    private static final boolean TEMP_ENABLE_TIME_POLICY_TEST_BYPASS = false;

    private AuthRepository authRepository;
    private NotificationLocalAlertCoordinator notificationLocalAlertCoordinator;
    private SingleGateMigrationRunner singleGateMigrationRunner;
    private NotificationRepository announcementGateRepository;
    private boolean isActivityResumed;
    private boolean isAnnouncementInterruptVisible;
    @Nullable
    private NotificationItem latestUnreadAnnouncement;
    @Nullable
    private String lastAnnouncementInterruptId;
    private String activeAnnouncementUid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        GuardLanguageHelper.syncApplicationLocalesForCurrentGuard(this);
        super.onCreate(savedInstanceState);
        boolean debugBuild = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        GuestPassTimePolicy.setTestingBypassEnabled(
                debugBuild && TEMP_ENABLE_TIME_POLICY_TEST_BYPASS
        );
        authRepository = RepositoryProvider.getAuthRepository();
        announcementGateRepository = new FirestoreNotificationRepository();
        notificationLocalAlertCoordinator = new NotificationLocalAlertCoordinator(getApplicationContext());
        singleGateMigrationRunner = new SingleGateMigrationRunner();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().setFragmentResultListener(
                AnnouncementInterruptDialogFragment.RESULT_KEY,
                this,
                (key, bundle) -> acknowledgeAnnouncement(bundle.getString(AnnouncementInterruptDialogFragment.RESULT_NOTIFICATION_ID, ""))
        );
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            bootstrapSessionFromLaunchIntent(getIntent());
            return;
        }
        enforceRestoredStateGuard();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) {
            bootstrapSessionFromLaunchIntent(intent);
            return;
        }
        showRoleHome(
                profile,
                true,
                resolveLaunchDestination(intent),
                shouldStartWithAnnouncementsCategory(intent)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityResumed = true;
        maybeShowAnnouncementInterrupt();
        enforceRestoredStateGuard();
    }

    @Override
    protected void onPause() {
        isActivityResumed = false;
        super.onPause();
    }

    private void bootstrapSessionFromLaunchIntent(@NonNull Intent launchIntent) {
        authRepository.validateCurrentSession((success, profile, message) -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (success && profile != null) {
                showRoleHome(
                        profile,
                        true,
                        resolveLaunchDestination(launchIntent),
                        shouldStartWithAnnouncementsCategory(launchIntent)
                );
                return;
            }
            showLogin(true);
        }));
    }

    @Nullable
    private RoleDestination resolveLaunchDestination(@Nullable Intent launchIntent) {
        if (launchIntent == null) {
            return null;
        }
        String destinationName = launchIntent.getStringExtra(EXTRA_LAUNCH_DESTINATION);
        if (destinationName == null || destinationName.trim().isEmpty()) {
            return null;
        }
        try {
            RoleDestination parsed = RoleDestination.valueOf(destinationName.trim());
            if (parsed == RoleDestination.LOGOUT) {
                return null;
            }
            return parsed;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean shouldStartWithAnnouncementsCategory(@Nullable Intent launchIntent) {
        return launchIntent != null
                && launchIntent.getBooleanExtra(EXTRA_OPEN_ANNOUNCEMENTS_CATEGORY, false);
    }

    private void enforceRestoredStateGuard() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        boolean isLoginScreenVisible = current == null || current instanceof LoginFragment;
        boolean hasSessionProfile = SessionManager.getCurrentProfile() != null;
        if (MainActivityStartupPolicy.shouldRedirectToLogin(isLoginScreenVisible, hasSessionProfile)) {
            authRepository.logout();
            SessionManager.clear();
            showLogin(true);
        }
    }

    @Override
    public void showLogin(boolean clearBackStack) {
        if (notificationLocalAlertCoordinator != null) {
            notificationLocalAlertCoordinator.stop();
        }
        stopAnnouncementInterruptMonitoring();
        GuardLanguageHelper.resetApplicationLocaleToDefault();
        if (clearBackStack) {
            clearBackStack();
        }
        replaceRootFragment(LoginFragment.newInstance(), false);
    }

    @Override
    public void showRoleHome(@NonNull UserProfile profile, boolean clearBackStack) {
        showRoleHome(profile, clearBackStack, null, false);
    }

    private void showRoleHome(
            @NonNull UserProfile profile,
            boolean clearBackStack,
            @Nullable RoleDestination preferredDestination,
            boolean startWithAnnouncementsCategory
    ) {
        SessionManager.setCurrentProfile(profile);
        startAnnouncementInterruptMonitoring(profile);
        GuardLanguageHelper.syncApplicationLocalesForCurrentGuard(this);
        ensurePostNotificationPermission(profile);
        if (notificationLocalAlertCoordinator != null) {
            notificationLocalAlertCoordinator.start(profile);
        }
        if (singleGateMigrationRunner != null) {
            singleGateMigrationRunner.runIfNeeded(profile.getRole(), profile.getUid());
        }
        if (clearBackStack) {
            clearBackStack();
        }
        RoleDestination landingDestination = preferredDestination == null
                ? RoleNavRouter.getDefaultDestinationForRole(profile.getRole())
                : RoleNavRouter.resolveDestinationForRole(preferredDestination, profile.getRole());
        Fragment landingFragment = resolveLandingFragment(
                profile,
                landingDestination,
                startWithAnnouncementsCategory
        );
        if (landingFragment == null) {
            landingFragment = RoleHomeFragment.newInstance(
                    profile.getUid(),
                    profile.getDisplayName(),
                    profile.getEmail(),
                    profile.getRole()
            );
        }
        replaceRootFragment(landingFragment, false);
    }

    @Override
    public void showFragment(@NonNull Fragment fragment, boolean addToBackStack) {
        FragmentManager manager = getSupportFragmentManager();
        boolean stateSaved = manager.isStateSaved();
        if (addToBackStack) {
            manager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(fragment.getClass().getSimpleName())
                    .commitAllowingStateLoss();
            return;
        }
        replaceRootFragment(fragment, stateSaved);
    }

    private void replaceRootFragment(@NonNull Fragment fragment, boolean alreadyStateSaved) {
        FragmentManager manager = getSupportFragmentManager();
        boolean stateSaved = alreadyStateSaved || manager.isStateSaved();
        if (stateSaved) {
            manager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitAllowingStateLoss();
            return;
        }
        manager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void clearBackStack() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    protected void onDestroy() {
        if (notificationLocalAlertCoordinator != null) {
            notificationLocalAlertCoordinator.stop();
        }
        stopAnnouncementInterruptMonitoring();
        super.onDestroy();
    }

    private void startAnnouncementInterruptMonitoring(@NonNull UserProfile profile) {
        if (!supportsForegroundAnnouncementInterrupt(profile.getRole())) {
            stopAnnouncementInterruptMonitoring();
            return;
        }
        if (announcementGateRepository == null) {
            announcementGateRepository = new FirestoreNotificationRepository();
        }
        activeAnnouncementUid = profile.getUid();
        latestUnreadAnnouncement = null;
        isAnnouncementInterruptVisible = false;
        lastAnnouncementInterruptId = null;
        announcementGateRepository.listenAnnouncements(
                profile.getUid(),
                new NotificationRepository.NotificationListener() {
                    @Override
                    public void onData(@NonNull List<NotificationItem> notifications) {
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            latestUnreadAnnouncement = findNewestUnreadAnnouncement(notifications);
                            if (latestUnreadAnnouncement == null) {
                                if (!isAnnouncementDialogStillMounted()) {
                                    isAnnouncementInterruptVisible = false;
                                }
                                return;
                            }
                            maybeShowAnnouncementInterrupt();
                        });
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                isAnnouncementInterruptVisible = isAnnouncementDialogStillMounted();
                            }
                        });
                    }
                }
        );
    }

    private void stopAnnouncementInterruptMonitoring() {
        latestUnreadAnnouncement = null;
        activeAnnouncementUid = "";
        isAnnouncementInterruptVisible = false;
        lastAnnouncementInterruptId = null;
        if (announcementGateRepository != null) {
            announcementGateRepository.removeListeners();
        }
    }

    private boolean supportsForegroundAnnouncementInterrupt(@Nullable String rawRole) {
        if (rawRole == null) {
            return false;
        }
        String role = rawRole.trim().toLowerCase(Locale.US);
        return "student".equals(role) || "faculty".equals(role);
    }

    @Nullable
    private NotificationItem findNewestUnreadAnnouncement(@NonNull List<NotificationItem> notifications) {
        for (NotificationItem item : notifications) {
            if (!item.isRead()) {
                return item;
            }
        }
        return null;
    }

    private void maybeShowAnnouncementInterrupt() {
        if (!isActivityResumed || isFinishing() || isDestroyed()) {
            return;
        }
        if (getSupportFragmentManager().isStateSaved()) {
            return;
        }
        if (isAnnouncementDialogStillMounted()) {
            isAnnouncementInterruptVisible = true;
            return;
        }
        isAnnouncementInterruptVisible = false;
        if (latestUnreadAnnouncement == null) {
            return;
        }
        String candidateId = latestUnreadAnnouncement.getId();
        if (candidateId.trim().isEmpty() || candidateId.equals(lastAnnouncementInterruptId)) {
            return;
        }
        AnnouncementInterruptDialogFragment.newInstance(latestUnreadAnnouncement)
                .show(getSupportFragmentManager(), AnnouncementInterruptDialogFragment.TAG);
        isAnnouncementInterruptVisible = true;
        lastAnnouncementInterruptId = candidateId;
    }

    private boolean isAnnouncementDialogStillMounted() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(AnnouncementInterruptDialogFragment.TAG);
        return fragment instanceof AnnouncementInterruptDialogFragment;
    }

    private void acknowledgeAnnouncement(@Nullable String rawNotificationId) {
        isAnnouncementInterruptVisible = false;
        String notificationId = rawNotificationId == null ? "" : rawNotificationId.trim();
        if (notificationId.isEmpty() || activeAnnouncementUid.trim().isEmpty()) {
            lastAnnouncementInterruptId = null;
            return;
        }
        if (announcementGateRepository == null) {
            announcementGateRepository = new FirestoreNotificationRepository();
        }
        announcementGateRepository.markNotificationRead(
                activeAnnouncementUid,
                notificationId,
                (success, message, exception) -> runOnUiThread(() -> {
                    if (success) {
                        maybeShowAnnouncementInterrupt();
                        return;
                    }
                    lastAnnouncementInterruptId = null;
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    @Nullable
    private Fragment resolveLandingFragment(
            @NonNull UserProfile profile,
            @NonNull RoleDestination destination,
            boolean startWithAnnouncementsCategory
    ) {
        String role = profile.getRole().trim().toLowerCase(Locale.US);
        if (startWithAnnouncementsCategory
                && RoleDestination.NOTIFICATIONS == destination
                && ("student".equals(role) || "faculty".equals(role))) {
            return NotificationCenterFragment.newInstanceWithAnnouncementsCategory();
        }
        return RoleNavRouter.createFragmentForDestination(destination, profile);
    }

    private void ensurePostNotificationPermission(@NonNull UserProfile profile) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (!NotificationLocalAlertCoordinator.isSupportedRole(profile.getRole())) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS
        );
    }
}
