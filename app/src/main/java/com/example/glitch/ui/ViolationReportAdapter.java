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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_violation_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViolationReport report = reports.get(position);
        holder.textLevel.setText(report.getViolationLevel().toUpperCase(Locale.getDefault()));
        holder.textStatus.setText(report.getStatus().toUpperCase(Locale.getDefault()));

        String subjectName = report.isGuestViolation()
                ? valueOr(report.getGuestName(), "Unknown Guest") + " (" + valueOr(report.getGuestCnic(), "N/A") + ")"
                : valueOr(report.getSubjectStudentName(), "Unknown Student");
        holder.textSubjectName.setText(subjectName);
        holder.textReporterName.setText("Reported by: " + valueOr(report.getReporterName(), "Unknown") + " (" + report.getReporterRole() + ")");
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textLevel, textStatus, textDate, textSubjectName, textReporterName, textDetailPreview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textLevel = itemView.findViewById(R.id.text_report_level_badge);
            textStatus = itemView.findViewById(R.id.text_report_status_badge);
            textDate = itemView.findViewById(R.id.text_report_date);
            textSubjectName = itemView.findViewById(R.id.text_report_subject_name);
            textReporterName = itemView.findViewById(R.id.text_report_reporter_name);
            textDetailPreview = itemView.findViewById(R.id.text_report_detail_preview);
        }
    }

    public interface OnReportClickListener {
        void onReportClicked(@NonNull ViolationReport report);
    }
}
