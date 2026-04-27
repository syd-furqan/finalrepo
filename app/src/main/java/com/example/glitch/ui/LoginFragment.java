package com.example.glitch.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.AuthRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Entry login screen for email/password authentication with Firestore profile validation.
 * Pattern: Form fragment coordinating AuthRepository callbacks and role routing.
 * Known issue: self-signup is intentionally disabled for this project policy.
 */
public class LoginFragment extends Fragment {
    private AuthRepository authRepository;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private MaterialButton buttonLogin;
    private ProgressBar progressBar;
    private TextView textHelp;

    @NonNull
    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authRepository = RepositoryProvider.getAuthRepository();
        inputEmail = view.findViewById(R.id.input_email);
        inputPassword = view.findViewById(R.id.input_password);
        buttonLogin = view.findViewById(R.id.button_login);
        progressBar = view.findViewById(R.id.progress_login);
        textHelp = view.findViewById(R.id.text_login_help);

        textHelp.setText(getString(R.string.login_help_text));
        buttonLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = readText(inputEmail);
        String password = readText(inputPassword);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Snackbar.make(requireView(), R.string.error_fill_login_fields, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!hasActiveInternetConnection()) {
            Snackbar.make(requireView(), R.string.error_no_internet_connection, Snackbar.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        authRepository.login(email, password, (success, profile, message) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                setLoading(false);
                if (success && profile != null) {
                    NavigationHost host = navigationHost();
                    if (host != null) {
                        host.showRoleHome(profile, true);
                    }
                    return;
                }
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
            });
        });
    }

    private void setLoading(boolean loading) {
        buttonLogin.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private String readText(@NonNull TextInputEditText input) {
        CharSequence raw = input.getText();
        return raw == null ? "" : raw.toString().trim();
    }

    private boolean hasActiveInternetConnection() {
        ConnectivityManager manager = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        Network network = manager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Nullable
    private NavigationHost navigationHost() {
        if (requireActivity() instanceof NavigationHost) {
            return (NavigationHost) requireActivity();
        }
        return null;
    }
}
