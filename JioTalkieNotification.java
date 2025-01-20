package com.jio.jiotalkie.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.application.customservice.wrapper.IMediaMessage;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.jio.jiotalkie.activity.DashboardActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.application.customservice.Mumble;

import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.network.RESTApiManager;
import com.jio.jiotalkie.util.ADCInfoUtils;
import com.jio.jiotalkie.util.EnumConstant;

public class JioTalkieNotification {
    private static final String TAG = "JioTalkieNotification";
    private static final int NOTIFICATION_ID = 2;
    private static final long VIBRATION_PATTERN[] = {0, 100};
    private final Set<Integer> notificationIds = new HashSet<>();

    private final HashMap<Integer,ArrayList<Integer>> mOneToOneNotificationIds = new HashMap<>();
    private final Set<Integer> mGroupNotificationIds = new HashSet<>();

    private final Context mContext;
    private final List<IMediaMessage> mUnreadMessages;

    private final NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mNotificationbuilder;

    public JioTalkieNotification(Context context) {
        mContext = context;
        mUnreadMessages = new ArrayList<>();
        mNotificationManager = NotificationManagerCompat.from(mContext);
        mNotificationbuilder = new NotificationCompat.Builder(mContext, getChannelId());
    }

    private String getChannelId() {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = EnumConstant.CHANNEL_ID;
            String channelName = mContext.getString(R.string.msg_received);
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    private void postNotification(Notification notification, int requestCode) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Posting notification");
            mNotificationManager.notify(requestCode, notification);
            notificationIds.add(requestCode);
        } else {
            Log.e(TAG, "Permission to post notifications is not granted");
        }
    }

    public void show(IMediaMessage message) {
        mUnreadMessages.add(message);
        int requestCode = (int) System.currentTimeMillis(); // Unique request code for each notification
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(mContext.getResources().getQuantityString(R.plurals.unread_message_count, mUnreadMessages.size(), mUnreadMessages.size()));
        for (IMediaMessage m : mUnreadMessages) {
            String line = mContext.getString(R.string.chat_notification_content, m.getOriginatorName(), m.getMessageContent());
            inboxStyle.addLine(line);
        }
        Intent channelListIntent = new Intent(mContext, DashboardActivity.class);
        boolean isGroupChat = message.getRecipientChannels() != null && !message.getRecipientChannels().isEmpty();
        if(message.isSOS()){
            isGroupChat=true;
        }
        String actorName = message.getOriginatorName();
        int userSession = message.getOriginatorId();
        if (!isGroupChat) {
            int senderUserId = message.getSenderUserId();
            Log.d(TAG, "show:  received personal chat Name " +actorName +" SenderUserId : "+ senderUserId);
            channelListIntent.putExtra(EnumConstant.Notification.CHAT_FRAGMENT.toString(), true);
            channelListIntent.putExtra(EnumConstant.Notification.USER_NAME.toString(), actorName);
            channelListIntent.putExtra(EnumConstant.Notification.USER_SESSION.toString(), userSession);
            channelListIntent.putExtra(EnumConstant.Notification.CHAT_TYPE.toString(), 1);
            if (mOneToOneNotificationIds.containsKey(senderUserId)) {
                Objects.requireNonNull(mOneToOneNotificationIds.get(senderUserId)).add(requestCode);
            } else {
                mOneToOneNotificationIds.put(senderUserId, new ArrayList<>(Arrays.asList(requestCode)));
            }
        } else {
            Log.d(TAG, "show:  received group chat" );
            channelListIntent.putExtra(EnumConstant.Notification.CHAT_FRAGMENT.toString(), true);
            channelListIntent.putExtra(EnumConstant.Notification.CHAT_TYPE.toString(), 2);
            mGroupNotificationIds.add(requestCode);
        }
        // Add a flag to indicate the notification was clicked
        channelListIntent.putExtra("notification_clicked", true);


        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, requestCode, channelListIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mNotificationbuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTicker(message.getOriginatorName())
                .setContentTitle(message.getOriginatorName())
                .setVibrate(VIBRATION_PATTERN);

        if (mUnreadMessages.size() > 0) {
            mNotificationbuilder.setNumber(mUnreadMessages.size());
        }

        // Handle text messages and location
        if (message.getMessageType() == Mumble.TextMessage.MsgType.TextMessageType) {
            if (message.getMessageContent().contains(EnumConstant.MESSAGE_TYPE_LOCATION)) {
                Log.d(TAG, "show: msg typ is location");
                mNotificationbuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.location_big))
                        .setContentText("Location received")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Location received"));
            }
            else {
                mNotificationbuilder.setSmallIcon(R.drawable.notification_icon);
                mNotificationbuilder.setLargeIcon((Bitmap) null);
                mNotificationbuilder.setContentText(message.getMessageContent())
                        .setStyle(inboxStyle);
            }
            Notification notification = mNotificationbuilder.build();
            postNotification(notification, requestCode);
        }

        else if (message.getMessageType() == Mumble.TextMessage.MsgType.ImageMessageType || message.getMessageType() == Mumble.TextMessage.MsgType.VideoMessageType) {
            GlideUrl glideMediaUrl = RESTApiManager.getInstance().getGlideMediaUrl(message.getMessageId());
            Log.d(TAG, "glideMediaUrl : " + glideMediaUrl.toStringUrl());

            // Call handleMediaNotification to process media
            handleMediaNotification(glideMediaUrl, message.getMessageContent(),requestCode, message.getOriginatorName(), inboxStyle);
        }
        // Handle document messages
        else if (message.getMessageType() == Mumble.TextMessage.MsgType.DocMessageType) {
            handleDocumentNotification(message, requestCode);
        }
        // SOS Receiver Notification
        else if( message.getMessageType() == Mumble.TextMessage.MsgType.VoiceMessageType && message.isSOS()){
            mNotificationbuilder.setContentText(message.getMessageContent())
                    .setStyle(inboxStyle);
            mNotificationbuilder.setSmallIcon(R.drawable.notification_icon);
            Notification notification = mNotificationbuilder.build();
            postNotification(notification, requestCode);
        }
    }

    private void handleMediaNotification(GlideUrl mediaURL, String contextText, int requestCode, String actorName, NotificationCompat.InboxStyle inboxStyle) {
        // Handle placeholder for image
        mNotificationbuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_group_placeholder)); // Set placeholder icon
        mNotificationbuilder.setStyle(new NotificationCompat.BigPictureStyle()
                .bigPicture(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_group_placeholder))); // Set placeholder icon

        Notification placeholderNotification = mNotificationbuilder.build();
        postNotification(placeholderNotification, requestCode);

        // Load image using Glide

        Glide.with(mContext).asBitmap().load(mediaURL).placeholder(R.drawable.ic_group_placeholder).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                Log.d(TAG, "Bitmap successfully loaded");
                NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                        .bigPicture(resource)
                        .bigLargeIcon(resource)
                        .setBigContentTitle(actorName)
                        .setSummaryText("Image received");
                mNotificationbuilder.setLargeIcon(resource)
                        .setStyle(bigPictureStyle);
                mNotificationbuilder.setContentText(contextText);
                getImageSize(resource);
                Notification notification = mNotificationbuilder.build();
                postNotification(notification, requestCode);
            }

            @Override
            public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                // Handle when the load is cleared
                Log.d(TAG, "Load cleared");

                mNotificationbuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_group_placeholder))
                        .setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_group_placeholder)));
                Notification notification = mNotificationbuilder.build();
                postNotification(notification, requestCode);
            }

            @Override
            public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                Log.e(TAG, "Bitmap loading failed");
                mNotificationbuilder.setContentText("Image loading failed")
                        .setStyle(inboxStyle);
                Notification notification = mNotificationbuilder.build();
                postNotification(notification, requestCode);
            }
        });
    }

    private void handleDocumentNotification(IMediaMessage message, int requestCode) {
        if (message.getMessageMimeType().equals(EnumConstant.TEXT_FILE_EXTENSION)) {
            mNotificationbuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.doc_file_txt_icon_black))
                    .setContentText(message.getMessageContent())
                    .setSmallIcon(R.drawable.notification_icon)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Text Document received"));
        } else if (message.getMessageMimeType().equals(EnumConstant.PDF_FILE_EXTENSION)) {
            mNotificationbuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.doc_file_pdf_icon_red))
                    .setContentText(message.getMessageContent())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("PDF Document received"));
        }
        Notification notification = mNotificationbuilder.build();
        postNotification(notification, requestCode);
    }

    private void getImageSize(Bitmap bitmap) {
        double byteCount = bitmap.getByteCount();
        ADCInfoUtils.sendInfoToAdc(EnumConstant.MessageType.IMAGE.toString(), byteCount / 1024, 0, 0, "", false,-1,-1,"",-1);
    }


    /**
     * Dismisses the unread messages notification, marking all messages read.
     */
    public void dismiss() {
        mUnreadMessages.clear();
        for (int id : notificationIds) {
            mNotificationManager.cancel(id);
        }
        notificationIds.clear();
    }

    public void clearOneToOneNotification(int userId) {
        if (mOneToOneNotificationIds.containsKey(userId)) {
            ArrayList<Integer> notificationIds = mOneToOneNotificationIds.get(userId);
            for (int id : notificationIds) {
                mNotificationManager.cancel(id);
            }
            // clear the notification id from Hash map.
            mOneToOneNotificationIds.remove(userId);
        }
    }

    public void clearGroupChatNotification() {
        for (int id : mGroupNotificationIds) {
            mNotificationManager.cancel(id);
        }
        // Clear all notification id from group hash set
        mGroupNotificationIds.clear();
    }
}
