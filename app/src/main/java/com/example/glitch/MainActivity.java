package com.example.glitch;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.glitch.auth.AuthRepository;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.UserProfile;
import com.example.glitch.ui.LoginFragment;
import com.example.glitch.ui.NavigationHost;
import com.example.glitch.ui.RoleHomeFragment;

/**
 * Host activity that owns the fragment container for the app's primary navigation surface.
 */
public class MainActivity extends AppCompatActivity implements NavigationHost {
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = RepositoryProvider.getAuthRepository();
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
}
