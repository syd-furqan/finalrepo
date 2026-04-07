package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.AccessEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for admin audit event timelines.
 * Pattern: List adapter with compact event summary rows.
 * Known issue: event actor names are not resolved from user profiles in v1.
 */
public class AccessEventAdapter extends RecyclerView.Adapter<AccessEventAdapter.EventViewHolder> {
    private final List<AccessEvent> items = new ArrayList<>();
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public void submitList(@NonNull List<AccessEvent> events) {
        items.clear();
        items.addAll(events);
        notifyDataSetChanged();
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
        holder.textEventType.setText(event.getEventType());
        holder.textDescription.setText(event.getDescription());
        holder.textRequestId.setText(holder.itemView.getContext().getString(R.string.request_id_label) + ": " + event.getRequestId());
        if (event.getCreatedAt() != null) {
            holder.textTime.setText(format.format(event.getCreatedAt().toDate()));
        } else {
            holder.textTime.setText("--");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView textEventType;
        final TextView textDescription;
        final TextView textRequestId;
        final TextView textTime;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventType = itemView.findViewById(R.id.text_event_type);
            textDescription = itemView.findViewById(R.id.text_event_description);
            textRequestId = itemView.findViewById(R.id.text_event_request_id);
            textTime = itemView.findViewById(R.id.text_event_time);
        }
    }
}
