package com.jio.jiotalkie.adapter;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dataclass.MessageReceiptStatus;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.fragment.MessageDeliveryStatusDialogFragment;
import com.jio.jiotalkie.network.RESTApiManager;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.DateUtils;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.application.customservice.wrapper.IJioPttService;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.JioTalkieChats;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.util.LocationHelperUtils;

public class ChatAdapter extends RecyclerView.Adapter<ChatViewHolder> {

    private static final String TAG = ChatAdapter.class.getName();
    List<JioTalkieChats> mList;
    Context mContext;
    public IJioPttService mService;
    JioTalkieChats message;
    private final DateFormat mDateFormat;
    private CustomAnimationClass customAnimationDrawable;
    private CustomSOSAnimationClass customSOSAnimationDrawable;
    private MediaPlayer mediaPlayer;
    private JioTalkieSettings mJioTalkieSettings;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private String mCurrentPlayingMedia = "";
    private Runnable mRunnable = null;
    private ChatAdapterProvider mChatAdapterProvider;
    private ImageButton currentAudioPlayButton = null;
    private ImageView currentSosPlayButton = null;
    private ProgressBar currentProgressBar=null;
    private List<RegisteredUser> mRegUserList = new ArrayList<>();
    private HashMap<Integer, MessageReceiptStatus> messageReceiptStatusList = new HashMap<>();
    private int selectedPosition = -1;

