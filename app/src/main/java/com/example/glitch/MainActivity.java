package com.example.glitch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

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
import com.example.glitch.model.GuestPassTimePolicy;
import com.example.glitch.model.UserProfile;
import com.example.glitch.notification.NotificationLocalAlertCoordinator;
import com.example.glitch.ui.LoginFragment;
import com.example.glitch.ui.NavigationHost;
import com.example.glitch.ui.RoleDestination;
import com.example.glitch.ui.RoleHomeFragment;
import com.example.glitch.ui.RoleNavRouter;

/**
 * Host activity that owns the fragment container for the app's primary navigation surface.
 */
public class MainActivity extends AppCompatActivity implements NavigationHost {
    public static final String ACTION_OPEN_NOTIFICATIONS = "com.example.glitch.action.OPEN_NOTIFICATIONS";
    public static final String EXTRA_LAUNCH_DESTINATION = "extra_launch_destination";
    public static final String DESTINATION_NOTIFICATIONS = "NOTIFICATIONS";

    private static final int REQUEST_POST_NOTIFICATIONS = 1201;
    private static final boolean TEMP_ENABLE_TIME_POLICY_TEST_BYPASS = false;

    private AuthRepository authRepository;
    private NotificationLocalAlertCoordinator notificationLocalAlertCoordinator;
    private SingleGateMigrationRunner singleGateMigrationRunner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        boolean debugBuild = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        GuestPassTimePolicy.setTestingBypassEnabled(
                debugBuild && TEMP_ENABLE_TIME_POLICY_TEST_BYPASS
        );
        authRepository = RepositoryProvider.getAuthRepository();
        notificationLocalAlertCoordinator = new NotificationLocalAlertCoordinator(getApplicationContext());
        singleGateMigrationRunner = new SingleGateMigrationRunner();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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
        showRoleHome(profile, true, resolveLaunchDestination(intent));
    }

    @Override
    protected void onResume() {
        super.onResume();
        enforceRestoredStateGuard();
    }

    private void bootstrapSessionFromLaunchIntent(@NonNull Intent launchIntent) {
        authRepository.validateCurrentSession((success, profile, message) -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (success && profile != null) {
                showRoleHome(profile, true, resolveLaunchDestination(launchIntent));
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
        if (clearBackStack) {
            clearBackStack();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, LoginFragment.newInstance())
                .commit();
    }

    @Override
    public void showRoleHome(@NonNull UserProfile profile, boolean clearBackStack) {
        showRoleHome(profile, clearBackStack, null);
    }

    private void showRoleHome(
            @NonNull UserProfile profile,
            boolean clearBackStack,
            @Nullable RoleDestination preferredDestination
    ) {
        SessionManager.setCurrentProfile(profile);
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
        Fragment landingFragment = RoleNavRouter.createFragmentForDestination(landingDestination, profile);
        if (landingFragment == null) {
            landingFragment = RoleHomeFragment.newInstance(
                    profile.getUid(),
                    profile.getDisplayName(),
                    profile.getEmail(),
                    profile.getRole()
            );
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, landingFragment)
                .commit();
    }

    @Override
    public void showFragment(@NonNull Fragment fragment, boolean addToBackStack) {
        if (addToBackStack) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(fragment.getClass().getSimpleName())
                    .commit();
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
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
        super.onDestroy();
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
