package com.jio.jiotalkie.service;

import static com.application.customservice.CustomService.*;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.application.customservice.CustomService;
import com.application.customservice.Mumble;
import com.application.customservice.common.JioPttEnums;
import com.application.customservice.dataManagment.imodels.IChannelModel;
import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.dataManagment.models.ChannelModel;
import com.application.customservice.dataManagment.models.Server;
import com.application.customservice.dataManagment.models.UserModel;
import com.application.customservice.exception.JioTalkieException;
import com.application.customservice.wrapper.IJioPttObserver;
import com.application.customservice.wrapper.IMediaMessage;
import com.application.customservice.wrapper.JioPttObserver;
import com.application.customservice.wrapper.MediaMessage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.jio.jiotalkie.JioTalkieApplication;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.fragment.GroupChatFragment;
import com.jio.jiotalkie.fragment.PersonalChatFragment;
import com.jio.jiotalkie.interfaces.JioTalkieServiceInterface;
import com.jio.jiotalkie.service.ipc.JioPttBroadcastReceiver;
import com.application.customservice.wrapper.IJioPttSession;
import com.application.customservice.wrapper.ServiceWrapper;
import com.jio.jiotalkie.util.ADCInfoUtils;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.EnumConstant;

import java.util.ArrayList;
import java.util.List;

import com.application.customservice.wrapper.Constants;


