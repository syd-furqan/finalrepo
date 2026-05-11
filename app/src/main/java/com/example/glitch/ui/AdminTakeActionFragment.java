package com.example.glitch.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.data.AnnouncementRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.InGateServiceControl;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import com.example.glitch.ui.UiAnimations;

/**
 * Admin workflow for high-priority announcements and in-gate controls.
 */
public class AdminTakeActionFragment extends Fragment {
    private android.view.ViewGroup animContent;
    private AnnouncementRepository repository;

    private MaterialCheckBox checkAllCampus;
    private MaterialCheckBox checkAudienceStudent;
    private MaterialCheckBox checkAudienceFaculty;
    private MaterialCheckBox checkAudienceGuard;
    private MaterialCheckBox checkAudienceMonitor;

    private TextInputEditText inputMessage;

    private MaterialCheckBox checkInhibit;
    private MaterialCheckBox checkAffectStudent;
    private MaterialCheckBox checkAffectFaculty;
    private RadioGroup groupMode;
    private TextView textEndsAt;
    private MaterialButton buttonPickEndsAt;
    private TextView textCurrentControl;

    private MaterialButton buttonPublish;
    private MaterialButton buttonRestore;

    @Nullable
    private Long selectedEndsAtMillis;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @NonNull
    public static AdminTakeActionFragment newInstance() {
        return new AdminTakeActionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_take_action, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animContent = view.findViewById(R.id.anim_content);
        if (AuthUiGuard.requireProfile(this) == null) {
            return;
        }

        repository = RepositoryProvider.getAnnouncementRepository();
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.ADMIN_TAKE_ACTION);

        checkAllCampus = view.findViewById(R.id.check_announce_all_campus);
        checkAudienceStudent = view.findViewById(R.id.check_announce_student);
        checkAudienceFaculty = view.findViewById(R.id.check_announce_faculty);
        checkAudienceGuard = view.findViewById(R.id.check_announce_guard);
        checkAudienceMonitor = view.findViewById(R.id.check_announce_monitor);

        inputMessage = view.findViewById(R.id.input_announce_message);

        checkInhibit = view.findViewById(R.id.check_inhibit_ingate);
        checkAffectStudent = view.findViewById(R.id.check_inhibit_student);
        checkAffectFaculty = view.findViewById(R.id.check_inhibit_faculty);
        groupMode = view.findViewById(R.id.group_inhibit_mode);
        textEndsAt = view.findViewById(R.id.text_inhibit_ends_at);
        buttonPickEndsAt = view.findViewById(R.id.button_pick_inhibit_end);
        textCurrentControl = view.findViewById(R.id.text_current_control_state);

        buttonPublish = view.findViewById(R.id.button_publish_announcement);
        buttonRestore = view.findViewById(R.id.button_restore_services);

