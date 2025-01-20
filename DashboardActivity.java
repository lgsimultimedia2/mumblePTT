package com.jio.jiotalkie.activity;


import static android.view.View.GONE;
import static com.jio.jiotalkie.util.Constants.BATTERY_STATUS;
import static com.jio.jiotalkie.util.Constants.HIGH_BATTERY_THRESHOLD;
import static com.jio.jiotalkie.util.Constants.LOW_BATTERY_THRESHOLD;
import static com.jio.jiotalkie.util.Constants.LTE_SIGNAL_THRESHOLD;
import static com.jio.jiotalkie.util.Constants.LTE_STATUS;
import static com.jio.jiotalkie.util.Constants.WIFI_SIGNAL_THRESHOLD;
import static com.jio.jiotalkie.util.Constants.WIFI_STATUS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.SimpleDateFormat;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.ParseException;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.application.customservice.common.JioPttEnums;
import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.exception.JioTalkieException;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.WorkManger.BackgroundTaskScheduler;
import com.jio.jiotalkie.adapter.CalenderListAdapter;
import com.jio.jiotalkie.adapter.provider.ConnectionStateProvider;
import com.jio.jiotalkie.dataclass.CurrentSpeakerState;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.drawable.CustomCircleView;
import com.jio.jiotalkie.drawable.decorator.CustomDayDecorator;
import com.jio.jiotalkie.drawable.decorator.DisableAfterTodayDecorator;
import com.jio.jiotalkie.drawable.decorator.SelectedDateDecorator;
import com.jio.jiotalkie.drawable.decorator.TodayDecorator;
import com.jio.jiotalkie.fragment.AboutAppFragment;
import com.jio.jiotalkie.fragment.AddUserFragment;
import com.jio.jiotalkie.fragment.BillingFragment;
import com.jio.jiotalkie.fragment.DispatcherActiveChannelFragment;
import com.jio.jiotalkie.fragment.DispatcherHomeFragment;
import com.jio.jiotalkie.fragment.DispatcherLocationFragment;
import com.jio.jiotalkie.fragment.DispatcherRoleFragment;
import com.jio.jiotalkie.fragment.DispatcherStatusFragment;
import com.jio.jiotalkie.fragment.GeoFenceFragment;
import com.jio.jiotalkie.fragment.GroupChatFragment;
import com.jio.jiotalkie.fragment.HelpFragment;
import com.jio.jiotalkie.fragment.ImageEditFragment;
import com.jio.jiotalkie.fragment.LocationHistoryFragment;
import com.jio.jiotalkie.fragment.LocationTimelineFragment;
import com.jio.jiotalkie.fragment.MarkAttendanceFragment;
import com.jio.jiotalkie.fragment.MediaPlayerFragment;
import com.jio.jiotalkie.fragment.MessageDeliveryStatusDialogFragment;
import com.jio.jiotalkie.fragment.MultipleMessageFragment;
import com.jio.jiotalkie.fragment.PersonalChatFragment;
import com.jio.jiotalkie.fragment.PhoneLoginFragment;
import com.jio.jiotalkie.fragment.PrivacyPolicyFragment;
import com.jio.jiotalkie.fragment.ProfileFragment;
import com.jio.jiotalkie.fragment.SOSBottomSheetDialogFragment;
import com.jio.jiotalkie.fragment.SOSChannelFragment;
import com.jio.jiotalkie.fragment.SettingAccountFragment;
import com.jio.jiotalkie.fragment.SubChannelFragment;
import com.jio.jiotalkie.fragment.SubChannelListFragment;
import com.jio.jiotalkie.fragment.UserMapLocation;
import com.jio.jiotalkie.interfaces.JioTalkieServiceInterface;
import com.jio.jiotalkie.model.CalendarItem;
import com.jio.jiotalkie.model.JioTalkieChats;
import com.jio.jiotalkie.model.api.ApkResponseModel;
import com.jio.jiotalkie.model.api.MessageRequestModel;
import com.jio.jiotalkie.network.RESTApiManager;
import com.jio.jiotalkie.performance.AdvancedPerformanceTracker;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.service.JioTalkieService;
import com.jio.jiotalkie.service.ipc.IService2;
import com.jio.jiotalkie.util.ADCInfoUtils;
import com.jio.jiotalkie.util.BitmapUtils;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.DateUtils;
import com.jio.jiotalkie.util.DeviceInfoUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.util.GeoFenceHelper;
import com.jio.jiotalkie.util.GpsUtils;
import com.jio.jiotalkie.util.MessageEngine;
import com.jio.jiotalkie.util.MessageIdUtils;
import com.jio.jiotalkie.util.ServerConstant;
import com.jio.jiotalkie.util.UserPinnedStateHandler;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.format.WeekDayFormatter;
import com.yalantis.ucrop.UCropFragment;
import com.yalantis.ucrop.UCropFragmentCallback;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.TextStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import com.application.customservice.dataManagment.models.Server;
import com.application.customservice.Mumble;

@TrackPerformance(threshold = 500, critical = true)
public class DashboardActivity extends AppCompatActivity implements CustomCircleView.CustomViewInterface, SensorEventListener, UCropFragmentCallback {
    private static final String TAG = DashboardActivity.class.getName();
    private final int REQUEST_CODE_MENU_ACTIVITY = 1001;

    private final boolean FLAG_LB = true; // use load balancer?

    private JioTalkieServiceInterface jioTalkieInterface ;
    private DashboardViewModel mViewModel;
    private boolean isCheckUpdate = false;
    private byte[] textureData;
    private Bitmap profilepic;

    private AudioManager audioManager;
    private int mAudioVolumeLevel;
    private Toolbar mToolbar;
    private ImageView mSOSFloatingBtn, mSOSDuringPtt;
    private FrameLayout mMainContainer;
    private byte[] mCertificate = null;
    private boolean isActivityCreatedCalled = false;

    BackgroundTaskScheduler mBackgroundTaskSchedular = null;
    private JioTalkieSettings mJioTalkieSettings;
    private boolean isGPS = false;
    private BottomNavigationView bottomNavigationView;
    private RelativeLayout mChannelDetailsView;
    private TextView mChannelName;
    private TextView mOnlineUserCount;
    private ImageView pushToTalk;

    private View mBaseToolLayout;
    private View mChannelToolLayout;

    private View mSearchToolLayout;
    private TextView mChannelNameTV;
    // Tools bar action items
    private ImageView actionBack;
    private ImageView actionHome;
    private ImageView actionSearch;
    private ImageView actionCalender;
    private ImageView actionProfile;
    private ImageView actionMenu;
    private TextView actionTitle;
    private TextView actionSubTitle;
    private ImageView actionCross;
    private ImageView actionPlus;
    private ImageView actionSaveImage;

    private EditText actionSearchQuery;

    private Fragment mLoadFragment;
    private Bundle mBundle;
    private RelativeLayout pttContainer;
    private FrameLayout pttIndicatorView;
    private TextView mTimer;
    private CustomCircleView customCircleView;
    private String userState = EnumConstant.pttAnimationState.USER_STATE_SPEAKER.name();
    private final Handler handler = new Handler();
    private int mMaxTime = 60;
    private int timeInSeconds;
    private String mReceiverUser;
    private Context mContext;
    private boolean isPushToTalkRunning;

    private boolean isNeedToShowSOSView = false;
    private int mCurrentSOSSpeakerId = -1;
    private boolean isDispatcherLoggedOut = false;
    private TelephonyManager telephonyManager;

    private PhoneStateListener mPhoneStateListener;
    private TelephonyCallbackListener telephonyCallback;

    private List<ConnectionStateProvider> mConnectionStateListners;

    private boolean isPTTCallGoing;

    private MaterialCalendarView calendarView;

    private CalendarDay selectedDate=CalendarDay.today();

    private TextView selectedMonth;

    private TextView selectedYear;

    private LinearLayout monthLayout;

    private LinearLayout yearLayout;

    private List<CalendarItem> months;

    private List<CalendarItem> years;

    private Dialog calendarDialog;
    private AlertDialog mNoNetworkDialog;

    private CalenderListAdapter calListAdapter;

    private long chatDurationFrom;
    private String mCallId = "";

    private TextView selectedDateTextView;
    private LiveData<List<JioTalkieChats>> mGroupChatObserver = null;
    private SOSBottomSheetDialogFragment sosBottomSheetDialogFragment;
    private IService2 myBoundService;

    private int mCurrentVersion = BuildConfig.VERSION_CODE;
    private AlertDialog progressDialog;
    private boolean isBound = false;

    private int mDensityMetric;

    private boolean isLayoutSet=false;

    private int lteSignalStrengthDbm = 0;

    private int lteSignalStrengthAsu = 0;

    private int mBatteryLevel=0;

    private int wifiSignalStrengthDbm=0;

    private SensorManager sensorManager;

    private Sensor lightSensor;
    private ImageView actionDelete;
    private ImageView actionInfo;
    private ImageView actionCopy;
    ClipboardManager clipboard;

    private float getBrightnessLevel(float lux) {
        if (lux <= 500) {
            return 0.25f; // dim
        } else if (lux <= 1000) {
            return 0.65f; // bright
        } else {
            return 1.0f; // very bright
        }
    }

