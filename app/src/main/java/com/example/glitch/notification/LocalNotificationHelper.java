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
    private static final String CHANNEL_ID_VEHICLE_PROGRAM = "vehicle_program_updates";
    private static final String CHANNEL_NAME_VEHICLE_PROGRAM = "Vehicle Program Updates";
    private static final String CHANNEL_DESCRIPTION_VEHICLE_PROGRAM = "Alerts for vehicle registration/removal application updates.";
    private static final String CHANNEL_ID_USER_NOTIFICATIONS = "user_notification_center";
    private static final String CHANNEL_NAME_USER_NOTIFICATIONS = "Notifications";
    private static final String CHANNEL_DESCRIPTION_USER_NOTIFICATIONS = "User notification center updates.";

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
        NotificationChannel vehicleChannel = new NotificationChannel(
                CHANNEL_ID_VEHICLE_PROGRAM,
                CHANNEL_NAME_VEHICLE_PROGRAM,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        vehicleChannel.setDescription(CHANNEL_DESCRIPTION_VEHICLE_PROGRAM);
        manager.createNotificationChannel(vehicleChannel);
        NotificationChannel userChannel = new NotificationChannel(
                CHANNEL_ID_USER_NOTIFICATIONS,
                CHANNEL_NAME_USER_NOTIFICATIONS,
                NotificationManager.IMPORTANCE_HIGH
        );
        userChannel.setDescription(CHANNEL_DESCRIPTION_USER_NOTIFICATIONS);
        manager.createNotificationChannel(userChannel);
    }

    public boolean showGuestPassLifecycleNotification(
            @NonNull String title,
            @NonNull String message,
            int notificationId
    ) {
        return showOnChannel(CHANNEL_ID_GUEST_PASS, title, message, notificationId);
    }

    public boolean showVehicleProgramNotification(
            @NonNull String title,
            @NonNull String message,
            int notificationId
    ) {
        return showOnChannel(CHANNEL_ID_VEHICLE_PROGRAM, title, message, notificationId);
    }

    public boolean showUserNotification(
            @NonNull String title,
            @NonNull String message,
            int notificationId
    ) {
        return showOnChannel(CHANNEL_ID_USER_NOTIFICATIONS, title, message, notificationId);
    }

    private boolean showOnChannel(
            @NonNull String channelId,
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
        launchIntent.setAction(MainActivity.ACTION_OPEN_NOTIFICATIONS);
        launchIntent.putExtra(MainActivity.EXTRA_LAUNCH_DESTINATION, MainActivity.DESTINATION_NOTIFICATIONS);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                appContext,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, channelId)
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
