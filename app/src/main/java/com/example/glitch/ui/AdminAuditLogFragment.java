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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
            AuditEventType.ENTRY_ALLOWED,
            AuditEventType.ENTRY_DENIED,
            AuditEventType.EXIT_LOGGED,
            AuditEventType.REQUEST_OVERDUE,
            AuditEventType.ENTRY_REPORTED_MANUAL,
            AuditEventType.ENTRY_REPORTED_OVERDUE,
            AuditEventType.PASS_USED,
            AuditEventType.PASS_DENIED,
            AuditEventType.PASS_REPORTED,
            AuditEventType.PASS_EXITED,
            AuditEventType.ENTRY_INVALIDATED_BAN
    );

    @NonNull
    static List<String> getGateAuditEventTypes() {
        return EVENT_TYPE_OPTIONS;
    }

    private AuditLogRepository repository;
    private AccessEventAdapter adapter;
    private TextView textEmpty;
    private TextView textFilterSummary;
    private AutoCompleteTextView inputActorRole;
    private TextInputEditText inputSearch;
    private MaterialButton buttonFilterEventTypes;
    private MaterialButton buttonExportCsv;
    private MaterialButton buttonExportPdf;
    private MaterialButton buttonOpenTrafficAnalytics;
    private MaterialButton buttonOpenFilters;

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
        buttonOpenTrafficAnalytics = view.findViewById(R.id.button_open_traffic_analytics);
        buttonOpenFilters = view.findViewById(R.id.button_open_audit_filters);
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

        // NestedScrollView drives all scrolling; trigger load-more when near the bottom
        NestedScrollView scrollView = view.findViewById(R.id.scroll_audit_logs);
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (sv, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (scrollY <= oldScrollY || loadMoreInProgress || !hasMore) return;
                    int totalHeight = sv.getChildAt(0).getMeasuredHeight();
                    int scrollViewHeight = sv.getMeasuredHeight();
                    if (scrollY >= totalHeight - scrollViewHeight - 300) {
                        loadMore();
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

        buttonOpenFilters.setOnClickListener(v -> openFilterSheet());
        buttonExportCsv.setOnClickListener(v -> exportCsv());
        buttonExportPdf.setOnClickListener(v -> exportPdf());
        buttonOpenTrafficAnalytics.setOnClickListener(
                v -> RoleNavRouter.route(this, RoleDestination.ADMIN_ANALYTICS)
        );

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.AUDIT);
        refreshFirstPage();
    }

    private void setupDefaultRange() {
        currentFilter = AuditLogFilter.last7Days();
        rangeFromMillis = currentFilter.getFromInclusiveMillis();
        rangeToMillis = currentFilter.getToInclusiveMillis();
        selectedEventTypes.clear();
        selectedEventTypes.addAll(EVENT_TYPE_OPTIONS);
    }

    private void setupRoleDropdown() {
        List<String> roles = Arrays.asList("", "guard", "student", "faculty", "admin", "system");
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

    private void openFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout content = createSheetContent("Audit Filters");
        TextInputEditText search = addTextInput(
                content,
                "Search actor/request/pass/description",
                read(inputSearch)
        );
        AutoCompleteTextView role = addDropdown(
                content,
                "Actor Role",
                Arrays.asList("", "guard", "student", "faculty", "admin", "system"),
                read(inputActorRole)
        );

        LinearLayout rangeRow = new LinearLayout(requireContext());
        rangeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rangeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rangeParams.topMargin = dp(12);
        rangeRow.setLayoutParams(rangeParams);
        addRangeButton(rangeRow, "24h", () -> applyRelativeRange(1, dialog));
        addRangeButton(rangeRow, "7d", () -> applyRelativeRange(7, dialog));
        addRangeButton(rangeRow, "30d", () -> applyRelativeRange(30, dialog));
        addRangeButton(rangeRow, "Custom", () -> {
            dialog.dismiss();
            openCustomRangePicker();
        });
        content.addView(rangeRow);

        MaterialButton eventTypes = new MaterialButton(requireContext());
        eventTypes.setText("Event Types (" + selectedEventTypes.size() + ")");
        eventTypes.setAllCaps(false);
        LinearLayout.LayoutParams eventParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        eventParams.topMargin = dp(12);
        eventTypes.setLayoutParams(eventParams);
        eventTypes.setOnClickListener(v -> openEventTypeSelector());
        content.addView(eventTypes);

        addSheetActions(
                content,
                () -> {
                    inputSearch.setText(read(search));
                    inputActorRole.setText(read(role), false);
                    refreshFirstPage();
                    dialog.dismiss();
                },
                () -> {
                    setupDefaultRange();
                    inputSearch.setText("");
                    inputActorRole.setText("", false);
                    refreshFirstPage();
                    dialog.dismiss();
                }
        );
        dialog.setContentView(content);
        dialog.show();
    }

    private void applyRelativeRange(int days, @NonNull BottomSheetDialog dialog) {
        long now = System.currentTimeMillis();
        rangeToMillis = now;
        rangeFromMillis = now - (days * 24L * 60L * 60L * 1000L);
        refreshFirstPage();
        dialog.dismiss();
    }

    @NonNull
    private LinearLayout createSheetContent(@NonNull String title) {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, padding, padding, padding);
        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(requireContext().getColor(R.color.text_dark));
        titleView.setTextSize(20);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(titleView);
        return content;
    }

    @NonNull
    private TextInputEditText addTextInput(
            @NonNull LinearLayout content,
            @NonNull String hint,
            @NonNull String value
    ) {
        TextInputLayout layout = new TextInputLayout(requireContext());
        layout.setHint(hint);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(12);
        layout.setLayoutParams(params);
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setSingleLine(true);
        input.setText(value);
        layout.addView(input);
        content.addView(layout);
        return input;
    }

    @NonNull
    private AutoCompleteTextView addDropdown(
            @NonNull LinearLayout content,
            @NonNull String hint,
            @NonNull List<String> options,
            @NonNull String selected
    ) {
        TextInputLayout layout = new TextInputLayout(requireContext());
        layout.setHint(hint);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(12);
        layout.setLayoutParams(params);
        AutoCompleteTextView input = new AutoCompleteTextView(requireContext());
        input.setInputType(0);
        input.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, options));
        input.setText(selected, false);
        layout.addView(input);
        content.addView(layout);
        return input;
    }

    private void addRangeButton(@NonNull LinearLayout row, @NonNull String label, @NonNull Runnable action) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(label);
        button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.setMarginEnd(dp(6));
        button.setLayoutParams(params);
        button.setOnClickListener(v -> action.run());
        row.addView(button);
    }

    private void addSheetActions(@NonNull LinearLayout content, @NonNull Runnable apply, @NonNull Runnable clear) {
        MaterialButton applyButton = new MaterialButton(requireContext());
        applyButton.setText("Apply Filters");
        applyButton.setAllCaps(false);
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        applyParams.topMargin = dp(16);
        applyButton.setLayoutParams(applyParams);
        applyButton.setOnClickListener(v -> apply.run());
        content.addView(applyButton);

        MaterialButton clearButton = new MaterialButton(requireContext());
        clearButton.setText("Clear");
        clearButton.setAllCaps(false);
        clearButton.setOnClickListener(v -> clear.run());
        content.addView(clearButton);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
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