public class JioTalkieService   implements JioTalkieServiceInterface,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = "JioTalkieService";
    private JioPttBroadcastReceiver mTalkReceiver;
    private boolean mSuppressNotifications;
    private JioTalkieSettings mJioTalkieSettings;
    private FusedLocationProviderClient mFusedLocationClinet;
    private Geocoder mGeoCoder;

    public static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
    public static final int RECONNECT_DELAY = 10000;
    private PowerManager.WakeLock mProximityLock;

    private boolean mPTTSoundEnabled;

    private boolean isTalkReceiverRegister = false;

    private JioTalkieNotification mMessageNotification;
    private JioTalkieConnectionNotification notification;

    PowerManager.WakeLock mWakeLock;

    private DashboardActivity boundActivity;

    private ServiceWrapper serviceWrapper;
    private Context mContext;

    private final JioPttObserver jioPttObserver = new JioPttObserver() {

        @Override
        public void onPttConnecting() {
//            notification = JioTalkieConnectionNotification.create(JioTalkieService.this,
//                    getString(R.string.jioTalkieConnecting),
//                    getString(R.string.connecting));
//            notification.show();
        }

        @Override
        public void onPttConnectionActive() {
//            if (notification != null) {
//                notification.setCustomTicker(getString(R.string.jioTalkieConnected));
//                notification.setCustomContentText(getString(R.string.connected));
//                notification.show();
//            }
        }

        @Override
        public void onPttConnectionLost(JioTalkieException e) {
            Log.d(TAG,"onDisconnected trying to stop Foreground Service mSuppressNotifications = "+mSuppressNotifications);
//            if (!mSuppressNotifications) {
//                if(notification!=null) {
//                    notification.hide();
//                    notification = null;
//                }
//            }
            onConnectionDisconnected(e);
        }

        @Override
        public void onConnectionSynchronizedUpdate(boolean isSynchronized) {
            if(isSynchronized)
                onConnectionSynchronized();


        }

        @Override
        public void onPttUserReceived(IUserModel user) {
            Log.d(TAG, "onUserConnected: called");

            if (user.getUserHashTexture() != null && user.getUserTexture() == null) {
                serviceWrapper.requestAvatarForPttUser(user.getSessionID());
            }
        }

        @Override
        public void onPttUserStateChange(IUserModel user) {
            Log.d(TAG, "onUserStateUpdated: called");
            // Handled HumlaDisconnectedException: Caller attempted to use the protocol while disconnected.
            if (user == null || serviceWrapper.getPttConnectionState() != JioPttEnums.connectionState.SERVER_SYNCHRONIZED) {
                return;
            }
            int selfSession = serviceWrapper.fetchSessionId();
            IChannelModel sessionChannel = serviceWrapper.getJioPttSession().fetchSessionPttChannel();
            if (user.getSOS() && (user.getUserChannel().getChannelID() == sessionChannel.getChannelID())) {
                if (user.getSessionID() != selfSession) { // Receiver SOS only
                    MediaMessage sosMessage = new MediaMessage(serviceWrapper.fetchSessionId(), user.getUserName(), new ArrayList<ChannelModel>(0),
                            new ArrayList<ChannelModel>(0), new ArrayList<UserModel>(0), "SOS Received", Mumble.TextMessage.MsgType.VoiceMessageType, "", "", true);
                    PowerManager mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                    mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp::JioTalkieDispatcherWakeTag");
                    try {
                        mWakeLock.acquire(10*1000L /*10 seconds*/);
                        CommonUtils.flashLightAndVibrate(mContext);
                    }catch(Exception e){
                        Log.d(TAG,"Flash light and Vibration code exception "+Log.getStackTraceString(e));
                    }finally {
                        new Handler().postDelayed(() -> {
                            if (mWakeLock!=null && mWakeLock.isHeld()) {
                                mWakeLock.release();
                            }
                        },12000);

                    }
                    if(!JioTalkieApplication.isAppInForeground()) {
                        mMessageNotification.show(sosMessage);
                    }
                }
            }
            if (user.getSessionID() == selfSession) {
                mJioTalkieSettings.setAudioMuteAndDeafen(user.isSelfMute(), user.isSelfDeaf()); // Update settings mute/deafen state
            }
            if (user.getUserHashTexture() != null && user.getUserTexture() == null) {
                serviceWrapper.requestAvatarForPttUser(user.getSessionID());
            }

        }

        @Override
        public void onPttMessageReceived(IMediaMessage message) {
            String msgType = String.valueOf(message.getMessageType());
            getJioPttSession().updateMsgStatus(message.getMessageId(),Mumble.MessageDelivery.MsgStatus.DeliveredToClient);
            List<ChannelModel> channelList = message.getRecipientChannels();
            IChannelModel sessionChannel = serviceWrapper.getJioPttSession().fetchSessionPttChannel();
            if(!channelList.isEmpty()) { //For channel chat messages
                for (ChannelModel channel : channelList) {
                    if (channel.getChannelID() == sessionChannel.getChannelID()) {
                        if(!(mJioTalkieSettings.isPowerSaverEnable() && CommonUtils.isSleepModeActive(mContext))) {
                            if(!JioTalkieApplication.isAppInForeground() || !isGroupChatVisible()) {
                                mMessageNotification.show(message);
                            }
                        }
                        break;
                    }
                }
            }else{  // for 1-1 chat messages
                List<UserModel> mTargetUsers = message.getRecipientUsers();
                for (UserModel user : mTargetUsers) {
                    if(user.getUserChannel().getChannelID() == sessionChannel.getChannelID()){
                        if(!JioTalkieApplication.isAppInForeground() || !isPersonalChatVisible(message.getOriginatorName())) {
                            mMessageNotification.show(message);
                        }
                    }
                }
            }
            Log.d(TAG, "onMessageLogged:messagenotification"+mMessageNotification);
            if (msgType.equals(EnumConstant.ServerMessageType.TextMessageType.toString())){
                ADCInfoUtils.calculateTextSize(message.getMessageContent(),false,-1,-1,"",-1);
            }
        }

        @Override
        public void onUserTalkStateUpdated(IUserModel user) {
            Log.d(TAG, "onUserTalkStateUpdated: called");
            int selfSession = -1;
            try {
                selfSession = serviceWrapper.fetchSessionId();
            } catch (IllegalStateException e) {
                Log.d(TAG, "exception in onUserTalkStateUpdated: " + e);
            }

            if (serviceWrapper.isConnectionEstablished() &&
                    user.getSessionID() == selfSession &&
                    // ToApp -> It is always PUSH TO TALK.
                    /* serviceWrapper.getTransmitMode() == Constants.TRANSMIT_PUSH_TO_TALK  && */
                    user.getUserTalkingState() == JioPttEnums.TalkingState.TALKING &&
                    mPTTSoundEnabled) {
                AudioManager audioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1);
            }
        }

    };

    public boolean isGroupChatVisible(){
        if(boundActivity!=null && (boundActivity.getVisibleFragment() instanceof GroupChatFragment))
            return true;
        return false;
    }

    public boolean isPersonalChatVisible(String senderName){
        if(boundActivity!=null && (boundActivity.getVisibleFragment() instanceof PersonalChatFragment)) {
            String personalChatTargetUser = ((PersonalChatFragment) boundActivity.getVisibleFragment()).getTargetUserName();
            if(senderName.equals(personalChatTargetUser))
                return true;
            else
                return false;
        }
        return false;
    }



    public  JioTalkieService(Context context){
        mContext = context;
        serviceWrapper = new ServiceWrapper(mContext);
        Log.d(TAG, "onCreate: called");
        serviceWrapper.addObserver(jioPttObserver);
        mFusedLocationClinet = LocationServices.getFusedLocationProviderClient(mContext);
        mGeoCoder = new Geocoder(mContext);

        mTalkReceiver = new JioPttBroadcastReceiver(serviceWrapper);
        mJioTalkieSettings = JioTalkieSettings.getInstance(mContext);
        mPTTSoundEnabled = mJioTalkieSettings.isSpeakerAlertEnabled();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.registerOnSharedPreferenceChangeListener(this);
        mMessageNotification = new JioTalkieNotification(mContext);

    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        Bundle changedExtras = new Bundle();
        boolean requiresReconnect = false;
        switch (key) {
            case JioTalkieSettings.PREFERENCE_AUDIO_MODE:
                int inputMethod = mJioTalkieSettings.getJioTalkieAudioMode();
                //TODO: All extras Key values need to be stored in seperate file
                changedExtras.putInt(CustomService.EXTRAS_TRANSMIT_MODE_TYPE, inputMethod);
                break;
            case JioTalkieSettings.PREFERENCE_HANDHELD_MODE:
                setProximitySensorOn(serviceWrapper.isConnectionEstablished() && mJioTalkieSettings.isHandheldMode());
                changedExtras.putInt(CustomService.EXTRAS_AUDIO_STREAM_TYPE, mJioTalkieSettings.isHandheldMode() ?
                        AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC);
                break;
            case JioTalkieSettings.PREF_SENSITIVITY_LEVEL:
                changedExtras.putFloat(CustomService.EXTRAS_DETECTION_THRESHOLD_VALUE,
                        mJioTalkieSettings.getSensitivityLevel());
                break;
            case JioTalkieSettings.PREFERENCE_SOUND_AMPLIFICATION:
                changedExtras.putFloat(EXTRAS_AMPLITUDE_BOOST_VALUE,
                        mJioTalkieSettings.getSoundAmplitudeBoost());
                break;
            case JioTalkieSettings.PREFERENCE_SIMPLEX_MODE:
                changedExtras.putBoolean(EXTRAS_HALF_DUPLEX_MODE, mJioTalkieSettings.isSimplexMode());
                break;
            case JioTalkieSettings.PREFERENCE_AUDIO_PROCESSING_ENABLED:
                changedExtras.putBoolean(EXTRAS_ENABLE_PREPROCESSOR_FLAG,
                        mJioTalkieSettings.isAudioProcessingEnabled());
                break;
            case JioTalkieSettings.PREFERENCE_SPEAKER_ALERT:
                mPTTSoundEnabled = mJioTalkieSettings.isSpeakerAlertEnabled();
                break;
            case JioTalkieSettings.PREFERENCE_AUDIO_BIT_QUALITY:
                changedExtras.putInt(EXTRAS_INPUT_QUALITY_LEVEL, mJioTalkieSettings.getAudioBitQuality());
                break;
            case JioTalkieSettings.PREFERENCE_AUDIO_SAMPLE_RATE:
                changedExtras.putInt(EXTRAS_INPUT_FREQUENCY, mJioTalkieSettings.getAudioSampleRate());
                break;
            case JioTalkieSettings.PREFERENCE_PACKET_INTERVAL:
                changedExtras.putInt(EXTRAS_FRAMES_PER_PACKET_COUNT, mJioTalkieSettings.getPacketFrames());
                break;
            case JioTalkieSettings.PREFERENCE_CERTIFICATE_IDENTIFIER:
            case JioTalkieSettings.PREFERENCE_USE_TCP_PROTOCOL:
            case JioTalkieSettings.PREFERENCE_ENABLE_TOR:
            case JioTalkieSettings.PREFERENCE_DISABLE_CODEC:
                // These are settings we flag as 'requiring reconnect'.
                requiresReconnect = true;
                break;
        }
        if (!changedExtras.isEmpty()) {
//            try {
//                // Reconfigure the service appropriately.
//
//                //TODO:- Connect again , as we are not exposing configureExtras() function.
//                requiresReconnect |= configureExtras(changedExtras);
//
//            } catch (AudioException e) {
//                e.printStackTrace();
//            }
        }

        if (requiresReconnect && serviceWrapper.isConnectionEstablished()) {
            Toast.makeText(mContext, R.string.settings_reconnect_required, Toast.LENGTH_LONG).show();
        }
    }

    private void setProximitySensorOn(boolean on) {
        if(on) {
            PowerManager pm = (PowerManager) mContext.getSystemService(POWER_SERVICE);
            mProximityLock = pm.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "Mumla:Proximity");
            mProximityLock.acquire();
        } else {
            if(mProximityLock != null) mProximityLock.release();
            mProximityLock = null;
        }
    }

    /* Below APIs are defined JioTalkieServiceInterface(Which extends jioPttService , which provides jioPttSessionInterface  through get call).
      1. Earlier they were Overriden by HumlaService. And It was exposed through JioTalkieServiceInterface.
      2. Now we are Overriding here itself and calling function in service Wrapper.
      3. Also added new APIs .Please check  */
    @Override
    public void startPttService(Bundle extras){
        if (extras != null) {
            serviceWrapper.connectToPttServer(extras);
        }
    }

    @Override
    public void  bindToPttServer(){
        serviceWrapper.bindToPttServer();
    }

    @Override
    public void unBindToPttServer(){
        serviceWrapper.unBindToPttServer();
    }

    @Override
    public boolean isBoundToPttServer() {
        return serviceWrapper.isBoundToPttServer();
    }

    @Override
    public void stopPttService(){
        Log.d(TAG, "onDestroy: called");
//        if(notification!=null) {
//            notification.hide();
//            notification = null;
//        }
        try {
            if (isTalkReceiverRegister) {
                mContext.unregisterReceiver(mTalkReceiver);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        serviceWrapper.removeObserver(jioPttObserver);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.unregisterOnSharedPreferenceChangeListener(this);

        serviceWrapper.disconnectFromServer();
    }

    @Override
    public void clearChatNotifications() {
        mMessageNotification.dismiss();
    }

    @Override
    public JioTalkieNotification getJioTalkieNotification() {
        return mMessageNotification;
    }
    @Override
    public boolean isErrorShown() {
        return false;
    }

    @Override
    public void onTalkKeyDown() {
        if(serviceWrapper.isConnectionEstablished()) {
            if (!mJioTalkieSettings.getToggleMode() && !serviceWrapper.isPttUserTalking()) {
                Log.d(TAG, "onTalkKeyDown: called setTalkingState to true");

                serviceWrapper.updatePttUserTalkingState(true); // Start talking
                Log.d(TAG, "onTalkKeyDown: sending audio SessionId = "+serviceWrapper.fetchSessionId());
            }
        }
    }

    @Override
    public void onTalkKeyUp() {
        if(serviceWrapper.isConnectionEstablished()) {
            if (mJioTalkieSettings.getToggleMode()) {
                serviceWrapper.updatePttUserTalkingState(!serviceWrapper.isPttUserTalking()); // Toggle talk state
            } else if (serviceWrapper.isPttUserTalking()) {
                Log.d(TAG, "onTalkKeyUp: called setTalkingState to false");
                serviceWrapper.updatePttUserTalkingState(false); // Stop talking
            }
        }
    }

    @Override
    public void setSuppressNotifications(boolean suppressNotifications) {
        mSuppressNotifications = suppressNotifications;
    }

    @Override
    public boolean isConnectionSynchronized() {
        return serviceWrapper.isSynchronized();
    }

    @Override
    public void setBoundActivity(Activity activity) {
        this.boundActivity = (DashboardActivity) activity;
    }
    @Override
    public void muteStreamAudio() {
        if(boundActivity!=null)
            boundActivity.muteStreamAudioVolume();
    }
    @Override
    public void resetStreamAudio(){
        if(boundActivity!=null){
            boundActivity.resetStreamAudioVolume();
        }
    }


    @Override
    public void addObserver(IJioPttObserver observer) {
        serviceWrapper.addObserver(observer);
    }

    @Override
    public void removeObserver(IJioPttObserver observer) {
        serviceWrapper.removeObserver(observer);
    }

    @Override
    public boolean isPttConnectionActive() {
        return  serviceWrapper.isPttConnectionActive();
    }

    @Override
    public void closePttConnection() {
        serviceWrapper.closePttConnection();
    }

    @Override
    public JioPttEnums.connectionState getPttConnectionState() {
        return serviceWrapper.getPttConnectionState();
    }

    @Override
    public JioTalkieException getPttConnectionError() {
        return serviceWrapper.getPttConnectionError();
    }

    @Override
    public boolean isPttReconnecting() {
        return serviceWrapper.isPttReconnecting();
    }

    @Override
    public void cancelPttReconnect() {
        serviceWrapper.cancelPttReconnect();
    }

    @Override
    public Server getConnectingServer() {
       return serviceWrapper.getConnectingServer();
    }

    @Override
    public IJioPttSession getJioPttSession() {
        return serviceWrapper.getJioPttSession();
    }



    // Called after callback , instead of overriding
    public void onConnectionSynchronized() {
        // Restore mute/deafen state
        if(mJioTalkieSettings.isMicrophoneMuted() || mJioTalkieSettings.isAudioDisabled()) {
            serviceWrapper.updateSelfMuteDeafState(mJioTalkieSettings.isMicrophoneMuted(), mJioTalkieSettings.isAudioDisabled());
        }

        if (mJioTalkieSettings.isHandheldMode()) {
            setProximitySensorOn(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            isTalkReceiverRegister = true;
            mContext.registerReceiver(mTalkReceiver, new IntentFilter(JioPttBroadcastReceiver.ACTION_TRIGGER_COMMUNICATION), RECEIVER_EXPORTED);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isTalkReceiverRegister = true;
                mContext.registerReceiver(mTalkReceiver, new IntentFilter(JioPttBroadcastReceiver.ACTION_TRIGGER_COMMUNICATION), Context.RECEIVER_NOT_EXPORTED);
            }
        }

        if ((ContextCompat.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            mFusedLocationClinet.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        Log.i(TAG, String.valueOf(location.getLatitude()));
                        Log.i(TAG, String.valueOf(location.getLongitude()));
                        final Mumble.UserState.Builder builder = Mumble.UserState.newBuilder();

                        builder.setUserLocation(location.getLatitude() + ";" + location.getLongitude());
                        serviceWrapper.sendPttRequest(builder.build(), JioPttEnums.TCPProtoMsgType.MsgUserState.ordinal());
                        Log.v(TAG, "Sending location in userstate " + builder.getUserLocation());
                    }
                }
            });
        }
    }

    // Called after callback , instead of overriding
    public void onConnectionDisconnected(JioTalkieException e) {
        try {
            if (isTalkReceiverRegister && mTalkReceiver!=null) {
                mContext.unregisterReceiver(mTalkReceiver);
            }
        } catch (IllegalArgumentException ile) {
            ile.printStackTrace();
        }
    }


    public static class JioTalkieServiceBinder extends Binder {
        private final JioTalkieService mService;

        private JioTalkieServiceBinder(JioTalkieService service) {
            Log.d(TAG, "JioTalkieServiceBinder: called");
            mService = service;
        }

        public JioTalkieServiceInterface getService() {
            Log.d(TAG, "JioTalkieServiceBinder >> getService: called");
            return mService;
        }
    }




    // Below  APIs are moved to service wrapper.
    public MediaMessage sendUserTextMessage(int session, String message, Mumble.TextMessage.MsgType msgType, String msgId, String mimeType, boolean isSOS) {
        return sendUserTextMessage(session, Constants.OFFLINE_USER_SESSION_ID, null, message, msgType, msgId, mimeType, isSOS);
    }
    public MediaMessage sendUserTextMessage(int session, int userId, String userName, String message, Mumble.TextMessage.MsgType msgType, String msgId, String mimeType, boolean isSOS) {
        MediaMessage msg = null;
//        msg = super.sendUserTextMessage(session,userId,userName,message,msgType,msgId,mimeType,isSOS);
        return msg;
    }
    public MediaMessage sendChannelTextMessage(int channel, String message, boolean tree, Mumble.TextMessage.MsgType msgType, String msgId, String mimeType, boolean isSOS) {
        MediaMessage msg = null;
//        msg = super.sendChannelTextMessage(channel, message, tree,msgType,msgId,mimeType,isSOS);
        return msg;
    }
    public void SendDeviceInfoToServer(String networkStrength, String batteryStrength, String location){
    }
    public void requestHistoricalDataList(int id ) {
    }
    public void updateUserTalkingState(boolean isTalking, String callId) {
    }
    public void updatePersonalChatUserState(boolean isTalking,int targetSessionId, int targetUserId, String callId) {
    }
    public void  updateMsgStatus(String id , Mumble.MessageDelivery.MsgStatus msgStatus){
    }

}
