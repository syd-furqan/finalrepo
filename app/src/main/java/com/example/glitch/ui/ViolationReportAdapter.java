package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.ViolationReport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViolationReportAdapter extends RecyclerView.Adapter<ViolationReportAdapter.ViewHolder> {
    private final List<ViolationReport> reports = new ArrayList<>();
    private OnReportClickListener listener;

    public void setListener(OnReportClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<ViolationReport> items) {
        reports.clear();
        reports.addAll(items);
        notifyDataSetChanged();
    }

    public int indexOfReportId(@NonNull String reportId) {
        String target = reportId.trim();
        if (target.isEmpty()) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < reports.size(); i++) {
            if (target.equals(reports.get(i).getId())) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_violation_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViolationReport report = reports.get(position);
        String level = report.getViolationLevel().trim().toLowerCase(Locale.getDefault());
        holder.textLevel.setText(formatLevelLabel(level));
        holder.textLevel.setBackgroundResource(levelBackground(level));

        String subjectName = report.isGuestViolation()
                ? valueOr(report.getGuestName(), "Unknown Visitor")
                : valueOr(report.getSubjectStudentName(), "Unknown Student");
        String subjectMeta = report.isGuestViolation()
                ? "CNIC: " + valueOr(report.getGuestCnic(), "N/A")
                + "  •  Phone: " + valueOr(report.getGuestPhone(), "N/A")
                : "Student ID: " + valueOr(report.getSubjectStudentId(), "N/A")
                + "  •  Email: " + valueOr(report.getSubjectStudentEmail(), "N/A");
        holder.textSubjectName.setText(subjectName);
        holder.textSubjectMeta.setText(subjectMeta);
        holder.textReporterName.setText("Reported by: " + valueOr(report.getReporterName(), "Unknown")
                + " (" + formatRoleLabel(report.getReporterRole()) + ")");
        holder.textDetailPreview.setText(report.getDetail());

        if (report.getCreatedAt() != null) {
            String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(report.getCreatedAt().toDate());
            holder.textDate.setText(date);
        } else {
            holder.textDate.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onReportClicked(report);
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    private String valueOr(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    @NonNull
    private String formatLevelLabel(@NonNull String level) {
        switch (level) {
            case "v1":
            case "minor":
                return "Minor";
            case "v2":
            case "moderate":
                return "Moderate";
            case "v3":
            case "severe":
                return "Severe";
            default:
                return level.isEmpty() ? "N/A" : level.substring(0, 1).toUpperCase(Locale.getDefault()) + level.substring(1);
        }
    }

    private int levelBackground(@NonNull String level) {
        switch (level) {
            case "v1":
            case "minor":
                return R.drawable.bg_chip_level_minor;
            case "v2":
            case "moderate":
                return R.drawable.bg_chip_level_moderate;
            case "v3":
            case "severe":
                return R.drawable.bg_chip_level_severe;
            default:
                return R.drawable.bg_chip_role;
        }
    }

    @NonNull
    private String formatRoleLabel(String role) {
        String normalized = valueOr(role, "").toLowerCase(Locale.getDefault());
        if (normalized.isEmpty()) {
            return "Unknown";
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.getDefault());
        }
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault()) + normalized.substring(1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textLevel, textDate, textSubjectName, textSubjectMeta, textReporterName, textDetailPreview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textLevel = itemView.findViewById(R.id.text_report_level_badge);
            textDate = itemView.findViewById(R.id.text_report_date);
            textSubjectName = itemView.findViewById(R.id.text_report_subject_name);
            textSubjectMeta = itemView.findViewById(R.id.text_report_subject_meta);
            textReporterName = itemView.findViewById(R.id.text_report_reporter_name);
            textDetailPreview = itemView.findViewById(R.id.text_report_detail_preview);
        }
    }

    public interface OnReportClickListener {
        void onReportClicked(@NonNull ViolationReport report);
    }
}
