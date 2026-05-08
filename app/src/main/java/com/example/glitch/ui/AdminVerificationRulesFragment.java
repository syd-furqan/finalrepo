package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.VerificationRulesRepository;
import com.example.glitch.model.VerificationRules;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Admin policy screen for credential verification rule configuration.
 */
public class AdminVerificationRulesFragment extends Fragment {
    private VerificationRulesRepository repository;
    private MaterialCheckBox checkEnforceExpiry;
    private TextInputEditText inputBannedCsv;

    @NonNull
    public static AdminVerificationRulesFragment newInstance() {
        return new AdminVerificationRulesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_verification_rules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getVerificationRulesRepository();
        checkEnforceExpiry = view.findViewById(R.id.checkbox_enforce_expiry);
        inputBannedCsv = view.findViewById(R.id.input_banned_csv);
        MaterialButton buttonSave = view.findViewById(R.id.button_save_rules);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.RULES);

        repository.listenRules(new VerificationRulesRepository.RulesListener() {
            @Override
            public void onData(@NonNull VerificationRules rules) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> bindRules(rules));
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_rules, Snackbar.LENGTH_LONG).show());
            }
        });

        buttonSave.setOnClickListener(v -> saveRules());
    }

    private void bindRules(@NonNull VerificationRules rules) {
        checkEnforceExpiry.setChecked(rules.isEnforceIdExpiry());
        inputBannedCsv.setText(rules.getBannedIdentifiersCsv());
    }

    private void saveRules() {
        String raw = read(inputBannedCsv);
        String normalized;
        if (raw.isEmpty()) {
            normalized = "";
        } else {
            String[] parts = raw.split(",");
            java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) {
                    set.add(t);
                }
            }
            normalized = String.join(",", set);
        }

        VerificationRules rules = new VerificationRules(
                checkEnforceExpiry.isChecked(),
                normalized
        );
        repository.saveRules(rules, (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
}
