package com.example.glitch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.glitch.auth.AuthRepository;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.SingleGateMigrationRunner;
import com.example.glitch.model.UserProfile;
import com.example.glitch.notification.GuestPassLocalAlertCoordinator;
import com.example.glitch.ui.LoginFragment;
import com.example.glitch.ui.NavigationHost;
import com.example.glitch.ui.RoleHomeFragment;

/**
 * Host activity that owns the fragment container for the app's primary navigation surface.
 */
public class MainActivity extends AppCompatActivity implements NavigationHost {
    private static final int REQUEST_POST_NOTIFICATIONS = 1201;

    private AuthRepository authRepository;
    private GuestPassLocalAlertCoordinator guestPassLocalAlertCoordinator;
    private SingleGateMigrationRunner singleGateMigrationRunner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = RepositoryProvider.getAuthRepository();
        guestPassLocalAlertCoordinator = new GuestPassLocalAlertCoordinator(getApplicationContext());
        singleGateMigrationRunner = new SingleGateMigrationRunner();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (MainActivityStartupPolicy.shouldForceFreshLogin(savedInstanceState != null)) {
            forceFreshLogin();
            return;
        }
        enforceRestoredStateGuard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enforceRestoredStateGuard();
    }

    private void forceFreshLogin() {
        authRepository.logout();
        SessionManager.clear();
        showLogin(true);
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
        if (guestPassLocalAlertCoordinator != null) {
            guestPassLocalAlertCoordinator.stop();
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
        SessionManager.setCurrentProfile(profile);
        ensurePostNotificationPermission(profile);
        if (guestPassLocalAlertCoordinator != null) {
            guestPassLocalAlertCoordinator.start(profile);
        }
        if (singleGateMigrationRunner != null) {
            singleGateMigrationRunner.runIfNeeded(profile.getRole(), profile.getUid());
        }
        if (clearBackStack) {
            clearBackStack();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, RoleHomeFragment.newInstance(
                        profile.getUid(),
                        profile.getDisplayName(),
                        profile.getEmail(),
                        profile.getRole()
                ))
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
        if (guestPassLocalAlertCoordinator != null) {
            guestPassLocalAlertCoordinator.stop();
        }
        super.onDestroy();
    }

    private void ensurePostNotificationPermission(@NonNull UserProfile profile) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (!GuestPassLocalAlertCoordinator.isSupportedSponsorRole(profile.getRole())) {
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
