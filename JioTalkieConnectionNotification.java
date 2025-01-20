package com.jio.jiotalkie.service;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;

public class JioTalkieConnectionNotification {
    private static final int NOTIFICATION_ID = 101;
    private Service mService;
    private String mCustomTicker;
    private String mCustomContentText;

    public static JioTalkieConnectionNotification create(Service service, String ticker, String contentText) {
        return new JioTalkieConnectionNotification(service, ticker, contentText);
    }

    private JioTalkieConnectionNotification(Service service, String ticker, String contentText) {
        mService = service;
        mCustomTicker = ticker;
        mCustomContentText = contentText;
    }

    public void setCustomTicker(String ticker) {
        mCustomTicker = ticker;
    }

    public void setCustomContentText(String text) {
        mCustomContentText = text;
    }

    public void show() {
        createNotification();
    }

    public void hide() {
        mService.stopForeground(true);
    }

    private void createNotification() {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "jiotalkie_connection_notification_channel";
            String channelName = mService.getString(R.string.status_connected);
            NotificationChannel chan = new NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = mService.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(chan);
        }
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mService, channelId);

        // app name is always displayed in notification on >= O
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            builder.setContentTitle(mService.getString(R.string.app_name));
        }
        builder.setContentText(mCustomContentText);
        builder.setTicker(mCustomTicker);
        builder.setSmallIcon(R.drawable.notification_icon);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setCategory(NotificationCompat.CATEGORY_CALL);
        builder.setShowWhen(false);
        builder.setOngoing(true);

        Intent channelListIntent = new Intent(mService, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mService, 0,
                channelListIntent, FLAG_CANCEL_CURRENT | FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mService.startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            mService.startForeground(NOTIFICATION_ID, notification);
        }
    }
}