    private void setAppBrightness(float brightnessValue) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = brightnessValue;
        getWindow().setAttributes(layoutParams);
    }
    private float getCurrentAppBrightness() {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        return layoutParams.screenBrightness;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Check user enable power saver toggle or not
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT && mViewModel.getSettings().isPowerSaverEnable()) {
            float ambientLightLux = sensorEvent.values[0];
            float newBrightnessValue = getBrightnessLevel(ambientLightLux);
            if (newBrightnessValue != getCurrentAppBrightness()) {
                setAppBrightness(newBrightnessValue);
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void loadingProgress(boolean showLoader) {
    }

    @Override
    public void onCropFinish(UCropFragment.UCropResult result) {
        mViewModel.setImageEditResult(result);
        handleOnBackPress();
    }



    public interface SearchQueryCallBack {
        void onTextChanged(String queryText, boolean isSearchVisible);
    }

    private SearchQueryCallBack mSearchQueryCallBack;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            IService2.LocalBinder binder = (IService2.LocalBinder) service;
            myBoundService = binder.getService();
            isBound = true;

            // Set the activity callback
            myBoundService.setActivityCallback(new IService2.ActivityCallback() {
                @Override
                public void onEventTriggered() {
                    // TODO : Disable Fall Device detection feature for release 9 JAN 2025
                    // MessageEngine.getInstance().msgToChannel(getString(R.string.emergency_msg));
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    // Not using , in new Implementation.

//    private ServiceConnection mServiceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            Log.d(TAG, " onServiceConnected() called");
//            jioTalkieInterface = ((JioTalkieService.JioTalkieServiceBinder) service).getService();
//
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            Log.d(TAG, "onServiceDisconnected: called ");
//            jioTalkieInterface = null;
//            mViewModel.initServiceInstance(null);
//        }
//    };

    private ConnectivityManager mConnectivityManager = null;
    private boolean isNetWorkDisconnected = false;
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            if (mNoNetworkDialog != null && mNoNetworkDialog.isShowing()) {
                mNoNetworkDialog.dismiss();
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isNetWorkDisconnected) {
                    if (jioTalkieInterface.isBoundToPttServer()) {
                        jioTalkieInterface.closePttConnection();
                        String msisdn = mViewModel.getSettings().getMSISDN();
                        String ssoToken = mViewModel.getSettings().getSsoToken();
                        String serverIp = ServerConstant.getMumbleServer();
                        String sni = mViewModel.getSettings().getServerSni();
                        Log.d("SNI","sni::"+sni);
                        int port = mViewModel.getSettings().getServerPort();
                        Toast.makeText(getApplicationContext(), getString(R.string.connecting_again_msg), Toast.LENGTH_SHORT).show();
                        mViewModel.callServerConnectionApi(new Server(-1, serverIp, port, msisdn, ssoToken,sni));
                    }
                    isNetWorkDisconnected = false;
                }
            });
        }

        @Override
        public void onLost(@NonNull Network network) {
            if (sosBottomSheetDialogFragment != null && sosBottomSheetDialogFragment.isVisible()) {
                sosBottomSheetDialogFragment.dismiss();
            }
            if (getVisibleFragment() instanceof ProfileFragment) {
                ((ProfileFragment) getVisibleFragment()).dismissPopUp();
            }

            if (getVisibleFragment() instanceof SubChannelFragment) {
                ((SubChannelFragment) getVisibleFragment()).dismissPopUp();
            }

            isNetWorkDisconnected = true;
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getApplicationContext(), getString(R.string.disconnecting_msg), Toast.LENGTH_SHORT).show();
                SettingAccountFragment fragment = (SettingAccountFragment) getSupportFragmentManager().findFragmentByTag(SettingAccountFragment.class.getName());
                if (fragment != null && fragment.isVisible()) {
                    return;
                }
                loadInnerFragment(EnumConstant.getSupportedFragment.SETUP_ACCOUNT_FRAGMENT.ordinal());
            });
        }
    };
    private final Handler mIdleHandler = new Handler(Looper.getMainLooper());
    private static final long IDLE_TIME = 30 * 60 * 1000; // 30 minutes
    private final Runnable mIdleRunnable = () -> {
        // Show Toast message after 30 minutes of inactivity
        Log.d(TAG, "App is idle for 30 minutes");
        MessageEngine.getInstance().msgToChannel(getString(R.string.idle_msg));
        // Timer reset and send message after time interval.
        resetIdleTimer();
    };

    private void initJioTalkieInterface(){
        jioTalkieInterface = new JioTalkieService(getApplicationContext());
        jioTalkieInterface.addObserver(mViewModel.jioPttObserver);
        jioTalkieInterface.setSuppressNotifications(true);
        jioTalkieInterface.clearChatNotifications(); // Clear chat notifications on resume.
        mViewModel.initServiceInstance(jioTalkieInterface);
        jioTalkieInterface.setBoundActivity(DashboardActivity.this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                AdvancedPerformanceTracker.getInstance().getFragmentLifecycleCallbacks(),
                true
        );
        mViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        initJioTalkieInterface();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = new TelephonyCallbackListener();
            telephonyManager.registerTelephonyCallback(Executors.newSingleThreadExecutor(), telephonyCallback);
        }
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                try {
                    if (signalStrength.isGsm()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            List<CellSignalStrength> signalStrengths = signalStrength.getCellSignalStrengths();
                            for(CellSignalStrength cellSignalStrength : signalStrengths){
                                if(cellSignalStrength instanceof CellSignalStrengthLte){
                                    lteSignalStrengthDbm = ((CellSignalStrengthLte) cellSignalStrength).getDbm();
                                    lteSignalStrengthAsu = ((CellSignalStrengthLte) cellSignalStrength).getAsuLevel();
                                    break;
                                }else if(cellSignalStrength instanceof CellSignalStrengthNr){
                                    lteSignalStrengthDbm = ((CellSignalStrengthNr) cellSignalStrength).getDbm();
                                    lteSignalStrengthAsu = ((CellSignalStrengthNr) cellSignalStrength).getAsuLevel();
                                    break;
                                }
                            }
                            boolean isLTESignalStatusSent = mViewModel.getSettings().getLTESignalStatusSent();
                            Log.d(TAG, "lte signal" + lteSignalStrengthDbm + " message sent " + isLTESignalStatusSent);
                            if (lteSignalStrengthDbm > LTE_SIGNAL_THRESHOLD && isLTESignalStatusSent) {
                                mViewModel.getSettings().setLTESignalStatusSent(false);
                            }
                            if (lteSignalStrengthDbm < LTE_SIGNAL_THRESHOLD && !isLTESignalStatusSent
                                    && (CommonUtils.getNetworkType(mContext) == NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                if (mViewModel.isJioTalkieServiceActive()) {
                                    Log.d(TAG, "LTE threshold reached, alert message lte " + lteSignalStrengthDbm);
                                    Toast.makeText(mContext, getString(R.string.lte_low_signal_status), Toast.LENGTH_LONG).show();
                                    mViewModel.getSettings().setLTESignalStatusSent(true);
                                    emergencyAlert(LTE_STATUS);
                                }
                            }
                        }
                    }
                }catch (Exception e){
                    Log.e(TAG,"Exception in LTE signal strength"+Log.getStackTraceString(e));
                }
            }
        };
        isActivityCreatedCalled = true;
        mViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        mContext = this;
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        mDensityMetric = metrics.densityDpi;
        mViewModel.init();
        mConnectionStateListners = new ArrayList<>();
        initView();
        registerConnectionStateObserver();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(mViewModel.getSettings().isHandheldMode() ?
                AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC);
        if (mViewModel.getSettings().isFirstLaunch()) {
            showFirstRunGuide();
        }
        calListAdapter = new CalenderListAdapter();
        DeviceInfoUtils.getLocation(this);
        UserPinnedStateHandler.getInstance().init(this);
        registerViewModelObserver();
        mJioTalkieSettings = JioTalkieSettings.getInstance(this.getApplication());
        String androidId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        Log.d(TAG, "AndroidID: " + androidId);
        mViewModel.setAndroidId(androidId);

        // Login flow
        // 1. SSO Token not expired
        //      - Call TCP Authenticate using existing token
        // 2. SSO Token not available OR SSO Token expired
        //      - Consider 1st time login. Check for JIO SIM, Call ZLA REST API, followed by TCP Authenticate.
        // Note: FIXME At present we are showing login screen for 1st time login which will be removed later
        String ssoToken = mViewModel.getSettings().getSsoToken();
        String encryptedMsisdn = mViewModel.getSettings().getEncryptedMsisdn();
        if (!ssoToken.isEmpty() && !mViewModel.refreshSSOToken()) {
            // SSO token has not expired, call login API with existing SSO token
            // Store sso token and Msisdn in RESTAPI manager for REST API .
            RESTApiManager.getInstance().setUp(this, encryptedMsisdn, ssoToken);

            loadInnerFragment(EnumConstant.getSupportedFragment.SETUP_ACCOUNT_FRAGMENT.ordinal());
            String msisdn = mViewModel.getSettings().getMSISDN();
            String sni = mViewModel.getSettings().getServerSni();
            int port = mViewModel.getSettings().getServerPort();
            if (!msisdn.isEmpty()) {
                Log.d(TAG, "Login using existing sso token: " + ssoToken.substring(0, 3) + "..."
                        + " msisdn: " + msisdn.substring(0, 3) + "...");
            }
            mViewModel.callServerConnectionApi(new Server(-1, ServerConstant.getMumbleServer(),
                    port, msisdn, ssoToken, sni));
        } else {
            // 1st time login flow
            // Check if user has JIO SIM before proceeding with authentication.
            //if (deviceInfoUtils.isJioSIM()) { // FIXME temporarily commented to allow login w/o JIO SIM
            // callZLAApi(); // FIXME in future we need to call ZLA API directly w/o showing login screen
            loadInnerFragment(EnumConstant.getSupportedFragment.PHONE_LOGIN_FRAGMENT.ordinal());
            //} else {
            // Cannot proceed with authentication.
            //    Toast.makeText(this,"JIO SIM not detected, cannot authenticate",
            //            Toast.LENGTH_SHORT).show();
            //}
        }
        setProfilePic();
        Intent intent = new Intent(DashboardActivity.this, IService2.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "updateApk:getApkUpdateTimeExpired" + mViewModel.getSettings().getApkUpdateTimeExpired());
        if (mViewModel.getSettings().getApkUpdateTimeExpired()) mViewModel.checkLatestVersion();
        long apkExpiredTime = EnumConstant.SET_UPDATE_AVAILABLE_API_TIME;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mViewModel.checkLatestVersion();
            }
        }, apkExpiredTime);
        registerReceivers();
        MessageEngine.getInstance().init(mViewModel);
    }

    public void muteStreamAudioVolume(){
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
    }

    public void resetStreamAudioVolume(){
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        Log.d(TAG, "onUserInteraction performed ");
        resetIdleTimer();
    }
    // Reset the timer for idle state
    private void resetIdleTimer() {
        // Remove any pending posts of idleRunnable and post it again to run after IDLE_TIME
        mIdleHandler.removeCallbacks(mIdleRunnable);
        mIdleHandler.postDelayed(mIdleRunnable, IDLE_TIME);
    }

    private void emergencyAlert(int mode) {
        if (!mViewModel.getJioTalkieService().isBoundToPttServer()) {
            Log.d(TAG, "getService()==null in emergencyAlert");
            return;
        }
        String message = "";
        if (mode == BATTERY_STATUS) {
            message = getString(R.string.battery_low_msg, mBatteryLevel);
        } else if (mode == LTE_STATUS) {
            message = getString(R.string.lte_signal_msg,lteSignalStrengthDbm);
        } else if (mode == WIFI_STATUS) {
            message = getString(R.string.wifi_signal_msg, wifiSignalStrengthDbm);
        }
        // Send Message to group
        MessageEngine.getInstance().msgToChannel(message);
        MessageEngine.getInstance().msgToCompanyAdmin(message);
    }

    private void registerReceivers() {
        //Battery
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryLevelReceiver, filter);
        //LTE
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        //WIFI
        IntentFilter wifiFilter = new IntentFilter(WifiManager.RSSI_CHANGED_ACTION);
        registerReceiver(wifiSignalReceiver, wifiFilter);
    }

    private void unRegisterReceivers(){
        unregisterReceiver(batteryLevelReceiver);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(wifiSignalReceiver);
    }

    BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            boolean isBatteryStatusSent = mViewModel.getSettings().getBatteryStatusSent();
            if(mBatteryLevel>=HIGH_BATTERY_THRESHOLD && isBatteryStatusSent){
                mViewModel.getSettings().setBatteryStatusSent(false);
            }
            if(mBatteryLevel<=LOW_BATTERY_THRESHOLD && !isBatteryStatusSent){
                if(mViewModel.isJioTalkieServiceActive()) {
                    Log.d(TAG,"Battery threshold reached, alert message battery "+mBatteryLevel);
                    mViewModel.getSettings().setBatteryStatusSent(true);
                    emergencyAlert(BATTERY_STATUS);
                }
            }
        }
    };

    BroadcastReceiver wifiSignalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            boolean isWifiSignalStatusSent = mViewModel.getSettings().getWifiStatusSent();
            wifiSignalStrengthDbm = wifiInfo.getRssi();
            Log.d(TAG,"wifi signal"+wifiSignalStrengthDbm+" message sent "+isWifiSignalStatusSent);
            if (wifiSignalStrengthDbm > LTE_SIGNAL_THRESHOLD && isWifiSignalStatusSent) {
                mViewModel.getSettings().setWifiStatusSent(false);
            }
            if(wifiSignalStrengthDbm < WIFI_SIGNAL_THRESHOLD && (CommonUtils.getNetworkType(mContext)==NetworkCapabilities.TRANSPORT_WIFI)
                && !isWifiSignalStatusSent){
                if(mViewModel.isJioTalkieServiceActive()){
                    Log.d(TAG,"wifi threshold reached, alert message wifi "+wifiSignalStrengthDbm);
                    Toast.makeText(mContext,getString(R.string.wifi_low_signal_status),Toast.LENGTH_LONG).show();
                    mViewModel.getSettings().setWifiStatusSent(true);
                    emergencyAlert(WIFI_STATUS);
                }
            }
        }
    };

    public void setConnectionStateListeners(ConnectionStateProvider listener) {
        if (mConnectionStateListners != null) {
            mConnectionStateListners.add(listener);
        }
    }

    private void registerNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).
                addTransportType(NetworkCapabilities.TRANSPORT_WIFI).
                addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build();
        mConnectivityManager = (ConnectivityManager) getSystemService(ConnectivityManager.class);
        mConnectivityManager.registerNetworkCallback(networkRequest, mNetworkCallback);
    }

    private void unregisterNetworkCallback() {
        if (mConnectivityManager != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        }
    }

    private void registerViewModelObserver() {
        mViewModel.observeCertificateDataFromDB().observe(this, data -> {
            Log.d(TAG, "registerViewModelObserver: observeCertificateDataFromDB called");
            if (!data.isEmpty()) {
                mCertificate = data.get(0).getData();
                mViewModel.setCertificateData(mCertificate);
            }
        });

        mViewModel.observeLoginState().observe(this, loginState -> {
            Log.d(TAG, "registerViewModelObserver: observeLoginState: " + loginState.name());
            String msisdn;
            String ssoToken;
            Server server;
            switch (loginState) {
                case AUTHTOKEN_VERIFY_SUCCESS:
                    Log.d(TAG, "Fetching of new SSO token via REST API succeeded");
                    // Call Refresh SSO Token TCP message
                    mViewModel.getJioTalkieService().getJioPttSession().refreshToken(
                            mViewModel.getSettings().getSsoToken(),
                            mViewModel.getSettings().getMSISDN());
                    break;
                case AUTHTOKEN_VERIFY_FAILURE:
                    Log.d(TAG, "Fetching of new SSO token via REST API failed");
                    // FIXME need to kick out the user. Based on UX, we need to change screen
                    break;
                case ZLA_SUCCESS:
                    if (!FLAG_LB) {
                        Log.d(TAG, "ZLA REST API call succeeded");
                        msisdn = mViewModel.getSettings().getMSISDN();
                        ssoToken = mViewModel.getSettings().getSsoToken();
                        server = new Server(-1,
                                mViewModel.getSettings().getServerIp(),
                                mViewModel.getSettings().getServerPort(), msisdn, ssoToken);
                        Log.d("SNI", "ZLA_SUCCESS  ::serverIp:::" + mViewModel.getSettings().getServerIp() + " ::port::" + mViewModel.getSettings().getServerPort() + "::msisdn::" + msisdn + ":: ssoToken::" + ssoToken);
                        mViewModel.callServerConnectionApi(server);
                    } else {
                        Log.d("SNI", "mViewModel.callUserSni()");
                        mViewModel.callUserSni();
                    }
                    break;
                case ZLA_FAILURE:
                    Log.d(TAG, "ZLA REST API call failed");
                    Toast.makeText(mContext, "Please try again later. Unable to login.", Toast.LENGTH_LONG).show();
                    loadInnerFragment(EnumConstant.getSupportedFragment.PHONE_LOGIN_FRAGMENT.ordinal());
                    break;
                case USER_SNI_SUCCESS:

                    // Call server connection API
                    Server server1 = new Server(-1,
                            ServerConstant.getMumbleServer(),
                            mViewModel.getSettings().getServerPort(),
                            mViewModel.getSettings().getMSISDN(),
                            mViewModel.getSettings().getSsoToken(),
                            mViewModel.getSettings().getServerSni());
                    Log.d(TAG, "USER_SNI_SUCCESS  " +
                            "::serverIp::- " + ServerConstant.getMumbleServer() +
                            "::port::- " + mViewModel.getSettings().getServerPort() +
                            "::msisdn::- " + mViewModel.getSettings().getMSISDN() +
                            "::ssoToken::- " + mViewModel.getSettings().getSsoToken() +
                            "::sni::- " + mViewModel.getSettings().getServerSni());
                    mViewModel.callServerConnectionApi(server1);
                    break;
                case USER_SNI_FAILURE:
                    // Clear data & start from beginning
                    mViewModel.getSettings().setSsoToken("");
                    mViewModel.getSettings().setJToken("");
                    mViewModel.getSettings().setMSISDN("");
                    loadInnerFragment(EnumConstant.getSupportedFragment.PHONE_LOGIN_FRAGMENT.ordinal());
                    break;
            }
        });

        mViewModel.observeRegisterUserData().observe(this, registeredUsers -> {
            Log.d(TAG, "observeRegisterUserData no of users: " + registeredUsers.size());
            mViewModel.RegUserList.clear();
            mViewModel.RegUserList.addAll(registeredUsers);
        });


        mViewModel.observeSelfUserTalkState().observe(this,
                userTalkState -> {
                    if (userTalkState.isSelfUser()) {
                        switch (userTalkState.getUserTalkState()) {
                            case TALKING:
                                if(bottomNavigationView!=null){
                                    enableDisableBottomLayout(bottomNavigationView,true);
                                }
                                if (getVisibleFragment() instanceof PersonalChatFragment) {
                                    return;
                                }
                                isPTTCallGoing = true;
                                break;
                            case PASSIVE:
                                if(bottomNavigationView!=null){
                                    enableDisableBottomLayout(bottomNavigationView,false);
                                }
                                if (getVisibleFragment() instanceof PersonalChatFragment) {
                                    return;
                                }

                                isPTTCallGoing = false;
                                break;
                        }
                    } else {
                        switch (userTalkState.getUserTalkState()) {
                            case TALKING:
                                isPTTCallGoing = true;
                                mSOSDuringPtt.setVisibility(View.VISIBLE);
                                if(bottomNavigationView!=null){
                                    enableDisableBottomLayout(bottomNavigationView,true);
                                }
                                if ((getVisibleFragment() instanceof PhoneLoginFragment) || (getVisibleFragment() instanceof SettingAccountFragment))
                                    return;
                                mReceiverUser = userTalkState.getReceiverUserTalking();
                                Log.d("SOS", "mReceiverUser::" + mReceiverUser);
                                dismissCalendarDialog();
                                updateState(EnumConstant.pttAnimationState.USER_STATE_RECEIVER.name());
                                if (getVisibleFragment() instanceof PersonalChatFragment) {
                                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pttContainer.getLayoutParams();
                                    if(mDensityMetric >= DisplayMetrics.DENSITY_450) {
                                        params.bottomMargin = 170;
                                    }else if(mDensityMetric >= DisplayMetrics.DENSITY_400){
                                        params.bottomMargin = 150;
                                    }else if (mDensityMetric >= DisplayMetrics.DENSITY_XHIGH) {
                                        params.bottomMargin = 110;
                                    }else if (mDensityMetric >= DisplayMetrics.DENSITY_280){
                                        params.bottomMargin = 105;
                                    }
                                    pttContainer.setLayoutParams(params);
                                    isLayoutSet=true;
                                }
                                pttContainer.setVisibility(View.VISIBLE);
                                if (getVisibleFragment() instanceof DispatcherHomeFragment)
                                    needSOSButton(false);
                                break;
                            case PASSIVE:
                                stopReceivingPttCall(false);
                                break;
                        }
                    }
                });

        mViewModel.observeCurrentSpeakerStateData().observe(this, this::updatePTTSpeakerView);
        mViewModel.observeSOSStateLiveData().observe(this, sosState -> {
            Log.d(TAG, "observeSOSStateLiveData: getSosState(): " + sosState.getSosState() +
                    "isNeedToShowSOSView = " + isNeedToShowSOSView + " CurrentSpeakerId == UserID :: " + (mCurrentSOSSpeakerId == sosState.getUserID()));
            Log.d(TAG, "registerStateObserver: observerUserStateData called userState =" + sosState);
            switch (sosState.getSosState()) {
                case RECEIVER:
                    String batteryPercentage = String.valueOf(sosState.getBatteryPercentage());
                    String location = sosState.getLocation();
                    String signalStrength = sosState.getSignalStrength();
                    Log.d(TAG, "SOS Observer: battery percentage " + batteryPercentage + " location " + location + " signal " + signalStrength);
                    Log.d("SOS Receiver", "mCurrentSOSSpeakerId ::" + mCurrentSOSSpeakerId + ":: sosState.getUserID():: " + sosState.getUserID());
                    if (mCurrentSOSSpeakerId == sosState.getUserID()) {
                        Fragment visibleFragment = getVisibleFragment();
                        if (visibleFragment instanceof MediaPlayerFragment) {
                            if (((MediaPlayerFragment) visibleFragment).isMediaPlayerPlaying()) {
                                ((MediaPlayerFragment) visibleFragment).pauseVideoPlay();
                            }
                        }
                        MessageDeliveryStatusDialogFragment dialog = (MessageDeliveryStatusDialogFragment) getSupportFragmentManager().findFragmentByTag("MessageDeliveryStatusDialogFragment");
                        if (dialog != null) {
                            dialog.dismissDialog();
                        }
                        if (isPushToTalkRunning) {
                            pttContainer.setVisibility(GONE);
                            updateState(EnumConstant.pttAnimationState.USER_STATE_DEFAULT.name());
                            handler.removeCallbacksAndMessages(null);
                        }

                        pauseAudio();
                        dismissCalendarDialog();
                        if (sosBottomSheetDialogFragment != null && sosBottomSheetDialogFragment.isVisible()) {
                            sosBottomSheetDialogFragment.pauseAudio();
                        }
                        loadSosReceiveAlert(sosState.getUserName(), batteryPercentage, location, signalStrength);
                    }
                    break;
                case DEFAULT:
                case SENDER:
                    break;
            }
        });

        mViewModel.observeConnectionSynchronizedState().observe(this, isSynchronized -> {
            Log.d(TAG, "ObserveConnectionSynchronizedState: isSynchronized = " + isSynchronized);
            if (isDispatcherLoggedOut) {
                return;
            }
            FragmentManager fm = getSupportFragmentManager();
            SettingAccountFragment fragment = (SettingAccountFragment) getSupportFragmentManager().findFragmentByTag(SettingAccountFragment.class.getName());
            if (!isSynchronized) {
                if (fragment != null && fragment.isVisible()) {
                    Log.d(TAG, "ObserveConnectionSynchronizedState : already in SettingAccountFragment Return");
                    return;
                }
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                loadInnerFragment(EnumConstant.getSupportedFragment.SETUP_ACCOUNT_FRAGMENT.ordinal());
            } else {
                Log.d(TAG, "ObserveConnectionSynchronizedState: isSynchronized == true >>> loading DispatcherHomeFragment");
                if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive() &&
                        mViewModel.getJioTalkieService().isConnectionSynchronized() && mViewModel.getJioTalkieService().getJioPttSession() != null) {
                    mViewModel.getJioTalkieService().getJioPttSession().fetchPttUserList();
                }
                if (isDispatcherUser()) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                    launchDispatcherHomeScreen();
                    setProfilePic();
                    if (BuildConfig.BUILD_TYPE.contains("devpreprod")) {
                    }
                } else {
                    showDispatcherInfoDialog();
                }
            }
        });
        mViewModel.observerLatestApkVersionCheck().observe(this, latestVersionCheck -> {
            Log.d(TAG, "updateApk:observerUpdateApk():latestVersionCheck:" + latestVersionCheck.getAndroidApkVersion());
            compareApkVersion(latestVersionCheck.getAndroidApkVersion());
        });
        mViewModel.observerDownloadApk().observe(this, downloadApk -> {
            Log.d(TAG, "updateApk:observerDownloadApk:latestApkVersion:" + downloadApk.getAndroidApkVersion() + ", ApkFile:" + downloadApk.getAndroidApkFile());
            downloadAndSaveApk(downloadApk);
        });
        mViewModel.observeDisablePttCall().observe(this, pttCallUserState -> {
            if (pttCallUserState.getmCallId().equals(mCallId) && pttCallUserState.isGroupCall()) {
                if (isPushToTalkRunning) {
                    pttContainer.setVisibility(GONE);
                    updateState(EnumConstant.pttAnimationState.USER_STATE_DEFAULT.name());
                    handler.removeCallbacksAndMessages(null);

                    if (mViewModel.getJioTalkieService().isBoundToPttServer()) {
                        JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
                        service.onTalkKeyUp();
                        service.getJioPttSession().updateUserTalkingState(false, mCallId);
                    }
                }
            }
        });

        mViewModel.observeUserStateUpdate().observe(this, user -> {
            if (user.getUserID() == mViewModel.getSelfUserId() && user.isMute()){
                stopPTTCall();
            }
            if (user.getUserID() == mViewModel.getSelfUserId() && user.isDeaf()){
                stopReceivingPttCall(true);
            }
        });
        mViewModel.observeUserAddedOrDeleted().observe(this, regUserState -> {
            Log.d(TAG, "observeUserAddedOrDeleted: = " + regUserState.toString());
            Log.d(TAG, "Logged self User Id  = " + mViewModel.getSelfUserId());
            if (regUserState.isDeleted() && (mViewModel.getSelfUserId() == regUserState.getUserId())) {
                Log.d(TAG, "Account deleted by admin. let's logout ");
                loggedUserDeleted();
            }
        });
    }

    public void startApkDownload() {
        // Create and show the progress dialog with a spinner
        AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
        builder.setView(new ProgressBar(DashboardActivity.this)); // Use the default spinner
        builder.setTitle(R.string.update_available_downloading_apk);
        builder.setMessage(R.string.update_available_please_wait);
        builder.setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
        //delete old apk
        mViewModel.removeOldApk();
        // Start apk download process
        mViewModel.downloadApk();
    }

    public void enableDisableBottomLayout(ViewGroup layout, boolean isDisabled) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(!isDisabled);
        }
        if (isDisabled) {
            layout.setAlpha(0.3f);
        } else {
            layout.setAlpha(1.0f);
        }
    }

    private void compareApkVersion(String androidApkVersion) {
        String latestVersion = androidApkVersion.replace("V", "");
        String[] updateVersion= latestVersion.split("[.]");
        int latestApkVersionCode = Integer.parseInt(updateVersion[0]) * 10000 + Integer.parseInt(updateVersion[1]) * 100 + Integer.parseInt(updateVersion[2]);
        Log.d(TAG, "updateApk:latestApkVersionCode:" + latestApkVersionCode + ",currentApkVersionCode:" + mCurrentVersion);
        if (latestApkVersionCode > mCurrentVersion) {
            runOnUiThread(() -> {
                new AlertDialog.Builder(this).setCancelable(false)
                        .setTitle(R.string.update_available_title)
                        .setMessage(getString(R.string.update_available_message1) + " " + androidApkVersion + " " + getString(R.string.update_available_message2))
                        .setPositiveButton(R.string.update_available_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startApkDownload();
                            }
                        }).setNegativeButton(R.string.update_available_negative, null).show();
            });

        } else {
            runOnUiThread(() -> {
                new AlertDialog.Builder(this).setCancelable(false)
                        .setTitle(R.string.positive_button_title_update)
                        .setMessage(getString(R.string.positive_button_subtitle_update) + " " + BuildConfig.VERSION_NAME)
                        .setPositiveButton(R.string.positive_button_got_it, null).show();
                isCheckUpdate = false;
            });
        }
        Log.d(TAG, "updateApk:No update available. You are using latest version");

    }

    public void updateCheckUpdate(boolean update) {
        isCheckUpdate = update;
    }


    private void downloadAndSaveApk(ApkResponseModel updateApkResponseModel) {
        String version = updateApkResponseModel.getAndroidApkVersion();
        String base64Apk = updateApkResponseModel.getAndroidApkFile(); // Extract only the APK file
        Log.d(TAG, "updateApk: latest version" + version + ",base64Apk:" + base64Apk);
        byte[] apkData = Base64.decode(base64Apk, Base64.DEFAULT);
        File apkFile = new File(getApplication().getApplicationContext().getExternalFilesDir(EnumConstant.UPDATE_APK_FOLDER_NAME).getAbsolutePath() + "/" + version + ".apk");
        try (FileOutputStream fos = new FileOutputStream(apkFile)) {
            {
                // Write the APK data in chunks to simulate progress
                int chunkSize = 4096; // 4KB
                int totalBytes = apkData.length;
                int writtenBytes = 0;
                while (writtenBytes < totalBytes) {
                    int bytesToWrite = Math.min(chunkSize, totalBytes - writtenBytes);
                    fos.write(apkData, writtenBytes, bytesToWrite);
                    writtenBytes += bytesToWrite;
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Uri apkUri = FileProvider.getUriForFile(this, EnumConstant.AUTHORITY_PROVIDER_NAME, apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, EnumConstant.UPDATE_APK_URI_TYPE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.error_while_saving_apk + e.getMessage(), Toast.LENGTH_SHORT).show());
            e.printStackTrace();
        }
    }


    public void dismissCalendarDialog() {
        if (calendarDialog != null && calendarDialog.isShowing()) {
            calendarDialog.dismiss();
        }
    }

    public Fragment getVisibleFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if (!fragments.isEmpty()) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible())
                    return fragment;
            }
        }
        return null;
    }

    private void updatePTTSpeakerView(CurrentSpeakerState speakerState) {
        isNeedToShowSOSView = speakerState.isSelfCurrentSpeakerSOS();
        if (isNeedToShowSOSView) {
            mCurrentSOSSpeakerId = speakerState.getCurrentSpeakerId();
        }
    }

    private void loadSosReceiveAlert(String userName, String batteryPercentage, String location, String signalStrength) {
        Bundle args = new Bundle();
        args.putString("userName", userName);
        args.putString("battery", batteryPercentage);
        args.putString("location", location);
        args.putString("signalStrength", signalStrength);
        sosBottomSheetDialogFragment = SOSBottomSheetDialogFragment.newInstance(args);
        sosBottomSheetDialogFragment.setCancelable(false);
        sosBottomSheetDialogFragment.show(getSupportFragmentManager(), sosBottomSheetDialogFragment.getTag());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        mToolbar = findViewById(R.id.toolbar_main);
        mBaseToolLayout = findViewById(R.id.base_tools_layout);
        mChannelToolLayout = findViewById(R.id.channel_tools_layout);
        mSearchToolLayout = findViewById(R.id.actionSearchLayout);
        // assign tools bar action item
        actionBack = findViewById(R.id.actionBack);
        actionHome = findViewById(R.id.actionHome);
        actionSearch = findViewById(R.id.actionSearch);
        actionCalender = findViewById(R.id.actionCalender);
        actionProfile = findViewById(R.id.actionProfile);
        actionMenu = findViewById(R.id.actionMenu);
        actionTitle = findViewById(R.id.actionTitle);
        actionSubTitle = findViewById(R.id.actionSubTitle);
        actionCross = findViewById(R.id.actionCross);
        actionPlus = findViewById(R.id.actionPlus);
        actionDelete = findViewById(R.id.actionDelete);
        actionCopy = findViewById(R.id.actionCopy);
        actionInfo = findViewById(R.id.actionInfo);
        actionSaveImage = findViewById(R.id.actionCropImage);
        actionSearchQuery = findViewById(R.id.actionSearchQuery);

        mChannelNameTV = findViewById(R.id.tv_dis_channel);
        mSOSFloatingBtn = findViewById(R.id.floating_sos);
        mSOSDuringPtt = findViewById(R.id.sos_during_ptt);
        mMainContainer = findViewById(R.id.content_frame);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        mChannelDetailsView = findViewById(R.id.channel_details_view);
        mChannelName = findViewById(R.id.channelName);
        mOnlineUserCount = findViewById(R.id.online_user_count_view);
        pushToTalk = findViewById(R.id.push_to_talk);
        pttContainer = findViewById(R.id.ptt_container_view);
        pttContainer.setOnClickListener(view -> { //This is added to avoid the clicks below this view as this view is transparent
            //Do nothing
        });
        pttIndicatorView = findViewById(R.id.ptt_indication_view);
        mTimer = findViewById(R.id.timer);
        customCircleView = new CustomCircleView(this);
        customCircleView.setCustomViewInterface(this);
        pttIndicatorView.addView(customCircleView);
        setSearchQueryListener();
        actionMenu.setOnClickListener(view -> Toast.makeText(this, "Feature not yet implemented !", Toast.LENGTH_SHORT).show());
        actionCalender.setOnClickListener(view -> {
            if (getVisibleFragment() instanceof GroupChatFragment || getVisibleFragment() instanceof PersonalChatFragment || getVisibleFragment() instanceof LocationHistoryFragment) {
                showCalendarDialog();
            } else {
                Toast.makeText(mContext, "Feature coming soon", Toast.LENGTH_LONG).show();
            }
        });
        actionSearch.setOnClickListener(view -> showActionBarSearchView(true));
        actionBack.setOnClickListener(view -> handleOnBackPress());
        actionProfile.setOnClickListener(view -> loadInnerFragment(EnumConstant.getSupportedFragment.PROFILE_FRAGMENT.ordinal()));
        mSOSFloatingBtn.setOnClickListener(view -> {
            if (isPushToTalkRunning) {
                Log.d(TAG, "Push to talk is going on, can not start SOS");
                return;
            }
            launchSOSFragment();
        });

        mSOSDuringPtt.setOnClickListener(view -> {
            pttContainer.setVisibility(GONE);
            launchSOSFragment();
        });
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // If the user taps on the selected tab from the bottom bar again, do not reload the same fragment.
            if (bottomNavigationView.getSelectedItemId() == item.getItemId()) {
                return false;
            }
            if (isPTTCallGoing) {
                return false;
            }
            return switch (item.getItemId()) {
                case R.id.navigation_home -> {
                    loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_HOME_FRAGMENT.ordinal());
                    yield true;
                }
                case R.id.navigation_active_channel -> {
                    loadInnerFragment(EnumConstant.getSupportedFragment.GROUP_CHAT_FRAGMENT.ordinal());
                    yield true;
                }
                case R.id.navigation_location -> {
                    if (GpsUtils.isLocationEnabled(mContext)) {
                        loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_LOCATION_FRAGMENT.ordinal());
                    } else {
                        Toast.makeText(mContext, getResources().getString(R.string.enable_location), Toast.LENGTH_SHORT).show();
                    }
                    yield true;
                }
                case R.id.navigation_status -> {
                    loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_STATUS_FRAGMENT.ordinal());
                    yield true;
                }
                default -> false;
            };
        });

        pushToTalk.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handleChatFragment();
                        // Since mute/deafen user are not allow so message to user.
                        if (mViewModel.isJioTalkieServiceActive() && mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser() != null) {
                            int selfSessionId = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getSessionID();
                            IUserModel self = mViewModel.getJioTalkieService().getJioPttSession().fetchPttUser(selfSessionId);
                            if (self.isMute() || self.isUserLocalMute() || self.isUserSuppressed()) {
                                Toast.makeText(getBaseContext(), getResources().getString(R.string.mute_message_alert), Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        }
                        isPushToTalkRunning = true;
                        pttContainer.setVisibility(View.VISIBLE);
                        updateState(EnumConstant.pttAnimationState.USER_STATE_SPEAKER.name());
                        needSOSButton(false);
                        pauseAudio();
                        if (mViewModel.isJioTalkieServiceActive()) {
                            JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
                            mCallId = MessageIdUtils.generateUUID();
                            service.getJioPttSession().updateUserTalkingState(true, mCallId);
                            service.onTalkKeyDown();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mViewModel.isJioTalkieServiceActive() && mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser() != null) {
                            int selfSessionId = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getSessionID();
                            IUserModel self = mViewModel.getJioTalkieService().getJioPttSession().fetchPttUser(selfSessionId);
                            // Since mute/deafen user are not allow to start PTT call. So no need to handle PTT related thing
                            if (self.isMute() || self.isUserLocalMute() || self.isUserSuppressed()) {
                                return true;
                            }
                        }
                        isPushToTalkRunning = false;
                        pttContainer.setVisibility(GONE);
                        updateState(EnumConstant.pttAnimationState.USER_STATE_DEFAULT.name());
                        handler.removeCallbacksAndMessages(null);

                        if (mViewModel.isJioTalkieServiceActive()) {
                            JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
                            service.onTalkKeyUp();
                            service.getJioPttSession().updateUserTalkingState(false, mCallId);
                        }
                        // SOS Button not require in chat windows
                        if (getVisibleFragment() instanceof DispatcherHomeFragment || getVisibleFragment() instanceof DispatcherStatusFragment
                            || getVisibleFragment() instanceof DispatcherLocationFragment) {
                            needSOSButton(true);
                        }
                        break;
                }
                return true;
            }
        });
    }

    private void launchSOSFragment() {
        if (GpsUtils.isLocationEnabled(DashboardActivity.this)) {
            loadInnerFragment(EnumConstant.getSupportedFragment.SOS_FRAGMENT.ordinal());
        } else {
            turnOnGPS();
        }
    }

    private void showCalendarDialog() {
        calendarDialog = new Dialog(this);
        calendarDialog.setContentView(R.layout.dialog_calendar);
        calendarView = calendarDialog.findViewById(R.id.calendarView);
        calendarView.addDecorator(new CustomDayDecorator());
        calendarView.setWeekDayFormatter(new WeekDayFormatter() {
            @Override
            public CharSequence format(DayOfWeek dayOfWeek) {
                String[] customLabels = new String[]{"S", "M", "T", "W", "T", "F", "S"};
                int index = dayOfWeek.getValue() % 7;
                return customLabels[index];
            }
        });
        selectedMonth = calendarDialog.findViewById(R.id.selected_month);
        selectedYear = calendarDialog.findViewById(R.id.selected_year);
        monthLayout = calendarDialog.findViewById(R.id.monthLayout);
        yearLayout = calendarDialog.findViewById(R.id.yearLayout);
        selectedDateTextView = calendarDialog.findViewById(R.id.selected_date);
        // Initialize month and year lists
        months = getMonthList();
        years = getYearList();

        monthLayout.setOnClickListener(view -> {
            calendarDialog.findViewById(R.id.monthLayout).setBackgroundDrawable(mContext.getDrawable(R.drawable.bt_rounded_corner_border_selected));
            calendarDialog.findViewById(R.id.yearLayout).setBackgroundDrawable(mContext.getDrawable(R.drawable.bt_rounded_corner_border));
            loadMonthList();
        });

        yearLayout.setOnClickListener(view -> {
            calendarDialog.findViewById(R.id.monthLayout).setBackgroundDrawable(mContext.getDrawable(R.drawable.bt_rounded_corner_border));
            calendarDialog.findViewById(R.id.yearLayout).setBackgroundDrawable(mContext.getDrawable(R.drawable.bt_rounded_corner_border_selected));
            loadYearList();
        });
        Button confirmButton = calendarDialog.findViewById(R.id.confirm_button);
        ImageButton closeButton = calendarDialog.findViewById(R.id.close_button);
        LocalDate currentDate = LocalDate.now();
        Log.d(TAG, "Current Month " + months.get(currentDate.getMonth().getValue() - 1));
        selectedMonth.setText(months.get(currentDate.getMonth().getValue() - 1).getName());
        Log.d(TAG, "Current Year " + LocalDate.now().getYear());
        selectedYear.setText("" + currentDate.getYear());
        selectedDateTextView.setText(CommonUtils.getSimpleDateFormat(new Date(System.currentTimeMillis())));
        // Initialize calendar view
        updateCalendar();

        closeButton.setOnClickListener(v -> {
            calListAdapter.clear();
            calendarDialog.dismiss();
        });

        confirmButton.setOnClickListener(v -> {
            if (chatDurationFrom == 0) {
                Toast.makeText(DashboardActivity.this, getResources().getString(R.string.no_date_selected), Toast.LENGTH_SHORT).show();
            } else {
                if (getVisibleFragment() instanceof GroupChatFragment) {
                    ((GroupChatFragment) getVisibleFragment()).filterChatMessages(chatDurationFrom);
                } else if (getVisibleFragment() instanceof PersonalChatFragment) {
                    ((PersonalChatFragment) getVisibleFragment()).filterChatMessages(chatDurationFrom);
                } else if (getVisibleFragment() instanceof LocationHistoryFragment) {
                    ((LocationHistoryFragment) getVisibleFragment()).filterLocationHistory(chatDurationFrom);
                }
                calListAdapter.clear();
                calendarDialog.dismiss();
            }
        });
        calendarDialog.setOnDismissListener(dialogInterface -> selectedDate = CalendarDay.today());
        calendarDialog.show();
    }

    private void loadYearList() {
        ImageButton button = calendarDialog.findViewById(R.id.back_button);
        button.setVisibility(View.VISIBLE);
        RecyclerView yearListView = calendarDialog.findViewById(R.id.calenderListView);
        calendarView.setVisibility(GONE);
        yearListView.setVisibility(View.VISIBLE);
        if (!calListAdapter.getCalendarList().isEmpty()) {
            if (!calListAdapter.isYearListSet()) {
                if (calListAdapter.getYearCalendarList().isEmpty()) {
                    calListAdapter.setYearList(getYearList());
                } else {
                    calListAdapter.setYearList(calListAdapter.getYearCalendarList());
                }
            }
        } else {
            calListAdapter.setYearList(getYearList());
        }
        calListAdapter.setCalendarUpdateProvider(update -> {
            selectedYear.setText(update);
            months = getMonthList();
        });
        yearListView.setAdapter(calListAdapter);
        yearListView.post(() -> yearListView.scrollToPosition(calListAdapter.getSelectedItemPosition()));
        button.setOnClickListener(view -> {
            calendarDialog.findViewById(R.id.monthLayout).setBackgroundDrawable(mContext.getDrawable(R.drawable.bt_rounded_corner_border));
            calendarDialog.findViewById(R.id.yearLayout).setBackgroundDrawable(mContext.getDrawable(R.drawable.bt_rounded_corner_border));
            button.setVisibility(GONE);
            selectedYear.setText(calListAdapter.getSelectedItem());
            yearListView.setVisibility(GONE);
            calendarView.setVisibility(View.VISIBLE);
            updateCalendar();
        });
    }

    private void loadMonthList() {
        ImageButton button = calendarDialog.findViewById(R.id.back_button);
        button.setVisibility(View.VISIBLE);
        RecyclerView monthListView = calendarDialog.findViewById(R.id.calenderListView);
        calendarView.setVisibility(GONE);
        monthListView.setVisibility(View.VISIBLE);
        if (!calListAdapter.getCalendarList().isEmpty()) {
            if (calListAdapter.isYearListSet()) {
                if (calListAdapter.getMonthCalendarList().isEmpty()) {
                    calListAdapter.setMonthList(getMonthList());
                } else {
                    calListAdapter.setMonthList(calListAdapter.getMonthCalendarList());
                }
            }
        } else {
            calListAdapter.setMonthList(getMonthList());
        }
        calListAdapter.setCalendarUpdateProvider(update -> selectedMonth.setText(update));
        monthListView.setAdapter(calListAdapter);
        monthListView.post(() -> monthListView.scrollToPosition(calListAdapter.getSelectedItemPosition()));
        button.setOnClickListener(view -> {
            calendarDialog.findViewById(R.id.monthLayout).setBackgroundDrawable(mContext.getDrawable(R.drawable.bt_rounded_corner_border));
            calendarDialog.findViewById(R.id.yearLayout).setBackgroundDrawable(mContext.getDrawable(R.drawable.bt_rounded_corner_border));
            selectedMonth.setText(calListAdapter.getSelectedItem());
            button.setVisibility(GONE);
            monthListView.setVisibility(GONE);
            calendarView.setVisibility(View.VISIBLE);
            updateCalendar();
        });
    }

    private void updateCalendar() {
        int month = findIndexByName(months, selectedMonth.getText().toString()) + 1; // Spinner position starts from 0
        int year = Integer.parseInt(selectedYear.getText().toString());

        LocalDate newDate = LocalDate.of(year, month, 1);
        CalendarDay firstDayOfMonth = CalendarDay.from(newDate.withDayOfMonth(1));
        CalendarDay lastDayOfMonth = CalendarDay.from(newDate.withDayOfMonth(newDate.lengthOfMonth()));

        calendarView.state().edit()
                .setMinimumDate(firstDayOfMonth)
                .setMaximumDate(lastDayOfMonth)
                .commit();
        calendarView.setTopbarVisible(false);
        if (selectedDate.equals(CalendarDay.today())) {
            calendarView.addDecorator(new TodayDecorator(DashboardActivity.this, CalendarDay.today()));
        }
        calendarView.addDecorator(new DisableAfterTodayDecorator());
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDate = date;
            ZonedDateTime zonedDateTime = date.getDate().atStartOfDay(ZoneId.systemDefault());
            Instant instant = zonedDateTime.toInstant();
            long epochMilli = instant.toEpochMilli();
            selectedDateTextView.setText(CommonUtils.getSimpleDateFormat(new Date(epochMilli)));
            chatDurationFrom = epochMilli;
            updateDecorators();
        });
    }

    public int findIndexByName(List<CalendarItem> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            CalendarItem item = list.get(i);
            if (item.getName().equals(name)) {
                return i; // Return the index if the name matches
            }
        }
        return -1;
    }

    private void updateDecorators() {
        calendarView.removeDecorators();
        if (!selectedDate.equals(CalendarDay.today())) {
            calendarView.addDecorator(new TodayDecorator(this, null));
        }
        calendarView.addDecorator(new CustomDayDecorator());
        calendarView.addDecorator(new DisableAfterTodayDecorator());
        calendarView.addDecorator(new SelectedDateDecorator(this, selectedDate));
    }

    private List<CalendarItem> getMonthList() {
        List<CalendarItem> monthList = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        boolean isCurrentMonth = false;
        String currentMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("en", "US"));
        for (int i = 1; i <= 12; i++) {
            String month = YearMonth.of(currentYear, i).getMonth().name();
            month = month.substring(0, 1).toUpperCase() + month.substring(1).toLowerCase();
            if (selectedYear.getText().toString().equals("" + currentYear)) {
                isCurrentMonth = month.equals(currentMonth);
            }
            monthList.add(new CalendarItem(month, isCurrentMonth));
            if (isCurrentMonth) {
                break;
            }
        }
        return monthList;
    }

    private List<CalendarItem> getYearList() {
        List<CalendarItem> yearList = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 10; i <= currentYear; i++) {
            boolean isCurrentYear = i == currentYear ? true : false;
            yearList.add(new CalendarItem(String.valueOf(i), isCurrentYear));
        }
        return yearList;
    }

    private void pauseAudio() {
        if (getVisibleFragment() instanceof GroupChatFragment) {
            ((GroupChatFragment) getVisibleFragment()).pauseAudio();
        } else if (getVisibleFragment() instanceof PersonalChatFragment) {
            ((PersonalChatFragment) getVisibleFragment()).pauseAudio();
        }
    }

    private boolean isDispatcherUser() {
        boolean isDispatcher = false;
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive() &&
                mViewModel.getJioTalkieService().getJioPttSession() != null) {
            Mumble.UserState.UserRole userRole = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserRole();
            Log.d(TAG, "Logged user role is :-  "+userRole);
            if (userRole == Mumble.UserState.UserRole.Dispatcher) {
                isDispatcher = true;
            }
        }
        if (BuildConfig.BUILD_TYPE.contains("devpreprod")) {
            isDispatcher = true;
        }
        return isDispatcher;
    }

    private RegisteredUser getCompanyAdmin() {
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive() &&
                mViewModel.getJioTalkieService().getJioPttSession() != null) {
            for(RegisteredUser user : mViewModel.RegUserList){
                if(user.getUserRole() == Mumble.UserState.UserRole.CompanyAdmin_VALUE)
                    return user;
            }
        }
        return null;
    }

    private void registerConnectionStateObserver() {
        mViewModel.observerConnectionStateData().observe(this, connectionState -> {
            Log.d(TAG, "onCreate: observerConnectionStateData called connectionState =" + connectionState);
            switch (connectionState) {
                case SERVER_CONNECTED:
                    break;
                case PERMISSION_DENY:
                    Toast.makeText(this, "Permission Deny", Toast.LENGTH_SHORT).show();
                    handleConnectionState();
                    break;
                case SERVER_DISCONNECTED:
                case SERVER_CONNECTING:
                    if (mBackgroundTaskSchedular != null) {
                        mBackgroundTaskSchedular.stopUpdates();
                    }
                    handleConnectionState();
                    break;
            }
        });
    }

    private void showDispatcherInfoDialog() {
        mViewModel.getJioTalkieService().closePttConnection();
        // remove server and user details for database
        mViewModel.mDatabase.clearJioTalkieServer();
        mJioTalkieSettings.setUserAgreementAccept(false);
        // Reset SSO Token if login using ZLA
        mJioTalkieSettings.setSsoToken("");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.login_info_title))
                .setMessage(mContext.getResources().getString(R.string.login_info_msg))
                .setCancelable(false)
                .setPositiveButton(mContext.getResources().getString(R.string.ok_got_it), (dialog, id) -> {
                    Toast.makeText(mContext, R.string.non_dispatcher_user_message, Toast.LENGTH_SHORT).show();
                    clearFragmentBackStack();
                    loadInnerFragment(EnumConstant.getSupportedFragment.PHONE_LOGIN_FRAGMENT.ordinal());
                    dialog.dismiss();
                });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    public void launchDispatcherHomeScreen() {
        loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_HOME_FRAGMENT.ordinal());
        callMessageHistoryDownload();
        mBackgroundTaskSchedular = new BackgroundTaskScheduler(getApplicationContext(), (JioTalkieService) mViewModel.getJioTalkieService());
        mBackgroundTaskSchedular.startUpdates();
    }

    private void callMessageHistoryDownload() {
        MessageRequestModel messageRequestModel = new MessageRequestModel(null, null, false, mViewModel.getChannelId(), null, null, true, 30, 0);
        mViewModel.downloadMessagesWithPagination(messageRequestModel);
    }
    private void loggedUserDeleted() {
        handlePTTCallUIOnDisconnection();
        mViewModel.getJioTalkieService().closePttConnection();
        // remove server and user details for database
        mViewModel.mDatabase.clearJioTalkieServer();
        mJioTalkieSettings.setUserAgreementAccept(false);
        // Reset SSO Token if login using ZLA
        mJioTalkieSettings.setSsoToken("");
        setLogoutStatus(true);
        loadInnerFragment(EnumConstant.getSupportedFragment.PHONE_LOGIN_FRAGMENT.ordinal());
        Toast.makeText(DashboardActivity.this, getResources().getString(R.string.user_deleted), Toast.LENGTH_SHORT).show();
    }

    public void logoutDispatcherUser() {
        ADCInfoUtils.logOutInfo(0, 0, "LoggedOut", mViewModel.getUserId(),
                mViewModel.getChannelId());
        handlePTTCallUIOnDisconnection();
        mViewModel.getJioTalkieService().closePttConnection();
        // remove server and user details for database
        mViewModel.mDatabase.clearJioTalkieServer();
        mJioTalkieSettings.setUserAgreementAccept(false);
        // Reset SSO Token if login using ZLA
        mJioTalkieSettings.setSsoToken("");
        ((ActivityManager) getBaseContext().getSystemService(ACTIVITY_SERVICE))
                .clearApplicationUserData(); // note: it has a return value!
        clearFragmentBackStack();
        loadInnerFragment(EnumConstant.getSupportedFragment.PHONE_LOGIN_FRAGMENT.ordinal());
    }

    public void setLogoutStatus(boolean status) {
        isDispatcherLoggedOut = status;
    }

    private void bindServiceConnection() {
        Log.d(TAG, " bindServiceConnection() JioTalkieService -> BindService ");
        jioTalkieInterface.bindToPttServer();
//        Intent intent = new Intent(DashboardActivity.this, JioTalkieService.class);
//        bindService(intent, mServiceConnection, 0);
    }

    public void setTempProfilePic(Bitmap image) {
        Glide.with(this)
                .load(image)
                .transform(new CircleCrop())
                .into(actionProfile);
    }

    public void setProfilePic() {
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive() &&
                mViewModel.getJioTalkieService().getJioPttSession() != null) {
            textureData = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserTexture();
        }
        if (textureData != null) {
            profilepic = BitmapFactory.decodeByteArray(textureData, 0, textureData.length);
            int targetW = actionProfile.getWidth();
            int targetH = actionProfile.getHeight();
            try {
                Bitmap oldimage = getBitmapFromImageView(actionProfile);
                if (!areBitmapsEqual(oldimage, profilepic)) {
                    actionProfile.setImageBitmap(BitmapUtils.getCircularBitmap(profilepic));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap getBitmapFromImageView(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        return null; // or handle the case where the drawable is not a BitmapDrawable
    }

    public boolean areBitmapsEqual(Bitmap bitmap1, Bitmap bitmap2) {
        if (bitmap1 == null || bitmap2 == null) {
            return bitmap1 == bitmap2; // true if both are null, false otherwise
        }
        if (bitmap1.getWidth() != bitmap2.getWidth() || bitmap1.getHeight() != bitmap2.getHeight()) {
            return false;
        }
        int width = bitmap1.getWidth();
        int height = bitmap1.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitmap1.getPixel(x, y) != bitmap2.getPixel(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void showNoNetworkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.internet_unavailable);
        builder.setMessage(R.string.disconnecting_msg);
        builder.setPositiveButton(R.string.ok_got_it, (dialog, which) -> finish());
        mNoNetworkDialog = builder.create();
        mNoNetworkDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        registerNetworkCallback();
        if (!CommonUtils.isNetworkAvailable(this)) {
            showNoNetworkDialog();
            return;
        }
        bindServiceConnection();
        Log.d(TAG, "onResume: called >>> jioTalkieInterface = " + jioTalkieInterface + " isActivityCreatedCalled =" + isActivityCreatedCalled);
        if (!isActivityCreatedCalled && !jioTalkieInterface.isBoundToPttServer()) {
            String msisdn = mViewModel.getSettings().getMSISDN();
            String ssoToken = mViewModel.getSettings().getSsoToken();
            String sni = mViewModel.getSettings().getServerSni();
            String serverIp = mViewModel.getSettings().getServerIp();
            int port = mViewModel.getSettings().getServerPort();
            if (!msisdn.isEmpty() && !ssoToken.isEmpty() && !serverIp.isEmpty()
                    && port != -1 && !sni.isEmpty()) {
                Server srv = new Server(-1,
                        serverIp, port, msisdn, ssoToken, sni);
                mViewModel.callServerConnectionApi(srv);
                //mViewModel.callServerConnectionApi(mCurrentServer);
            }
        }
        isActivityCreatedCalled = false;
        mAudioVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0);
        long durationTillLong;
        durationTillLong = System.currentTimeMillis();
        String durationTill = DateUtils.getStringDateFromLong(durationTillLong, DateUtils.dateFormatServer);
        String durationFrom = mJioTalkieSettings.getDurationFrom();
        if (durationFrom.equals(" ")) {
            durationFrom = getDurationFrom(durationTill);
        }
        mJioTalkieSettings.setDurationFrom(durationFrom);
        mJioTalkieSettings.setDurationTill(durationTill);
        setProfilePic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        String durationFrom = DateUtils.getStringDateFromLong(System.currentTimeMillis(), DateUtils.dateFormatServer);
        mJioTalkieSettings.setDurationFrom(durationFrom);

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called ");
        jioTalkieInterface.stopPttService();
        isActivityCreatedCalled = false;
        pauseAudio();
        if (jioTalkieInterface.isBoundToPttServer()) {
            jioTalkieInterface.removeObserver(mViewModel.jioPttObserver);
            jioTalkieInterface.setSuppressNotifications(false);
            jioTalkieInterface.closePttConnection();
            jioTalkieInterface.unBindToPttServer();
//            unbindService(mServiceConnection);
//            mServiceConnection = null;
            jioTalkieInterface = null;
        }
        if (mBackgroundTaskSchedular != null) {
            mBackgroundTaskSchedular.stopUpdates();
            mBackgroundTaskSchedular = null;
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioVolumeLevel, 0);
        unregisterTelephonyCallback();
        Intent intent = new Intent(getApplicationContext(), IService2.class);
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        unRegisterReceivers();
        // Cleanup when the activity is destroyed
        mIdleHandler.removeCallbacks(mIdleRunnable);
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MENU_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                int result = data.getIntExtra("result", -1);
                Log.d(TAG, "onActivityResult: " + result);
                if (result == -1) return;
                loadInnerFragment(result);
            }
        }
    }

    public void callZLAApi(String phoneNumber) {
        loadInnerFragment(EnumConstant.getSupportedFragment.SETUP_ACCOUNT_FRAGMENT.ordinal());
        if (!mViewModel.callZLAApi(phoneNumber)) {
            Toast.makeText(this, "Please check if internet is working on mobile data.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void hideShowToolbar(boolean show) {
        if (show)
            mToolbar.setVisibility(View.VISIBLE);
        else {
            mToolbar.setVisibility(GONE);
        }
    }

    public void needBottomNavigation(boolean needToShow) {
        if (needToShow) {
            mChannelDetailsView.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = bottomNavigationView.getLayoutParams();
            params.height = getResources().getDimensionPixelSize(R.dimen.navigation_tab_height);
            bottomNavigationView.setLayoutParams(params);
            bottomNavigationView.setVisibility(View.VISIBLE);
        } else {
            mChannelDetailsView.setVisibility(GONE);
            bottomNavigationView.setVisibility(GONE);
        }
    }

    public void needChannelDetailsView(boolean needToShow) {
        if (needToShow) {
            mChannelDetailsView.setVisibility(View.VISIBLE);
        } else {
            mChannelDetailsView.setVisibility(GONE);
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop Called ");
        pauseAudio();
        unregisterNetworkCallback();
        if (mNoNetworkDialog != null && mNoNetworkDialog.isShowing()) {
            mNoNetworkDialog.dismiss();
        }
        super.onStop();
    }

    public void hideNavigationBottomTab() {
        bottomNavigationView.setVisibility(View.INVISIBLE);
        ViewGroup.LayoutParams params = bottomNavigationView.getLayoutParams();
        params.height = 0;
        bottomNavigationView.setLayoutParams(params);
    }

    public void needSOSButton(boolean needToShow) {
        if (pttContainer.getVisibility() != View.VISIBLE || !needToShow) {
            mSOSFloatingBtn.setVisibility(needToShow ? View.VISIBLE : GONE);
        }
    }

    public void setCurrentChannelName(String channelName) {
        mChannelNameTV.setText(channelName);
    }

    public void showChannelTools(boolean show) {
        if (show) {
            mChannelToolLayout.setVisibility(View.VISIBLE);
            mBaseToolLayout.setVisibility(GONE);
            mSearchToolLayout.setVisibility(GONE);
        } else {
            mChannelToolLayout.setVisibility(GONE);
            mSearchToolLayout.setVisibility(GONE);
            mBaseToolLayout.setVisibility(View.VISIBLE);
        }
    }

    public void showActionBarSearchView(boolean show) {
        if (show) {
            mChannelToolLayout.setVisibility(GONE);
            mBaseToolLayout.setVisibility(GONE);
            actionSearchQuery.setText("");
            // Show keyboard 
            actionSearchQuery.requestFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(actionSearchQuery, InputMethodManager.SHOW_IMPLICIT);
            mSearchToolLayout.setVisibility(View.VISIBLE);
        } else {
            hideKeyboard(this);
            mSearchToolLayout.setVisibility(GONE);
            mBaseToolLayout.setVisibility(View.VISIBLE);
            mChannelToolLayout.setVisibility(GONE);
        }
    }

    // Fragment will get search query callback
    public void setSearchQueryCallBack(SearchQueryCallBack searchQueryCallBack) {
        mSearchQueryCallBack = searchQueryCallBack;
    }

    private void setSearchQueryListener() {
        actionSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Show and hide cross icon based on search length
                if (charSequence.length() > 0) {
                    actionSearchQuery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_back, 0, R.drawable.ic_cross, 0);
                } else {
                    actionSearchQuery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0);
                }
                // If Fragment set call back than return call back method.
                if (mSearchQueryCallBack != null) {
                    mSearchQueryCallBack.onTextChanged(charSequence.toString(), mSearchToolLayout.getVisibility() == View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        actionSearchQuery.setOnTouchListener((v, event) -> {
            final int DRAWABLE_LEFT = 0;
            final int DRAWABLE_TOP = 1;
            final int DRAWABLE_RIGHT = 2;
            final int DRAWABLE_BOTTOM = 3;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Right/End icon click event
                if (actionSearchQuery.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    if (event.getRawX() >= (actionSearchQuery.getRight() - actionSearchQuery.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        actionSearchQuery.setText("");
                        return true;
                    }
                }
                // Left/Start icon click event
                if (actionSearchQuery.getCompoundDrawables()[DRAWABLE_LEFT] != null) {
                    if (event.getRawX() <= (actionSearchQuery.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width())) {
                        showActionBarSearchView(false);
                        actionSearchQuery.setText("");
                        return true;
                    }
                }
            }
            return false;
        });
    }

    public void showHomeToolsBar() {
        mBaseToolLayout.setVisibility(View.VISIBLE);
        actionHome.setVisibility(View.VISIBLE);
        actionSearch.setVisibility(View.VISIBLE);
        actionProfile.setVisibility(View.VISIBLE);
        actionTitle.setText(getString(R.string.dispatcher_app_name));
        actionMenu.setVisibility(GONE);
        actionCalender.setVisibility(GONE);
        actionBack.setVisibility(GONE);
        actionSubTitle.setVisibility(GONE);
        actionPlus.setVisibility(GONE);
        actionCross.setVisibility(GONE);
        mSearchToolLayout.setVisibility(GONE);
        actionInfo.setVisibility(GONE);
        actionDelete.setVisibility(GONE);
        actionCopy.setVisibility(GONE);
        actionSaveImage.setVisibility(GONE);
    }

    public void showToolWithBack(String toolBarTitle) {
        mBaseToolLayout.setVisibility(View.VISIBLE);
        actionBack.setVisibility(View.VISIBLE);
        actionTitle.setText(toolBarTitle);
        actionMenu.setVisibility(GONE);
        actionHome.setVisibility(GONE);
        actionSearch.setVisibility(GONE);
        actionCalender.setVisibility(GONE);
        actionProfile.setVisibility(GONE);
        actionSubTitle.setVisibility(GONE);
        actionPlus.setVisibility(GONE);
        actionCross.setVisibility(GONE);
        mSearchToolLayout.setVisibility(GONE);
        actionSaveImage.setVisibility(GONE);
    }

    public void showSubTitle(String subTitle) {
        actionSubTitle.setVisibility(View.VISIBLE);
        actionSubTitle.setText(subTitle);
    }

    public void updateToolbarColor(boolean isSOSView) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (isSOSView) {
            mToolbar.setBackgroundColor(getResources().getColor(R.color.sos_fragment_color));
            getWindow().setStatusBarColor(getResources().getColor(R.color.sos_fragment_color));
        } else {
            mToolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        }

    }

    public void loadInnerFragment(int id, Bundle bundle) {
        mBundle = bundle;
        loadInnerFragment(id);
    }

    private void loadInnerFragment(int id) {
        Class<? extends Fragment> fragmentName = null;
        boolean showToolbar = true;
        boolean addToBackStack = false;
        if (mBundle == null) {
            mBundle = new Bundle();
        }
        if (id == EnumConstant.getSupportedFragment.PHONE_LOGIN_FRAGMENT.ordinal()) {
            showToolbar = false;
            fragmentName = PhoneLoginFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.SETUP_ACCOUNT_FRAGMENT.ordinal()) {
            showToolbar = false;
            fragmentName = SettingAccountFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.GROUP_CHAT_FRAGMENT.ordinal()) {
            fragmentName = GroupChatFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.SOS_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = SOSChannelFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_HOME_FRAGMENT.ordinal()) {
            fragmentName = DispatcherHomeFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_ACTIVE_CHANNEL_FRAGMENT.ordinal()) {
            fragmentName = DispatcherActiveChannelFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_LOCATION_FRAGMENT.ordinal()) {
            fragmentName = DispatcherLocationFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_STATUS_FRAGMENT.ordinal()) {
            fragmentName = DispatcherStatusFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.PERSONAL_CHAT_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = PersonalChatFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_ADD_USER_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = AddUserFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_USER_MAP_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = UserMapLocation.class;
        } else if (id == EnumConstant.getSupportedFragment.PROFILE_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = ProfileFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.BILLING_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = BillingFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_ROLE_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = DispatcherRoleFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_ABOUT_APP_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = AboutAppFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.HELP_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = HelpFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.PRIVACY_POLICY_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = PrivacyPolicyFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.DISPATCHER_SUB_CHANNEL_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = SubChannelFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.MEDIAPLAYER_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = MediaPlayerFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.LOCATION_HISTORY_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = LocationHistoryFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.LOCATION_TIMELINE_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = LocationTimelineFragment.class;
        }else if (id == EnumConstant.getSupportedFragment.DISPATCHER_SUB_CHANNEL_LIST.ordinal()) {
            addToBackStack = true;
            fragmentName = SubChannelListFragment.class;
        }else if (id == EnumConstant.getSupportedFragment.MULTIPLE_MESSAGE_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = MultipleMessageFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.GEO_FENCE_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = GeoFenceFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.MARK_ATTENDANCE.ordinal()) {
            addToBackStack = true;
            fragmentName = MarkAttendanceFragment.class;
        } else if (id == EnumConstant.getSupportedFragment.IMAGE_EDIT_FRAGMENT.ordinal()) {
            addToBackStack = true;
            fragmentName = ImageEditFragment.class;
        }

        hideShowToolbar(showToolbar);
        assert fragmentName != null;
        // Make sure bundle not null.
        mLoadFragment = Fragment.instantiate(this, fragmentName.getName(), mBundle);
        if (addToBackStack) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, mLoadFragment, fragmentName.getName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(fragmentName.getName())
                    .commitAllowingStateLoss();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, mLoadFragment, fragmentName.getName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss();
        }
    }

    public void clearFragmentBackStack() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry first = manager.getBackStackEntryAt(0);
            manager.popBackStack(first.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(EnumConstant.Notification.CHAT_FRAGMENT.toString(), false)) {
            openChatFragment(intent);
        }
    }

    private void openChatFragment(Intent intent) {
        Bundle bundle = new Bundle();
        int chatType = intent.getIntExtra(EnumConstant.Notification.CHAT_TYPE.toString(), 0);
        int id = 0;
        Log.d(TAG, "openFragment: chat type" + chatType);
        if (chatType == 1) {
            bundle.putString(EnumConstant.TARGET_USER_NAME, intent.getStringExtra(EnumConstant.Notification.USER_NAME.toString()));
            if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()) {
                int targetSession = intent.getIntExtra(EnumConstant.Notification.USER_SESSION.toString(), -1);
                int userID = mViewModel.getJioTalkieService().getJioPttSession().fetchPttUser(targetSession).getUserID();
                bundle.putInt(EnumConstant.TARGET_USER_ID, userID);
            }
            id = EnumConstant.getSupportedFragment.PERSONAL_CHAT_FRAGMENT.ordinal();
        } else if (chatType == 2) {
            id = EnumConstant.getSupportedFragment.GROUP_CHAT_FRAGMENT.ordinal();
            bottomNavigationView.setSelectedItemId(R.id.navigation_active_channel);
        }
        loadInnerFragment(id, bundle);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (jioTalkieInterface.isBoundToPttServer() && keyCode == mViewModel.getSettings().getPTTKey()) {
            jioTalkieInterface.onTalkKeyDown();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (jioTalkieInterface.isBoundToPttServer() && keyCode == mViewModel.getSettings().getPTTKey()) {
            jioTalkieInterface.onTalkKeyUp();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void handleConnectionState() {
        if (!jioTalkieInterface.isBoundToPttServer()) {
            return;
        }
        Log.d(TAG, "updateConnectionState: called");
        if (jioTalkieInterface.getPttConnectionState() == JioPttEnums.connectionState.SERVER_CONNECTION_LOST) {
            if (!mViewModel.getJioTalkieService().isBoundToPttServer()) {
                return;
            }
            JioTalkieException error = mViewModel.getJioTalkieService().getPttConnectionError();
            if (error != null) {
                Log.e(TAG, "JioTalkieException occur:- " + error.getMessage());
            }
            if (error != null && jioTalkieInterface.isPttReconnecting()) {
                Toast.makeText(DashboardActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            } else if (error != null && error.getExceptionType() == JioTalkieException.ExceptionType.REQUEST_REJECTED) {
                Toast.makeText(DashboardActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            } else if (error != null && error.getExceptionType() == JioTalkieException.ExceptionType.USER_REMOVED) {
                Log.d(TAG, "Login: handleConnectionState: USER_REMOVE");
                Toast.makeText(DashboardActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            } else if (error != null && error.getExceptionType() == JioTalkieException.ExceptionType.CERTIFICATE_ERROR) {
                Toast.makeText(DashboardActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            } else if (error != null && error.getExceptionType() == JioTalkieException.ExceptionType.OTHER_ERROR) {
                Toast.makeText(DashboardActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(DashboardActivity.this, "Connection error!", Toast.LENGTH_LONG).show();
            }
            handlePTTCallUIOnDisconnection();
            loadInnerFragment(EnumConstant.getSupportedFragment.PHONE_LOGIN_FRAGMENT.ordinal());
        }
    }

    public void handlePTTCallUIOnDisconnection() {
        Log.d(TAG, "handlePTTCallUIOnDisconnection ");
        if (calendarDialog != null && calendarDialog.isShowing()) {
            calendarDialog.dismiss();
        }
        pttContainer.setVisibility(GONE);
        if (!mConnectionStateListners.isEmpty()) {
            for (ConnectionStateProvider listener : mConnectionStateListners) {
                listener.isConnected(false);
            }
        }
    }

    private void showFirstRunGuide() {
        // Prompt the user to generate a certificate.
        if (mViewModel.getSettings().hasCertificate()) {
            return;
        }

        mViewModel.getSettings().setFirstLaunch(false);
    }

    public void callLoginAPI(Server server) {
        loadInnerFragment(EnumConstant.getSupportedFragment.SETUP_ACCOUNT_FRAGMENT.ordinal());
        mViewModel.mDatabase.addServer(server);
    }

    public void closeApp() {
        if (mViewModel != null && mViewModel.getJioTalkieService().isBoundToPttServer()) {
            mViewModel.getJioTalkieService().onTalkKeyUp();
            JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
            service.getJioPttSession().updateUserTalkingState(false, mCallId);
        }
        finish();
    }

    private void unregisterTelephonyCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (telephonyManager != null) {
                telephonyManager.unregisterTelephonyCallback(telephonyCallback);
            }
        }
    }

    public void handleOnBackPress() {
        Fragment visibleFragment = getVisibleFragment();
        if (visibleFragment != null && visibleFragment instanceof GroupChatFragment) {
            GroupChatFragment groupChatFragment = (GroupChatFragment) visibleFragment;
            if (visibleFragment != null && groupChatFragment.isItemSelected()) {
                // Deselect the item or do any other action when the item is selected
                groupChatFragment.onItemDeselected();
                return;
            }
        }

        if (visibleFragment != null && visibleFragment instanceof PersonalChatFragment) {
            PersonalChatFragment personalChatFragment = (PersonalChatFragment) visibleFragment;
            if (visibleFragment != null && personalChatFragment.isItemSelected()) {
                // Deselect the item or do any other action when the item is selected
                personalChatFragment.onItemDeselected();
                return;
            }
        }
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            Log.d(TAG, "handleOnBackPress: called count = " + fm.getBackStackEntryCount() + " getName =" +
                    fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName());
            hideKeyboard(this);
            if (!fm.isStateSaved()) {
                fm.popBackStack();
            }
        } else {
            onBackPressed();
        }
    }


    @Override
    public void onBackPressed() {
        // User exit from app only from Home screen (DispatcherHomeFragment)
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            Fragment fm = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            Log.d(TAG, " onBackPressed last active Fragment : " + fm.getTag());
            if (fm instanceof DispatcherLocationFragment
                    || fm instanceof DispatcherStatusFragment
                    || fm instanceof GroupChatFragment) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_home);
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }

    }

    public void handleChatFragment() {
        Fragment visibleFragment = getVisibleFragment();
        if (visibleFragment != null && visibleFragment instanceof GroupChatFragment) {
            GroupChatFragment groupChatFragment = (GroupChatFragment) visibleFragment;
            if (visibleFragment != null && groupChatFragment.isItemSelected()) {
                // Deselect the item or do any other action when the item is selected
                groupChatFragment.onItemDeselected();
                return;
            }
        }

        if (visibleFragment != null && visibleFragment instanceof PersonalChatFragment) {
            PersonalChatFragment personalChatFragment = (PersonalChatFragment) visibleFragment;
            if (visibleFragment != null && personalChatFragment.isItemSelected()) {
                // Deselect the item or do any other action when the item is selected
                personalChatFragment.onItemDeselected();
                return;
            }
        }
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    private String getDurationFrom(String durationTillStr) {
        String durationFrom = new String();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = dateFormat.parse(durationTillStr);
        } catch (ParseException | java.text.ParseException e) {
            e.printStackTrace();
        }
        long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000;
        Date threeDaysBefore = new Date(date.getTime() - threeDaysInMillis);
        durationFrom = dateFormat.format(threeDaysBefore);
        return durationFrom;
    }

    private void turnOnGPS() {
        new GpsUtils(this).turnGPSOn(isGPSEnable -> isGPS = isGPSEnable);
    }

    public void updateChannelInfo(String channelName, int totalUser, int onlineUser) {
        mChannelName.setText(channelName);
        mOnlineUserCount.setText(String.valueOf(onlineUser) + "/" + String.valueOf(totalUser) + " online");
    }

    public void updateState(String newState) {
        handler.removeCallbacksAndMessages(null);
        this.userState = newState;
        updateViewsBasedOnUserState();
    }

    private void updateViewsBasedOnUserState() {
        // Set receiver name if user is in receiver state
        if (userState.equals(EnumConstant.pttAnimationState.USER_STATE_RECEIVER.name())) {
            mTimer.setText(mReceiverUser);
        }
        customCircleView.setColorImage(userState);
        customCircleView.startFillingAnimation();
    }

    @Override
    public void onProgressTimerUpdate(String timeInStringFormat) {
        startTimer();
    }

    public View getPTTCallView() {
        return pttContainer;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    public void startTimer() {
        // Delete previous handler callbacks
        handler.removeCallbacksAndMessages(null);
        /* if (isSOSEnable)
            timeInSeconds = mSOSTime;
        else*/
        timeInSeconds = mMaxTime; // Set the initial time in seconds
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Set the text view text.
                final String timeInStringFormat = formatTime(timeInSeconds);
                mTimer.setText(timeInStringFormat);
                Log.d(TAG, "timeInSeconds: " + timeInStringFormat);
                // Decrease time by 1 second
                timeInSeconds--;
                if (timeInSeconds < 0) {
                    handler.removeCallbacks(this); // Remove the callback if the time reaches 0
                    pttContainer.setVisibility(View.INVISIBLE);
                    // Perform any actions after the timer finishes here
                    if (mViewModel.getJioTalkieService().isBoundToPttServer()) {
                        mViewModel.getJioTalkieService().onTalkKeyUp();
                    }
                    /* if (isSOSEnable)
                        isSOSEnable = false;
                    userState = EnumConstant.pttAnimationState.USER_STATE_DEFAULT.name(); // Reset user state to default
                    updateState(userState); // Update views*/
                    return;
                }
                // Post the code again with a delay of 1 second.
                handler.postDelayed(this, 1000);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int seconds) {
        int minutes = seconds / 60; // Calculate the minutes
        int remainingSeconds = seconds % 60; // Calculate the remaining seconds

        if (minutes > 0 && remainingSeconds == 0) {
            // If there are minutes and no remaining seconds, adjust the minutes and set remaining seconds to 60
            minutes -= 1;
            remainingSeconds = 60;
        }
        return String.format("%02d:%02d sec", minutes, remainingSeconds);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private class TelephonyCallbackListener extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // The phone is ringing
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // A call is currently active
                    closeApp();
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // The phone is neither ringing nor in a call
                    break;
            }
        }
    }

    public boolean isFileSizeAllow(File file, int fileType) {
        boolean isValid = true;
        long fileSizeInMB = file.length() / (1024 * 1024); // convert into MB
        if (fileType == EnumConstant.FILE_TYPE_IMAGE || fileType == EnumConstant.FILE_TYPE_DOC) {
            if (fileSizeInMB > EnumConstant.IMAGE_DOC_MAX_SIZE_MB) {
                Toast.makeText(this, getResources().getString(R.string.image_doc_size_msg), Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        } else if (fileType == EnumConstant.FILE_TYPE_VIDEO && fileSizeInMB > EnumConstant.VIDEO_MAX_SIZE_MB) {
            Toast.makeText(this, getResources().getString(R.string.video_size_msg), Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
    }

    public void launchPersonalChat(String userName, int userId, boolean isPttCall) {
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()) {
            Bundle bundle = new Bundle();
            bundle.putString(EnumConstant.TARGET_USER_NAME, userName);
            bundle.putInt(EnumConstant.TARGET_USER_ID, userId);
            bundle.putBoolean(PersonalChatFragment.IS_PTT_CALL, isPttCall);
            loadInnerFragment(EnumConstant.getSupportedFragment.PERSONAL_CHAT_FRAGMENT.ordinal(), bundle);
        } else {
            Toast.makeText(this, "Service not connected", Toast.LENGTH_SHORT).show();
        }
    }

    public DashboardViewModel getViewModel() {
        return mViewModel;
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle the broadcast
            if (intent != null && "com.jio.jiotalkie.dispatch.MY_ACTION".equals(intent.getAction())) {
                // TODO : Disable Fall Device detection feature for release 9 JAN 2025
              //  MessageEngine.getInstance().msgToChannel(getString(R.string.emergency_msg));
            }
        }
    }

    public void addGeofence(LatLng latLng, float radius) {
        String GEOFENCE_ID = "SOME_GEOFENCE_ID";
        GeoFenceHelper geoFenceHelper = new GeoFenceHelper(this);
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(this);
        Geofence geofence = geoFenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geoFenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geoFenceHelper.getPendingIntent();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Geofence Added...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geoFenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                    }
                });
    }

    public void copyTextToClipboard(String textToCopy) {
        ClipData clip = ClipData.newPlainText("label", textToCopy);
        clipboard.setPrimaryClip(clip);
    }

    public void stopPTTCall() {
        if (isPushToTalkRunning) {
            isPushToTalkRunning = false;
            pttContainer.setVisibility(GONE);
            updateState(EnumConstant.pttAnimationState.USER_STATE_DEFAULT.name());
            handler.removeCallbacksAndMessages(null);

            if (mViewModel.isJioTalkieServiceActive()) {
                JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
                service.onTalkKeyUp();
                service.updateUserTalkingState(false, mCallId);
            }
            if (getVisibleFragment() instanceof DispatcherHomeFragment || getVisibleFragment() instanceof DispatcherStatusFragment
                    || getVisibleFragment() instanceof DispatcherLocationFragment) {
                needSOSButton(true);
            }
            Toast.makeText(getApplicationContext(), getString(R.string.mute_by_server), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopReceivingPttCall(boolean isDeafen) {
        if (isPTTCallGoing) {
            isPTTCallGoing = false;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0);
            mSOSDuringPtt.setVisibility(GONE);
            if (bottomNavigationView != null) {
                enableDisableBottomLayout(bottomNavigationView, false);
            }
            updateState(EnumConstant.pttAnimationState.USER_STATE_DEFAULT.name());
            if (isPushToTalkRunning) {
                updateState(EnumConstant.pttAnimationState.USER_STATE_SPEAKER.name());
            } else {
                if (isLayoutSet) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pttContainer.getLayoutParams();
                    if (mDensityMetric >= DisplayMetrics.DENSITY_450) {
                        params.bottomMargin = 350;
                    } else if (mDensityMetric >= DisplayMetrics.DENSITY_400) {
                        params.bottomMargin = 310;
                    } else if (mDensityMetric >= DisplayMetrics.DENSITY_XHIGH) {
                        params.bottomMargin = 230;
                    } else if (mDensityMetric >= DisplayMetrics.DENSITY_280) {
                        params.bottomMargin = 215;
                    }
                    pttContainer.setLayoutParams(params);
                    isLayoutSet = false;
                }
                pttContainer.setVisibility(View.GONE);
            }
            if (getVisibleFragment() instanceof DispatcherHomeFragment)
                needSOSButton(true);
            if (isDeafen) {
                Toast.makeText(getApplicationContext(), getString(R.string.deafen_by_server), Toast.LENGTH_SHORT).show();
            }
        }
    }
}




