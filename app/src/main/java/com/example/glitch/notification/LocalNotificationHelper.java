package com.example.glitch.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.glitch.MainActivity;
import com.example.glitch.R;

/**
 * Thin helper that owns Android notification-channel creation and dispatch.
 */
public class LocalNotificationHelper {
    private static final String CHANNEL_ID_GUEST_PASS = "guest_pass_lifecycle";
    private static final String CHANNEL_NAME_GUEST_PASS = "Guest Pass Updates";
    private static final String CHANNEL_DESCRIPTION_GUEST_PASS = "Alerts for guest pass lifecycle changes.";

    private final Context appContext;

    public LocalNotificationHelper(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_GUEST_PASS,
                CHANNEL_NAME_GUEST_PASS,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(CHANNEL_DESCRIPTION_GUEST_PASS);
        manager.createNotificationChannel(channel);
    }

    public boolean showGuestPassLifecycleNotification(
            @NonNull String title,
            @NonNull String message,
            int notificationId
    ) {
        ensureChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        Intent launchIntent = new Intent(appContext, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                appContext,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID_GUEST_PASS)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(appContext).notify(notificationId, builder.build());
        return true;
    }
}
