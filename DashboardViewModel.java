package com.jio.jiotalkie.viewmodel;

import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.application.customservice.Mumble;
import com.application.customservice.common.model.RegUserState;
import com.application.customservice.dataManagment.imodels.IChannelModel;
import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.dataManagment.models.ChannelModel;
import com.application.customservice.dataManagment.models.Server;
import com.application.customservice.dataManagment.models.UserModel;
import com.application.customservice.exception.JioTalkieException;
import com.application.customservice.wrapper.IMediaMessage;
import com.application.customservice.wrapper.JioPttObserver;
import com.application.customservice.wrapper.MediaMessage;
import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.dataclass.AudioDownloadState;
import com.jio.jiotalkie.dataclass.CurrentSpeakerState;
import com.jio.jiotalkie.dataclass.DocumentDownloadState;
import com.jio.jiotalkie.dataclass.PttCallUserState;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.dataclass.SOSDataState;
import com.jio.jiotalkie.dataclass.SubChannelCreationData;
import com.jio.jiotalkie.dataclass.UserConnectionState;
import com.jio.jiotalkie.dataclass.UserTalkState;
import com.jio.jiotalkie.dataclass.VideoDownloadState;
import com.jio.jiotalkie.db.JioTalkieDatabaseRepository;
import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.interfaces.JioTalkieServiceInterface;
import com.jio.jiotalkie.model.FilterChatMessageList;
import com.jio.jiotalkie.model.JioTalkieCertificates;
import com.jio.jiotalkie.model.JioTalkieChats;
import com.jio.jiotalkie.model.JioTalkieServer;
import com.jio.jiotalkie.model.PaginatedGroupChat;
import com.jio.jiotalkie.model.PaginatedPersonalChat;
import com.jio.jiotalkie.model.api.ApkResponseModel;
import com.jio.jiotalkie.model.api.AuthtokenVerifyModel;
import com.jio.jiotalkie.model.api.BuildInfo;
import com.jio.jiotalkie.model.api.Info;
import com.jio.jiotalkie.model.api.MediaUploadModel;
import com.jio.jiotalkie.model.api.MediaUploadResponse;
import com.jio.jiotalkie.model.api.MessageListResponseModel;
import com.jio.jiotalkie.model.api.MessageRequestModel;
import com.jio.jiotalkie.model.api.MessageResponseModel;
import com.jio.jiotalkie.model.api.OtpResponseModel;
import com.jio.jiotalkie.model.api.Platform;
import com.jio.jiotalkie.model.api.UserSniResponseModel;
import com.jio.jiotalkie.model.api.ZLAResponseModel;
import com.jio.jiotalkie.network.CustomServerConnectTask;
import com.jio.jiotalkie.network.RESTApiManager;
import com.jio.jiotalkie.network.RetrofitClient;
import com.jio.jiotalkie.util.ADCInfoUtils;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.DateUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.util.ServerConstant;
import com.yalantis.ucrop.UCropFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardViewModel extends AndroidViewModel implements DefaultLifecycleObserver {

    private static final String TAG = DashboardViewModel.class.getSimpleName();

    private static final long RECORDING_STOP_DELAY = 300;
    private static final long OFFSET_IN_GMT = 19800000L;
    private static final int WAV_HEADER_SIZE = 44;
    private static final int ENCODING_PCM_16BIT_BITS_PER_SAMPLE = 16;
    private static final int SAMPLE_RATE = 48000;
    public JioTalkieDatabaseRepository mDatabase;
    private JioTalkieSettings mJioTalkieSettings;
    private int mCurrentUserChannelId = -1;
    private byte[] mCertificate = null;
    private final Handler mAudioHandler = new Handler();
    private UserModel current_sos_user;
    private String adcMsiSdn = "";
    private int mTargetId = EnumConstant.DEFAULT_TARGET_USER_ID;
    private JioTalkieServiceInterface mJioTalkieService = null;
    public List<RegisteredUser> RegUserList = new ArrayList<>();
    private List<RegisteredUser> mMyChannelRegList = new ArrayList<>();
    private int mSelfUserId;
    private long startTime = 0;
    private long endTime = 0;
    private String msgType = "";
    private MediaMessage audioMessage;
    private String adcMessageType = "";
    private String adcMessageCategory = "";
    private int mAudioRecordFileLen = 0;
    private RandomAccessFile mAudioRecordFileHandle;
    private String mAudioRecordFilePath;
    private int mChannelId = -1;
    private int mUserId = -1;
    private String mTargetUserName;
    private boolean isAudioRecording = false;
    private String mAndroidId;


    private final MutableLiveData<EnumConstant.connectionState> mConnectionState = new MutableLiveData<>();
    private final MutableLiveData<UserConnectionState> mUserState = new MutableLiveData<>();
    private final MutableLiveData<SOSDataState> mSOSUserState = new MutableLiveData<>();
    private final MutableLiveData<UserTalkState> mUserTalkState = new MutableLiveData<>();
    private final MutableLiveData<JioTalkieChats> mChatMessageState = new MutableLiveData<>();
    private final MutableLiveData<EnumConstant.LoginState> mLoginState = new MutableLiveData<>();
    private final MutableLiveData<List<RegisteredUser>> mRegisterUserList = new MutableLiveData<>();
    private final MutableLiveData<List<Mumble.UserList.User>> mAllRegisterUsers = new MutableLiveData<>();
    private final MutableLiveData<SubChannelCreationData> subChannelCreationData = new MutableLiveData<>();
    private final MutableLiveData<UCropFragment.UCropResult> mImageEditResult = new MutableLiveData<>();
    private final MutableLiveData<CurrentSpeakerState> mCurrentSpeakerState = new MutableLiveData<>();
    private final MutableLiveData<MediaUploadResponse> mMediaUploadState = new MutableLiveData<>();
    private final MutableLiveData<ApkResponseModel> mUpdateApkResponseModel = new MutableLiveData<>();
    private final MutableLiveData<ApkResponseModel> mDownloadApkResponseModel = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mConnectionSynchronized = new MutableLiveData<>();
    private final MutableLiveData<Boolean> oneToOneTargetBusy = new MutableLiveData<>();
    private final MutableLiveData<PttCallUserState> mDisableGroupPttCall = new MutableLiveData<>();
    private final MutableLiveData<List<Mumble.HistoricalDataList.HistoricalData>> mHistoricalDataList = new MutableLiveData<List<Mumble.HistoricalDataList.HistoricalData>>();
    private final MutableLiveData<Mumble.MessageDelivery> mMessageDeliveryStatus = new MutableLiveData<>();
    private final MutableLiveData<AudioDownloadState> mDownloadAudioState = new MutableLiveData<>();
    private final MutableLiveData<DocumentDownloadState> mDownloadDocumentState = new MutableLiveData<>();
    private final MutableLiveData<VideoDownloadState> mDownloadVideoState = new MutableLiveData<>();
    private final MutableLiveData<Mumble.ACL> mACLReports = new MutableLiveData<>();
    private final MutableLiveData<IUserModel> mUserStateUpdate = new MutableLiveData<>();
    private final MutableLiveData<FilterChatMessageList> mFilteredChatFromServer = new MutableLiveData<>();
    private final MutableLiveData<PaginatedGroupChat> mPaginatedGroupChats = new MutableLiveData<>();
    private final MutableLiveData<PaginatedPersonalChat> mPaginatedPersonalChats = new MutableLiveData<>();
    private final MutableLiveData<IChannelModel> mChannelState = new MutableLiveData<>();
    private final MutableLiveData<RegUserState> mRegUserStateChanged = new MutableLiveData<>();



    public JioPttObserver jioPttObserver = new JioPttObserver() {

        @Override
        public void onRegisteredUserListChanged(List<Mumble.UserList.User> registeredUserList) {
            Log.d(TAG, "onRegisteredUserListChanged: called registerUser size =" + registeredUserList.size());
            mAllRegisterUsers.setValue(registeredUserList);
            int selfChannelId = -1;
            if (getJioTalkieService().isBoundToPttServer() && getJioTalkieService().isPttConnectionActive() &&
                    getJioTalkieService().getJioPttSession() != null && getJioTalkieService().getJioPttSession().fetchSessionPttChannel() != null) {
                // My Channel ID
                selfChannelId = getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID();
            }
            Log.d(TAG, "self channel Id: " + selfChannelId);

            // filter out my channel registered list
            mMyChannelRegList.clear();
            for (Mumble.UserList.User user : registeredUserList) {
                //Log.d(TAG, "onRegisteredUserListChanged : name: " + user.getUserName()
                //        + " channel: " + user.getLastChannel());
                if (selfChannelId == user.getLastChannel()) {
                    RegisteredUser regUser = new RegisteredUser(user);
                    mMyChannelRegList.add(regUser);
                }
            }
            mRegisterUserList.setValue(mMyChannelRegList);
        }

        @Override
        public void joinChannelByDeeplink(int sessionId, int channelId) {
            Log.d(TAG, " JioPttObserver joinChannelByDeeplink() channelId = " + channelId);
            mCurrentUserChannelId = channelId;
        }

        @Override
        public void onRegisteredHistoricalDataListChanged(List<Mumble.HistoricalDataList.HistoricalData> historicalDataList) {
            Log.d(TAG, "onRegisteredHistoricalDataListChanged: called historicalDataList size =" + historicalDataList.size());
            mHistoricalDataList.setValue(historicalDataList);
        }
        @Override
        public void onPttConnectionActive() {
            Log.d(TAG, " JioPttObserver onConnected() called");
            mConnectionState.setValue(EnumConstant.connectionState.SERVER_CONNECTED);

            if (getJioTalkieService().isBoundToPttServer() && getJioTalkieService().isPttConnectionActive() &&
                    getJioTalkieService().isConnectionSynchronized() && getJioTalkieService().getJioPttSession() != null) {
                int sessionId=getJioTalkieService().getJioPttSession().fetchSessionId();
                Log.d(TAG, " JioPttObserver onConnected() called and sessionID " +sessionId);
                mUserId = getJioTalkieService().getJioPttSession().fetchPttUser(sessionId).getUserID();
                mChannelId = getJioTalkieService().getJioPttSession().fetchPttUser(sessionId).getUserChannel().getChannelID();
                setUserId(mUserId);
                setChannelId(mChannelId);
                updateApkCheckTimer();
            }
            ADCInfoUtils.loginInfo(1,0,"",mUserId,mChannelId,adcMsiSdn);
        }

        @Override
        public void onPttConnecting() {
            Log.d(TAG, " JioPttObserver onConnecting() called");
            mConnectionState.setValue(EnumConstant.connectionState.SERVER_CONNECTING);
        }

        @Override
        public void onPttConnectionLost(JioTalkieException e) {
            Log.d(TAG, " JioPttObserver onDisconnected() called");
            mConnectionState.setValue(EnumConstant.connectionState.SERVER_DISCONNECTED);
            ADCInfoUtils.serverConnectionInfo(0,5,"server connection error ",mUserId,mChannelId);
        }

        @Override
        public void onTLSHandshakeFailed() {
            Log.d(TAG, " JioPttObserver onTLSHandshakeFailed() called");
            final Server lastServer = getJioTalkieService().getConnectingServer();

            // Below Changes Handled from Lib side.
//            if (chain.length == 0) return;
//
//            try {
//                final X509Certificate x509 = chain[0];
//                String alias = lastServer.getHost();
//                KeyStore trustStore = JioTalkieStore.getTrustStore(DashboardViewModel.this.getApplication());
//                trustStore.setCertificateEntry(alias, x509);
//                JioTalkieStore.saveTrustStore(DashboardViewModel.this.getApplication(), trustStore);
//                callServerConnectionApi(lastServer);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }

        @Override
        public void onPermissionRefused(String reason) {
            Log.d(TAG, " JioPttObserver onPermissionDenied() called");
            mConnectionState.setValue(EnumConstant.connectionState.PERMISSION_DENY);
        }

        @Override
        public void onPttUserReceived(IUserModel user) {
            Log.d(TAG, "onUserConnected: called user name" + user.getUserName());
            Log.d(TAG, "onUserConnected: SOS user name" + user.getSOS());
            mUserState.setValue(new UserConnectionState(EnumConstant.userState.USER_CONNECTED, user));
        }

        @Override
        public void onPttUserEnteredChannel(IUserModel user, IChannelModel newChannel, IChannelModel oldChannel) {
            Log.d(TAG, "onUserJoinedChannel: called user name = " + user.getUserName() +" "+newChannel.getChannelName() );
            mUserState.setValue(new UserConnectionState(EnumConstant.userState.USER_JOINED, user));
        }

        @Override
        public void onPttUserRemoved(IUserModel user, String reason) {
            Log.d(TAG, "onUserRemoved: called user name =" + user.getUserName());
            mUserState.setValue(new UserConnectionState(EnumConstant.userState.USER_REMOVED, user));
        }

        @Override
        public void currentOneToOneBusy(boolean isOneToOneBusy){
            Log.i(TAG ,"observeOneToOneBusy: currentOneToOneBusy: "+isOneToOneBusy);
            oneToOneTargetBusy.setValue(isOneToOneBusy);
        }
        @Override
        public void onPttSubChannelCreationResponse(IChannelModel channel) {
            subChannelCreationData.setValue(new SubChannelCreationData(true , channel , null));
        }
        @Override
        public void onUserTalkStateUpdated(IUserModel user) {
//            if (getJioTalkieService().isBoundToPttServer() || !getJioTalkieService().isPttConnectionActive()) {
//                return;
//            }
            int selfSession;
            try {
                selfSession = getJioTalkieService().getJioPttSession().fetchSessionId();
            } catch (IllegalStateException e) {
                Log.d(TAG, "exception in onUserStateUpdated: " + e);
                return;
            }

            if (user == null) {
                return;
            }
            Log.d(TAG, "onUserTalkStateUpdated: before if user value user.getSosvalue " + user.getSOS());
            if (user.getSessionID() == selfSession) {
                switch (user.getUserTalkingState()) {
                    case TALKING:
                    case SHOUTING:
                    case WHISPERING:
                        Log.d(TAG, "onUserTalkStateUpdated: self user: " + user.getUserName() + " TALKING State");
                        mUserTalkState.setValue(new UserTalkState(EnumConstant.userTalkState.TALKING, "", true));
                        break;
                    case PASSIVE:
                        Log.d(TAG, "onUserTalkStateUpdated: self user: " + user.getUserName() + " PASSIVE state");
                        mUserTalkState.setValue(new UserTalkState(EnumConstant.userTalkState.PASSIVE, "", true));
                        break;
                }
            } else {
                switch (user.getUserTalkingState()) {
                    case TALKING:
                    case SHOUTING:
                    case WHISPERING:
                        Log.d(TAG, "onUserTalkStateUpdated: other user: " + user.getUserName() + " TALKING State");
                        if(!user.getSOS()) {
                            mUserTalkState.setValue(new UserTalkState(EnumConstant.userTalkState.TALKING, user.getUserName(), false));
                        }
                        break;
                    case PASSIVE:
                        Log.d(TAG, "onUserTalkStateUpdated: other user: " + user.getUserName() + " PASSIVE state");
                        if(!user.getSOS()) {
                            mUserTalkState.setValue(new UserTalkState(EnumConstant.userTalkState.PASSIVE, "", false));
                        }
                        break;
                }
            }
        }

        @Override
        public void onRegisteredUserStateChanged(RegUserState regUserState) {
            super.onRegisteredUserStateChanged(regUserState);
            mRegUserStateChanged.setValue(regUserState);
        }

        @Override
        public void onPttUserStateChange(IUserModel user) {
            Log.d(TAG, "onUserStateUpdated: username: " + user.getUserName()
                    + " SOS: " + user.getSOS());
            if (user != null && user.getSOS() == true) {
                if (current_sos_user.getUserID() == 0) {
                    current_sos_user.setUserID(user.getUserID());
                } else if (current_sos_user.getUserID() == user.getUserID()) {
                    return;
                }
            } else if (current_sos_user.getUserID() == user.getUserID()) {
                current_sos_user.setUserID(0);
            }
            if (!getJioTalkieService().isBoundToPttServer() || !getJioTalkieService().isPttConnectionActive()) {
                return;
            }

            mUserStateUpdate.setValue(user);
            int selfSession;
            try {
                selfSession = getJioTalkieService().getJioPttSession().fetchSessionId();
            } catch (IllegalStateException e) {
                Log.d(TAG, "exception in onUserStateUpdated: " + e);
                return;
            }

            boolean isSelfUser = false;
            if (user != null && user.getSessionID() == selfSession) {
                //Log.d(TAG, "onUserStateUpdated: called self user");
                //configureInput();
                mSelfUserId = user.getUserID();
                isSelfUser = true;
            }
            String msgId = "";
            boolean isSOS = user.getSOS();
            if (mCurrentSpeakerState.getValue() != null) {
                if (mCurrentSpeakerState.getValue().getCurrentSpeakerId() != 0
                        && mCurrentSpeakerState.getValue().getCurrentSpeakerId() == user.getUserID()) {
                    if (user.isUserTalking())
                        msgId = mCurrentSpeakerState.getValue().getCallId();
                    handleRecordingState(user, isSelfUser, true, msgId);
                    Log.d(TAG,"DashboardViewModel group call received");
                    if(!isSelfUser && !isSOS && CommonUtils.isSleepModeActive(getApplication()) && getSettings().isPowerSaverEnable()) {
                        getJioTalkieService().muteStreamAudio();
                    }
                } else if (mCurrentSpeakerState.getValue().getCurrentSpeakerOneToOne() != 0
                        && mCurrentSpeakerState.getValue().getCurrentListenerOneToOne() != 0
                        && (mCurrentSpeakerState.getValue().getCurrentSpeakerOneToOne() == user.getUserID()
                        || mCurrentSpeakerState.getValue().getCurrentListenerOneToOne() == user.getUserID())) {

                    boolean isSelfPtt = mCurrentSpeakerState.getValue().getCurrentSpeakerOneToOne() == mSelfUserId;
                    if (user.isUserTalking())
                        msgId = mCurrentSpeakerState.getValue().getCallId();
                    handleRecordingState(user, isSelfPtt, false, msgId);
                    if(!isSelfUser && CommonUtils.isSleepModeActive(getApplication()) && getSettings().isPowerSaverEnable()) {
                        getJioTalkieService().resetStreamAudio();
                    }
                }
            }

            // If SOS Speaker is true, set the username in mSOSUserState.
            // If SOS Speaker is false, check the current SOS Speaker name in mSOSUserState,
            // if same then set as false, else ignore.

            if (user != null && user.getSOS() && mCurrentSpeakerState.getValue() != null && mCurrentSpeakerState.getValue().isSelfCurrentSpeakerSOS()) {
                if (user.getSessionID() == selfSession) {
                    Log.d(TAG, " onUserStateUpdated: setting SOS Sender: " + user.getUserName());
                    mSOSUserState.setValue(new SOSDataState(EnumConstant.sosState.SENDER,
                            user.getUserName(), user.getBatteryPercentage(), user.getLocation(), user.getSOS(), user.getSignalStrength(), user.getUserID()));
                } else {
                    Log.d(TAG, "  onUserStateUpdated: setting SOS Received from: " + user.getUserName());
                    mSOSUserState.setValue(new SOSDataState(EnumConstant.sosState.RECEIVER,
                            user.getUserName(), user.getBatteryPercentage(), user.getLocation(), user.getSOS(), user.getSignalStrength(), user.getUserID()));
                }
            } else {
                assert user != null;
                if (mSOSUserState.getValue() != null) {
                    if (mSOSUserState.getValue().getUserName().equals(user.getUserName())) {
                        Log.d(TAG, " onUserStateUpdated: setting NON SOS: " + user.getUserName());
                        mSOSUserState.setValue(new SOSDataState(EnumConstant.sosState.DEFAULT,
                                user.getUserName(), user.getBatteryPercentage(), user.getLocation(), user.getSOS(), user.getSignalStrength(), user.getUserID()));
                    }
                }
            }
        }

        @Override
        public void onPttMessageReceived(IMediaMessage message) {
            String downloadURI = ServerConstant.getDownloadAWSServer();
            String messageType = EnumConstant.MessageType.TEXT.name();
            boolean isGroupChat = message.getRecipientChannels() != null && !message.getRecipientChannels().isEmpty();
            String mediaPath = "";
            Log.d(TAG,"onMessageLogged, messageType: " + message.getMessageType());
            if (message.getMessageType() == Mumble.TextMessage.MsgType.ImageMessageType) {
                messageType = EnumConstant.MessageType.IMAGE.name();
                adcMessageType = EnumConstant.ADCMessageType.IMAGE.name();
            }
            else if (message.getMessageType() == Mumble.TextMessage.MsgType.VoiceMessageType) {
                mediaPath = downloadURI + message.getMessageId();
                messageType = EnumConstant.MessageType.AUDIO.name();
            }
            else if (message.getMessageContent().contains(EnumConstant.MESSAGE_TYPE_LOCATION)) {
                messageType = EnumConstant.MessageType.LOCATION.name();
            }
            else if (message.getMessageType() == Mumble.TextMessage.MsgType.VideoMessageType){
                mediaPath = downloadURI + message.getMessageId();
                messageType = EnumConstant.MessageType.VIDEO.name();
                adcMessageType = EnumConstant.ADCMessageType.VIDEO.name();
            } else if (message.getMessageType() == Mumble.TextMessage.MsgType.DocMessageType) {
                mediaPath = downloadURI + message.getMessageId();
                messageType = EnumConstant.MessageType.DOCUMENT.name();
            }
            storeMessageDataInDB(message, false, isGroupChat, messageType, mediaPath,EnumConstant.MsgStatus.Undelivered.ordinal());
            Log.d(TAG, "onMessageLogged: message value " + message.getMessageContent());
        }

        @Override
        public void onCurrentSpeakerStateUpdated(int currentSpeakerId, boolean isCurrentSpeakerSOS, int currentSpeakerChannel,
                                                 int currentSpeakerOneToOne, int currentListenerOneToOne, boolean isOneToOneTargetBusy, String callId) {
            if (getJioTalkieService().isBoundToPttServer() && getJioTalkieService().isPttConnectionActive() && getJioTalkieService().getJioPttSession() != null) {
                Log.d(TAG, "DashboardViewModel onCurrentSpeakerStateUpdated: currentSpeakerId = " +currentSpeakerId +
                        "isCurrentSpeakerSOS = " + isCurrentSpeakerSOS +
                        " currentSpeakerOneToOne = "+currentSpeakerOneToOne +
                        " currentListenerOneToOne = "+currentListenerOneToOne +" currentSpeakerChannel = "+currentSpeakerChannel);
                int userId = getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserID();
                if (currentSpeakerId != 0 && currentSpeakerId != -1 &&
                        getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID() == currentSpeakerChannel) {
                    if (userId == currentSpeakerId) {
                        Log.d(TAG, "onCurrentSpeakerStateUpdated Self User case : currentSpeakerId = " + currentSpeakerId + " isCurrentSpeakerSOS " + isCurrentSpeakerSOS);
                        mCurrentSpeakerState.setValue(new CurrentSpeakerState(true, isCurrentSpeakerSOS, currentSpeakerId,
                                0, 0, true, callId));
                    } else {
                        Log.d(TAG, "onCurrentSpeakerStateUpdated Other User case : currentSpeakerId = " + currentSpeakerId + " isCurrentSpeakerSOS " + isCurrentSpeakerSOS);
                        mCurrentSpeakerState.setValue(new CurrentSpeakerState(false, isCurrentSpeakerSOS, currentSpeakerId,
                                0, 0, true, callId));

                    }
                } else if ((!isOneToOneTargetBusy && currentSpeakerOneToOne != 0 && currentSpeakerOneToOne != -1
                        && currentListenerOneToOne != 0 && currentListenerOneToOne != -1) ||
                        (isOneToOneTargetBusy && currentListenerOneToOne != 0 && currentListenerOneToOne != -1)) {
                    mCurrentSpeakerState.setValue(new CurrentSpeakerState(userId == currentSpeakerOneToOne, false, 0,
                            currentSpeakerOneToOne, currentListenerOneToOne, isOneToOneTargetBusy, callId));
                }
            }
            Log.i(TAG , "DashboardViewModel onCurrentSpeakerStateUpdated: mCurrentSpeakerState: "+mCurrentSpeakerState.toString());
        }


        @Override
        public void deliveryStatusReport(Mumble.MessageDelivery deliveryStatusReport) {
            updateMessageStatusInDB(deliveryStatusReport.getStatus().ordinal(), deliveryStatusReport.getMsgId(), String.valueOf(deliveryStatusReport.getReceiverDisplayedListList()), String.valueOf(deliveryStatusReport.getReceiverDeliveredListList()));
            mMessageDeliveryStatus.setValue(deliveryStatusReport);
        }

        @Override
        public void onACLReport(Mumble.ACL aclReports) {
            mACLReports.setValue(aclReports);
        }

        @Override
        public void onPttChannelStateChange(IChannelModel channel) {
           mChannelState.setValue(channel);
        }

        @Override
        public void onConnectionSynchronizedUpdate(boolean isSynchronized) {
            Log.d(TAG, "onConnectionSynchronizedUpdate: called isSynchronized = "+isSynchronized);
            mConnectionSynchronized.setValue(isSynchronized);
        }

        @Override
        synchronized public void onRawAudioDataReceived(byte[] pcmData) {
            if (mAudioRecordFileHandle != null) {
                try {
                    if (mAudioRecordFileHandle.getFD() != null && mAudioRecordFileHandle.getFD().valid()) {
                        mAudioRecordFileLen += pcmData.length;
                        mAudioRecordFileHandle.write(pcmData);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Log.w(TAG, "onRawAudioDataReceived: WAV recording file handle is null");
            }
        }

        @Override
        public void onDisablePttCall(String callId, boolean isGroupCall) {
            super.onDisablePttCall(callId, isGroupCall);
            mDisableGroupPttCall.setValue(new PttCallUserState(callId, isGroupCall));
        }
    };



    public DashboardViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "DashboardViewModel: constructor called");
        current_sos_user= new UserModel();
        current_sos_user.setUserID(0);
    }

    public void init() {
        Log.d(TAG, "init: called");
        mDatabase = new JioTalkieDatabaseRepository(this.getApplication());
        mJioTalkieSettings = JioTalkieSettings.getInstance(this.getApplication());


    }
    public LiveData<SubChannelCreationData> observeSubChannelCreation() {
        return subChannelCreationData;
    }

    public void resetObserveSubChannelCreation(){
        subChannelCreationData.setValue(null);
    }

    public void initServiceInstance(JioTalkieServiceInterface service) {
        mJioTalkieService = service;
    }

    public void setCertificateData(byte[] data) {
        mCertificate = data;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        //mDatabase.close();
        if (mJioTalkieService.isBoundToPttServer()) {
            mJioTalkieService.removeObserver(jioPttObserver);
            mJioTalkieService.closePttConnection();
        }
    }

    public String getTargetUserName() {
        return mTargetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.mTargetUserName = targetUserName;
    }

    public JioTalkieSettings getSettings() {
        return mJioTalkieSettings;
    }

    public JioTalkieServiceInterface getJioTalkieService() {
        return mJioTalkieService;
    }

    //FIXME we are calling this function from personal chat before initiating 1-1 PTT to reset the currentSpeakerState.
    //FIXME We have to do this because server is not calling messageUserState() function for 1-1 PTT.
    public void resetCurrentSpeakerState() {
        Log.d(TAG, "resetCurrentSpeakerState: called");
        mCurrentSpeakerState.setValue(new CurrentSpeakerState(false, false, -1,
                -1, -1, true, ""));
    }
    public void checkLatestVersion() {
        Call<ApkResponseModel> call = RetrofitClient.getRetrofitClient(RetrofitClient.BaseUrlType.ApkDownloadURL).updateApkVersion();
        call.enqueue(new Callback<ApkResponseModel>() {
            @Override
            public void onResponse(Call<ApkResponseModel> call, Response<ApkResponseModel> response) {
                if (response.code() == 200){
                    ApkResponseModel updateApkResponseModel = response.body();
                    Log.d("Tarun APK", "updateApk:updateApkResponseModel:" + updateApkResponseModel + ", getAndroidApkVersion:" + updateApkResponseModel.getAndroidApkVersion() +
                            ",getAndroidApkFile:" + updateApkResponseModel.getAndroidApkFile()) ;
                    if (updateApkResponseModel != null) {
                        mUpdateApkResponseModel.setValue(new ApkResponseModel(updateApkResponseModel.getAndroidApkVersion(),updateApkResponseModel.getAndroidApkFile()));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApkResponseModel> call, Throwable t) {
                Log.e(TAG,"updateApk:onFailure:" + t.getMessage());
            }
        });
    }

    public void downloadApk() {
        Call<ApkResponseModel> call = RetrofitClient.getRetrofitClient(RetrofitClient.BaseUrlType.ApkDownloadURL).updateApk();
        call.enqueue(new Callback<ApkResponseModel>() {
            @Override
            public void onResponse(Call<ApkResponseModel> call, Response<ApkResponseModel> response) {
                if (response.code() == 200){
                    ApkResponseModel updateApkResponseModel = response.body();
                    Log.d(TAG, "updateApk:downloadApk():updateApkResponseModel:" + updateApkResponseModel);
                    if (updateApkResponseModel != null){
                        mDownloadApkResponseModel.setValue(new ApkResponseModel(updateApkResponseModel.getAndroidApkVersion(),updateApkResponseModel.getAndroidApkFile()));
                    }
                } else {
                    Log.d(TAG, "updateApk:response.code():" + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApkResponseModel> call, Throwable t) {
                Log.e(TAG,"updateApk:onFailure:" + t.getMessage());
            }
        });
    }

    public void removeOldApk() {
        File file = new File(getApplication().getApplicationContext().getExternalFilesDir(EnumConstant.UPDATE_APK_FOLDER_NAME).getAbsolutePath());
        if (file.exists()){
            file.delete();
        }
    }
    public void storeMessageDataInDB(IMediaMessage message, boolean isSelfChat, boolean isGroupChat, String messageType, String mediaPath, int msgStatus) {
        String targetChannel = "";
        String targetUser = "";
        String targetTree = "";
        int batteryInfo = 0;
        String locationCoordinates = "";
        String latitude = "";
        String longitude = "";
        String msg = message.getMessageContent();
        if (!messageType.equals(EnumConstant.MessageType.AUDIO.name()) && !messageType.equals(EnumConstant.MessageType.SOS_AUDIO.name())) {
            if (isGroupChat) {
                if (message.getRecipientChannels() != null && !message.getRecipientChannels().isEmpty() && message.getRecipientChannels().get(0) !=null) {
                    targetChannel = message.getRecipientChannels().get(0).toString();
                }
            } else {
                if (message.getRecipientUsers() != null &&  message.getRecipientUsers().get(0) != null && !message.getRecipientUsers().isEmpty()) {
                    targetUser = message.getRecipientUsers().get(0).getUserName();
                    Log.d(TAG, "Jio:targetUser:" + targetUser);
                }
            }

            if (message.getRecipientTrees() != null && !message.getRecipientTrees().isEmpty() && message.getRecipientTrees().get(0) !=null) {
                targetTree = message.getRecipientTrees().get(0).toString();
            }
        }
        if (messageType.equals(EnumConstant.MessageType.SOS_AUDIO.name())) {
            if (!mSOSUserState.getValue().getBatteryPercentage().isEmpty()) {
                batteryInfo = Integer.parseInt(mSOSUserState.getValue().getBatteryPercentage());
            }
            locationCoordinates = mSOSUserState.getValue().getLocation();
            if (!locationCoordinates.isEmpty()) {
                if(locationCoordinates.contains(";")){
                    String[] parts = locationCoordinates.split(";");
                    latitude = parts[0];
                    longitude = parts[1];
                } else if (locationCoordinates.contains(",")) {
                    String[] parts = locationCoordinates.split(",");
                    latitude = parts[0];
                    longitude = parts[1];
                }
            }
        }
        if (isSelfChat && messageType.equals(EnumConstant.MessageType.AUDIO.name()) && !isGroupChat) {
            targetUser = getTargetUserName();
            Log.d(TAG, "WHISPER:voice target:" + targetUser);
        }

        if (messageType.equals(EnumConstant.MessageType.IMAGE.name())) {
            msg = ServerConstant.getDownloadAWSServer() + message.getMessageId();
        }

        JioTalkieChats jioTalkieChats = new JioTalkieChats(
                message.getOriginatorId(),
                message.getOriginatorName(),
                targetChannel,
                targetTree,
                targetUser,
                msg,
                msgStatus,
                message.getReceivedTimestamp(),
                message.getMessageId(),
                messageType,
                message.getMessageMimeType(),
                mediaPath,
                message.isSOS(),
                isSelfChat,
                isGroupChat,
                latitude,
                longitude,
                batteryInfo,
                -1,
                null,
                null,
                "");
        if (messageType.equals(EnumConstant.MessageType.LOCATION.name())) {
            String[] parts = message.getMessageContent().split("/");
            jioTalkieChats.setLatitude(parts[1]);
            jioTalkieChats.setLongitude(parts[2]);
        }

        long result = mDatabase.addChat(jioTalkieChats);
        if (result != -1)
            mChatMessageState.setValue(jioTalkieChats);
    }

    public void updateJioTalkieChats(String fileUploadStatus, String msg_id, String message) {
        mDatabase.updateJioTalkieChats(fileUploadStatus, msg_id, message);
    }

    private void handleRecordingState(IUserModel user, boolean isSelfUser, boolean isGroupChat, String msgId) {
        Log.d(TAG, "handleRecordingState() called  userTalking = "+ user.isUserTalking() + " isAudioRecording = "+ isAudioRecording
                + " isSelfUser = "+ isSelfUser + " isGroupChat = "+isGroupChat + " isPersonalChat = "+ !isGroupChat);
        /* This case is handle when :
         *  Already 1-1 or Normal PTT call is ongoing and SOS or priority speaker call come in between */
        if (user.isUserTalking() && isAudioRecording && isGroupChat) {
            Log.d(TAG, "handleRecordingState() >>>>>  Stop ongoing recording and start new audio recording");
            manageRecording(user.getUserName(), user.getUserID(),false, isSelfUser, true, msgId);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    manageRecording(user.getUserName(), user.getUserID(),true, isSelfUser, true, msgId);
                }
            }, RECORDING_STOP_DELAY); // 300ms delay added because mediaRecorder will take time to stop and release recording.
        }
        /* This case is handle for Normal audio recording start for 1-1, group PTT and SOS */
        else if (user.isUserTalking() && !isAudioRecording) {
            Log.d(TAG, "handleRecordingState() >>>>>  start Audio Recording");
            manageRecording(user.getUserName(), user.getUserID(),true, isSelfUser, isGroupChat, msgId);
        }
        /* This case is handle to stop audio recording for 1-1, group PTT and SOS */
        else if (!user.isUserTalking()) {
            Log.d(TAG, "handleRecordingState() >>>>>  Stop Audio Recording");
            manageRecording(user.getUserName(),user.getUserID(), false, isSelfUser, isGroupChat, msgId);
            resetCurrentSpeakerState();
        }
    }

    //Removing usages for this as the core function has been already moved to a thread - Priyanshu.Vijay
    public void storeMessageHistoryIntoDB(JioTalkieChats jioTalkieChats) {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                mDatabase.addChat(jioTalkieChats);
            }
        });

    }

    public LiveData<EnumConstant.connectionState> observerConnectionStateData() {
        return mConnectionState;
    }

    public LiveData<UserConnectionState> observerUserStateData() {
        return mUserState;
    }

    public LiveData<UserTalkState> observeSelfUserTalkState() {
        return mUserTalkState;
    }

    public LiveData<SOSDataState> observeSOSStateLiveData() {
        return mSOSUserState;
    }

    public LiveData<JioTalkieChats> observeChatMessageData() {
        return mChatMessageState;
    }

    public LiveData<List<JioTalkieServer>> observeServerDataFromDB() {
        return mDatabase.getJioTalkieServers();
    }

    public LiveData<AudioDownloadState> observeDownloadAudio() {
        return mDownloadAudioState;
    }
    public LiveData<DocumentDownloadState> observeDownloadDocument() {
        return mDownloadDocumentState;
    }
    public LiveData<VideoDownloadState> observeDownloadVideo() {
        return mDownloadVideoState;
    }

    public LiveData<List<JioTalkieCertificates>> observeCertificateDataFromDB() {
        return mDatabase.getJioTalkieCertificates();
    }

    public LiveData<List<JioTalkieChats>> observeChatFromDB() {
        return mDatabase.getJioTalkieChats();
    }

    public LiveData<List<JioTalkieChats>> observeGroupChatFromDB() {
        return mDatabase.getJioTalkieGroupChats();
    }

    public LiveData<List<JioTalkieChats>> observeFilteredGroupChatFromDB(long receivedTime) {
        return mDatabase.getFilteredJioTalkieGroupChats(receivedTime);
    }

    public LiveData<List<JioTalkieChats>> observePersonalChatFromDB(String targetUser) {
        return mDatabase.getJioTalkiePersonalChats(targetUser);
    }

    public LiveData<List<JioTalkieChats>> observeFilteredPersonalChatFromDB(String targetUser,long receivedTime) {
        return mDatabase.getFilteredJioTalkiePersonalChats(targetUser,receivedTime);
    }

    public LiveData<JioTalkieChats> observeChatByMsgId(String msgId) {
        return mDatabase.getJioTalkieChatByMsgId(msgId);
    }

    public LiveData<List<JioTalkieChats>> observeSOSChatFromDB(String targetUser) {
        return mDatabase.getJioTalkieSOSChats(targetUser);
    }

    public LiveData<EnumConstant.LoginState> observeLoginState() {
        return mLoginState;
    }

    public LiveData<List<RegisteredUser>> observeRegisterUserData() {
        return mRegisterUserList;
    }

    public LiveData<List<Mumble.UserList.User>> observeAllRegisterUser() {
        return mAllRegisterUsers;
    }

    public MutableLiveData<List<Mumble.HistoricalDataList.HistoricalData>> observeHistoricalData() {
        return mHistoricalDataList;
    }

    public LiveData<CurrentSpeakerState> observeCurrentSpeakerStateData() {
        return mCurrentSpeakerState;
    }

    public LiveData<MediaUploadResponse> observeFileUploadStateData(){
        return mMediaUploadState;
    }

    public LiveData<ApkResponseModel> observerLatestApkVersionCheck(){
        return  mUpdateApkResponseModel;
    }
    public LiveData<ApkResponseModel> observerDownloadApk(){
        return mDownloadApkResponseModel;
    }

    public MutableLiveData<Mumble.MessageDelivery> observeMsgDeliveryReport() {
        return mMessageDeliveryStatus;
    }

    public LiveData<Boolean> observeConnectionSynchronizedState() {
        return mConnectionSynchronized;
    }

    public LiveData<IUserModel> observeUserStateUpdate(){
        return mUserStateUpdate;
    }

    public LiveData<FilterChatMessageList> observeFilteredChatFromServer() {
        return mFilteredChatFromServer;
    }

    public LiveData<PaginatedGroupChat> observePaginatedGroupChat() {
        return mPaginatedGroupChats;
    }

    public LiveData<PaginatedPersonalChat> observePaginatedPersonalChat() {
        return mPaginatedPersonalChats;
    }

    public LiveData<Boolean> observeOneToOneTargetBusy() {
        return oneToOneTargetBusy;
    }
    public void resetOneToOneTargetBusy(){
        oneToOneTargetBusy.setValue(null);
    }

    public LiveData<PttCallUserState> observeDisablePttCall() {
        return mDisableGroupPttCall;
    }
    public LiveData<IChannelModel> observeChannelState() {
        return mChannelState;
    }
    public LiveData<Mumble.ACL> observeACLReport() {
        return mACLReports;
    }
    public LiveData<RegUserState> observeUserAddedOrDeleted(){
        return mRegUserStateChanged;
    }
    public void callServerConnectionApi(Server server) {
        if (server == null)
            return;

        if (!server.getHost().isEmpty() && !server.getMsisdn().isEmpty()
                && !server.getSsoToken().isEmpty()) {
            Log.d(TAG, "Login: callServerConnectionApi: IP: " + server.getHost()
                    + " Port: " + server.getPort());
//            CommonUtils.logSensitiveData(TAG,"Login: callServerConnectionApi: MSISDN", server.getMsisdn());
//            CommonUtils.logSensitiveData(TAG,"Login: callServerConnectionApi: sso token", server.getSsoToken());
        } else {
            Log.e(TAG,"Login: callServerConnectionApi: Incorrect server connection parameters");
        }
        if (mJioTalkieService.isBoundToPttServer() && mJioTalkieService.isPttConnectionActive()) {
            mJioTalkieService.addObserver(new JioPttObserver() {
                @Override
                public void onPttConnectionLost(JioTalkieException e) {
                    callServerConnectionApi(server);
                    mJioTalkieService.removeObserver(this);
                }
            });
            mJioTalkieService.closePttConnection();
            return;
        }
        CustomServerConnectTask connectTask = new CustomServerConnectTask(this.getApplication(), mCertificate , getJioTalkieService());
        connectTask.execute(server);
    }
