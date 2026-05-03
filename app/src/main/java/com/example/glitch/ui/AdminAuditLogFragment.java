package com.example.glitch.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.AuditLogRepository;
import com.example.glitch.data.AuditPageCursor;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.AccessEvent;
import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.AuditExportFile;
import com.example.glitch.model.AuditLogFilter;
import com.example.glitch.model.GuestPassTimePolicy;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Admin audit log screen with server-side filtering, infinite scroll and export.
 */
public class AdminAuditLogFragment extends Fragment {
    private static final int PAGE_SIZE = 50;

    private static final List<String> EVENT_TYPE_OPTIONS = Arrays.asList(
            AuditEventType.REQUEST_CREATED,
            AuditEventType.ENTRY_ALLOWED,
            AuditEventType.ENTRY_DENIED,
            AuditEventType.EXIT_LOGGED,
            AuditEventType.REQUEST_OVERDUE,
            AuditEventType.PASS_ISSUED,
            AuditEventType.PASS_CANCELLED,
            AuditEventType.PASS_EXPIRED,
            AuditEventType.PASS_USED,
            AuditEventType.PASS_DENIED,
            AuditEventType.PASS_EXITED,
            AuditEventType.PENDING_DECISION_CREATED,
            AuditEventType.PENDING_DECISION_RESOLVED_ALLOW,
            AuditEventType.PENDING_DECISION_RESOLVED_DENY,
            AuditEventType.PENDING_DECISION_INVALIDATED,
            "VEHICLE_REQUEST_CREATED",
            "VEHICLE_REQUEST_UPDATED",
            "VEHICLE_REQUEST_APPROVED",
            "VEHICLE_REQUEST_DENIED"
    );

    private AuditLogRepository repository;
    private AccessEventAdapter adapter;
    private TextView textEmpty;
    private TextView textFilterSummary;
    private AutoCompleteTextView inputActorRole;
    private com.google.android.material.textfield.TextInputEditText inputSearch;
    private MaterialButton buttonFilterEventTypes;
    private MaterialButton buttonExportCsv;
    private MaterialButton buttonExportPdf;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final List<String> selectedEventTypes = new ArrayList<>();
    private long rangeFromMillis;
    private long rangeToMillis;
    private AuditLogFilter currentFilter = AuditLogFilter.last7Days();
    private AuditPageCursor currentCursor = AuditPageCursor.initial();
    private boolean hasMore = false;
    private boolean loadMoreInProgress = false;
    private int listToken = 0;

    @NonNull
    public static AdminAuditLogFragment newInstance() {
        return new AdminAuditLogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_admin_audit_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getAuditLogRepository();
        textEmpty = view.findViewById(R.id.text_audit_empty);
        textFilterSummary = view.findViewById(R.id.text_filter_summary);
        inputActorRole = view.findViewById(R.id.input_actor_role);
        inputSearch = view.findViewById(R.id.input_audit_search);
        buttonFilterEventTypes = view.findViewById(R.id.button_filter_event_types);
        buttonExportCsv = view.findViewById(R.id.button_export_csv);
        buttonExportPdf = view.findViewById(R.id.button_export_pdf);
        Chip chip24h = view.findViewById(R.id.chip_range_24h);
        Chip chip7d = view.findViewById(R.id.chip_range_7d);
        Chip chip30d = view.findViewById(R.id.chip_range_30d);
        Chip chipCustom = view.findViewById(R.id.chip_range_custom);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_audit_logs);

        setupDefaultRange();
        setupRoleDropdown();

