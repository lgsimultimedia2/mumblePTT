package com.jio.jiotalkie.WorkManger;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.jio.jiotalkie.service.JioTalkieService;
import com.jio.jiotalkie.util.DeviceInfoUtils;


public class BackgroundTaskScheduler {

    private static final long INTERVAL = 15 * 60 * 1000; // 15 minutes interval
    private final Handler mHandler = new Handler();
    private final Context mContext;
    private final JioTalkieService mService;

    public BackgroundTaskScheduler(Context context, JioTalkieService service) {
        mContext = context;
        mService = service;
    }

    public void startUpdates() {
        mHandler.postDelayed(mDeviceStatusRunnable, INTERVAL);
    }

    public void stopUpdates() {
        mHandler.removeCallbacks(mDeviceStatusRunnable);
    }

    private final Runnable mDeviceStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (mService != null && mService.isPttConnectionActive() && isValidInfo()) {
                mService.getJioPttSession().SendDeviceInfoToServer(getDeviceSignalStrength(), getBatteryLevel(), getCurrentLocation());
            }
            mHandler.postDelayed(this, INTERVAL);
        }
    };

    private boolean isValidInfo() {
        return DeviceInfoUtils.getBatteryPercentage(mContext) != -1 &&
                !TextUtils.isEmpty(getCurrentLocation()) &&
                !TextUtils.isEmpty(getDeviceSignalStrength());
    }

    private String getBatteryLevel() {
        return String.valueOf(DeviceInfoUtils.getBatteryPercentage(mContext));
    }

    private String getDeviceSignalStrength() {
        return DeviceInfoUtils.getSignalStrength(mContext);
    }

    private String getCurrentLocation() {
        return DeviceInfoUtils.getCurrentLocation();
    }
}
