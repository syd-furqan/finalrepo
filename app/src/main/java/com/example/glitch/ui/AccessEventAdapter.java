package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.AccessEvent;

import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for admin audit event timelines.
 */
public class AccessEventAdapter extends RecyclerView.Adapter<AccessEventAdapter.EventViewHolder> {
    interface ActionListener {
        void onViewDetails(@NonNull AccessEvent event);
    }

    private final List<AccessEvent> items = new ArrayList<>();
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    @Nullable
    private ActionListener actionListener;

    void setActionListener(@Nullable ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    void submitFirstPage(@NonNull List<AccessEvent> events) {
        items.clear();
        items.addAll(events);
        notifyDataSetChanged();
    }

    void appendPage(@NonNull List<AccessEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        Set<String> existingIds = new HashSet<>();
        for (AccessEvent event : items) {
            existingIds.add(event.getId());
        }
        int start = items.size();
        int added = 0;
        for (AccessEvent event : events) {
            if (existingIds.contains(event.getId())) {
                continue;
            }
            items.add(event);
            existingIds.add(event.getId());
            added++;
        }
        if (added > 0) {
            notifyItemRangeInserted(start, added);
        }
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_access_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        AccessEvent event = items.get(position);

        holder.textDescription.setText(
                event.getDescription().trim().isEmpty() ? "No description" : cleanDescription(event.getDescription())
        );
        holder.textActor.setText("By: " + capitalize(valueOrFallback(event.getActorRole())));
        holder.textEventType.setText(friendlyEventType(event.getEventType()));
        bindBadgeStyles(holder, event);

        String gate = event.getGateLabel().trim().isEmpty() ? null : event.getGateLabel().trim();
        String reason = event.getReasonCode().trim().isEmpty() ? null : friendlyReason(event.getReasonCode().trim());
        StringBuilder summary = new StringBuilder();
        if (gate != null) summary.append(gate);
        if (reason != null) {
            if (summary.length() > 0) summary.append("  ·  ");
            summary.append(reason);
        }
        if (summary.length() > 0) {
            holder.textSummary.setText(summary.toString());
            holder.textSummary.setVisibility(View.VISIBLE);
        } else {
            holder.textSummary.setVisibility(View.GONE);
        }

        if (event.getCreatedAt() != null) {
            holder.textTime.setText(format.format(event.getCreatedAt().toDate()));
        } else {
            holder.textTime.setText("--");
        }

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onViewDetails(event);
            }
        });
    }

    @NonNull
    private String friendlyEventType(@NonNull String raw) {
        switch (raw.trim().toLowerCase(Locale.getDefault())) {
            case "entry_allowed":         return "Entry Allowed";
            case "entry_denied":          return "Entry Denied";
            case "exit_logged":           return "Exit Logged";
            case "request_overdue":       return "Overdue";
            case "entry_reported_manual": return "Reported";
            case "entry_reported_overdue":return "Auto-Reported";
            case "pass_used":             return "Pass Used";
            case "pass_denied":           return "Pass Denied";
            case "pass_reported":         return "Pass Reported";
            case "pass_exited":           return "Pass Exited";
            case "entry_invalidated_ban":         return "Banned";
            case "violation_report_submitted":    return "Violation Reported";
            case "alert_created":                 return "Alert Created";
            case "alert_resolved":                return "Alert Resolved";
            default:
                if (raw.trim().isEmpty()) return "Event";
                return toTitleCase(raw.trim().replace('_', ' '));
        }
    }

    @NonNull
    private String friendlyReason(@NonNull String raw) {
        switch (raw.toLowerCase(Locale.getDefault())) {
            case "pass_admitted":            return "Pass admitted";
            case "pass_reported":            return "Pass reported";
            case "entry_allowed":            return "Entry allowed";
            case "entry_denied":             return "Entry denied";
            case "request_overdue":          return "Request overdue";
            case "overdue_grace_elapsed":    return "Grace period elapsed";
            case "system_overdue":           return "System: overdue";
            case "ban_active":               return "Active ban";
            case "cnic_mismatch":            return "CNIC mismatch";
            case "manual_override":          return "Manual override";
            default:
                return toTitleCase(raw.replace('_', ' '));
        }
    }

    @NonNull
    private String toTitleCase(@NonNull String s) {
        if (s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) sb.append(w.substring(1).toLowerCase(Locale.getDefault()));
        }
        return sb.toString();
    }

    @NonNull
    private String capitalize(@NonNull String value) {
        if (value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.getDefault());
    }

    @NonNull
    private String valueOrFallback(@NonNull String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "N/A" : trimmed;
    }

    private void bindBadgeStyles(@NonNull EventViewHolder holder, @NonNull AccessEvent event) {
        // Outcome chip — hide if empty
        String outcome = event.getOutcome().trim().toLowerCase(Locale.getDefault());
        if (outcome.isEmpty()) {
            holder.textEventOutcome.setVisibility(View.GONE);
        } else {
            holder.textEventOutcome.setVisibility(View.VISIBLE);
            holder.textEventOutcome.setText(friendlyOutcome(outcome));
            if ("denied".equals(outcome) || "failure".equals(outcome) || "blocked".equals(outcome)) {
                // dark red bg → white text
                holder.textEventOutcome.setBackgroundResource(R.drawable.bg_chip_role_danger);
                holder.textEventOutcome.setTextColor(0xFFFFFFFF);
            } else if ("success".equals(outcome) || "allowed".equals(outcome)) {
                // light green bg → dark green text
                holder.textEventOutcome.setBackgroundResource(R.drawable.bg_chip_success);
                holder.textEventOutcome.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success_green));
            } else if ("reported".equals(outcome) || "overdue".equals(outcome)) {
                // light amber bg → dark orange text
                holder.textEventOutcome.setBackgroundResource(R.drawable.bg_chip_alert_warning);
                holder.textEventOutcome.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.semantic_warning));
            } else {
                holder.textEventOutcome.setBackgroundResource(R.drawable.bg_chip_role);
                holder.textEventOutcome.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_navy));
            }
        }

        // Event type chip
        String eventType = event.getEventType().trim().toLowerCase(Locale.getDefault());
        if (eventType.contains("denied") || eventType.contains("invalidated") || eventType.contains("ban")) {
            // dark red bg → white text
            holder.textEventType.setBackgroundResource(R.drawable.bg_chip_role_danger);
            holder.textEventType.setTextColor(0xFFFFFFFF);
        } else if (eventType.contains("reported") || eventType.contains("overdue")) {
            // light amber bg → dark orange text
            holder.textEventType.setBackgroundResource(R.drawable.bg_chip_alert_warning);
            holder.textEventType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.semantic_warning));
        } else {
            // light blue bg → navy text
            holder.textEventType.setBackgroundResource(R.drawable.bg_chip_role);
            holder.textEventType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_navy));
        }
    }

    @NonNull
    private String cleanDescription(@NonNull String raw) {
        return raw
                .replace("QR_SCAN", "QR scan")
                .replace("PASS_CODE", "pass code")
                .replace("MANUAL", "manual")
                .replace("CNIC_SCAN", "CNIC scan")
                .replace("GUARD_DECISION", "guard decision")
                .replace("SYSTEM", "system")
                .trim();
    }

    @NonNull
    private String friendlyOutcome(@NonNull String raw) {
        switch (raw) {
            case "success":  return "Success";
            case "allowed":  return "Allowed";
            case "denied":   return "Denied";
            case "failure":  return "Failed";
            case "blocked":  return "Blocked";
            case "reported": return "Reported";
            case "overdue":  return "Overdue";
            default:         return toTitleCase(raw.replace('_', ' '));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView textEventType;
        final TextView textEventOutcome;
        final TextView textDescription;
        final TextView textActor;
        final TextView textRequestId;
        final TextView textSummary;
        final TextView textTime;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventType = itemView.findViewById(R.id.text_event_type);
            textEventOutcome = itemView.findViewById(R.id.text_event_outcome);
            textDescription = itemView.findViewById(R.id.text_event_description);
            textActor = itemView.findViewById(R.id.text_event_actor);
            textRequestId = itemView.findViewById(R.id.text_event_request_id);
            textSummary = itemView.findViewById(R.id.text_event_summary);
            textTime = itemView.findViewById(R.id.text_event_time);
        }
    }
}
