package com.example.glitch.ui;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.NotificationItem;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for simple notification timeline cards.
 * Pattern: Adapter + ViewHolder for immutable notification items.
 * Known issue: read/unread state styling is not implemented in v1.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private final List<NotificationItem> notifications = new ArrayList<>();
    private final NotificationActionListener actionListener;

    public NotificationAdapter() {
        this((item) -> {
        });
    }

    public NotificationAdapter(@NonNull NotificationActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(@NonNull List<NotificationItem> items) {
        notifications.clear();
        notifications.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.textTitle.setText(item.getTitle());
        holder.textMessage.setText(item.getMessage());
        holder.textType.setText(item.getType().toUpperCase());
        if (item.isRead()) {
            holder.textTitle.setTypeface(Typeface.DEFAULT);
            holder.textTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_muted));
            holder.itemView.setAlpha(0.8f);
        } else {
            holder.textTitle.setTypeface(Typeface.DEFAULT_BOLD);
            holder.textTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_dark));
            holder.itemView.setAlpha(1f);
        }
        holder.itemView.setOnClickListener(v -> actionListener.onNotificationSelected(item));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textMessage;
        final TextView textType;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_notification_title);
            textMessage = itemView.findViewById(R.id.text_notification_message);
            textType = itemView.findViewById(R.id.text_notification_type);
        }
    }

    public interface NotificationActionListener {
        void onNotificationSelected(@NonNull NotificationItem item);
    }
}
