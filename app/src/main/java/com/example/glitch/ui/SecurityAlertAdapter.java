package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    public void submitList(@NonNull List<SecurityAlert> alerts) {
        items.clear();
        items.addAll(alerts);
        notifyDataSetChanged();
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
        holder.textIdentifier.setText(alert.getIdentifier());
        String severity = alert.getSeverity() == null ? "" : alert.getSeverity();
        holder.textSeverity.setText(severity.toUpperCase(Locale.getDefault()));
        ChipStyle chipStyle = resolveSeverityStyle(severity);
        holder.textSeverity.setBackgroundResource(chipStyle.backgroundRes);
        holder.textSeverity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), chipStyle.textColorRes));
        holder.textMessage.setText(alert.getMessage() + " (fails: " + alert.getFailCount() + ")");
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
}
