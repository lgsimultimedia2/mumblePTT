package com.jio.jiotalkie.fragment;

import static android.content.Context.VIBRATOR_SERVICE;

import android.annotation.SuppressLint;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.util.LocationHelperUtils;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.HashMap;
import java.util.Objects;

@TrackPerformance(threshold = 300)
public class SOSBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private static final String TAG = "SOSBottomSheet";
    View view;
    private MediaPlayer mediaPlayer;
    AnimationDrawable animation;
    private TextView userNameTextView;
    private ImageView sosImageView;
    private String mFileName = null;
    private ImageView playButton;
    private Button navigateBtn;
    private Button okBtn;
    private int counterTime = 10;
    private TextView timerText;
    private TextView batteryText;
    private TextView localityView;
    private TextView addressView;
    private DashboardViewModel mViewModel;
    private String userName;
    private CountDownTimer countDownTimer = null;
    private String batteryPercentage;
    private String locationCoordinates;
    private String receivedLocation;
    private String receivedAddress;
    private double latitude;
    private double longitude;
    private boolean hasCameraFlash = false;
    private CameraManager cameraManager;
    private String cameraId;
    private int currentFrameIndex = 0; // Tracks the current frame of animation
    private Handler handler = new Handler();
    private Runnable animationRunnable;

    public static SOSBottomSheetDialogFragment newInstance(Bundle args) {
        SOSBottomSheetDialogFragment fragment = new SOSBottomSheetDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.JioBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.sos_receiver_alert, container, false);
        mViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(DashboardViewModel.class);
        getDialog().setCanceledOnTouchOutside(false);
        Bundle args = getArguments();
        if (args != null) {
            userName = args.getString("userName");
            batteryPercentage = args.getString("battery");
            locationCoordinates = args.getString("location");
            try {
                if (!locationCoordinates.isEmpty()) {
                    if (locationCoordinates.contains(";")) {
                        String[] parts = locationCoordinates.split(";");
                        latitude = Double.parseDouble(parts[0]);
                        longitude = Double.parseDouble(parts[1]);
                    } else if (locationCoordinates.contains(",")) {
                        String[] parts = locationCoordinates.split(",");
                        latitude = Double.parseDouble(parts[0]);
                        longitude = Double.parseDouble(parts[1]);
                    }
                    HashMap<String, String> localityAddress = LocationHelperUtils.getLocalityAddress(getActivity(), latitude, longitude);
                    receivedLocation = localityAddress.get(LocationHelperUtils.LOCALITY);
                    receivedAddress = localityAddress.get(LocationHelperUtils.ADDRESS);

                }
            } catch (Exception e) {
                Log.d(TAG, "Invalid Location: ");
            }
            registerViewModelObserver(userName);
        }
        initView(view);
        //vibrateDevice();
        return view;
    }

    private void initView(View view) {
        userNameTextView = view.findViewById(R.id.user_name);
        playButton = view.findViewById(R.id.iv_sos_play);
        navigateBtn = view.findViewById(R.id.btn_navigate);
        okBtn = view.findViewById(R.id.btn_ok);
        sosImageView = view.findViewById(R.id.iv_sos_receiver_wave);
        timerText = view.findViewById(R.id.timer_text);
        batteryText = view.findViewById(R.id.battery_status_text);
        localityView = view.findViewById(R.id.location);
        addressView = view.findViewById(R.id.location_text);
        playButton.setVisibility(View.INVISIBLE);
        navigateBtn.setEnabled(false);
        okBtn.setEnabled(false);
        userNameTextView.setText(userName);
        batteryText.setText(batteryPercentage + "%");
        if(receivedAddress!=null && !receivedAddress.isEmpty()) {
            addressView.setVisibility(View.VISIBLE);
            addressView.setText(receivedAddress);
        }
        if(receivedLocation!=null && !receivedLocation.isEmpty()){
            localityView.setVisibility(View.VISIBLE);
            localityView.setText(receivedLocation);
        }
        Log.d(TAG, "onCreateView: ");
        initializeAnimation();
        startCountdownTimer();
        playButton.setOnClickListener(v -> {
            playAudioFile(mFileName);

        });
        navigateBtn.setOnClickListener(v -> {
            loadMapNavigationFragment();
            mViewModel.resetCurrentSpeakerState();
            getDialog().dismiss();
        });
        okBtn.setOnClickListener(v -> {
            mViewModel.resetCurrentSpeakerState();
            getDialog().dismiss();
        });
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "onKey: handleOnBackPress() >>>");
                    return true;
                }
                return false;
            }
        });
    }

    @SuppressLint("LongLogTag")
    private void registerViewModelObserver(String userName) {
        mViewModel.observeSOSChatFromDB(userName).observe(this, sosChat -> {
            Log.d(TAG, "registerViewModelObserver: soschatsize" + sosChat.size());
            if (!sosChat.isEmpty()) {
                mFileName = sosChat.get(sosChat.size() - 1).getMedia_path();
            }
        });
    }

    private void playAudioFile(String mediaUri) {
        if (mediaUri == null) {
            return;
        }
        Log.d(TAG, "playAudioFile: called mediaUri =" + mediaUri);

        if (mediaPlayer != null) {
            Log.d(TAG, "playAudioFile: called  isPlaying true");
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                if (animation != null) {
                    pauseAnimation();
                }
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_sos_play));
            } else {
                mediaPlayer.start();
                if (animation != null) {
                    startAnimation();
                }
                if (countDownTimer != null) {
                    startCountdownTimer();
                }
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_sos_resume));
            }

        } else {
            Log.d(TAG, "playAudioFile: called  isPlaying false");
            mediaPlayer = MediaPlayer.create(getActivity(), Uri.parse(mediaUri));
            if (mediaPlayer != null) {
                mediaPlayer.start();
                startAnimation();
                startCountdownTimer();
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_sos_resume));
                Log.d(TAG, "playAudioFile: called  isPlaying false >>> playing started");
            }
        }
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_sos_play));
                    handler.removeCallbacks(animationRunnable);
                }
            });
        }
    }

    public void initializeAnimation() {
        animation = (AnimationDrawable) getResources().getDrawable(R.drawable.sos_animation);
        startAnimation();
    }

    private void startAnimation() {
        sosImageView.setImageDrawable(animation);
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                animation.selectDrawable(currentFrameIndex);
                currentFrameIndex++;
                if (currentFrameIndex < animation.getNumberOfFrames()) {
                    handler.postDelayed(this, 300); // Re-run after 300ms
                } else {
                    resetCurrentFrameIndex();
                }
            }
        };
        handler.post(animationRunnable);
    }

    private void pauseAnimation() {
        if (animationRunnable != null) {
            handler.removeCallbacks(animationRunnable);
        }

        if (animation != null) {
            currentFrameIndex = getCurrentFrameIndex();
        }
    }

    private int getCurrentFrameIndex() {
        return mediaPlayer.getCurrentPosition() / 300;
    }

    private void resetCurrentFrameIndex() {
        currentFrameIndex = 0;
        playButton.setVisibility(View.VISIBLE);
        sosImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_sos_receive_wave));
    }

    public void startCountdownTimer() {
        countDownTimer = new CountDownTimer(counterTime * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                counterTime = (int) (millisUntilFinished / 1000);
                timerText.setText("00:0" + counterTime + " sec");
                Log.d(TAG, "onTick: " + counterTime);
            }

            @Override
            public void onFinish() {
                counterTime = 10;
                timerText.setText("00:" + counterTime + " sec");
                playButton.setVisibility(View.VISIBLE);
                if (getActivity() != null) {
                    okBtn.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.bt_acknowledge));
                    if (!locationCoordinates.isEmpty()) {
                        navigateBtn.setEnabled(true);
                        navigateBtn.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.bt_navigate));
                    }
                    playButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_sos_play));
                    okBtn.setEnabled(true);
                    view.setOnKeyListener(null);
                }
            }
        };
        countDownTimer.start();
    }

    private void loadMapNavigationFragment(){
        Bundle args=new Bundle();
        args.putDouble("latitude",latitude);
        args.putDouble("longitude",longitude);
        Fragment fragment = new UserLocationFragment();
        fragment.setArguments(args);
        Class<? extends Fragment> fragmentName = UserLocationFragment.class;
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, fragmentName.getName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(fragmentName.getName())
                .commitAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: ");
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
        if (animation != null) {
            animation = null;
        }
        countDownTimer.cancel();
        countDownTimer = null;
        if (mViewModel.observeSOSStateLiveData() != null) {
            mViewModel.observeSOSStateLiveData().removeObservers(this);
        }
        if(view !=null){
            view.setOnKeyListener(null);
        }
    }

    /**
     *  Vibrates the device for 1 second when SOS alert is received
     */
    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.EFFECT_HEAVY_CLICK));
            } else {
                vibrator.vibrate(1000);
            }
        }
    }

    public void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (animation != null) {
                pauseAnimation();
            }
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_sos_play));
        }
    }
}