        checkAllCampus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setAudienceRoleEnabled(!isChecked);
            if (isChecked) {
                checkAudienceStudent.setChecked(false);
                checkAudienceFaculty.setChecked(false);
                checkAudienceGuard.setChecked(false);
                checkAudienceMonitor.setChecked(false);
            }
        });

        checkInhibit.setOnCheckedChangeListener((buttonView, isChecked) -> updateInhibitionSectionState());
        groupMode.setOnCheckedChangeListener((group, checkedId) -> updateInhibitionSectionState());
        buttonPickEndsAt.setOnClickListener(v -> pickEndsAt());

        buttonPublish.setOnClickListener(v -> publishAnnouncement());
        buttonRestore.setOnClickListener(v -> restoreServices());

        updateInhibitionSectionState();
        refreshCurrentControlState();
    }

    private void setAudienceRoleEnabled(boolean enabled) {
        checkAudienceStudent.setEnabled(enabled);
        checkAudienceFaculty.setEnabled(enabled);
        checkAudienceGuard.setEnabled(enabled);
        checkAudienceMonitor.setEnabled(enabled);
    }

    private void updateInhibitionSectionState() {
        boolean inhibit = checkInhibit.isChecked();
        checkAffectStudent.setEnabled(inhibit);
        checkAffectFaculty.setEnabled(inhibit);

        boolean timed = groupMode.getCheckedRadioButtonId() == R.id.radio_inhibit_mode_timed;
        buttonPickEndsAt.setEnabled(inhibit && timed);
        textEndsAt.setEnabled(inhibit && timed);

        if (!inhibit) {
            selectedEndsAtMillis = null;
            textEndsAt.setText(getString(R.string.inhibit_end_not_set));
            return;
        }

        if (!timed) {
            selectedEndsAtMillis = null;
            textEndsAt.setText(getString(R.string.inhibit_end_manual_mode));
            return;
        }

        if (selectedEndsAtMillis == null) {
            textEndsAt.setText(getString(R.string.inhibit_end_not_set));
        } else {
            textEndsAt.setText(dateFormat.format(new Date(selectedEndsAtMillis)));
        }
    }

    private void pickEndsAt() {
        final java.util.Calendar calendar = java.util.Calendar.getInstance();
        if (selectedEndsAtMillis != null) {
            calendar.setTimeInMillis(selectedEndsAtMillis);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(java.util.Calendar.YEAR, year);
                    calendar.set(java.util.Calendar.MONTH, month);
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(java.util.Calendar.MINUTE, minute);
                                calendar.set(java.util.Calendar.SECOND, 0);
                                selectedEndsAtMillis = calendar.getTimeInMillis();
                                textEndsAt.setText(dateFormat.format(new Date(selectedEndsAtMillis)));
                            },
                            calendar.get(java.util.Calendar.HOUR_OF_DAY),
                            calendar.get(java.util.Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void publishAnnouncement() {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }

        String message = read(inputMessage);
        if (message.isEmpty()) {
            showMessage(getString(R.string.announce_error_message_required));
            return;
        }

        boolean allCampus = checkAllCampus.isChecked();
        Set<String> selectedAudience = new HashSet<>();
        if (checkAudienceStudent.isChecked()) selectedAudience.add("student");
        if (checkAudienceFaculty.isChecked()) selectedAudience.add("faculty");
        if (checkAudienceGuard.isChecked()) selectedAudience.add("guard");
        if (checkAudienceMonitor.isChecked()) selectedAudience.add("monitor");

        if (!allCampus && selectedAudience.isEmpty()) {
            showMessage(getString(R.string.announce_error_audience_required));
            return;
        }

        boolean inhibit = checkInhibit.isChecked();
        Set<String> affectedRoles = new HashSet<>();
        if (checkAffectStudent.isChecked()) affectedRoles.add("student");
        if (checkAffectFaculty.isChecked()) affectedRoles.add("faculty");

        boolean timed = groupMode.getCheckedRadioButtonId() == R.id.radio_inhibit_mode_timed;
        if (inhibit && affectedRoles.isEmpty()) {
            showMessage(getString(R.string.announce_error_affected_roles_required));
            return;
        }
        if (inhibit && timed && (selectedEndsAtMillis == null || selectedEndsAtMillis <= System.currentTimeMillis())) {
            showMessage(getString(R.string.announce_error_end_time_required));
            return;
        }

        setButtonsEnabled(false);
        repository.publishAnnouncement(
                profile.getUid(),
                profile.getRole(),
                profile.getDisplayName(),
                message,
                allCampus,
                selectedAudience,
                inhibit,
                affectedRoles,
                timed,
                selectedEndsAtMillis,
                (success, resultMessage, exception) -> {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        setButtonsEnabled(true);
                        showMessage(resultMessage);
                        if (success) {
                            selectedEndsAtMillis = null;
                            textEndsAt.setText(getString(R.string.inhibit_end_not_set));
                            refreshCurrentControlState();
                        }
                    });
                }
        );
    }

    private void restoreServices() {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }

        String reason = read(inputMessage);
        if (reason.isEmpty()) {
            reason = getString(R.string.restore_default_message);
        }

        setButtonsEnabled(false);
        repository.restoreInGateServices(
                profile.getUid(),
                profile.getRole(),
                profile.getDisplayName(),
                reason,
                (success, message, exception) -> {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        setButtonsEnabled(true);
                        showMessage(message);
                        if (success) {
                            refreshCurrentControlState();
                        }
                    });
                }
        );
    }

    private void refreshCurrentControlState() {
        repository.getCurrentInGateControl(new AnnouncementRepository.ControlStateListener() {
            @Override
            public void onData(@NonNull InGateServiceControl control) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (!control.isActive()) {
                        textCurrentControl.setText(getString(R.string.current_control_inactive));
                        return;
                    }

                    String affected = control.getAffectedRoles().isEmpty()
                            ? getString(R.string.current_control_affected_none)
                            : String.join(", ", control.getAffectedRoles());
                    String untilText;
                    if (InGateServiceControl.MODE_TIMED.equals(control.getMode()) && control.getEndsAt() != null) {
                        untilText = dateFormat.format(control.getEndsAt().toDate());
                    } else {
                        untilText = getString(R.string.inhibit_until_further_notice);
                    }
                    textCurrentControl.setText(getString(
                            R.string.current_control_active_template,
                            affected,
                            untilText,
                            control.getReason().trim().isEmpty()
                                    ? getString(R.string.current_control_reason_none)
                                    : control.getReason()
                    ));
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        textCurrentControl.setText(getString(R.string.current_control_load_failed))
                );
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        buttonPublish.setEnabled(enabled);
        buttonRestore.setEnabled(enabled);
        buttonPublish.setAlpha(enabled ? 1f : 0.6f);
        buttonRestore.setAlpha(enabled ? 1f : 0.6f);
    }

    private void showMessage(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (animContent != null) UiAnimations.animateFallIn(animContent);
    }
}