    public void updateAudioUI(Integer position, String filePath, boolean isLeft, boolean isSos, ChatViewHolder viewHolderForCurrentPosition) {
        if(mList.isEmpty()) {
            return;
        }
        if (position>=0 && position< mList.size()) {
            mList.get(position).setMedia_path(filePath);
            notifyItemChanged(position);
            if (isSos) {
                if (isLeft) {
                    viewHolderForCurrentPosition.mediaPlayerLoadingBarReceiveSOS.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.buttonDownloadAudioSos.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.sosAudioPlayButton.setVisibility(View.VISIBLE);
                } else {
                    viewHolderForCurrentPosition.mediaPlayerLoadingBarSentSOS.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.buttonDownloadAudioSos2.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.sosSentAudioPlayButton.setVisibility(View.VISIBLE);
                    try {
                        viewHolderForCurrentPosition.audioDurationSender.setText(getMediaDuration(filePath));
                    }catch(Exception e){
                        Log.d(TAG,"Exception while setting audio duration");
                    }
                }
            } else {
                if (isLeft) {
                    viewHolderForCurrentPosition.mediaPlayerLoadingBarReceiveAudio.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.buttonDownloadAudio.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.buttonPlay.setVisibility(View.VISIBLE);
                    try {
                        viewHolderForCurrentPosition.audioDurationReceiver.setText(getMediaDuration(filePath));
                    }catch(Exception e){
                        Log.d(TAG,"Exception while setting audio duration");
                    }
                } else {
                    viewHolderForCurrentPosition.mediaPlayerLoadingBarSentAudio.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.buttonDownloadAudio2.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.buttonPlay2.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void updateVideoUI(int position, String filePath, boolean isLeft, boolean isSos, ChatViewHolder viewHolderForCurrentPosition) {
        Log.v("VideoMessages","updateVideoUI");
        if (position>=0 && position< mList.size()) {
            mList.get(position).setMedia_path(filePath);
            notifyItemChanged(position);
            if (isLeft) {
                if (viewHolderForCurrentPosition != null) {
                    viewHolderForCurrentPosition.progressBarVideoReceive.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.videoPlayReceiveButton.setVisibility(View.VISIBLE);
                    viewHolderForCurrentPosition.videoDownloadReceiveButton.setVisibility(View.GONE);
                }
            } else {
                if (viewHolderForCurrentPosition != null) {
                    viewHolderForCurrentPosition.progressBarVideoSend.setVisibility(View.GONE);
                    viewHolderForCurrentPosition.videoPlaySendButton.setVisibility(View.VISIBLE);
                    viewHolderForCurrentPosition.videoDownloadSendButton.setVisibility(View.GONE);
                }
            }
        }
    }

    public void updateDocumentUI(Integer position, String filePath, boolean isLeft, ChatViewHolder viewHolderForCurrentPosition) {
        if (position>=0 && position< mList.size()) {
            mList.get(position).setMedia_path(filePath);
            notifyItemChanged(position);
            if (isLeft) {
                viewHolderForCurrentPosition.progressBarDocReceiver.setVisibility(View.GONE);
                viewHolderForCurrentPosition.documentImageViewReceiver.setVisibility(View.VISIBLE);
                viewHolderForCurrentPosition.documentExtensionReceiver.setVisibility(View.VISIBLE);
                viewHolderForCurrentPosition.documentFileNameReceiver.setVisibility(View.VISIBLE);
                viewHolderForCurrentPosition.docDownloadReceiverBtn.setVisibility(View.GONE);
            } else {
                viewHolderForCurrentPosition.progressBarDocSend.setVisibility(View.GONE);
                viewHolderForCurrentPosition.docDownloadSendBtn.setVisibility(View.GONE);
            }
        }
    }
    public ChatAdapter(Context context, IJioPttService service, ChatAdapterProvider chatAdapterProvider) {
        this.mContext = context;
        this.mList = new ArrayList<>();
        this.mService = service;
        this.mDateFormat = SimpleDateFormat.getTimeInstance();
        this.mChatAdapterProvider = chatAdapterProvider;
        mJioTalkieSettings = JioTalkieSettings.getInstance(context.getApplicationContext());

    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View photoView = inflater.inflate(R.layout.list_chat_item, parent, false);
        ChatViewHolder viewHolder = new ChatViewHolder(photoView);
        return viewHolder;

    }


    @Override
    public void onBindViewHolder(@NonNull final ChatViewHolder holder, @SuppressLint("RecyclerView") int position) {
        message = mList.get(position);
        holder.playerViewLayoutSend.setVisibility(View.GONE);
        holder.playerViewLayoutReceive.setVisibility(View.GONE);
        if (message.getIs_self_chat()) {
            holder.userProfile.setVisibility(View.GONE);
            holder.messageBoxWithTime2.setVisibility(View.VISIBLE);
            holder.triangle2.setVisibility(View.VISIBLE);
            holder.messageBoxWithTime.setVisibility(View.GONE);
            holder.triangle.setVisibility(View.GONE);
            holder.target2.setVisibility(View.GONE);

            // Update background based on selection state
            if (selectedPosition == position) {
                holder.messageBoxWithTime2.setForeground(new ColorDrawable(Color.parseColor("#166416c9")));
            } else {
                holder.messageBoxWithTime2.setForeground(null);
            }

            holder.messageBoxWithTime2.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    JioTalkieChats clickedItem = mList.get(position);
                    boolean isTextMessage = false;
                    if (clickedItem.getMessage_type().equals(EnumConstant.MessageType.TEXT.name())) {
                        isTextMessage = true;
                    }
                    String textMessage = clickedItem.getMessage();
                    int previousPosition = selectedPosition;
                    selectedPosition = position;
                    mChatAdapterProvider.onItemSelected(clickedItem.getMsg_id(), selectedPosition, true, isTextMessage, textMessage);
                    // Notify changes to update views
                    notifyItemChanged(previousPosition);
                    notifyItemChanged(selectedPosition);
                    return true;
                }
            });

            holder.messageBoxWithTime2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position == selectedPosition) {
                        mChatAdapterProvider.onItemDeselected();
                        onItemDeselected();
                    }
                }
            });

            holder.triangle2.getBackground().setColorFilter(mContext.getResources().getColor(R.color.chat_triangle_self), PorterDuff.Mode.SRC_ATOP);
            if (message.getIs_group_chat() &&
                    message.getReceiver_displayed() != null && !message.getReceiver_displayed().isEmpty()) {
                String receiveDisplay = message.getReceiver_displayed().stream().collect(Collectors.joining(","));
                int messageReadUserCount;
                if (receiveDisplay.contains("[]") || receiveDisplay.contains("{}")) {
                    messageReadUserCount = 0;
                } else {
                    messageReadUserCount  = receiveDisplay.split(",").length;
                }
                int totalUserCount =  mRegUserList.size() - 1;
                if (totalUserCount == messageReadUserCount || messageReadUserCount > totalUserCount) {
                    message.setMsgStatus(EnumConstant.MsgStatus.Read.ordinal());
                }
            }

            setMessageStatusIcon(holder.messageStatusIcon,message.getMsgStatus());
            if (message.getMessage_type().equals(EnumConstant.MessageType.TEXT.name())) {
                holder.audioLayout2.setVisibility(View.GONE);
                holder.imageCard2.setVisibility(View.GONE);
                holder.documentViewSender.setVisibility(View.GONE);
                holder.text2.setVisibility(View.VISIBLE);
                holder.audioDurationSender.setVisibility(View.GONE);
                holder.text2.setText(Html.fromHtml(message.getMessage(), Html.FROM_HTML_MODE_LEGACY));
                holder.mapView2.setVisibility(View.GONE);
                holder.time2.setText(getFormattedTime(message.getReceived_time()));
                holder.sosSentMessageLayout.setVisibility(View.GONE);
            } else if (message.getMessage_type().equals(EnumConstant.MessageType.IMAGE.name())) {
                holder.audioLayout2.setVisibility(View.GONE);
                holder.imageCard2.setVisibility(View.VISIBLE);
                holder.documentViewSender.setVisibility(View.GONE);
                holder.text2.setVisibility(View.GONE);
                holder.mapView2.setVisibility(View.GONE);
                holder.time2.setText(getFormattedTime(message.getReceived_time()));
                holder.sosSentMessageLayout.setVisibility(View.GONE);
                holder.audioDurationSender.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(message.getMedia_path())) {
                    if (message.getFile_upload_status().equals(mContext.getResources().getString(R.string.uploading_pending))) {
                        holder.progressBarImageSend.setVisibility(View.VISIBLE);
                        holder.messageStatusIcon.setVisibility(View.GONE);
                    } else {
                        holder.progressBarImageSend.setVisibility(View.GONE);
                        holder.messageStatusIcon.setVisibility(View.VISIBLE);
                    }
                    Glide.with(mContext)
                            .load(message.getMedia_path())
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                    holder.time2.setText(getFormattedTime(message.getReceived_time()));
                                    return false;
                                }
                            })
                            .skipMemoryCache(true)   // Skips memory cache
                            .into(holder.image2);
                } else {
                    if (message.getMessage().contains("http") || message.getMessage().contains("https")) {
                        GlideUrl glideMediaUrl = RESTApiManager.getInstance().getGlideMediaUrl(message.getMsg_id());

                        message.setMessage(glideMediaUrl.toStringUrl());
                        Log.d(TAG, "Sent Image size (bytes):  "+message.getSize());
                        if (message.getSize() != -1) {
                            String imageSize = convertByteToFileSize(message.getSize());
                            holder.time2.setText(getFormattedTime(message.getReceived_time()) + "|" + imageSize);
                        } else {
                            fetchImageSize(message.getMsg_id(), message.getReceived_time(), holder.time2);
                        }
                        Glide.with(mContext)
                             .load(glideMediaUrl)
                             .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                        return false;
                                    }
                                })
                             .placeholder(R.drawable.ic_group_placeholder)
                             .into(holder.image2);
                    } else {
                        // TODO : need to fetch when it's called
                        Bitmap bitmap = getImageBitmapFromBase64(message.getMessage());
                        holder.image2.setImageBitmap(bitmap);
                        String imageSize = getBitmapSize(bitmap);
                        holder.time2.setText(getFormattedTime(message.getReceived_time()));
                    }
                }
                holder.image2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // We need to pass Auth info to Image URL.
                        GlideUrl localUrl = RESTApiManager.getInstance().getGlideMediaUrl(mList.get(position).getMsg_id());
                        mChatAdapterProvider.playFullScreen(localUrl.toStringUrl(), false);
                    }
                });
            } else if (message.getMessage_type().equals(EnumConstant.MessageType.SOS_AUDIO.name())) {
                JioTalkieChats currMessage = mList.get(position);
                holder.imageCard2.setVisibility(View.GONE);
                holder.audioLayout2.setVisibility(View.GONE);
                holder.documentViewSender.setVisibility(View.GONE);
                holder.text2.setVisibility(View.GONE);
                holder.mapView2.setVisibility(View.GONE);
                holder.audioDurationSender.setVisibility(View.GONE);
                holder.triangle2.getBackground().setColorFilter(mContext.getResources().getColor(R.color.chat_triangle_sos), PorterDuff.Mode.SRC_ATOP);
                String sosAudioSize = getMediaSize(mList.get(position).getMedia_path());
                holder.time2.setText(getFormattedTime(message.getReceived_time())+"|"+sosAudioSize);
                holder.sosSentMessageLayout.setVisibility(View.VISIBLE);
                String time = DateUtils.covertTimeToText(DateUtils.getStringDateFromLong(message.getReceived_time(),"yyyy-MM-dd'T'HH:mm:ss"));
                holder.sosSentTimeText.setText(time);
                String locationInfo = "";
                if (message.getLatitude() != null && message.getLongitude() != null && !message.getLatitude().isEmpty() && !message.getLongitude().isEmpty()) {
                    locationInfo = LocationHelperUtils.getLocationFromCoordinates(mContext, Double.parseDouble(message.getLatitude()), Double.parseDouble(message.getLongitude()));
                }
                holder.sosSentLocationText.setText(locationInfo);
                holder.sosSentBatteryText.setText(message.getBattery_info() + "%");
                holder.mediaPlayerLoadingBarSentSOS.setVisibility(View.GONE);
                if (currMessage.getMedia_path().contains("http")){
                    holder.buttonDownloadAudioSos2.setVisibility(View.VISIBLE);
                    holder.sosSentAudioPlayButton.setVisibility(View.GONE);
                }
                else {
                    holder.buttonDownloadAudioSos2.setVisibility(View.GONE);
                    holder.sosSentAudioPlayButton.setVisibility(View.VISIBLE);
                }

                holder.buttonDownloadAudioSos2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.mediaPlayerLoadingBarSentSOS.setVisibility(View.VISIBLE);
                        mChatAdapterProvider.downloadMediaFile(position,currMessage,true,true, currMessage.getMime_type(), currMessage.getMessage_type());
                    }
                });
                holder.sosSentAudioPlayButton.setOnClickListener(v -> {
                    Log.v("RestAPIMessages", "While Playing SOS mediaPath SENDER = "+currMessage.getMedia_path());
                    playAudioFile(mList.get(position).getMedia_path(), holder.buttonPlay2,
                            holder.sosSentAudioPlayButton, holder.seekbar2, holder.sosSentAnimationImageView,
                            true, holder.sosSentTimerText, holder.mediaPlayerLoadingBarSentSOS);
                });
            } else if (message.getMessage_type().equals(EnumConstant.MessageType.AUDIO.name())) {
                JioTalkieChats currMessage = mList.get(position);
                holder.imageCard2.setVisibility(View.GONE);
                holder.audioLayout2.setVisibility(View.VISIBLE);
                holder.documentViewSender.setVisibility(View.GONE);
                holder.text2.setVisibility(View.GONE);
                holder.mapView2.setVisibility(View.GONE);
                holder.sosSentMessageLayout.setVisibility(View.GONE);
                String audioSize = getMediaSize(mList.get(position).getMedia_path());
                holder.time2.setText(getFormattedTime(message.getReceived_time())+"|"+audioSize);
                holder.mediaPlayerLoadingBarSentAudio.setVisibility(View.GONE);
                if (currMessage.getMedia_path().contains("http")){
                    holder.buttonPlay2.setVisibility(View.GONE);
                    holder.buttonDownloadAudio2.setVisibility(View.VISIBLE);
                   // holder.audioDurationSender.setVisibility(View.VISIBLE);
                   // holder.audioDurationSender.setText(mContext.getResources().getString(R.string.default_audio_timer));
                } else {
                    holder.buttonPlay2.setVisibility(View.VISIBLE);
                    holder.buttonDownloadAudio2.setVisibility(View.GONE);
                    try {
                        holder.audioDurationSender.setVisibility(View.VISIBLE);
                        holder.audioDurationSender.setText(getMediaDuration(mList.get(position).getMedia_path()));
                    } catch (Exception e) {
                        Log.d(TAG,"sender audio duration exception "+Log.getStackTraceString(e));
                    }
                }
                if (message.getFile_upload_status().equals(mContext.getResources().getString(R.string.uploading_pending))) {
                    holder.mediaPlayerLoadingBarSentAudio.setVisibility(View.VISIBLE);
                    holder.messageStatusIcon.setVisibility(View.GONE);
                } else {
                    holder.mediaPlayerLoadingBarSentAudio.setVisibility(View.GONE);
                    holder.messageStatusIcon.setVisibility(View.VISIBLE);
                }
                holder.buttonDownloadAudio2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.mediaPlayerLoadingBarSentAudio.setVisibility(View.VISIBLE);
                        mChatAdapterProvider.downloadMediaFile(position,currMessage,false,false, currMessage.getMime_type(), currMessage.getMessage_type());
                    }
                });
                holder.buttonPlay2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.v("RestAPIMessages", "While Playing mediaPath SENDER = "+currMessage.getMedia_path());
                        playAudioFile(mList.get(position).getMedia_path(), holder.buttonPlay2,
                                holder.sosAudioPlayButton, holder.seekbar2, holder.sosSentAnimationImageView,
                                false, holder.sosTimerText, holder.mediaPlayerLoadingBarSentAudio);
                    }
                });
            }

            //Location MapView Added by Priyanshu.Vijay
            else if (message.getMessage_type().equals(EnumConstant.MessageType.LOCATION.name())) {
                if ((message.getLongitude().isEmpty() || message.getLatitude().isEmpty()) || (Objects.equals(message.getLongitude(), "null") || Objects.equals(message.getLatitude(), "null")))
                    return;

                holder.audioLayout2.setVisibility(View.GONE);
                holder.imageCard2.setVisibility(View.GONE);
                holder.text2.setVisibility(View.GONE);
                holder.documentViewSender.setVisibility(View.GONE);
                holder.mapView2.setVisibility(View.VISIBLE);
                holder.time2.setText(getFormattedTime(message.getReceived_time()));
                holder.sosSentMessageLayout.setVisibility(View.GONE);
                holder.audioDurationSender.setVisibility(View.GONE);
                Log.v("ChatAdapter","right "+message.getLatitude()+" "+message.getLongitude());
                updateUserLocation(holder.mapView2, Double.parseDouble(message.getLatitude()), Double.parseDouble(message.getLongitude()));
            }
            else if(message.getMessage_type().equals(EnumConstant.MessageType.VIDEO.name())){
                JioTalkieChats currMessage = mList.get(position);
                holder.audioLayout2.setVisibility(View.GONE);
                holder.imageCard2.setVisibility(View.GONE);
                holder.text2.setVisibility(View.GONE);
                holder.documentViewSender.setVisibility(View.GONE);
                holder.mapView2.setVisibility(View.GONE);
                String videoSize = getMediaSize(mList.get(position).getMedia_path());
                holder.time2.setText(getFormattedTime(message.getReceived_time())+"|"+videoSize);
                holder.sosSentMessageLayout.setVisibility(View.GONE);
                holder.playerViewLayoutSend.setVisibility(View.VISIBLE);
                holder.audioDurationSender.setVisibility(View.GONE);
                if (currMessage.getMedia_path().contains("http")){
                    holder.videoDownloadSendButton.setVisibility(View.VISIBLE);
                    holder.videoPlaySendButton.setVisibility(View.GONE);
                    holder.videoImageSend.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.progressBarVideoSend.setVisibility(View.VISIBLE);
                            mChatAdapterProvider.downloadMediaFile(position,currMessage,false,false, currMessage.getMime_type(), currMessage.getMessage_type());
                        }
                    });
                }
                else {
                    if (message.getFile_upload_status().equals(mContext.getResources().getString(R.string.uploading_pending))) {
                        holder.progressBarVideoSend.setVisibility(View.VISIBLE);
                        holder.videoPlaySendButton.setVisibility(View.GONE);
                        holder.messageStatusIcon.setVisibility(View.GONE);
                        holder.videoImageSend.setClickable(false);
                    } else {
                        holder.progressBarVideoSend.setVisibility(View.GONE);
                        holder.videoPlaySendButton.setVisibility(View.VISIBLE);
                        holder.messageStatusIcon.setVisibility(View.VISIBLE);
                        holder.videoImageSend.setClickable(true);
                    }
                    holder.videoDownloadSendButton.setVisibility(View.GONE);

                    holder.videoImageSend.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mChatAdapterProvider.playFullScreen(currMessage.getMedia_path(), true);
                        }
                    });
                }
                RequestOptions requestOptions = new RequestOptions();
                GlideUrl glideMediaUrl = RESTApiManager.getInstance().getGlideMediaUrl(currMessage.getMsg_id());
                currMessage.setMessage(glideMediaUrl.toStringUrl());
                Glide.with(mContext)
                        .load(glideMediaUrl)
                        .apply(requestOptions)
                        .thumbnail(Glide.with(mContext).load(glideMediaUrl))
                        .into(holder.videoImageSend);
                holder.videoDownloadSendButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.progressBarVideoSend.setVisibility(View.VISIBLE);
                        mChatAdapterProvider.downloadMediaFile(position,currMessage,false,false, currMessage.getMime_type(), currMessage.getMessage_type());
                    }
                });
            } else if(message.getMessage_type().equals(EnumConstant.MessageType.DOCUMENT.name())) {
                JioTalkieChats currMessage = mList.get(position);
                String fileName = message.getMedia_path();
                String mimeType = message.getMime_type();

                holder.text2.setVisibility(View.GONE);
                holder.mapView2.setVisibility(View.GONE);
                holder.imageCard2.setVisibility(View.GONE);
                holder.audioLayout2.setVisibility(View.GONE);
                holder.sosSentMessageLayout.setVisibility(View.GONE);
                holder.documentViewSender.setVisibility(View.VISIBLE);
                holder.audioDurationSender.setVisibility(View.GONE);
                if (currMessage.getMedia_path().contains("http")) {
                    holder.docDownloadSendBtn.setVisibility(View.VISIBLE);
                    holder.documentViewSender.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.docDownloadSendBtn.setVisibility(View.GONE);
                            holder.progressBarDocSend.setVisibility(View.VISIBLE);
                            mChatAdapterProvider.downloadMediaFile(position, currMessage, false, false, currMessage.getMime_type(), currMessage.getMessage_type());
                        }
                    });
                } else {
                    holder.documentViewSender.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openDocFiles(fileName, mimeType);
                        }
                    });
                    holder.docDownloadSendBtn.setVisibility(View.GONE);
                }
                if (message.getFile_upload_status().equals(mContext.getResources().getString(R.string.uploading_pending))) {
                    holder.progressBarDocSend.setVisibility(View.VISIBLE);
                    holder.messageStatusIcon.setVisibility(View.GONE);
                } else {
                    holder.progressBarDocSend.setVisibility(View.GONE);
                    holder.messageStatusIcon.setVisibility(View.VISIBLE);
                }
                setSenderDocView(holder);
            }
        } else {
            holder.userProfile.setVisibility(View.VISIBLE);
            holder.messageBoxWithTime2.setVisibility(View.GONE);
            holder.triangle2.setVisibility(View.GONE);
            holder.messageBoxWithTime.setVisibility(View.VISIBLE);
            holder.triangle.setVisibility(View.VISIBLE);
            holder.target2.setVisibility(View.GONE);
            holder.target.setVisibility(View.VISIBLE);

            // Update background based on selection state
            if (selectedPosition == position) {
                holder.messageBoxWithTime.setForeground(new ColorDrawable(Color.parseColor("#166416c9")));
            } else {
                holder.messageBoxWithTime.setForeground(null);
            }
            holder.messageBoxWithTime.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    JioTalkieChats clickedItem = mList.get(position);
                    boolean isTextMessage = false;
                    String textMessage;
                    if (clickedItem.getMessage_type().equals(EnumConstant.MessageType.TEXT.name())) {
                        isTextMessage = true;
                    }
                    textMessage = clickedItem.getMessage();
                    int previousPosition = selectedPosition;
                    selectedPosition = position;
                    mChatAdapterProvider.onItemSelected(clickedItem.getMsg_id(), selectedPosition, false, isTextMessage, textMessage);
                    // Notify changes to update views
                    notifyItemChanged(previousPosition);
                    notifyItemChanged(selectedPosition);
                    return true;
                }
            });
            holder.messageBoxWithTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position == selectedPosition) {
                        mChatAdapterProvider.onItemDeselected();
                        onItemDeselected();
                    }
                }
            });
            holder.triangle.getBackground().setColorFilter(mContext.getResources().getColor(R.color.chat_triangle_other), PorterDuff.Mode.SRC_ATOP);
            if(message.getUser_name() != null && message.getUser_name().length()!=0)
            {
                holder.userProfile.setText(message.getUser_name().substring(0, 1));
            }
            holder.target.setText(message.getUser_name());
            if (message.getMessage_type().equals(EnumConstant.MessageType.TEXT.name())) {
                holder.audioLayout.setVisibility(View.GONE);
                holder.imageCard.setVisibility(View.GONE);
                holder.mapView.setVisibility(View.GONE);
                holder.documentViewReceiver.setVisibility(View.GONE);
                holder.text.setVisibility(View.VISIBLE);
                holder.text.setText(Html.fromHtml(message.getMessage(),Html.FROM_HTML_MODE_LEGACY));
                holder.time.setText(getFormattedTime(message.getReceived_time()));
                holder.target.setText(message.getUser_name());
                holder.sosReceivedMessageLayout.setVisibility(View.GONE);
            } else if (message.getMessage_type().equals(EnumConstant.MessageType.IMAGE.name())) {
                holder.mapView.setVisibility(View.GONE);
                holder.audioLayout.setVisibility(View.GONE);
                holder.documentViewReceiver.setVisibility(View.GONE);
                holder.imageCard.setVisibility(View.VISIBLE);
                holder.text.setVisibility(View.GONE);
                holder.time.setText(getFormattedTime(message.getReceived_time()));
                holder.target.setText(message.getUser_name());
                holder.sosReceivedMessageLayout.setVisibility(View.GONE);
                if(message.getMessage().contains("http") || message.getMessage().contains("https")) {
                    GlideUrl glideMediaUrl = RESTApiManager.getInstance().getGlideMediaUrl(message.getMsg_id());
                    Log.d(TAG, "Received Image size (bytes):  "+message.getSize());
                    if (message.getSize() != -1) {
                        String imageSize = convertByteToFileSize(message.getSize());
                        holder.time.setText(getFormattedTime(message.getReceived_time()) + "|" + imageSize);
                    } else {
                        fetchImageSize(message.getMsg_id(), message.getReceived_time(), holder.time);
                    }
                    message.setMessage(glideMediaUrl.toStringUrl());
                    Glide.with(mContext)
                         .load(glideMediaUrl)
                         .listener(new RequestListener<Drawable>() {
                             @Override
                             public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                                 return false;
                             }

                             @Override
                             public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                 return false;
                             }
                         })
                         .placeholder(R.drawable.ic_group_placeholder)
                         .into(holder.image);
                } else {
                    // TODO : Need to be check when it's called
                    Bitmap bitmap = getImageBitmapFromBase64(message.getMessage());
                    holder.image.setImageBitmap(bitmap);
                    String imageSize = getBitmapSize(bitmap);
                    holder.time.setText(getFormattedTime(message.getReceived_time()));
                }
                holder.imageCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mChatAdapterProvider.playFullScreen(mList.get(position).getMessage(), false);
                    }
                });
            } else if (message.getMessage_type().equals(EnumConstant.MessageType.AUDIO.name())) {
                JioTalkieChats currMessage = mList.get(position);
                holder.mapView.setVisibility(View.GONE);
                holder.imageCard.setVisibility(View.GONE);
                holder.documentViewReceiver.setVisibility(View.GONE);
                holder.audioLayout.setVisibility(View.VISIBLE);
                holder.text.setVisibility(View.GONE);
                String audioSize = getMediaSize(mList.get(position).getMedia_path());
                holder.time.setText(getFormattedTime(message.getReceived_time())+"|"+audioSize);
                holder.target.setText(message.getUser_name());
                holder.sosReceivedMessageLayout.setVisibility(View.GONE);
                holder.mediaPlayerLoadingBarReceiveAudio.setVisibility(View.GONE);

                if (currMessage.getMedia_path().contains("http")){
                    holder.buttonPlay.setVisibility(View.GONE);
                    holder.buttonDownloadAudio.setVisibility(View.VISIBLE);
                    holder.audioDurationReceiver.setVisibility(View.GONE);
                    //holder.audioDurationReceiver.setText(mContext.getResources().getString(R.string.default_audio_timer));
                }
                else {
                    holder.buttonPlay.setVisibility(View.VISIBLE);
                    holder.buttonDownloadAudio.setVisibility(View.GONE);
                    try {
                        holder.audioDurationReceiver.setVisibility(View.VISIBLE);
                        holder.audioDurationReceiver.setText(getMediaDuration(mList.get(position).getMedia_path()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                holder.buttonDownloadAudio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.mediaPlayerLoadingBarReceiveAudio.setVisibility(View.VISIBLE);
                        mChatAdapterProvider.downloadMediaFile(position,currMessage,true,false, currMessage.getMime_type(), currMessage.getMessage_type());
                    }
                });
                holder.buttonPlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.v("RestAPIMessages", "While Playing mediaPath RECEIVE= "+currMessage.getMedia_path());
                        playAudioFile(mList.get(position).getMedia_path(), holder.buttonPlay, holder.sosAudioPlayButton, holder.seekbar, holder.sosAnimationImageView, false, holder.sosTimerText, holder.mediaPlayerLoadingBarReceiveAudio);
                    }
                });
            } else if(message.getMessage_type().equals(EnumConstant.MessageType.VIDEO.name())){
                JioTalkieChats currMessage = mList.get(position);
                holder.audioLayout.setVisibility(View.GONE);
                holder.imageCard.setVisibility(View.GONE);
                holder.documentViewReceiver.setVisibility(View.GONE);
                holder.text.setVisibility(View.GONE);
                holder.mapView.setVisibility(View.GONE);
                String videoSize = getMediaSize(mList.get(position).getMedia_path());
                holder.time.setText(getFormattedTime(message.getReceived_time())+"|"+videoSize);
                holder.sosReceivedMessageLayout.setVisibility(View.GONE);
                holder.playerViewLayoutReceive.setVisibility(View.VISIBLE);
                if (currMessage.getMedia_path().contains("http")){
                    holder.videoDownloadReceiveButton.setVisibility(View.VISIBLE);
                    holder.videoPlayReceiveButton.setVisibility(View.GONE);
                    holder.videoImageReceive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.progressBarVideoReceive.setVisibility(View.VISIBLE);
                            mChatAdapterProvider.downloadMediaFile(position,currMessage,true,false, currMessage.getMime_type(), currMessage.getMessage_type());
                        }
                    });
                }
                else {
                    holder.videoDownloadReceiveButton.setVisibility(View.GONE);
                    holder.videoPlayReceiveButton.setVisibility(View.VISIBLE);
                    holder.videoImageReceive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mChatAdapterProvider.playFullScreen(currMessage.getMedia_path(), true);
                        }
                    });
                }
                RequestOptions requestOptions = new RequestOptions();
                GlideUrl glideMediaUrl = RESTApiManager.getInstance().getGlideMediaUrl(currMessage.getMsg_id());
                currMessage.setMessage(glideMediaUrl.toStringUrl());
                Glide.with(mContext)
                        .load(currMessage.getMedia_path())
                        .apply(requestOptions)
                        .thumbnail(Glide.with(mContext).load(glideMediaUrl))
                        .into(holder.videoImageReceive);
                holder.videoDownloadReceiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.progressBarVideoReceive.setVisibility(View.VISIBLE);
                        mChatAdapterProvider.downloadMediaFile(position,currMessage,true,false, currMessage.getMime_type(), currMessage.getMessage_type());
                    }
                });

            } else if (message.getMessage_type().equals(EnumConstant.MessageType.SOS_AUDIO.name())) {
                JioTalkieChats currMessage = mList.get(position);
                holder.audioLayout.setVisibility(View.GONE);
                holder.imageCard.setVisibility(View.GONE);
                holder.text.setVisibility(View.GONE);
                holder.target.setVisibility(View.GONE);
                holder.mapView.setVisibility(View.GONE);
                holder.documentViewReceiver.setVisibility(View.GONE);
                holder.sosReceivedMessageLayout.setVisibility(View.VISIBLE);
                holder.triangle.getBackground().setColorFilter(mContext.getResources().getColor(R.color.chat_triangle_sos), PorterDuff.Mode.SRC_ATOP);
                String sosAudioSize = getMediaSize(mList.get(position).getMedia_path());
                holder.time.setText(getFormattedTime(message.getReceived_time())+"|"+sosAudioSize);
                holder.sosSenderName.setText(message.getUser_name() + " sent SOS alert");
                holder.sosBatteryStatusText.setText(message.getBattery_info() + "%");
                String time = DateUtils.covertTimeToText(DateUtils.getStringDateFromLong(message.getReceived_time(),"yyyy-MM-dd'T'HH:mm:ss"));
                holder.sosReceiveTimeText.setText(time);
                holder.mediaPlayerLoadingBarReceiveSOS.setVisibility(View.GONE);

                if (currMessage.getMedia_path().contains("http")){
                    holder.buttonDownloadAudioSos.setVisibility(View.VISIBLE);
                    holder.sosAudioPlayButton.setVisibility(View.GONE);
                }
                else {
                    holder.buttonDownloadAudioSos.setVisibility(View.GONE);
                    holder.sosAudioPlayButton.setVisibility(View.VISIBLE);
                }
                if (message.getLatitude() != null && message.getLongitude() != null && !message.getLatitude().trim().isEmpty() && !message.getLongitude().trim().isEmpty()) {
                    String receivedLocation = LocationHelperUtils.getLocationFromCoordinates(mContext, Double.parseDouble(message.getLatitude()), Double.parseDouble(message.getLongitude()));
                    holder.sosLocationText.setText(receivedLocation);
                }
                holder.buttonDownloadAudioSos.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.mediaPlayerLoadingBarReceiveSOS.setVisibility(View.VISIBLE);
                        mChatAdapterProvider.downloadMediaFile(position,currMessage,true,true, currMessage.getMime_type(), currMessage.getMessage_type());
                    }
                });
                holder.sosAudioPlayButton.setOnClickListener(v -> {
                    Log.v("RestAPIMessages", "While Playing SOS mediaPath RECEIVE = "+currMessage.getMedia_path());
                    playAudioFile(mList.get(position).getMedia_path(), holder.buttonPlay, holder.sosAudioPlayButton, holder.seekbar, holder.sosAnimationImageView, true, holder.sosTimerText, holder.mediaPlayerLoadingBarReceiveSOS);
                });
                holder.sosReceiveNavigationImage.setOnClickListener(v -> {
                    if (mList.get(position).getLatitude() != null && mList.get(position).getLongitude() != null && !mList.get(position).getLatitude().isEmpty() && !mList.get(position).getLongitude().isEmpty()) {
                        mChatAdapterProvider.onNavigateButtonClick(Double.parseDouble(mList.get(position).getLatitude()), Double.parseDouble(mList.get(position).getLongitude()), holder.view);
                    } else {
                        Toast.makeText(mContext, "No Location Found", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (message.getMessage_type().equals(EnumConstant.MessageType.LOCATION.name())) {
                if ((message.getLongitude().isEmpty() || message.getLatitude().isEmpty()) || (Objects.equals(message.getLongitude(), "null") || Objects.equals(message.getLatitude(), "null")))
                    return;
                holder.audioLayout.setVisibility(View.GONE);
                holder.imageCard.setVisibility(View.GONE);
                holder.text.setVisibility(View.GONE);
                holder.documentViewReceiver.setVisibility(View.GONE);
                holder.mapView.setVisibility(View.VISIBLE);
                holder.time.setText(getFormattedTime(message.getReceived_time()));
                holder.target.setText(message.getUser_name());

                holder.sosReceivedMessageLayout.setVisibility(View.GONE);

                Log.v("ChatAdapter","left"+message.getLatitude()+" "+message.getLongitude()+" "+message.getLatitude().getClass().getName());
                updateUserLocation(holder.mapView, Double.parseDouble(message.getLatitude()), Double.parseDouble(message.getLongitude()));
            }
            else if (message.getMessage_type().equals(EnumConstant.MessageType.DOCUMENT.name())) {
                JioTalkieChats currMessage = mList.get(position);
                String fileName = message.getMedia_path();
                String mimeType = message.getMime_type();
                holder.text.setVisibility(View.GONE);
                holder.mapView.setVisibility(View.GONE);
                holder.imageCard.setVisibility(View.GONE);
                holder.audioLayout.setVisibility(View.GONE);
                holder.sosReceivedMessageLayout.setVisibility(View.GONE);
                holder.documentViewReceiver.setVisibility(View.VISIBLE);
                if (currMessage.getMedia_path().contains("http")) {
                    holder.docDownloadReceiverBtn.setVisibility(View.VISIBLE);
                    holder.documentViewReceiver.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.docDownloadReceiverBtn.setVisibility(View.GONE);
                            holder.progressBarDocReceiver.setVisibility(View.VISIBLE);
                            mChatAdapterProvider.downloadMediaFile(position,currMessage,true,false, currMessage.getMime_type(), currMessage.getMessage_type());
                        }
                    });
                } else {
                    holder.documentViewReceiver.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openDocFiles(fileName,mimeType);
                        }
                    });
                    holder.docDownloadReceiverBtn.setVisibility(View.GONE);
                }
                setReceiverDocView(holder);
            }
        }
        long time1 = message.getReceived_time();
        long time2 = 1;
        String date1 = DateUtils.getStringDateFromLong(time1, DateUtils.dateFormatDateDivider);
        if (position > 0) {
            time2 = mList.get(position - 1).getReceived_time();

            String date2 = DateUtils.getStringDateFromLong(time2, DateUtils.dateFormatDateDivider);

            if (!date1.equals(date2)) {
                holder.dateDivider.setText(DateUtils.CompareDate(date1));
                holder.dateDivider.setVisibility(View.VISIBLE);
            } else {
                holder.dateDivider.setVisibility(View.GONE);
            }

        } else if (position == 0) {
            holder.dateDivider.setText(DateUtils.CompareDate(date1));
            holder.dateDivider.setVisibility(View.VISIBLE);
        }

    }

    private void fetchImageSize(String msdId, long receivedTime, TextView time) {
        Call<Void> call = RESTApiManager.getInstance().fetchMediaFileSize(msdId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Retrieve the Content-Length header
                    String contentLength = response.headers().get("Content-Length");
                    if (contentLength != null) {
                        Log.d(TAG, "Chat fetchImageSize Content-Length: " + contentLength);
                        long imageSizeInByte = Long.parseLong(contentLength);
                        mChatAdapterProvider.updateImageSize(message.getMsg_id(), imageSizeInByte);
                        String imageSize = convertByteToFileSize(imageSizeInByte);
                        time.setText(getFormattedTime(receivedTime) + "|" + imageSize);
                    } else {
                        Log.d(TAG, "Chat fetchImageSize Content-Length header not found");
                    }
                } else {
                    Log.d(TAG, "Chat fetchImageSize Request failed with code: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Chat fetchImageSize Request error: " + t.getMessage());
            }
        });
    }

    private String getMediaDuration(String mediaPath) throws IOException {
        long duration =-1;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mediaPath);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                duration= Long.parseLong(durationStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting media duration", e);
        } finally {
            retriever.release();
        }
        return formatDuration(duration); // Return -1 if there was an error
    }

    private String getMediaSize(String mediaPath){
        File file = new File(mediaPath);
        long fileSizeInBytes = file.length();
        return convertByteToFileSize(fileSizeInBytes);
    }
    private String convertByteToFileSize(long fileSizeInBytes) {
        double thresholdSizeInKB = 1000.0;
        String mediaSize = "";
        // Convert file size to KB, MB, or GB
        double fileSizeInKB = fileSizeInBytes / 1024.0;
        mediaSize = String.format("%.2f kb", fileSizeInKB);
        if (fileSizeInKB >= thresholdSizeInKB) {
             double fileSizeInMB = fileSizeInKB / 1024.0;
            mediaSize = String.format("%.2f mb", fileSizeInMB);
        }
        return mediaSize;
    }

    private String getBitmapSize(Bitmap bitmap){
        double thresholdSizeInKB = 1000.0;
        String mediaSize="";
        // Convert file size to KB, MB, or GB
        double fileSizeInKB = bitmap.getByteCount() / 1024.0;
        mediaSize= String.format("%.2f kb", fileSizeInKB);
        if(fileSizeInKB >= thresholdSizeInKB){
            double fileSizeInMB = fileSizeInKB / 1024.0;
            mediaSize= String.format("%.2f mb", fileSizeInMB);
        }
        return mediaSize;
    }

    private String getImageSizeFromGlide(int width,int height){
        double thresholdSizeInKB = 1000.0;
        int bytesPerPixel = 4;  // ARGB_8888 uses 4 bytes per pixel
        int imageSizeInBytes = width * height * bytesPerPixel;
        double imageSizeInKB = imageSizeInBytes / 1024.0;
        String mediaSize="";
        mediaSize= String.format("%.2f kb", imageSizeInKB);
        if(imageSizeInKB >= thresholdSizeInKB){
            double imageSizeInMB = imageSizeInKB / 1024.0;
            mediaSize= String.format("%.2f mb", imageSizeInMB);
        }
        return mediaSize;
    }

    private String formatDuration(long durationMillis) {
        if (durationMillis < 0) {
            return "00:00";
        }

        int totalSeconds = (int) (durationMillis / 1000);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if(seconds==0 && minutes!=1){
            seconds=1;
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateUserLocation(MapView map, Double latitude, Double longitude) {
        if(map==null)
        {
            return;
        }
        map.onCreate(null);
        map.onResume();
//        Double latitude,longitude;
//        latitude= 12.9716;
//        longitude= 77.5946;
//        Log.v("ChatAdapter", latitude + " " + longitude);

        map.getMapAsync(googleMap -> {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 13f));
            googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)));
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        });

    }

    private void playAudioFile(String mediaUri, ImageButton audioPlayButton, ImageView sosPlayButton, ImageView audioAnimView, ImageView sosAnimView, boolean isSOSAudio, TextView sosTimerText, ProgressBar mediaPlayerLoadingBar) {
        if (currentSosPlayButton != null) {
            currentSosPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_play));
        }
        if (currentAudioPlayButton != null) {
            currentAudioPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_play));
        }
        if(currentProgressBar!=null){
            currentProgressBar.setVisibility(View.GONE);
        }

        if (mediaPlayer != null && !mediaUri.equals(mCurrentPlayingMedia)) {

            mHandler.removeCallbacks(mRunnable);
            mediaPlayer.release();
            mediaPlayer = null;
            if (customSOSAnimationDrawable != null) {
                customSOSAnimationDrawable.stop();
                customSOSAnimationDrawable = null;
            }
            if (customAnimationDrawable != null) {
                customAnimationDrawable.stop();
                customAnimationDrawable = null;
            }

        }

        if (mediaPlayer != null) {
            //pause
            if (mediaPlayer.isPlaying()) {
                mHandler.removeCallbacks(mRunnable);
                mediaPlayer.pause();
                if (isSOSAudio) {
                    sosPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_play));
                    if (customSOSAnimationDrawable != null) {
                        customSOSAnimationDrawable.pause();
                    }
                } else {
                    audioPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_play));
                    if (customAnimationDrawable != null) {
                        customAnimationDrawable.pause();
                    }
                }
                // resume
            } else {
                if (isSOSAudio) {
                    mediaPlayer.start();
                    sosPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_resume));
                    if (mediaPlayer.getCurrentPosition() == 0 || mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()) {
                        if(customSOSAnimationDrawable!=null)
                            customSOSAnimationDrawable.start();
                    } else {
                        customSOSAnimationDrawable.resume();
                    }
                } else {
                    mediaPlayer.start();
                    audioPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_resume));
                    if (mediaPlayer.getCurrentPosition() == 0 || mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()) {
                        if(customAnimationDrawable!=null)
                            customAnimationDrawable.start();
                    } else {
                        customAnimationDrawable.resume();
                    }
                }
            }

        } else {
            mCurrentPlayingMedia = mediaUri;
            if (!isSOSAudio) {
                currentAudioPlayButton = audioPlayButton;
            } else {
                currentSosPlayButton = sosPlayButton;
            }
            currentProgressBar = mediaPlayerLoadingBar;
            if (mCurrentPlayingMedia.contains("http")) {
                mediaPlayerLoadingBar.setVisibility(View.VISIBLE);

                if(mediaPlayer!=null)mediaPlayer.reset();
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    mediaPlayer.setDataSource(mCurrentPlayingMedia);
                    mediaPlayer.prepareAsync();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayerLoadingBar.setVisibility(View.GONE);
                        if (isSOSAudio) {
                            sosPlayButton.setVisibility(View.VISIBLE);
                            customSOSAnimationDrawable = new CustomSOSAnimationClass(mContext, sosAnimView,
                                    mp.getDuration(), sosTimerText);
                            sosPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_resume));
                            mp.start();
                            customSOSAnimationDrawable.start();
                        } else {
                            audioPlayButton.setVisibility(View.VISIBLE);
                            customAnimationDrawable = new CustomAnimationClass(mContext, audioAnimView,
                                    mp.getDuration());
                            audioPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_resume));
                            mp.start();
                            customAnimationDrawable.start();
                        }
                    }
                });
            } else {
                mediaPlayer = MediaPlayer.create(mContext, Uri.parse(mediaUri));
                if (mediaPlayer != null) {
                    if (isSOSAudio) {
                        customSOSAnimationDrawable = new CustomSOSAnimationClass(mContext, sosAnimView,
                                mediaPlayer.getDuration(), sosTimerText);
                        sosPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_resume));
                        mediaPlayer.start();
                        customSOSAnimationDrawable.start();
                    } else {
                        customAnimationDrawable = new CustomAnimationClass(mContext, audioAnimView, mediaPlayer.getDuration());
                        audioPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_resume));
                        mediaPlayer.start();
                        customAnimationDrawable.start();
                    }
                }
            }
        }

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mHandler.removeCallbacks(mRunnable);
                    if (!isSOSAudio) {
                        if(customAnimationDrawable!=null)
                            customAnimationDrawable.stop();
                        currentAudioPlayButton.setVisibility(View.VISIBLE);
                        currentAudioPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_play));
                    } else {
                        if(customSOSAnimationDrawable!=null)
                            customSOSAnimationDrawable.stop();
                        currentSosPlayButton.setVisibility(View.VISIBLE);
                        currentSosPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_play));
                    }
                }
            });
        }
    }
    public void pauseAudio(){
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (currentAudioPlayButton != null)
                currentAudioPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_play));
            if (customAnimationDrawable != null){
                customAnimationDrawable.pause();
            }
            if (currentSosPlayButton != null) {
                currentSosPlayButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sos_play));
            }
            if (customSOSAnimationDrawable != null) {
                customSOSAnimationDrawable.pause();
            }
        }
    }
    private void setImageBitmap(String message, ImageView image) {
        image.setImageBitmap(getImageBitmapFromBase64(message));
    }

    private Bitmap getImageBitmapFromBase64(String message) {
        try {
            String tempString = message.split(",")[1];
            String encodedString = "";
            if (tempString.contains("\"")) {
                encodedString = tempString.split("\"/>")[0];
            } else if (tempString.contains(" />")) {
                //Image received from Kai Case
                encodedString = tempString.split(" />")[0];
            }
            byte[] decodedBytes = Base64.decode(encodedString, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getItemCount() {
        if (mList != null && !mList.isEmpty()) {
            return mList.size();
        } else {
            return 0;
        }
    }

    public void clear() {
        int size = mList.size();
        mList.clear();
        notifyItemRangeRemoved(0, size);
    }

    public void add(JioTalkieChats message) {
        if (TextUtils.isEmpty(message.getUser_name().trim())) {
            message.setUser_name(getUsername(message.getUser_session_id()));
        }
        mList.add(message);
        notifyItemInserted(mList.size() - 1);
    }

    public void refreshData(JioTalkieChats message) {
        for (int i = 0;i<mList.size();i++) {
            if (mList.get(i).getMsg_id().equals(message.getMsg_id())) {
                mList.get(i).setFile_upload_status(message.getFile_upload_status());
                mList.get(i).setMessage(message.getMessage());
                notifyItemChanged(i);
                break;
            }
        }
    }

    private String getFormattedTime(long receivedTime) {
        return CommonUtils.getFormattedDateTime(receivedTime);
    }

    private void setMessageStatusIcon(ImageView statusIcon, int status) {
        if (status == EnumConstant.MsgStatus.DeliveredToServer.ordinal() || status == EnumConstant.MsgStatus.Undelivered.ordinal()) {
            statusIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_delivered));
        } else if (status == EnumConstant.MsgStatus.DeliveredToClient.ordinal()) {
            statusIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_delivered_to_client));
        } else if (status == EnumConstant.MsgStatus.Read.ordinal()) {
            statusIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_read_icon));
        }
    }

    public void updateMessageStatus(String msgId, int status, String receiverDisplayedList, String receiverDeliveredList) {
        if(msgId == null || msgId.isEmpty()){
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mList.parallelStream().filter(item -> item.getMsg_id() != null && item.getMsg_id().equals(msgId)).forEach(data -> {
                data.setMsgStatus(status);
                if (receiverDisplayedList != null && receiverDeliveredList != null) {
                    data.setReceiver_displayed(Arrays.asList(receiverDisplayedList.split(",")));
                    data.setReceiver_delivered(Arrays.asList(receiverDeliveredList.split(",")));
                }
            });
            notifyDataSetChanged();
        }
    }

    private void setSenderDocView(ChatViewHolder holder){
        holder.time2.setText(getFormattedTime(message.getReceived_time()));
        holder.documentFileNameSender.setText(message.getMessage());
        holder.documentExtensionSender.setText(CommonUtils.getMineTypeText(message.getMime_type()));
        holder.documentImageViewSender.setBackgroundResource(CommonUtils.getIconMineType(message.getMime_type()));
    }
    private void setReceiverDocView(ChatViewHolder holder){
        holder.time.setText(getFormattedTime(message.getReceived_time()));
        holder.documentFileNameReceiver.setText(message.getMessage());
        holder.documentExtensionReceiver.setText(CommonUtils.getMineTypeText(message.getMime_type()));
        holder.documentImageViewReceiver.setBackgroundResource(CommonUtils.getIconMineType(message.getMime_type()));
    }
    private void openDocFiles(String fileName,String mimeType) {
        File file = new File(fileName);
        Uri path = FileProvider.getUriForFile(mContext.getApplicationContext(),EnumConstant.AUTHORITY_PROVIDER_NAME,file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        /** Right now we are support only txt and pdf file extension.
         In future, if we supports others format like like
         DOC,PPT, PPTX, RSS or HTML,etc. then we have to call setDataAndType() for others file extension.
         **/
        if (mimeType.equals(EnumConstant.TEXT_FILE_EXTENSION)) {
            intent.setDataAndType(path, EnumConstant.MIME_TYPE_TEXT_PLAIN_DOC);
        } else {
            intent.setDataAndType(path, mimeType);
        }
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, mContext.getString(R.string.no_app_found), Toast.LENGTH_SHORT).show();
        }
    }

    public void updateRegisterUserList(List<RegisteredUser> regUserList) {
        mRegUserList.clear();
        mRegUserList = regUserList;
    }

    public String getUsername(int userId) {
        String username = mContext.getResources().getString(R.string.status_unknown);
        int position = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mRegUserList.size() > 0) {
                try {
                    position = IntStream.range(0, mRegUserList.size())
                            .filter(index -> (mRegUserList.get(index).getUserId() == userId))
                            .findFirst()
                            .getAsInt();
                    username = mRegUserList.get(position).getName();
                    Log.d(TAG, "getUsername: index " + position + " userId: " + userId
                            + " username: " + username);
                } catch (NoSuchElementException e) {
                    Log.d(TAG, "getUsername: not found for userId " + userId);
                }
            }
        }
        return username;
    }

    public void onItemDeselected() {
        int previousPosition = selectedPosition;
        selectedPosition = -1;
        notifyItemChanged(previousPosition);
    }

    public void showDeliveryStatus() {
        JioTalkieChats selectedItem = mList.get(selectedPosition);
        if (selectedItem.getIs_group_chat()) {
            messageReceiptStatusList.clear();
            for (RegisteredUser user : mRegUserList) {
                messageReceiptStatusList.put(user.getUserId(), new MessageReceiptStatus(user.getUserId(), user.getName(), EnumConstant.MsgStatus.DeliveredToServer));
            }
            UpdateMessageReceiptStatus(selectedItem.getReceiver_delivered(), EnumConstant.MsgStatus.DeliveredToClient);
            UpdateMessageReceiptStatus(selectedItem.getReceiver_displayed(), EnumConstant.MsgStatus.Read);
            FragmentManager fragmentManager = ((DashboardActivity) mContext).getSupportFragmentManager();
            MessageDeliveryStatusDialogFragment dialogFragment = MessageDeliveryStatusDialogFragment.newInstance(messageReceiptStatusList);
            dialogFragment.show(fragmentManager, "MessageDeliveryStatusDialogFragment");
        }
    }

    public void removeItem(int position) {
        mList.remove(position); // Remove from data source
        notifyItemRemoved(position); // Notify RecyclerView
        notifyItemRangeChanged(position, mList.size());
    }


    private void UpdateMessageReceiptStatus(List<String> userIdList, EnumConstant.MsgStatus status) {
        String userIdString = userIdList.stream().collect(Collectors.joining(","));
        if (!TextUtils.isEmpty(userIdString)) {
            if (userIdString.trim().length() > 3) {
                userIdString = userIdString.substring(1, userIdString.length() - 1);
                if (!TextUtils.isEmpty(userIdString)) {
                    String[] stringArray = userIdString.split(",");
                    for (String str : stringArray) {
                        for (RegisteredUser regUsers : mRegUserList) {
                            if (regUsers.getUserId() == Integer.parseInt(str.trim())) {
                                messageReceiptStatusList.put(regUsers.getUserId(), new MessageReceiptStatus(regUsers.getUserId(), regUsers.getName(), status));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public interface ChatAdapterProvider {
        void onNavigateButtonClick(double latitude, double longitude, View view);

        void downloadMediaFile(int position, JioTalkieChats currMessage, boolean isLeft, boolean isSos, String mimeType, String messageType);

        void playFullScreen(String mediaPath, boolean isVideo);

        void onItemSelected(String msgId, int selectedPosition, boolean isSelfMessage, boolean isTextMessage, String textMessage);

        void onItemDeselected();

        void updateImageSize(String msgId, long imageSize);
    }
}
