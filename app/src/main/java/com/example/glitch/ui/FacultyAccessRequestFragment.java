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
import com.example.glitch.model.PhoneValidationResult;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Faculty access-request form for sponsoring guest entry.
 */
public class FacultyAccessRequestFragment extends Fragment implements GuestPassAdapter.GuestPassActionListener {
    private GuestPassRepository repository;
    private GuestPassAdapter adapter;
    private TextView textEmpty;
    private TextInputEditText inputGuestName;
    private TextInputEditText inputGuestId;
    private TextInputEditText inputGuestPhone;
    private TextInputEditText inputVehiclePlate;
    private Spinner spinnerCountryCode;
    private TextInputLayout layoutGuestName;
    private TextInputLayout layoutGuestCnic;
    private TextInputLayout layoutGuestPhone;
    private TextInputLayout layoutVehiclePlate;
    private MaterialCheckBox checkHasVehicle;
    private ListenerRegistration passListener;
    private CnicOcrHelper cnicOcrHelper;

    @NonNull
    public static FacultyAccessRequestFragment newInstance() {
        return new FacultyAccessRequestFragment();
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
        return inflater.inflate(R.layout.fragment_faculty_access_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getGuestPassRepository();

        inputGuestName = view.findViewById(R.id.input_guest_name);
        inputGuestId = view.findViewById(R.id.input_guest_id);
        inputGuestPhone = view.findViewById(R.id.input_guest_phone);
        inputVehiclePlate = view.findViewById(R.id.input_guest_vehicle_plate);
        spinnerCountryCode = view.findViewById(R.id.spinner_guest_country_code);
        layoutGuestName = view.findViewById(R.id.layout_guest_name);
        layoutGuestCnic = view.findViewById(R.id.layout_guest_cnic);
        layoutGuestPhone = view.findViewById(R.id.layout_guest_phone);
        layoutVehiclePlate = view.findViewById(R.id.layout_guest_vehicle_plate);
        checkHasVehicle = view.findViewById(R.id.check_guest_has_vehicle);
        MaterialButton buttonSubmit = view.findViewById(R.id.button_submit_request);
        MaterialButton buttonArchived = view.findViewById(R.id.button_view_archived_passes);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_guest_passes);
        textEmpty = view.findViewById(R.id.text_guest_pass_empty);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.FACULTY_REQUEST);

        adapter = new GuestPassAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        ImageButton buttonCnicCamera = view.findViewById(R.id.button_guest_cnic_camera);
        ImageButton buttonCnicGallery = view.findViewById(R.id.button_guest_cnic_gallery);
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
                public void onData(@NonNull java.util.List<GuestPass> passes) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        adapter.submitList(passes);
                        textEmpty.setVisibility(passes.isEmpty() ? View.VISIBLE : View.GONE);
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

        buttonSubmit.setOnClickListener(v -> submitGuestPass());
        buttonArchived.setOnClickListener(v -> openArchivedPasses());
    }

    private void submitGuestPass() {
        String guestName = read(inputGuestName);
        String guestId = read(inputGuestId);
        String nationalNumber = read(inputGuestPhone);
        boolean hasVehicle = checkHasVehicle.isChecked();
        String vehiclePlateInput = read(inputVehiclePlate);
        UserProfile userProfile = AuthUiGuard.requireProfile(this);

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

        if (!nameValid || normalizedCnic == null || userProfile == null) {
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

        repository.issueGuestPassWithEntryRequest(
                userProfile.getUid(),
                userProfile.getRole(),
                userProfile.getDisplayName(),
                userProfile.getEmail(),
                userProfile.getStudentId(),
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

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
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
}
