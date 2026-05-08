package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.StudentWarning;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentWarningAdapter extends RecyclerView.Adapter<StudentWarningAdapter.ViewHolder> {
    private final List<StudentWarning> warnings = new ArrayList<>();

    public void submitList(@NonNull List<StudentWarning> items) {
        warnings.clear();
        warnings.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_warning, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentWarning warning = warnings.get(position);
        holder.textLevel.setText(warning.getViolationLevel().toUpperCase(Locale.getDefault()));
        holder.textDetail.setText(warning.getDetail());
        if (warning.getCreatedAt() != null) {
            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(warning.getCreatedAt().toDate());
            holder.textDate.setText(date);
        } else {
            holder.textDate.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return warnings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textLevel, textDate, textDetail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textLevel = itemView.findViewById(R.id.text_warning_level);
            textDate = itemView.findViewById(R.id.text_warning_date);
            textDetail = itemView.findViewById(R.id.text_warning_detail);
        }
    }
}
