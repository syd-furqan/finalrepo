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
import com.example.glitch.model.SecurityAlert;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for automated security alerts.
 * Pattern: Stateless list adapter with severity chip rendering.
 * Known issue: acknowledge/snooze actions are outside v1 scope.
 */
public class SecurityAlertAdapter extends RecyclerView.Adapter<SecurityAlertAdapter.AlertViewHolder> {
    private final List<SecurityAlert> items = new ArrayList<>();
    @Nullable
    private AlertActionListener actionListener;

    public void submitList(@NonNull List<SecurityAlert> alerts) {
        items.clear();
        items.addAll(alerts);
        notifyDataSetChanged();
    }

    public void setActionListener(@Nullable AlertActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_security_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        SecurityAlert alert = items.get(position);
        String severity = alert.getSeverity() == null ? "" : alert.getSeverity();
        holder.textSeverity.setText(severity.toUpperCase(Locale.getDefault()));
        ChipStyle chipStyle = resolveSeverityStyle(severity);
        holder.textSeverity.setBackgroundResource(chipStyle.backgroundRes);
        holder.textSeverity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), chipStyle.textColorRes));
        String requestId = alert.getEntryRequestId().trim().isEmpty()
                ? alert.getIdentifier()
                : alert.getEntryRequestId();
        holder.textIdentifier.setText(labelForAlert(alert) + ": " + requestId);
        String reporter = alert.getReportedByName().trim();
        if (reporter.isEmpty()) {
            reporter = alert.getReportedByUid().trim();
        }
        String reporterRole = alert.getReportedByRole().trim();
        if (!reporterRole.isEmpty()) {
            reporter = reporter.isEmpty() ? reporterRole : reporter + " (" + reporterRole + ")";
        }
        if (reporter.trim().isEmpty()) {
            reporter = "Unknown guard";
        }
        String guestName = alert.getGuestName().trim().isEmpty() ? "Unknown" : alert.getGuestName().trim();
        String guestId = alert.getGuestIdNumber().trim().isEmpty() ? "N/A" : alert.getGuestIdNumber().trim();
        String host = alert.getHostName().trim().isEmpty() ? "N/A" : alert.getHostName().trim();
        String requester = alert.getRequesterUid().trim().isEmpty() ? "N/A" : alert.getRequesterUid().trim();
        if (!alert.getRequesterRole().trim().isEmpty()) {
            requester = requester + " (" + alert.getRequesterRole().trim() + ")";
        }
        String gate = alert.getGateLabel().trim().isEmpty() ? "In-Gate" : alert.getGateLabel().trim();
        String incidentStatus = alert.getIncidentStatus().trim().isEmpty()
                ? "new"
                : alert.getIncidentStatus().trim();
        String intervention = alert.getInterventionSummary().trim().isEmpty()
                ? "N/A"
                : alert.getInterventionSummary().trim();
        holder.textMessage.setText(
                alert.getMessage()
                        + "\nIncident: " + incidentStatus
                        + "\nIntervention: " + intervention
                        + "\nGuard: " + reporter
                        + "\nVisitor: " + guestName + " [" + guestId + "]"
                        + "\nSponsor: " + host + " / " + requester
                        + "\nGate: " + gate
        );
        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAlertSelected(alert);
            }
        });
    }

    @NonNull
    static String labelForAlert(@NonNull SecurityAlert alert) {
        String type = alert.getAlertType().trim().toLowerCase(Locale.getDefault());
        if ("manual_violation".equals(type)) {
            return "Violation Report";
        }
        if ("vehicle_review".equals(type)) {
            return "Vehicle Review";
        }
        if ("charge_review".equals(type)) {
            return "Charge Review";
        }
        if ("scan_risk".equals(type)) {
            return "Scan Risk";
        }
        return "Entry Report";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        final TextView textIdentifier;
        final TextView textSeverity;
        final TextView textMessage;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            textIdentifier = itemView.findViewById(R.id.text_alert_identifier);
            textSeverity = itemView.findViewById(R.id.text_alert_severity);
            textMessage = itemView.findViewById(R.id.text_alert_message);
        }
    }

    @NonNull
    static ChipStyle resolveSeverityStyle(@NonNull String rawSeverity) {
        String severity = rawSeverity.trim().toLowerCase(Locale.getDefault());
        if ("critical".equals(severity) || "high".equals(severity)) {
            return new ChipStyle(R.drawable.bg_chip_alert_critical, R.color.danger_red);
        }
        if ("medium".equals(severity) || "warning".equals(severity)) {
            return new ChipStyle(R.drawable.bg_chip_alert_warning, R.color.primary_navy);
        }
        return new ChipStyle(R.drawable.bg_chip_role, R.color.primary_navy);
    }

    static class ChipStyle {
        final int backgroundRes;
        final int textColorRes;

        ChipStyle(int backgroundRes, int textColorRes) {
            this.backgroundRes = backgroundRes;
            this.textColorRes = textColorRes;
        }
    }

    interface AlertActionListener {
        void onAlertSelected(@NonNull SecurityAlert alert);
    }
}
