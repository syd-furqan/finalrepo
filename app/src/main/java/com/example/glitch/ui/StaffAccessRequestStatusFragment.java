package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.EntryRequest;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.UserProfile;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Staff screen for viewing their own access request submissions and statuses (US-09).
 * Pattern: Read-only realtime list fragment backed by EntryRequestRepository.
 * Known issue: staff cannot edit or cancel requests in this v1 view.
 */
public class StaffAccessRequestStatusFragment extends Fragment {

    private EntryRequestRepository repository;
    private AccessRequestStatusAdapter adapter;
    private TextView textEmpty;
    private TextView textStatusSummary;

    @NonNull
    public static StaffAccessRequestStatusFragment newInstance() {
        return new StaffAccessRequestStatusFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_staff_access_request_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getRepository();

        RecyclerView recyclerView = view.findViewById(R.id.recycler_access_requests);
        textEmpty = view.findViewById(R.id.text_access_empty);
        textStatusSummary = view.findViewById(R.id.text_access_status_summary);

        adapter = new AccessRequestStatusAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.PASSES);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile != null) {
            repository.listenRequestsByRequester(profile.getUid(), new EntryRequestRepository.RequestListListener() {
                @Override
                public void onData(@NonNull List<EntryRequest> requests) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        adapter.submitList(requests);
                        textEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                        bindStatusSummary(requests);
                    });
                }

                @Override
                public void onError(@NonNull Exception exception) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(), R.string.staff_access_load_error, Snackbar.LENGTH_LONG).show());
                }
            });
        }
    }

    private void bindStatusSummary(@NonNull List<EntryRequest> requests) {
        if (requests.isEmpty()) {
            textStatusSummary.setVisibility(View.GONE);
            return;
        }
        int pending = 0;
        int approved = 0;
        int denied = 0;
        for (EntryRequest r : requests) {
            String status = r.getStatus().trim().toLowerCase(Locale.getDefault());
            if ("active".equals(status) || "exited".equals(status)) {
                approved++;
            } else if ("denied".equals(status)) {
                denied++;
            } else {
                pending++;
            }
        }
        textStatusSummary.setVisibility(View.VISIBLE);
        textStatusSummary.setText(getString(R.string.staff_access_status_summary, pending, approved, denied));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }

    // ── Inline adapter ───────────────────────────────────────────────────────

    static class AccessRequestStatusAdapter
            extends RecyclerView.Adapter<AccessRequestStatusAdapter.ViewHolder> {

        private final List<EntryRequest> items = new ArrayList<>();

        void submitList(@NonNull List<EntryRequest> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_access_request_status, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EntryRequest item = items.get(position);

            holder.textGuestName.setText(
                    item.getFullName().isEmpty() ? holder.itemView.getContext().getString(R.string.staff_access_unknown_guest) : item.getFullName()
            );
            holder.textGate.setText(
                    holder.itemView.getContext().getString(
                            R.string.staff_access_gate_label,
                            GatePolicy.toDisplayLabel(item.getGateLabel())
                    )
            );
            holder.textGuestId.setText(
                    holder.itemView.getContext().getString(R.string.staff_access_guest_id_label, item.getGuestIdNumber())
            );
            holder.textSubmittedAt.setText(formatTimestamp(holder, item.getEnteredAt()));

            String status = item.getStatus();
            holder.textStatus.setText(status.toUpperCase(Locale.getDefault()));
            applyStatusStyle(holder, status);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private void applyStatusStyle(@NonNull ViewHolder holder, @NonNull String rawStatus) {
            String status = rawStatus.trim().toLowerCase(Locale.getDefault());
            int bgRes;
            int colorRes;
            if ("active".equals(status) || "exited".equals(status)) {
                bgRes = R.drawable.bg_chip_success;
                colorRes = R.color.success_green;
            } else if ("denied".equals(status)) {
                bgRes = R.drawable.bg_chip_alert_critical;
                colorRes = R.color.danger_red;
            } else {
                bgRes = R.drawable.bg_chip_role;
                colorRes = R.color.primary_navy;
            }
            holder.textStatus.setBackgroundResource(bgRes);
            holder.textStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), colorRes)
            );
        }

        @NonNull
        private String formatTimestamp(@NonNull ViewHolder holder, @Nullable Timestamp ts) {
            if (ts == null) {
                return holder.itemView.getContext().getString(R.string.staff_access_submitted_unknown);
            }
            DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            return holder.itemView.getContext().getString(
                    R.string.staff_access_submitted_at_label,
                    fmt.format(ts.toDate())
            );
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textGuestName;
            final TextView textStatus;
            final TextView textGate;
            final TextView textGuestId;
            final TextView textSubmittedAt;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textGuestName = itemView.findViewById(R.id.text_access_guest_name);
                textStatus = itemView.findViewById(R.id.text_access_status);
                textGate = itemView.findViewById(R.id.text_access_gate);
                textGuestId = itemView.findViewById(R.id.text_access_guest_id);
                textSubmittedAt = itemView.findViewById(R.id.text_access_submitted_at);
            }
        }
    }
}