        adapter = new AccessEventAdapter();
        adapter.setActionListener(event ->
                AuditEventDetailBottomSheetFragment.newInstance(event)
                        .show(getChildFragmentManager(), "audit_event_details")
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || loadMoreInProgress || !hasMore) {
                    return;
                }
                int visible = layoutManager.getChildCount();
                int total = layoutManager.getItemCount();
                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                if ((visible + firstVisible) >= (total - 6)) {
                    loadMore();
                }
            }
        });

        chip24h.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            rangeToMillis = now;
            rangeFromMillis = now - (24L * 60L * 60L * 1000L);
            refreshFirstPage();
        });
        chip7d.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            rangeToMillis = now;
            rangeFromMillis = now - (7L * 24L * 60L * 60L * 1000L);
            refreshFirstPage();
        });
        chip30d.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            rangeToMillis = now;
            rangeFromMillis = now - (30L * 24L * 60L * 60L * 1000L);
            refreshFirstPage();
        });
        chipCustom.setOnClickListener(v -> openCustomRangePicker());

        buttonFilterEventTypes.setOnClickListener(v -> openEventTypeSelector());

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                searchRunnable = AdminAuditLogFragment.this::refreshFirstPage;
                handler.postDelayed(searchRunnable, 350);
            }
        });

        inputActorRole.setOnItemClickListener((parent, itemView, position, id) -> refreshFirstPage());

        buttonExportCsv.setOnClickListener(v -> exportCsv());
        buttonExportPdf.setOnClickListener(v -> exportPdf());

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DASHBOARD);
        refreshFirstPage();
    }

    private void setupDefaultRange() {
        currentFilter = AuditLogFilter.last7Days();
        rangeFromMillis = currentFilter.getFromInclusiveMillis();
        rangeToMillis = currentFilter.getToInclusiveMillis();
    }

    private void setupRoleDropdown() {
        List<String> roles = Arrays.asList("", "guard", "student", "faculty", "staff", "admin", "system");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        inputActorRole.setAdapter(adapter);
        inputActorRole.setText("", false);
    }

    private void openEventTypeSelector() {
        boolean[] checked = new boolean[EVENT_TYPE_OPTIONS.size()];
        for (int i = 0; i < EVENT_TYPE_OPTIONS.size(); i++) {
            checked[i] = selectedEventTypes.contains(EVENT_TYPE_OPTIONS.get(i));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select event types")
                .setMultiChoiceItems(
                        EVENT_TYPE_OPTIONS.toArray(new String[0]),
                        checked,
                        (dialog, which, isChecked) -> {
                            String type = EVENT_TYPE_OPTIONS.get(which);
                            if (isChecked) {
                                if (!selectedEventTypes.contains(type)) {
                                    selectedEventTypes.add(type);
                                }
                            } else {
                                selectedEventTypes.remove(type);
                            }
                        }
                )
                .setNegativeButton(R.string.cancel_action, null)
                .setPositiveButton(R.string.confirm_action, (dialog, which) -> refreshFirstPage())
                .show();
    }

    private void openCustomRangePicker() {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(rangeFromMillis);
        DatePickerDialog startPicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar pickedStart = Calendar.getInstance();
                    pickedStart.set(year, month, dayOfMonth, 0, 0, 0);
                    pickedStart.set(Calendar.MILLISECOND, 0);
                    rangeFromMillis = pickedStart.getTimeInMillis();

                    Calendar end = Calendar.getInstance();
                    end.setTimeInMillis(rangeToMillis);
                    DatePickerDialog endPicker = new DatePickerDialog(
                            requireContext(),
                            (view2, year2, month2, dayOfMonth2) -> {
                                Calendar pickedEnd = Calendar.getInstance();
                                pickedEnd.set(year2, month2, dayOfMonth2, 23, 59, 59);
                                pickedEnd.set(Calendar.MILLISECOND, 999);
                                rangeToMillis = pickedEnd.getTimeInMillis();
                                if (rangeToMillis < rangeFromMillis) {
                                    long swap = rangeFromMillis;
                                    rangeFromMillis = rangeToMillis;
                                    rangeToMillis = swap;
                                }
                                refreshFirstPage();
                            },
                            end.get(Calendar.YEAR),
                            end.get(Calendar.MONTH),
                            end.get(Calendar.DAY_OF_MONTH)
                    );
                    endPicker.show();
                },
                start.get(Calendar.YEAR),
                start.get(Calendar.MONTH),
                start.get(Calendar.DAY_OF_MONTH)
        );
        startPicker.show();
    }

    private void refreshFirstPage() {
        listToken++;
        final int token = listToken;
        currentFilter = buildCurrentFilter();
        textFilterSummary.setText(buildFilterSummary(currentFilter));
        loadMoreInProgress = false;

        repository.listenFirstPage(currentFilter, PAGE_SIZE, new AuditLogRepository.AuditPageListener() {
            @Override
            public void onPage(
                    @NonNull List<AccessEvent> events,
                    @NonNull AuditPageCursor nextCursor,
                    boolean more
            ) {
                if (!isAdded() || token != listToken) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.submitFirstPage(events);
                    currentCursor = nextCursor;
                    hasMore = more;
                    textEmpty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded() || token != listToken) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_load_audit_logs, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void loadMore() {
        if (loadMoreInProgress || !hasMore) {
            return;
        }
        loadMoreInProgress = true;
        final int token = listToken;
        repository.loadMore(currentFilter, currentCursor, PAGE_SIZE, new AuditLogRepository.AuditPageListener() {
            @Override
            public void onPage(
                    @NonNull List<AccessEvent> events,
                    @NonNull AuditPageCursor nextCursor,
                    boolean more
            ) {
                if (!isAdded() || token != listToken) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.appendPage(events);
                    currentCursor = nextCursor;
                    hasMore = more;
                    loadMoreInProgress = false;
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded() || token != listToken) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    loadMoreInProgress = false;
                    Snackbar.make(requireView(), R.string.error_load_audit_logs, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void exportCsv() {
        buttonExportCsv.setEnabled(false);
        repository.exportCsv(currentFilter, (success, file, exception) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                buttonExportCsv.setEnabled(true);
                if (!success || file == null) {
                    Snackbar.make(requireView(), R.string.error_export_logs, Snackbar.LENGTH_LONG).show();
                    return;
                }
                shareExport(file, getString(R.string.audit_export_subject));
            });
        });
    }

    private void exportPdf() {
        buttonExportPdf.setEnabled(false);
        UserProfile profile = SessionManager.getCurrentProfile();
        String uid = profile == null ? "unknown_user" : profile.getUid();
        String role = profile == null ? "admin" : profile.getRole();

        repository.exportPdf(
                currentFilter,
                uid,
                role,
                GuestPassTimePolicy.CAMPUS_TIME_ZONE_ID,
                (success, file, exception) -> {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        buttonExportPdf.setEnabled(true);
                        if (!success || file == null) {
                            Snackbar.make(requireView(), R.string.error_export_logs, Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        shareExport(file, getString(R.string.audit_export_subject));
                    });
                }
        );
    }

    private void shareExport(@NonNull AuditExportFile file, @NonNull String subject) {
        File cacheDir = requireContext().getCacheDir();
        File outFile = new File(cacheDir, file.getFileName());
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(file.getContent());
        } catch (IOException ioEx) {
            Snackbar.make(requireView(), R.string.error_export_logs, Snackbar.LENGTH_LONG).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                outFile
        );
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(file.getMimeType());
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.export_action)));
        } catch (Exception ex) {
            Toast.makeText(requireContext(), R.string.error_export_logs, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private AuditLogFilter buildCurrentFilter() {
        String role = read(inputActorRole);
        String search = read(inputSearch);
        List<String> roles = new ArrayList<>();
        if (!role.isEmpty()) {
            roles.add(role.toLowerCase(Locale.getDefault()));
        }
        return new AuditLogFilter(
                rangeFromMillis,
                rangeToMillis,
                new ArrayList<>(selectedEventTypes),
                roles,
                search
        );
    }

    @NonNull
    private String buildFilterSummary(@NonNull AuditLogFilter filter) {
        int typesCount = filter.getEventTypes().size();
        String types = typesCount == 0 ? "all events" : (typesCount + " event type(s)");
        String roles = filter.getActorRoles().isEmpty() ? "all roles" : filter.getActorRoles().toString();
        String search = filter.getSearchText().trim().isEmpty() ? "no text filter" : ("search: " + filter.getSearchText().trim());
        return "Range active • " + types + " • " + roles + " • " + search;
    }

    @NonNull
    private String read(@Nullable TextView view) {
        if (view == null || view.getText() == null) {
            return "";
        }
        return view.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
        if (searchRunnable != null) {
            handler.removeCallbacks(searchRunnable);
            searchRunnable = null;
        }
    }
}
