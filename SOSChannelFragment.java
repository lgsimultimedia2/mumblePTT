package com.jio.jiotalkie.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.application.customservice.common.JioPttEnums;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.drawable.SOSCustomCircleView;
import com.jio.jiotalkie.service.JioTalkieService;
import com.jio.jiotalkie.util.ADCInfoUtils;
import com.jio.jiotalkie.util.DeviceInfoUtils;
import com.jio.jiotalkie.util.MessageIdUtils;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.Objects;

import com.application.customservice.Mumble;

public class SOSChannelFragment extends Fragment implements SOSCustomCircleView.CountdownListener {

    private final static String TAG = SOSChannelFragment.class.getName();
    private static final long USER_TALKING_FALSE_DELAY = 500;
    private SOSCustomCircleView sosCustomCircleView;
    FrameLayout frameLayout;
    private TextView textView;
    private DashboardActivity mActivity;
    private DashboardViewModel mViewModel;
    private boolean isCurrentSpeakerSOS = false;
    private ProgressBar mRequestFloorProgress;
    private TextView mRequestFloorMsg;
    private CountDownTimer countDownTimer;
    private boolean isSOSAnimationRunning = false;
    private String mCallID;
    private boolean isSOSCompleted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.s_o_s_channel, container, false);
        mViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(DashboardViewModel.class);
        mActivity = (DashboardActivity) getActivity();
        mCallID = MessageIdUtils.generateUUID();
        setSOSToServer(true);
        initView(view);
        registerStateObserver();
        return view;
    }

    private void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            mActivity.updateToolbarColor(true);
            mActivity.showChannelTools(true);
            mActivity.needSOSButton(false);
            mActivity.needBottomNavigation(false);
            View decorView = mActivity.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        } else {
            mActivity.updateToolbarColor(false);
            mActivity.showChannelTools(false);
            mActivity.needSOSButton(true);
            mActivity.needBottomNavigation(true);
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

    }
    private void initView(View view) {
        frameLayout = view.findViewById(R.id.sos_frame_layout);
        textView = view.findViewById(R.id.online_count_text);
        mRequestFloorProgress = view.findViewById(R.id.progress_request_floor);
        mRequestFloorMsg = view.findViewById(R.id.tv_requesting_floor);
        startSOSCountdownTimer();
        mRequestFloorProgress.setVisibility(View.VISIBLE);
        mRequestFloorMsg.setVisibility(View.VISIBLE);
        frameLayout.setVisibility(View.GONE);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "onKey: handleOnBackPress() >>>");
                    if (sosCustomCircleView != null) {
                        //sosCustomCircleView.forceStopAnimation();
                        return true;
                    }
                }
                return false;
            }
        });
        setFullscreen(true);
    }

    private void startSOSCountdownTimer() {
        countDownTimer = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                mRequestFloorProgress.setVisibility(View.VISIBLE);
                mRequestFloorMsg.setVisibility(View.VISIBLE);
            }
            public void onFinish() {

                if (mViewModel.getJioTalkieService().isBoundToPttServer()) {
                    mViewModel.getJioTalkieService().onTalkKeyUp();
                }
                if (sosCustomCircleView != null) {
                    sosCustomCircleView.forceStopAnimation();
                }
                ADCInfoUtils.floorGrantedInfo(false,0,"", mViewModel.getUserId(), mViewModel.getChannelId(), "sos");
                setSOSToServer(false);
                setFullscreen(false);
                sosCustomCircleView = null;
                mActivity.handleOnBackPress();
            }
        }.start();
    }

    private void updateCount() {
        int onlineCount = 0;
        int totalCount = 0;
        isSOSAnimationRunning = true;
        sosCustomCircleView.startFillingAnimation();
        sosCustomCircleView.startCountdownTimer();
        sosCustomCircleView.setColorImage();
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive() &&
                mViewModel.getJioTalkieService().getJioPttSession() != null) {
            onlineCount = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getUserList().size();
            totalCount = mViewModel.getTotalRegUserCount();
            mViewModel.getJioTalkieService().onTalkKeyDown();
        }
        textView.setText(mActivity.getString(R.string.sos_user_info,onlineCount,totalCount));
    }

    private void registerStateObserver() {
        mViewModel.observeCurrentSpeakerStateData().observe(this, state -> {
            if (isSOSAnimationRunning) {
                Log.d(TAG, "registerStateObserver: return.. isSOSAnimationRunning already running");
                return;
            }
            isCurrentSpeakerSOS = state.isSelfCurrentSpeaker() && state.isSelfCurrentSpeakerSOS();
            updateSOSSpeakerView();
        });
    }

    private void updateSOSSpeakerView() {
        if (!isCurrentSpeakerSOS) {
            return;
        }

        if (isSOSCompleted) {
            mActivity.handleOnBackPress();
            return;
        }

        countDownTimer.cancel();
        mRequestFloorProgress.setVisibility(View.GONE);
        mRequestFloorMsg.setVisibility(View.GONE);
        frameLayout.setVisibility(View.VISIBLE);
        if (mViewModel.getJioTalkieService().isBoundToPttServer()) {
            mViewModel.getJioTalkieService().getJioPttSession().setSosEnable(true, null);
        }
        sosCustomCircleView = new SOSCustomCircleView(getActivity());
        sosCustomCircleView.setCountdownListener(this);
        frameLayout.addView(sosCustomCircleView);
        ADCInfoUtils.floorGrantedInfo(true,0,"",mViewModel.getUserId(),mViewModel.getChannelId(),"sos");
        updateCount();
    }

    private void setSOSToServer(boolean sosValue) {
        JioTalkieService jioTalkieService = (JioTalkieService) mViewModel.getJioTalkieService();
        if (jioTalkieService.isBoundToPttServer()) {
            Mumble.UserState.Builder sosState = Mumble.UserState.newBuilder();
            sosState.setSosSpeaker(sosValue);
            sosState.setUserTalking(sosValue);
            String currentLocation = DeviceInfoUtils.getCurrentLocation();
            if (currentLocation != null && !TextUtils.isEmpty(currentLocation)) {
                sosState.setUserLocation(currentLocation);
            }
            if (sosValue) {
                String battery_strength = String.valueOf(DeviceInfoUtils.getBatteryPercentage(getContext()));
                if (!TextUtils.isEmpty(battery_strength)) {
                    sosState.setUserBatteryStength(battery_strength);
                }
                String signal_strength = DeviceInfoUtils.getSignalStrength(getContext());
                if (!TextUtils.isEmpty(signal_strength)) {
                    sosState.setUserNetworkStength(signal_strength);
                }
            }
            sosState.setCallId(mCallID);
            jioTalkieService.getJioPttSession().sendPttRequest(sosState.build(), JioPttEnums.TCPProtoMsgType.MsgUserState.ordinal());
        }
    }

    private void showSentMessage() {
        Snackbar snackbar = Snackbar.make(frameLayout, mActivity.getString(R.string.sos_sent_msg), BaseTransientBottomBar.LENGTH_LONG);
        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(mActivity.getColor(R.color.sos_snack_bg_color));
        TextView snackTV = snackBarView.findViewById(R.id.snackbar_text);
        snackTV.setTextColor(mActivity.getColor(R.color.sos_fragment_color));
        snackbar.show();
    }

    @Override
    public void onTimerFinish() {
        isSOSCompleted = true;
        if (getActivity() != null) {
            if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()
                    && mViewModel.getJioTalkieService().getJioPttSession() != null) {
                mViewModel.getJioTalkieService().onTalkKeyUp();
                mViewModel.getJioTalkieService().getJioPttSession().setSosEnable(false, null);
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> setSOSToServer(false), USER_TALKING_FALSE_DELAY);
            isSOSAnimationRunning = false;
            sosCustomCircleView = null;
            setFullscreen(false);
            showSentMessage();
            mActivity.handleOnBackPress();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}