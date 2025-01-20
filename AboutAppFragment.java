package com.jio.jiotalkie.fragment;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.api.ApkResponseModel;
import com.jio.jiotalkie.network.RetrofitClient;
import com.jio.jiotalkie.util.CommonUtils;

import java.io.File;
import java.net.HttpURLConnection;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AboutAppFragment extends Fragment {
    private static final String TAG = AboutAppFragment.class.getName();
    private DashboardActivity mActivity;
    private TextView mVersion;
    private TextView mServerIp;
    private TextView mBuildType;
    private TextView mServerPort;
    private TextView mAwsPort;
    private TextView mLastUpdated;
    private TextView mApiInfo;
    private TextView mChatSize;
    private Button mCheckVersion;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about_app, container, false);
        mVersion = view.findViewById(R.id.app_version_value);
        mChatSize = view.findViewById(R.id.userdata_value);
        mServerIp = view.findViewById(R.id.serverip_value);
        mServerPort = view.findViewById(R.id.server_port_value);
        mAwsPort = view.findViewById(R.id.aws_port_value);
        mBuildType = view.findViewById(R.id.build_type_value);
        mLastUpdated = view.findViewById(R.id.apk_checked_date);
        mApiInfo = view.findViewById(R.id.apk_api_info);
        mCheckVersion = view.findViewById(R.id.check_version);
        mCheckVersion.setOnClickListener(view1 -> {
            mApiInfo.setVisibility(View.INVISIBLE);
            if (mCheckVersion.getText().equals(getString(R.string.check_version))) {
                enableCheckVersionButton(false, getString(R.string.checking_version));
                checkUpdatedVersion();
            } else if (mCheckVersion.getText().equals(getString(R.string.download_and_install))) {
                mActivity.startApkDownload();
            }
        });
        initValue();
        return view;
    }

    private void enableCheckVersionButton(boolean enable, String title) {
        mCheckVersion.setEnabled(enable);
        mCheckVersion.setText(title);
        if (enable) {
            mCheckVersion.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
            mCheckVersion.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.btn_bg_enable_color));
        } else {
            mCheckVersion.setTextColor(ContextCompat.getColor(getContext(), R.color.card_outline_color));
            mCheckVersion.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.btn_bg_disble_color));
        }
    }

    private boolean isNewVersionFound(ApkResponseModel apkResponseModel) {
        String latestVersion = apkResponseModel.getAndroidApkVersion().replace("V", "");
        String[] updateVersion = latestVersion.split("[.]");
        int latestApkVersionCode = Integer.parseInt(updateVersion[0]) * 10000 + Integer.parseInt(updateVersion[1]) * 100 + Integer.parseInt(updateVersion[2]);
        return latestApkVersionCode > BuildConfig.VERSION_CODE;
    }

    private void checkUpdatedVersion() {
        Call<ApkResponseModel> call = RetrofitClient.getRetrofitClient(RetrofitClient.BaseUrlType.ApkDownloadURL).updateApkVersion();
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApkResponseModel> call, @NonNull Response<ApkResponseModel> response) {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    mActivity.getViewModel().getSettings().setLastVersionCheckDateTime(CommonUtils.getFormattedDateTime(System.currentTimeMillis()));
                    ApkResponseModel apkResponseModel = response.body();
                    mApiInfo.setVisibility(View.VISIBLE);
                    mLastUpdated.setText(mActivity.getViewModel().getSettings().getLastVersionCheckDateTime());
                    if (isNewVersionFound(apkResponseModel)) {
                        mApiInfo.setText(getString(R.string.new_version_found, apkResponseModel.getAndroidApkVersion()));
                        enableCheckVersionButton(true, getString(R.string.download_and_install));
                    } else {
                        enableCheckVersionButton(true, getString(R.string.check_version));
                        mApiInfo.setText(R.string.version_upto_date);
                    }

                }
            }
            @Override
            public void onFailure(Call<ApkResponseModel> call, Throwable t) {
                Log.e(TAG, "updateApk:onFailure:" + t.getMessage());
                enableCheckVersionButton(true, getString(R.string.check_version));
                mApiInfo.setText("Unable to check version,Please try later");
                mApiInfo.setVisibility(View.VISIBLE);
            }
        });
    }

    private long getApkInstalledTime() {
        PackageManager pm = mActivity.getPackageManager();
        long installedTime = 0;
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            String appFile = appInfo.sourceDir;
            installedTime = new File(appFile).lastModified(); //Epoch Time
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "App Installed Time " + installedTime);
        return installedTime;

    }

    private void initValue() {
        mVersion.setText(BuildConfig.VERSION_NAME);
        mServerIp.setText(BuildConfig.SERVER_IP);
        mBuildType.setText(BuildConfig.BUILD_TYPE);
        mServerPort.setText(""+mActivity.getViewModel().getSettings().getServerPort());
        mAwsPort.setText(String.valueOf(BuildConfig.AWS_SERVER_PORT));
        String lastVersionTime = mActivity.getViewModel().getSettings().getLastVersionCheckDateTime();
        if (TextUtils.isEmpty(lastVersionTime)) {
            lastVersionTime = CommonUtils.getFormattedDateTime(getApkInstalledTime());
        }
        mLastUpdated.setText(lastVersionTime);
        mChatSize.setText(CommonUtils.getUserChatSize(getActivity()));
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
        mActivity.showToolWithBack(mActivity.getResources().getString(R.string.about_app));
    }
}