//    public void callServerConnectionApi(Server server) {
//        if (server == null)
//            return;
//
//        Log.d(TAG, "callServerConnectionApi: called connecting with username : "+server.getUsername());
//        if (mJioTalkieService != null && mJioTalkieService.isConnected()) {
//            adcMsiSdn = server.getMsisdn();
//            mJioTalkieService.registerObserver(new JioPttObserver() {
//                @Override
//                public void onDisconnected(HumlaException e) {
//                    callServerConnectionApi(server);
//                    mJioTalkieService.unregisterObserver(this);
//                }
//            });
//            mJioTalkieService.disconnect();
//            return;
//        }
//        ServerConnectTask connectTask = new ServerConnectTask(this.getApplication(), mCertificate);
//        connectTask.execute(server);
//    }

    public void manageRecording(String userName,int senderUserId, final boolean isListening, boolean isSelfChat, final boolean isGroupChat, String msgId) {
        if (isAudioRecording && isListening) {
            Log.d(TAG, "manageRecording: return already recording on going");
            return;
        }
        Log.d(TAG, " manageRecording: username: " + userName + " isListening: " + isListening
                + " isSelfChat: " + isSelfChat+ " isGroupChat: "+isGroupChat);
        File file;
        file = new File(getApplication().getApplicationContext().getExternalFilesDir("media").getAbsolutePath());

        if (!file.exists()) {
            file.mkdirs();
        }

        if (getJioTalkieService().isBoundToPttServer() && getJioTalkieService().getJioPttSession() != null && getJioTalkieService().getJioPttSession().fetchSessionPttUser() != null) {
            int selfSessionId = getJioTalkieService().getJioPttSession().fetchSessionPttUser().getSessionID();
            IUserModel self =getJioTalkieService().getJioPttSession().fetchPttUser(selfSessionId);
            if (self.isDeaf()) {
                return ;
            }
        }

        mAudioHandler.removeCallbacksAndMessages(null);
        mAudioHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isListening) {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
                    String dateTime = "JioTalkie_" + simpleDateFormat.format(calendar.getTime()).toString();
                    mAudioRecordFilePath = getApplication().getApplicationContext().getApplicationContext()
                            .getExternalFilesDir("media").getAbsolutePath() +"/" + dateTime +".wav";
                    Log.d(TAG, "manageRecording: Start Voice Recording.");

                    // Create empty file in which we will write PCM audio samples sent from Humla
                    mAudioRecordFileLen = 0;
                    File fp1 = new File(mAudioRecordFilePath);
                    try {
                        fp1.createNewFile();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        mAudioRecordFileHandle = new RandomAccessFile(fp1, "rw");
                        // Move file pointer pass WAV header. We will write WAV header while closing
                        // the file because we do not know audio length at present.
                        // WAV header size is 44.
                        mAudioRecordFileHandle.seek(WAV_HEADER_SIZE); // moving to 44th position
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Mumble.TextMessage.MsgType messageType;
                    boolean isSOS = false;


                    // Check if the talking user is the SOS Speaker
                    if (mCurrentSpeakerState.getValue() != null && mCurrentSpeakerState.getValue().isSelfCurrentSpeakerSOS()) {
                        msgType = EnumConstant.MessageType.SOS_AUDIO.name();
                        messageType = Mumble.TextMessage.MsgType.VoiceMessageType;
                        adcMessageType = EnumConstant.ADCMessageType.SOS.name();
                        isSOS = true;
                        Log.d(TAG, " manageRecording: Storing message in DB as SOS with msgId = "+msgId);
                    } else {
                        msgType = EnumConstant.MessageType.AUDIO.name();
                        messageType = Mumble.TextMessage.MsgType.VoiceMessageType;
                        adcMessageType = EnumConstant.ADCMessageType.VOICE.name();
                        Log.d(TAG, "manageRecording: Storing message in DB as AUDIO with msgId = "+msgId);
                    }
                    audioMessage = new MediaMessage(getJioTalkieService().getJioPttSession().fetchSessionId(), userName, new ArrayList<ChannelModel>(0),
                            new ArrayList<ChannelModel>(0), new ArrayList<UserModel>(0), "", messageType, msgId, "", isSOS);
                    isAudioRecording = true;
                    startTime = System.currentTimeMillis();
                } else {
                    Log.d(TAG, "manageRecording: Stop Voice Recording.");
                    if (mAudioRecordFileHandle != null) {
                        try {
                            // Write WAV header at the beginning of the file.
                            mAudioRecordFileHandle.seek(0);
                            writeWavHeader(mAudioRecordFileHandle,mAudioRecordFileLen);
                            mAudioRecordFileHandle.close();
                            mAudioRecordFileHandle = null;

                            endTime = System.currentTimeMillis();
                            long timeDifference = endTime - startTime;
                            double timeDifferenceSeconds = timeDifference / 1000.0;
                            storeMessageDataInDB(audioMessage, isSelfChat, isGroupChat, msgType, mAudioRecordFilePath, EnumConstant.MsgStatus.Undelivered.ordinal());
                            int userID = 0;
                            int targetUserid = 0;
                            if (isGroupChat) {
                                adcMessageCategory = EnumConstant.ADCMessageCategory.GROUP.name();
                                targetUserid = EnumConstant.DEFAULT_TARGET_USER_ID;
                            } else {
                                adcMessageCategory = EnumConstant.ADCMessageCategory.ONE_TO_ONE.name();
                                targetUserid = mTargetId;
                            }
                            if (isSelfChat) {
                                userID = mUserId;
                            } else {
                                userID = senderUserId;
                            }ADCInfoUtils.calculatePTTSize(mAudioRecordFilePath, timeDifferenceSeconds,isSelfChat,adcMessageType, userID, mChannelId, adcMessageCategory, targetUserid);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        isAudioRecording = false;
                    }
                }
            }
        });
    }
    public  void setOneToOneTargetId(int targetId)  {
        mTargetId = targetId;
    }

    public void writeWavHeader(RandomAccessFile outputStream, int totalAudioLen) throws IOException {
        Log.d(TAG,"writeWavHeader: totalAudioLen: " + totalAudioLen);
        int numChannels = AudioFormat.CHANNEL_OUT_DEFAULT; // 1
        int sampleRate = SAMPLE_RATE; // 48000
        int bitsPerSample = ENCODING_PCM_16BIT_BITS_PER_SAMPLE; // 16

        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int totalDataLen = totalAudioLen + WAV_HEADER_SIZE - 8;

        ByteBuffer buffer = ByteBuffer.allocate(WAV_HEADER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // RIFF chunk descriptor
        buffer.put("RIFF".getBytes());
        buffer.putInt(totalDataLen); // ChunkSize
        buffer.put("WAVE".getBytes());

        // fmt subchunk
        buffer.put("fmt ".getBytes());
        buffer.putInt(16); // Subchunk1Size (16 for PCM)
        buffer.putShort((short) 1); // AudioFormat (1 for PCM)
        buffer.putShort((short) numChannels); // ChannelModel
        buffer.putInt(sampleRate); // SampleRate
        buffer.putInt(byteRate); // ByteRate
        buffer.putShort((short) (numChannels * bitsPerSample / 8)); // BlockAlign
        buffer.putShort((short) bitsPerSample); // BitsPerSample

        // data subchunk
        buffer.put("data".getBytes());
        buffer.putInt(totalAudioLen); // Subchunk2Size

        // Write the header to the output stream
        outputStream.write(buffer.array(), 0, WAV_HEADER_SIZE);
    }

    public void messagesDownloadApi(int senderUserId, int targetUserId,boolean isOneToOneChat,String durationFrom, String durationTill, boolean isCalendarFilter) {
        MessageRequestModel messageRequestModel;
        if (isOneToOneChat){
            messageRequestModel = new MessageRequestModel(senderUserId, targetUserId, isOneToOneChat, durationFrom, durationTill);
        }else {
            messageRequestModel = new MessageRequestModel(null,null,null, getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID(), durationFrom, durationTill, null, null, null);
        }
        Call<MessageResponseModel> call = RESTApiManager.getInstance().fetchMessagesData(messageRequestModel);
        call.enqueue(new Callback<MessageResponseModel>() {
            @Override
            public void onResponse(Call<MessageResponseModel> call, Response<MessageResponseModel> response) {
                if (response.code()==200){
                    String totalMessages = "";
                    MessageResponseModel messageResponse = response.body();
                    assert messageResponse != null;
                    List<MessageListResponseModel> messageList = messageResponse.getData();
                    if (messageList != null && !messageList.isEmpty()) {
                        Log.d(TAG, "Total message count = " + messageList.size());
                        messageListDownloadThread(messageList, durationFrom, isCalendarFilter, null, false);
                    }
                }
                else {
                    Log.d(TAG, "Different Response Code = " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MessageResponseModel> call, Throwable t) {
                if (t instanceof ConnectException)Toast.makeText(getApplication(),"ConnectException: Rest Server Not Reachable",Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });

    }

    public void messageListDownloadThread(List<MessageListResponseModel> messageList, String durationFrom, boolean isCalendarFilter, MessageRequestModel messageRequestModel, boolean isLast) {
        Thread messageHistoryDownloaderRestApi = new Thread(new Runnable() {
            @Override
            public void run() {
                {
                    for (int i = 0; i < messageList.size(); i++) {
                        MessageListResponseModel msg = messageList.get(i);
                        if (msg.getMessage() == null || msg.getMessage().isEmpty())
                            continue;
                        int actor = msg.getActor();
                        boolean isSelfChat = false;

                        boolean isGroupChat = true;
                        if (getJioTalkieService().isBoundToPttServer() && getJioTalkieService().getJioPttSession() != null
                                && getJioTalkieService().isPttConnectionActive()
                                && getJioTalkieService().isConnectionSynchronized()
                                && getJioTalkieService().getJioPttSession().fetchSessionPttUser() != null
                                && msg != null
                                && getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserID() == msg.getActor()) {
                            isSelfChat = true;
                        }
                        if (msg.getChannelId() == -1) {
                            isGroupChat = false;
                        }
                        String actor_name = getUsername(actor);
                        String channels_name = String.valueOf(msg.getChannelId());
                        String trees_name = String.valueOf(msg.getTreeId());
                        String target_user;
                        if(isGroupChat) {
                            target_user = String.valueOf(msg.getReceiver());
                        } else {
                            target_user = String.valueOf(getUsername(msg.getReceiver()));
                        }
                        String message = msg.getMessage();
                        String preFormatTime = msg.getReceivedTime().split("\\.")[0];
                        Log.v("RestAPIMessages", "Downloading Username = " + actor);
                        String time = null;
                        String[] timeparts = preFormatTime.split(" ");
                        if (timeparts.length > 1 && preFormatTime.split(" ")[0] != null && preFormatTime.split(" ")[1] != null) {
                            time = preFormatTime.split(" ")[0] + "T" + preFormatTime.split(" ")[1] + "Z";

                        } else if (timeparts.length == 1 && preFormatTime.split(" ")[0] != null) {
                            time = preFormatTime.split(" ")[0] + "Z";
                        }
                        Instant instant = null;
                        long received_time = 0;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            instant = Instant.parse(time);
                            received_time = instant.toEpochMilli() - OFFSET_IN_GMT;
                        }

                        String message_type = "";
                        String media_path = "";

                        EnumConstant.ServerMessageType serverMessageType = EnumConstant.ServerMessageType.values()[Integer.parseInt(msg.getMessageType())];
                        if (serverMessageType.equals(EnumConstant.ServerMessageType.TextMessageType)) {
                            message_type = EnumConstant.MessageType.TEXT.name();
                        } else if (serverMessageType.equals(EnumConstant.ServerMessageType.ImageMessageType)) {
                            message_type = EnumConstant.MessageType.IMAGE.name();
                            message = ServerConstant.getDownloadAWSServer() + msg.getMsgId();
                        } else if (serverMessageType.equals(EnumConstant.ServerMessageType.VoiceMessageType)) {
                            if (msg.getIsSos() == 1) {
                                message_type = EnumConstant.MessageType.SOS_AUDIO.name();
                            } else {
                                message_type = EnumConstant.MessageType.AUDIO.name();
                            }
                            media_path = ServerConstant.getDownloadAWSServer() + msg.getMsgId();
                            message = "";
                            Log.v("RestAPIMessages", "While Downloading mediaPath = " + media_path + "\n msgID = " + msg.getMsgId());
                        } else if (serverMessageType.equals(EnumConstant.ServerMessageType.VideoMessageType)) {
                            message_type = EnumConstant.MessageType.VIDEO.name();
                            media_path = ServerConstant.getDownloadAWSServer() + msg.getMsgId();
                            message = "";
                        } else if (serverMessageType.equals(EnumConstant.ServerMessageType.DocMessageType)) {
                            message_type = EnumConstant.MessageType.DOCUMENT.name();
                            media_path = ServerConstant.getDownloadAWSServer() + msg.getMsgId();
                            String[] fileName = msg.getMessage().split(msg.getMsgId());
                            if (fileName.length > 1) {
                                message = fileName[1].replaceFirst("_", "");
                            } else if (fileName.length == 1) {
                                message = fileName[0].replaceFirst("_", "");
                            }
                        }
                        String latitude = "";
                        String longitude = "";
                        if (msg.getHasLocation() == 1) {
                            if (msg.getLocation().contains(";")) {
                                String[] location = msg.getLocation().split(";");
                                if (location.length > 1) {
                                    latitude = location[0];
                                    longitude = location[1];
                                }
                            } else if (msg.getLocation().contains(",")) {
                                String[] locationCoordinates = msg.getLocation().split(",");
                                if (locationCoordinates.length > 1) {
                                    latitude = locationCoordinates[0];
                                    longitude = locationCoordinates[1];
                                }
                            }
                        }
                        if (msg.getMessage().contains(EnumConstant.MESSAGE_TYPE_LOCATION)) {
                            message_type = EnumConstant.MessageType.LOCATION.name();
                            String[] parts = msg.getMessage().split("/");
                            latitude = parts[1];
                            longitude = parts[2];
                        }
                        int battery_info = 100;
                        if (msg.getHasBatteryStrength() == 1 && !msg.getBatteryStrength().equals(" ")) {
                            try {
                                battery_info = Integer.parseInt(msg.getBatteryStrength());

                            } catch (NumberFormatException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                        int size = -1;// if size not set
                        String mimeType = msg.getMime_type();
                        String msgId = msg.getMsgId();
                        int msgStatus = msg.getMessageDeliveryStatus();
                        Boolean isSos = msg.getIsSos() == 1;
                        List<String> displayReceiverList = msg.getDisplayReceiverList();
                        List<String> deliveredReceiverList = msg.getDeliveredReceiverList();
                        Log.v("RestAPIMessages", "Downloading isSOS = " + isSos);
                        JioTalkieChats jioTalkieChats = new JioTalkieChats(actor, actor_name, channels_name, trees_name, target_user, message, msgStatus, received_time, msgId, message_type, mimeType, media_path, isSos, isSelfChat, isGroupChat, latitude, longitude, battery_info, size, displayReceiverList, deliveredReceiverList,"");
                        mDatabase.addChat(jioTalkieChats);
                        if (serverMessageType.equals(EnumConstant.ServerMessageType.ImageMessageType)) {
                            updateImageSize(msgId);
                        }
                    }

                    if (!isCalendarFilter && (messageRequestModel != null  && messageRequestModel.getPagination())) {
                        if (messageRequestModel.getOneToOneChat()) {
                            PaginatedPersonalChat paginatedPersonalChat = new PaginatedPersonalChat(mDatabase.getPaginatedPersonalChat(getUsername(messageRequestModel.getUserIdRecv())), isLast, messageRequestModel.getUserIdRecv(), messageRequestModel.getUserIdSend());
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mPaginatedPersonalChats.setValue(paginatedPersonalChat);
                                }
                            });
                        } else {
                            PaginatedGroupChat paginatedGroupChat = new PaginatedGroupChat( mDatabase.getPaginationGroupChats(), isLast);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mPaginatedGroupChats.setValue(paginatedGroupChat);
                                }
                            });
                        }
                    }

                    if (isCalendarFilter) {
                        FilterChatMessageList filterChatMessageList = new FilterChatMessageList();
                        List<JioTalkieChats> filterChatFromServer = mDatabase.getFilteredJioTalkieGroupChatList(DateUtils.getLongFromStringDate(durationFrom));
                        filterChatMessageList.setJioTalkieChats(filterChatFromServer);
                        filterChatMessageList.setDurationFrom(durationFrom);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mFilteredChatFromServer.setValue(filterChatMessageList);
                            }
                        });
                    }
                }
            }
        });
        messageHistoryDownloaderRestApi.start();
    }

    public void updateImageSizeInDB(String msgId, long imageSize) {
        mDatabase.updateImageSizeByMsgId(msgId, imageSize);
    }

    private void updateImageSize(String msgId) {
        Call<Void> call = RESTApiManager.getInstance().fetchMediaFileSize(msgId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Retrieve the Content-Length header
                    String contentLength = response.headers().get("Content-Length");
                    if (contentLength != null) {
                        Log.d(TAG, "fetchMediaFileSize Content-Length: " + contentLength);
                        long imageSize = Long.parseLong(contentLength);
                        updateImageSizeInDB(msgId,imageSize);
                    } else {
                        Log.d(TAG, "fetchMediaFileSize Content-Length header not found");
                    }
                } else {
                    Log.d(TAG, "fetchMediaFileSize Request failed with code: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "fetchMediaFileSize Request error: " + t.getMessage());
            }
        });
    }

    public String getUsername(int userId) {
        String username = "";
        int position = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (RegUserList.size() > 0) {
                try {
                    position = IntStream.range(0, RegUserList.size())
                            .filter(index -> (RegUserList.get(index).getUserId() == userId))
                            .findFirst()
                            .getAsInt();
                    username = RegUserList.get(position).getName();
                    Log.d(TAG, "getUsername: index " + position + " userId: " + userId
                            + " username: " + username);
                } catch (NoSuchElementException e) {
                    Log.d(TAG, "getUsername: not found for userId "+userId);
                }
            }
        }
        return username;
    }

    public void updateMessageInDB(String msgId, String mediaPath) {
        Log.v("RestApiMessages","Updating Database for : "+msgId+" as :"+mediaPath);
        mDatabase.updateJioTalkieChats(mediaPath,msgId);
    }

    public void downloadMediaAndDocumentFile(int position, JioTalkieChats currMessage, boolean isLeft, boolean isSos, String mimeType, String messageType) {
        Log.d("MSISDN downloadMediaAndDocumentFile", "downloadMediaAndDocumentFile");
        Log.v("RestApiMessages","Downloading Media File : "+currMessage.getMedia_path());
        Call<ResponseBody> call = RESTApiManager.getInstance().downloadFile(currMessage.getMsg_id());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    final File file;
                    if (currMessage.getMime_type().equals(EnumConstant.PDF_FILE_EXTENSION) || currMessage.getMime_type().equals(EnumConstant.TEXT_FILE_EXTENSION)) {
                        file = new File(getApplication().getApplicationContext().getExternalFilesDir("Documents").getAbsolutePath(), currMessage.getMessage());
                    } else {
                        file = new File(getApplication().getApplicationContext().getExternalFilesDir("media").getAbsolutePath(), currMessage.getMsg_id()+"."+mimeType.split("\\/")[1]);
                    }
                    BufferedSink sink = Okio.buffer(Okio.sink(file));
                    sink.writeAll(response.body().source());
                    sink.flush();
                    sink.close();
                    file.deleteOnExit();

                    if (messageType.equals(EnumConstant.MessageType.AUDIO.name()) || messageType.equals(EnumConstant.MessageType.SOS_AUDIO.name()))
                        mDownloadAudioState.setValue(new AudioDownloadState(position, file.getAbsolutePath(), isLeft,isSos));
                    if (messageType.equals(EnumConstant.MessageType.VIDEO.name()))
                        mDownloadVideoState.setValue(new VideoDownloadState(position, file.getAbsolutePath(), isLeft,isSos));
                    if (messageType.equals(EnumConstant.MessageType.DOCUMENT.name()))
                        mDownloadDocumentState.setValue(new DocumentDownloadState(position, file.getAbsolutePath(), isLeft,isSos));
                    Log.v("RestApiMessages","Downloaded File "+file.getAbsolutePath());
                    updateMessageInDB(currMessage.getMsg_id(),file.getAbsolutePath());

                } catch (FileNotFoundException fileNotFoundException){
                    if (fileNotFoundException.getMessage().contains("EACCES")) {
                        Log.v("RestApiMessages", "File already in directory, Updating respective file path to current Audio\n" + fileNotFoundException.getMessage());
                        String failedFilePath = fileNotFoundException.getMessage().split("\\: ")[0];
                        updateMessageInDB(currMessage.getMsg_id(),failedFilePath);
                    }
                    else fileNotFoundException.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
    public int getSelfUserId() {
        return mSelfUserId;
    }

    public void uploadMediaToServer(String filePath, String mimeType, String messageId, int fileType, String targetUser, boolean isGroupChat, String uploadStatus) {
        String messageType = "";
        int userSessionId = getJioTalkieService().getJioPttSession().fetchSessionPttUser().getSessionID();
        String userName = getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserName();
        String channelName = getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelName();
        long receivedTime = new Date().getTime();
        if (fileType == EnumConstant.FILE_TYPE_VIDEO) {
            messageType  = EnumConstant.MessageType.VIDEO.name();
        } else if (fileType == EnumConstant.FILE_TYPE_DOC) {
            messageType = EnumConstant.MessageType.DOCUMENT.name();
        } else if (fileType == EnumConstant.FILE_TYPE_IMAGE) {
            messageType = EnumConstant.MessageType.IMAGE.name();
        } else if (fileType == EnumConstant.FILE_TYPE_AUDIO) {
            messageType = EnumConstant.MessageType.AUDIO.name();
        }
        long fileSize = new File(filePath).length();
        JioTalkieChats jioTalkieChats = new JioTalkieChats(
                userSessionId,
                userName,
                channelName,
                "",
                targetUser,
                "",
                0,
                receivedTime,
                messageId,
                messageType,
                mimeType,
                filePath,
                false,
                true,
                isGroupChat,
                "",
                "",
                0,
                fileSize,
                null,
                null,
                uploadStatus);

        long result = mDatabase.addChat(jioTalkieChats);
        if (result != -1)
            mChatMessageState.setValue(jioTalkieChats);

        File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(MultipartBody.FORM, file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("InputFile", file.getName(), requestFile);
        MediaUploadModel mediaUploadModel = new MediaUploadModel(fileType, mimeType, messageId);
        Call<MediaUploadResponse> call = RESTApiManager.getInstance().uploadImage(body, mediaUploadModel);
        call.enqueue(new Callback<MediaUploadResponse>() {
            @Override
            public void onResponse(Call<MediaUploadResponse> call, Response<MediaUploadResponse> response) {
                if (response.code() == 200) {
                    MediaUploadResponse uploadResponse = response.body();
                    if (uploadResponse != null) {
                        Log.d(TAG, "File Upload SuccessFull, messageID " + uploadResponse.getMsgId());
                        Log.d(TAG, "File Upload SuccessFull, MimeType " + mimeType);
                        mMediaUploadState.setValue(new MediaUploadResponse(uploadResponse.getMsgId(),
                                uploadResponse.getUploadStatus(), true, mimeType, fileType, filePath));
                        if (file.exists() && fileType != EnumConstant.FILE_TYPE_AUDIO && fileType != EnumConstant.FILE_TYPE_VIDEO && fileType != EnumConstant.FILE_TYPE_DOC) {
                            file.delete();
                        }
                    }
                } else {
                    Log.d(TAG, "Update failed: " + response.message());
                    mMediaUploadState.setValue(new MediaUploadResponse("",
                            "File Upload Failed!", false, "", 0, ""));
                }
            }

            @Override
            public void onFailure(Call<MediaUploadResponse> call, Throwable t) {
                mMediaUploadState.setValue(new MediaUploadResponse("",
                        "File Upload Failed!", false, "", 0, ""));
                if (file.exists() && fileType != EnumConstant.FILE_TYPE_AUDIO && fileType != EnumConstant.FILE_TYPE_VIDEO && fileType != EnumConstant.FILE_TYPE_DOC) {
                    file.delete();
                }
            }

        });
    }

    public boolean callZLAApi(String phoneNumber) {
        // ZLA API call should go over CELLULAR network for MSISDN injection to HTTP header
        // Bind process temporarily to cellular data network for this call
        boolean ret = bindProcessToCellularDataNetwork();
        // FIXME need to enable below 'if' condition for production
        //if (!ret) {
        //    Log.d(TAG, "Could not bind to Cellular network. Please check internet connection of cellular network");
        //    return ret; // Let the caller show some error message to user
        //}

        Log.d(TAG, "callZLAApi");
        String consumptionDeviceName = android.os.Build.MODEL;
        //android.provider.Settings.Secure.ANDROID_ID;
        String number;
        // FIXME Eventually we need not send number in header as it will be inserted somewhere in network
        // This is temporary code.
        number = Objects.requireNonNullElse(phoneNumber, "7977737420");
        // FIXME headers are hardcoded for testing purpose. To be changed when final API is available.
        Call<ZLAResponseModel> call = RetrofitClient.getRetrofitClient(RetrofitClient.BaseUrlType.LoginUrlHttp).callZLAApi(
                BuildConfig.LOGIN_API_KEY,
                BuildConfig.LOGIN_APP_NAME,
                "405866100001851",
                number,
                "androidos",
                Build.MODEL,
                "android",
                mAndroidId
        );
        call.enqueue(new Callback<ZLAResponseModel>() {
            @Override
            public void onResponse(@NonNull Call<ZLAResponseModel> call,  @NonNull Response<ZLAResponseModel> response) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ConnectivityManager cm = (ConnectivityManager) getApplication().getApplicationContext()
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    // Unbind the process
                    cm.bindProcessToNetwork(null);
                    // It is safe to call this even if process is not bounded to any network
                }
                int code = response.code();
                Log.d(TAG, "onResponse: callZLAApi: HTTP response code: " + code);
                if (code == 200) {
                    if (response.body() != null) {
                        if (response.body().getmsisdn() != null) {
                            Log.d(TAG, "onResponse: callZLAApi: MSISDN: "
                                    + response.body().getmsisdn().substring(0,3) + "...");
                        }
                        if (response.body().getSsoToken() != null) {
                            Log.d(TAG, "onResponse: callZLAApi: ssoToken: "
                                    + response.body().getSsoToken().substring(0,3) + "...");
                        }
                        if (response.body().getjToken() != null) {
                            Log.d(TAG, "onResponse: callZLAApi: jToken: "
                                    + response.body().getjToken().substring(0,3) + "...");
                        }
                        // Store SSO Token in shared pref
                        mJioTalkieSettings.setEncryptedMsisdn(response.body().getmsisdn());
                        mJioTalkieSettings.setSsoToken(response.body().getSsoToken());
                        mJioTalkieSettings.setJToken(response.body().getjToken());
                        // Store sso token and Msisdn in RESTAPI manager for REST API .

                        RESTApiManager.getInstance().setUp(getApplication().getApplicationContext(),
                                response.body().getmsisdn(),response.body().getSsoToken());
                        try {
                            String deys_msisdn=decrypt_msisdn(response.body().getmsisdn(),response.body().getSsoToken());
                            Log.d("DEYS_MSISDN", deys_msisdn.substring(0,3) + "...");
                            mJioTalkieSettings.setMSISDN(deys_msisdn);
                        } catch (NoSuchPaddingException e) {
                            throw new RuntimeException(e);
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        } catch (InvalidKeyException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalBlockSizeException e) {
                            throw new RuntimeException(e);
                        } catch (BadPaddingException e) {
                            throw new RuntimeException(e);
                        }

                        long currentTime = System.currentTimeMillis();
                        mJioTalkieSettings.setSsotokenExpiry(currentTime + EnumConstant.SSO_TOKEN_REFRESH_TIME);
                        refreshSSOToken();
                    }
                    mLoginState.setValue(EnumConstant.LoginState.ZLA_SUCCESS);
                } else {
                    mLoginState.setValue(EnumConstant.LoginState.ZLA_FAILURE);
                }
            }


                    @Override
            public void onFailure(@NonNull Call<ZLAResponseModel> call, @NonNull Throwable t) {
                Log.d(TAG, "onFailure: callZLAApi");
                mLoginState.setValue(EnumConstant.LoginState.ZLA_FAILURE);
            }
        });
        // return ret; FIXME enable this for production
        return true;
    }
    public void callUserSni() {
        Log.d(TAG, "START -SNI API");
        Call<UserSniResponseModel> call = RESTApiManager.getInstance().callUserSni();
        call.enqueue(new Callback<UserSniResponseModel>() {
            @Override
            public void onResponse(@NonNull Call<UserSniResponseModel> call,
                                   @NonNull Response<UserSniResponseModel> response) {
                int code = response.code();
                Log.d(TAG, "SNI API  response HTTP Code: " + code);
                if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
                    if (response.body() != null) {
                        Log.d(TAG, "SNI API Response Body :" + response.body().toString());
                        String portNumber = response.body().getPort();
                        if (BuildConfig.BUILD_TYPE.equals("sit")) {
                            if (TextUtils.isEmpty(portNumber)) {
                                Log.d(TAG, "SNI API PORT NUMBER NOT FOUND");
                                Toast.makeText(getApplication(), getApplication().getResources().getString(R.string.connecting_error_msg), Toast.LENGTH_SHORT).show();
                                mLoginState.setValue(EnumConstant.LoginState.USER_SNI_FAILURE);
                            } else {
                                saveSniInfo(response);
                                mLoginState.setValue(EnumConstant.LoginState.USER_SNI_SUCCESS);
                            }
                        } else {
                            saveSniInfo(response);
                            mLoginState.setValue(EnumConstant.LoginState.USER_SNI_SUCCESS);
                        }
                    }
                } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    if (isUserNotFound(response)) {
                        Log.d(TAG, "SNI API USER NOT FOUND ");
                        mLoginState.setValue(EnumConstant.LoginState.USER_SNI_FAILURE);
                        Toast.makeText(getApplication(), getApplication().getResources().getString(R.string.phone_number_not_found), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mLoginState.setValue(EnumConstant.LoginState.USER_SNI_FAILURE);
                }
            }

            @Override
            public void onFailure(Call<UserSniResponseModel> call, Throwable t) {
                Log.d(TAG, "SNI API onFailure: " + t.getMessage());
            }
        });

    }
    private  void saveSniInfo(Response<UserSniResponseModel> response){
        // Check port number present or not in response - START
        int port = -1;
        String portNumber = response.body().getPort();
        if (!TextUtils.isEmpty(portNumber)) {
            port = Integer.parseInt(portNumber);
        }
        // Save port number in shared preference for further use - END
        getSettings().setServerPort(port);
        // Format: <Enterprise_ID>-<docker_Ext-port>.jio.com
        String sni = response.body().getHash() + ".com";
        getSettings().setServerSni(sni);
    }
    private boolean isUserNotFound(Response<UserSniResponseModel> response) {
        boolean isNotFound = false;
        if (response.errorBody() != null) {
            try {
                String errorMsg = response.errorBody().string();
                if (!TextUtils.isEmpty(errorMsg) && errorMsg.toLowerCase().contains("user not found")) {
                    isNotFound = true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return isNotFound;
    }
    private boolean bindProcessToCellularDataNetwork() {
        boolean ret = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ConnectivityManager cm = (ConnectivityManager) getApplication().getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            Network[] networks  = cm.getAllNetworks();

            Log.d(TAG, "Fetch all networks. Number of networks found: " + networks.length);
            for (int i = 0; i < networks.length; i++) {
                NetworkInfo netInfo = cm.getNetworkInfo(networks[i]);
                NetworkCapabilities nc = cm.getNetworkCapabilities(networks[i]);
                assert netInfo != null;
                assert nc != null;
                Log.d(TAG, "--------- network " + i + " -----------");
                if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    Log.d(TAG, "CELLULAR network: type: " + netInfo.getTypeName() + " extraInfo: " + netInfo.getExtraInfo()
                            + " subtype: " + netInfo.getSubtypeName() + " available: " + netInfo.isAvailable()
                            + " state: " + netInfo.getState() + " netid: " + networks[i].toString());

                    // Check if network has INTERNET capability & it is VALIDATED
                    if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        Log.d(TAG, "TRANSPORT_CELLULAR: " + nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
                        Log.d(TAG, "TRANSPORT_WIFI: " + nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                        Log.d(TAG, "NET_CAPABILITY_INTERNET: " + nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
                        Log.d(TAG, "NET_CAPABILITY_VALIDATED: " + nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
                        //Log.d(TAG,"Network has INTERNET capability and VALIDATED");

                        ret = cm.bindProcessToNetwork(networks[i]);

                        Log.d(TAG, "Bind process to CELLULAR network: " + ret);
                        Log.d(TAG, "--------- exiting loop as we found the network to bind -----------");
                        break; // No need to check remaining networks
                    } else {
                        Log.d(TAG, "Network does not have INTERNET capability and/or it is not VALIDATED");
                    }
                } else {
                    Log.d(TAG, "NON CELLULAR network: type: " + netInfo.getTypeName() + " extraInfo: " + netInfo.getExtraInfo()
                            + " subtype: " + netInfo.getSubtypeName() + " available: " + netInfo.isAvailable()
                            + " state: " + netInfo.getState() + " netid: " + networks[i].toString());
                }
                Log.d(TAG, "TRANSPORT_CELLULAR: " + nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
                Log.d(TAG, "TRANSPORT_WIFI: " + nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                Log.d(TAG, "NET_CAPABILITY_INTERNET: " + nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
                Log.d(TAG, "NET_CAPABILITY_VALIDATED: " + nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            }
        }

        return ret;
    }

    public String decrypt_msisdn(String encrypted_msisdn, String sso_token) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if(encrypted_msisdn!=null)
        {
            String enc_key= BuildConfig.MSISDN_DECRYPT_KEY;
            String token= sso_token.substring(sso_token.length()-16);
            String pwd=enc_key.substring(0,16);
            byte[] bufA = pwd.getBytes(StandardCharsets.US_ASCII);
            byte[] bufB = token.getBytes(StandardCharsets.US_ASCII);
            byte[] xorOutput = new byte[bufA.length];
            for (int i = 0; i < bufA.length; i++) {
                xorOutput[i] = (byte) (bufA[i] ^ bufB[i]);
            }

            try {
                Cipher decipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec key = new SecretKeySpec(xorOutput, "AES");
                IvParameterSpec iv = new IvParameterSpec(xorOutput);
                decipher.init(Cipher.DECRYPT_MODE, key, iv);
                byte[] decryptedBytes = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    decryptedBytes = decipher.doFinal(Base64.getDecoder().decode(encrypted_msisdn));
                }
                return new String(decryptedBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void updateMessageStatusInDB(int msgStatus, String msgId, String receiverDisplayedList, String receiverDeliveredList){
        mDatabase.updateJioTalkieChatStatus(msgStatus,msgId,receiverDisplayedList,receiverDeliveredList);
    }

    public void resetMediaUploadStateObserver() {
        mMediaUploadState.setValue(new MediaUploadResponse("",
                "", false,"",0,""));
    }

    public boolean refreshSSOToken() {
        boolean tokenExpired;

        // read SSO expiry from shared pref
        long ssoExpiry = mJioTalkieSettings.getSsotokenExpiry();
        Log.d(TAG, "refreshSSOToken: ssoExpiry: "
                + DateFormat.format("dd/MM/yyyy hh:mm:ss", ssoExpiry).toString());

        // check if SSO token has expired
        if (System.currentTimeMillis() >= ssoExpiry) {
            tokenExpired = true;
            // SSO token expired. Do 1st time login.
            Log.d(TAG, "refreshSSOToken: SSO Token Expired. Do 1st time login.");
            // String jtoken = mSettings.getJToken();
            // callAuthtokenVerifyApi(jtoken);
        } else {
            tokenExpired = false;
            // SSO token not expired. Refresh in future.
            Log.d(TAG, "refreshSSOToken: SSO Token not expired yet. Schedule refresh.");
            long timer = ssoExpiry - System.currentTimeMillis();
            //Log.d(TAG, "refreshSSOToken: SSO Token refresh request will be sent after: "
            //        + timer / 1000 + " seconds");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "refreshSSOToken: Running Handler: Refresh SSO Token.");
                    String jtoken = mJioTalkieSettings.getJToken();
                    callAuthtokenVerifyApi(jtoken);
                }
            }, timer);
        }
        return tokenExpired;
    }

    private AuthtokenVerifyModel getAuthtokenVerifyModel(String token) {
        AuthtokenVerifyModel avm = new AuthtokenVerifyModel();

        AuthtokenVerifyModel.DeviceInfo1 deviceInfo = avm.getDeviceInfo1();
        deviceInfo.setToken(token);

        // hard coded values
        deviceInfo.setConsumptionDeviceName(Build.MODEL);
        //device info -> info
        Info info = deviceInfo.getInfo();
        info.setType("android");
        //device info -> info -> platform
        Platform platform = info.getPlatform();
        platform.setName("androidos");
        platform.setVersion("");
        //device info -> info -> build
        BuildInfo build = info.getBuild();
        build.setBoard("");
        build.setCpuAbi("");
        build.setDevice("");
        build.setHost("");
        build.setManufacturer("");
        build.setModel("");
        build.setProduct("");
        build.setType("");
        build.setUser("");
        build.setFingerprint("");
        build.setBootloader("");
        build.setSerial("");
        // device info -> info
        info.setImei("");
        info.setImsi("");
        info.setAndroidId(mAndroidId);
        info.setMac("");
        info.setBluetoothAddress("");
        info.setLatitude("");
        info.setLongitude("");

        return avm;
    }

    public void downloadMessagesWithPagination(MessageRequestModel messageRequestModel) {
        Call<MessageResponseModel> call = RESTApiManager.getInstance().fetchMessagesData(messageRequestModel);
        call.enqueue(new Callback<MessageResponseModel>() {
            @Override
            public void onResponse(Call<MessageResponseModel> call, Response<MessageResponseModel> response) {
                if (response.code() == 200) {
                    MessageResponseModel messageResponse = response.body();
                    assert messageResponse != null;
                    List<MessageListResponseModel> messageList = messageResponse.getData();
                    if (messageList != null && !messageList.isEmpty()) {
                        Log.d(TAG, "Total message count = " + messageList.size() + " isLast " +messageResponse.getLast());
                        messageListDownloadThread(messageList, null, false, messageRequestModel, messageResponse.getLast());
                    }
                } else {
                    Log.d(TAG, "Different Response Code = " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MessageResponseModel> call, Throwable t) {
                if (t instanceof ConnectException)
                    Toast.makeText(getApplication(), "ConnectException: Rest Server Not Reachable", Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    public void callAuthtokenVerifyApi(String jtoken) {
        if (jtoken != null) {
            Log.d(TAG,"callAuthtokenVerifyApi: jtoken: " + jtoken.substring(0,3) + "...");
        }
        AuthtokenVerifyModel avm = getAuthtokenVerifyModel(jtoken);
        Call<OtpResponseModel> call = RetrofitClient.getRetrofitClient(RetrofitClient.BaseUrlType.LoginUrlHttps).callAuthtokenVerifyApi(
                BuildConfig.LOGIN_API_KEY, BuildConfig.LOGIN_APP_NAME, avm);
        call.enqueue(new Callback<OtpResponseModel>() {
            @Override
            public void onResponse(@NonNull Call<OtpResponseModel> call,
                                   @NonNull Response<OtpResponseModel> response) {
                int code = response.code();
                Log.d(TAG, "onResponse: callAuthtokenVerifyApi: code: " + code);
                Log.d(TAG, "onResponse: callAuthtokenVerifyApi: body: " + response.body());
                if (code >= 200 && code < 300) {
                    if (response.body() != null) {
                        Log.d(TAG, "onResponse: callAuthtokenVerifyApi: ssoToken: "
                                + response.body().getSsoToken().substring(0,3) + "...");
                        // Store SSO Token in shared pref
                        mJioTalkieSettings.setSsoToken(response.body().getSsoToken());
                        // TODO fetch Expiry time from server response
                        long currentTime = System.currentTimeMillis();
                        mJioTalkieSettings.setSsotokenExpiry(currentTime + EnumConstant.SSO_TOKEN_REFRESH_TIME);
                        refreshSSOToken();
                    }
                    mLoginState.setValue(EnumConstant.LoginState.AUTHTOKEN_VERIFY_SUCCESS);
                    // This is to allow observer to be triggered if we set same value back to back.
                    // We set AUTHTOKEN_VERIFY_SUCCESS back to back when we do refresh token periodically.
                    mLoginState.setValue(EnumConstant.LoginState.DUMMY);
                } else {
                    mLoginState.setValue(EnumConstant.LoginState.AUTHTOKEN_VERIFY_FAILURE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OtpResponseModel> call, @NonNull Throwable t) {
                Log.d(TAG, "onFailure: callAuthtokenVerifyApi called");
                mLoginState.setValue(EnumConstant.LoginState.AUTHTOKEN_VERIFY_FAILURE);
            }
        });
    }

    public void setAndroidId(String id) {
        mAndroidId = id;
    }

    public void resetVideoDownloadObserver(){
        mDownloadVideoState.setValue(new VideoDownloadState(-1,"", false,false));
    }
    public void resetAudioDownloadObserver(){
        mDownloadAudioState.setValue(new AudioDownloadState(-1,"", false,false));
    }
    public void resetDocumentDownloadObserver(){
        mDownloadDocumentState.setValue(new DocumentDownloadState(-1,"", false,false));
    }
    public int getTotalRegUserCount() {
        return RegUserList != null ? RegUserList.size() : 0;
    }
    public void setUserId(int userId){
        mUserId = userId;
    }
    public int getUserId(){
        return  mUserId;
    }

    public void setChannelId(int channelId){
        mChannelId = channelId;
    }
    public int getChannelId(){
        return mChannelId;
    }

    public void setUpdateApkCheckTimer() {
        Log.d(TAG, "updateApk:called setUpdateApkCheckTimer()");
        long currentTime = System.currentTimeMillis();
        mJioTalkieSettings.setUpdateAvailableCheck(currentTime + EnumConstant.SET_UPDATE_AVAILABLE_API_TIME);
    }

    public void updateApkCheckTimer() {
        long currentTime = System.currentTimeMillis();
        long  updateTime = getUpdateAvailableCheck();
        long difference = updateTime-currentTime;
        long hours = difference / (60 * 60 * 1000) % 24;
        Log.d(TAG,"updateApk:called updateApkCheckTimer():currentTime:" + DateFormat.format("dd/MM/yyyy hh:mm:ss",currentTime).toString() +
                ",updateTime:" + DateFormat.format("dd/MM/yyyy hh:mm:ss",updateTime).toString() + ", hours left for check update:" + hours);
        if (hours == -1 || hours == 0) {
            mJioTalkieSettings.setApkUpdateTimeExpired(true);
            mJioTalkieSettings.setUpdateAvailableCheck(currentTime + EnumConstant.SET_UPDATE_AVAILABLE_API_TIME);
        } else {
            mJioTalkieSettings.setApkUpdateTimeExpired(false);
        }
    }
    public long getUpdateAvailableCheck() {
        return mJioTalkieSettings.getUpdateAvailableCheck();
    }

    public boolean isJioTalkieServiceActive() {
        return getJioTalkieService().isPttConnectionActive()
                && getJioTalkieService().isPttConnectionActive()
                && getJioTalkieService().getJioPttSession() != null;
    }

    public int deleteJioTalkieByMsgId(String msgId) {
        return mDatabase.deleteJioTalkieByMsgId(msgId);
    }

    public LiveData<UCropFragment.UCropResult> getImageEditResult() {
        return this.mImageEditResult;
    }

    public void setImageEditResult(UCropFragment.UCropResult mImageEditResult) {
        this.mImageEditResult.setValue(mImageEditResult);
    }
}

