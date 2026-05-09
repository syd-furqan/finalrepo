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
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.UserManagementRepository;
import com.example.glitch.model.VehicleStickerPolicy;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

/**
 * Admin user management screen for add/edit/deactivate flows (US-14).
 * Pattern: Form + toggle list fragment bound to UserManagementRepository.
 * Known issue: creating a profile here does not create FirebaseAuth credentials automatically.
 */
public class AdminUserManagementFragment extends Fragment implements UserManagementAdapter.UserActionListener {
    private UserManagementRepository repository;
    private UserManagementAdapter adapter;
    private TextView textEmpty;
    private TextInputEditText inputUid;
    private TextInputEditText inputEmail;
    private TextInputEditText inputRole;
    private TextInputEditText inputName;
    private TextInputEditText inputStudentCategory;
    private TextInputEditText inputStudentId;

    @NonNull
    public static AdminUserManagementFragment newInstance() {
        return new AdminUserManagementFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_user_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getUserManagementRepository();
        inputUid = view.findViewById(R.id.input_user_uid);
        inputEmail = view.findViewById(R.id.input_user_email);
        inputRole = view.findViewById(R.id.input_user_role);
        inputName = view.findViewById(R.id.input_user_name);
        inputStudentCategory = view.findViewById(R.id.input_user_student_category);
        inputStudentId = view.findViewById(R.id.input_user_student_id);
        MaterialButton buttonSave = view.findViewById(R.id.button_save_user);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_users);
        textEmpty = view.findViewById(R.id.text_users_empty);

        adapter = new UserManagementAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.USERS);

        repository.listenUsers(new UserManagementRepository.UserListListener() {
            @Override
            public void onData(@NonNull java.util.List<UserProfile> users) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(users);
                    textEmpty.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_users, Snackbar.LENGTH_LONG).show());
            }
        });

        buttonSave.setOnClickListener(v -> saveUser());
    }

    private void saveUser() {
        String uid = read(inputUid);
        String email = read(inputEmail);
        String role = read(inputRole).toLowerCase(Locale.getDefault());
        String name = read(inputName);
        String studentCategory = read(inputStudentCategory).toLowerCase(Locale.getDefault());
        String studentId = read(inputStudentId);
        if (uid.isEmpty() || email.isEmpty() || role.isEmpty() || name.isEmpty()) {
            Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!("guard".equals(role)
                || "faculty".equals(role)
                || "student".equals(role)
                || "admin".equals(role)
                || "monitor".equals(role))) {
            Snackbar.make(requireView(), R.string.error_invalid_user_role, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if ("student".equals(role)
                && !VehicleStickerPolicy.isSupportedStudentCategory(studentCategory)) {
            Snackbar.make(requireView(), "Student category must be day_scholar, hostelite, or redc.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if ("student".equals(role) && studentId.isEmpty()) {
            Snackbar.make(requireView(), "Student ID is required for student users.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        repository.upsertUser(uid, email, role, name, studentCategory, studentId, true, (success, message, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    @Override
    public void onToggleActive(@NonNull UserProfile user, boolean isActive) {
        repository.setUserActive(user.getUid(), isActive, (success, message, exception) -> {
            if (!isAdded()) {
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
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
}
