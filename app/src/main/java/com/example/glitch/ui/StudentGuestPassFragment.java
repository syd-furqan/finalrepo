package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.PhoneValidationService;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.CnicScanResult;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.example.glitch.model.PhoneValidationResult;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import com.example.glitch.ui.UiAnimations;

/**
 * Student guest pass generation screen.
 */
public class StudentGuestPassFragment extends Fragment implements GuestPassAdapter.GuestPassActionListener {
    private android.view.ViewGroup animContent;
    private GuestPassRepository repository;
    private GuestPassAdapter adapter;
    private TextInputEditText inputGuestName;
    private TextInputEditText inputGuestId;
    private TextInputEditText inputGuestPhone;
    private Spinner spinnerCountryCode;
    private TextInputLayout layoutGuestName;
    private TextInputLayout layoutGuestCnic;
    private TextInputLayout layoutGuestPhone;
    private TextInputLayout layoutVehiclePlate;
    private TextInputEditText inputVehiclePlate;
    private MaterialCheckBox checkHasVehicle;
    private TextView textEmpty;
    private MaterialButton buttonCreate;
    private ListenerRegistration passListener;
    private CnicOcrHelper cnicOcrHelper;

    @NonNull
    public static StudentGuestPassFragment newInstance() {
        return new StudentGuestPassFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cnicOcrHelper = new CnicOcrHelper(this, new CnicOcrHelper.Callback() {
            @Override
            public void onScanStarted() {
                if (isAdded()) {
                    Snackbar.make(requireView(), "Scanning CNIC…", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onResult(@NonNull CnicScanResult result) {
                if (!isAdded()) return;
                if (result.isSuccess()) {
                    inputGuestId.setText(result.getNormalizedCnic());
                    layoutGuestCnic.setError(null);
                    Snackbar.make(requireView(),
                            "CNIC detected: " + result.getNormalizedCnic(),
                            Snackbar.LENGTH_LONG).show();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("CNIC Not Detected")
                            .setMessage(result.getFailureReason()
                                    + "\n\nTip: Make sure the CNIC number is well-lit and in focus.")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
        cnicOcrHelper.register();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_student_guest_pass, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animContent = view.findViewById(R.id.anim_content);
        repository = RepositoryProvider.getGuestPassRepository();
        inputGuestName = view.findViewById(R.id.input_pass_guest_name);
        inputGuestId = view.findViewById(R.id.input_pass_guest_id);
        inputGuestPhone = view.findViewById(R.id.input_pass_guest_phone);
        spinnerCountryCode = view.findViewById(R.id.spinner_pass_country_code);
        layoutGuestName = view.findViewById(R.id.layout_pass_guest_name);
        layoutGuestCnic = view.findViewById(R.id.layout_pass_guest_cnic);
        layoutGuestPhone = view.findViewById(R.id.layout_pass_guest_phone);
        layoutVehiclePlate = view.findViewById(R.id.layout_pass_vehicle_plate);
        inputVehiclePlate = view.findViewById(R.id.input_pass_vehicle_plate);
        checkHasVehicle = view.findViewById(R.id.check_pass_has_vehicle);
        textEmpty = view.findViewById(R.id.text_guest_pass_empty);
        buttonCreate = view.findViewById(R.id.button_create_pass);
        MaterialButton buttonArchived = view.findViewById(R.id.button_view_archived_passes);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_guest_passes);

        adapter = new GuestPassAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.STUDENT_PASSES);

        ImageButton buttonCnicCamera = view.findViewById(R.id.button_pass_cnic_camera);
        ImageButton buttonCnicGallery = view.findViewById(R.id.button_pass_cnic_gallery);
        buttonCnicCamera.setOnClickListener(v -> cnicOcrHelper.launchCameraOrRequestPermission());
        buttonCnicGallery.setOnClickListener(v -> cnicOcrHelper.launchGallery());

        GuestIdentityInputSupport.attachGuestNameFilter(inputGuestName);
        GuestIdentityInputSupport.setupCountrySpinnerAndPhoneFormatter(
                spinnerCountryCode, inputGuestPhone, layoutGuestPhone, requireContext());
        GuestIdentityInputSupport.attachCnicFormatter(inputGuestId);
        GuestIdentityInputSupport.attachVehiclePlateFormatter(inputVehiclePlate);

        checkHasVehicle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutVehiclePlate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                inputVehiclePlate.setText("");
                layoutVehiclePlate.setError(null);
            }
        });

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile != null) {
            passListener = repository.listenGuestPasses(profile.getUid(), new GuestPassRepository.PassListListener() {
                @Override
                public void onData(@NonNull List<GuestPass> passes) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        adapter.submitList(passes);
                        textEmpty.setVisibility(passes.isEmpty() ? View.VISIBLE : View.GONE);
                        updateUIForActivePass(passes);
                    });
                }

                @Override
                public void onError(@NonNull Exception exception) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(), R.string.error_guest_pass_load, Snackbar.LENGTH_LONG).show());
                }
            });
        }

        buttonCreate.setOnClickListener(v -> createGuestPass());
        buttonArchived.setOnClickListener(v -> openArchivedPasses());
    }

    private void updateUIForActivePass(List<GuestPass> currentPasses) {
        boolean hasInProgress = false;
        for (GuestPass pass : currentPasses) {
            if (GuestPassStatusRules.blocksStudentIssuance(pass.getStatus())) {
                hasInProgress = true;
                break;
            }
        }
        if (hasInProgress) {
            buttonCreate.setEnabled(false);
            buttonCreate.setText("Pass Already Active");
            buttonCreate.setAlpha(0.5f);
        } else {
            buttonCreate.setEnabled(true);
            buttonCreate.setText(R.string.create_pass_action);
            buttonCreate.setAlpha(1.0f);
        }
    }

    private void createGuestPass() {
        String guestName = read(inputGuestName);
        String guestId = read(inputGuestId);
        String nationalNumber = read(inputGuestPhone);
        boolean hasVehicle = checkHasVehicle.isChecked();
        String vehiclePlateInput = read(inputVehiclePlate);
        UserProfile profile = AuthUiGuard.requireProfile(this);

        layoutGuestName.setError(null);
        layoutGuestCnic.setError(null);
        layoutGuestPhone.setError(null);
        layoutVehiclePlate.setError(null);

        // Name validation
        boolean nameValid = GuestIdentityPolicy.isValidGuestName(guestName);
        if (!nameValid) {
            layoutGuestName.setError("Name must contain letters only.");
        }

        // CNIC validation
        String normalizedCnic = GuestIdentityPolicy.normalizeCnic(guestId);
        if (normalizedCnic == null) {
            layoutGuestCnic.setError(getString(R.string.error_invalid_cnic));
        }

        // Phone validation via libphonenumber
        PhoneValidationService.CountryEntry selectedCountry =
                GuestIdentityInputSupport.getSelectedCountry(spinnerCountryCode);
        PhoneValidationResult phoneResult =
                PhoneValidationService.validate(nationalNumber, selectedCountry);

        if (!phoneResult.isValid()) {
            layoutGuestPhone.setError(getString(R.string.error_invalid_phone));
            if (!nameValid || normalizedCnic == null) {
                Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
            } else {
                showInvalidPhoneDialog(phoneResult.getFailureReason());
            }
            return;
        }

        if (!nameValid || normalizedCnic == null || profile == null) {
            Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Vehicle plate validation
        String normalizedPlate = "";
        if (hasVehicle) {
            normalizedPlate = GuestIdentityPolicy.normalizeVehiclePlate(vehiclePlateInput);
            if (normalizedPlate == null) {
                layoutVehiclePlate.setError(getString(R.string.error_invalid_vehicle_plate));
                Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
                return;
            }
        }

        buttonCreate.setEnabled(false);

        // Pass the E.164 number and the full validation result to the repository
        repository.issueGuestPassWithEntryRequest(
                profile.getUid(),
                profile.getRole(),
                profile.getDisplayName(),
                profile.getEmail(),
                profile.getStudentId(),
                guestName,
                normalizedCnic,
                phoneResult.getFormattedE164(),
                hasVehicle,
                normalizedPlate,
                phoneResult,
                (success, message, issuedPass, exception) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                        if (success) {
                            inputGuestName.setText("");
                            inputGuestId.setText("");
                            inputGuestPhone.setText("");
                            checkHasVehicle.setChecked(false);
                            inputVehiclePlate.setText("");
                            layoutGuestName.setError(null);
                            layoutGuestCnic.setError(null);
                            layoutGuestPhone.setError(null);
                            layoutVehiclePlate.setError(null);
                        } else {
                            buttonCreate.setEnabled(true);
                        }
                    });
                }
        );
    }

    private void showInvalidPhoneDialog(@NonNull String reason) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Invalid Phone Number")
                .setMessage("The phone number you entered is not valid.\n\n" + reason
                        + "\n\nPlease check the number and try again.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onCancelPass(@NonNull GuestPass pass) {
        repository.cancelGuestPass(pass.getId(), (success, message, exception) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    @Override
    public void onSharePass(@NonNull GuestPass pass) {
        if (!isAdded()) return;
        try {
            PassShareHelper.share(this, pass);
        } catch (Exception exception) {
            Snackbar.make(requireView(), R.string.error_export_logs, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onViewPassDetails(@NonNull GuestPass pass) {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) return;
        ((NavigationHost) requireActivity()).showFragment(GuestPassDetailsFragment.newInstance(pass), true);
    }

    private void openArchivedPasses() {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) return;
        ((NavigationHost) requireActivity()).showFragment(GuestPassArchiveFragment.newInstance(), true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (passListener != null) {
            passListener.remove();
            passListener = null;
        }
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
