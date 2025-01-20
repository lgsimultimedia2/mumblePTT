package com.jio.jiotalkie.fragment;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.jio.jiotalkie.dispatch.BuildConfig;

import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.model.api.ApkResponseModel;
import com.jio.jiotalkie.network.RetrofitClient;
import com.jio.jiotalkie.util.CommonUtils;

import java.net.HttpURLConnection;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@TrackPerformance(threshold = 300)
public class PhoneLoginFragment extends Fragment {

    private static final String TAG = PhoneLoginFragment.class.getName();
    private DashboardActivity mActivity;
    private Button loginBtn,updateBtn;
    private EditText mPhoneNumber;

    private TextView termsConditions;

    private JioTalkieSettings mJioTalkieSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phone_login_screen, container, false);
        mActivity = (DashboardActivity) getActivity();
        assert mActivity != null;
        mActivity.hideShowToolbar(false);
        mActivity.needBottomNavigation(false);
        mJioTalkieSettings = JioTalkieSettings.getInstance(mActivity.getApplication());
        mActivity.needSOSButton(false);
        mActivity.updateToolbarColor(false);
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        loginBtn =  view.findViewById(R.id.btnLogin);
        updateBtn =  view.findViewById(R.id.updateApk);
        mPhoneNumber =  view.findViewById(R.id.numberedittext);
        termsConditions = view.findViewById(R.id.termsConditions);
        termsConditions.setPaintFlags(termsConditions.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        enableLoginButton(false);
        loginBtn.setOnClickListener(view1 -> {
            if (mActivity != null) {
                startLogin();
            }
        });
        updateBtn.setOnClickListener(view1 -> {
            if (mActivity != null) {
                if (updateBtn.getText().equals(getString(R.string.check_version))) {
                    enableCheckVersionButton(false, getString(R.string.checking_version));
                    checkUpdatedVersion();
                } else if (updateBtn.getText().equals(getString(R.string.download_and_install))) {
                    mActivity.startApkDownload();
                }
            }
        });
        termsConditions.setOnClickListener(view12 -> {
           showEulaDialog();
        });
        mPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                enableLoginButton(charSequence.toString().trim().length() == 10);
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });

        return view;
    }

    private void enableLoginButton(boolean enable) {
        loginBtn.setEnabled(enable);
        if (enable) {
            loginBtn.setAlpha(1f);
            loginBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
            loginBtn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.active_navigation_item_icon_color));
        } else {
            loginBtn.setAlpha(0.5f);
            loginBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
            loginBtn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.active_navigation_item_icon_color));
        }
    }

    private void showEulaDialog() {
        EulaDialogFragment eulaDialogFragment = new EulaDialogFragment();
        eulaDialogFragment.setEulaAcceptCallBack(this::startLogin);
        eulaDialogFragment.show(getChildFragmentManager(), "Eula_dialog");
    }

    private void startLogin() {
        mJioTalkieSettings.setUserAgreementAccept(true);
        mActivity.setLogoutStatus(false);
        mActivity.callZLAApi(mPhoneNumber.getText().toString());
    }

    private void enableCheckVersionButton(boolean enable, String title) {
        updateBtn.setEnabled(enable);
        updateBtn.setText(title);
        if (enable) {
            updateBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
            updateBtn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.btn_bg_enable_color));
        } else {
            updateBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.card_outline_color));
            updateBtn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.btn_bg_disble_color));
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

                    if (isNewVersionFound(apkResponseModel)) {
                        enableCheckVersionButton(true, getString(R.string.download_and_install));
                    } else {
                        enableCheckVersionButton(true, getString(R.string.check_version));
                        Toast.makeText(mActivity,R.string.version_upto_date,Toast.LENGTH_LONG).show();
                    }

                }
            }
            @Override
            public void onFailure(Call<ApkResponseModel> call, Throwable t) {
                Log.e(TAG, "updateApk:onFailure:" + t.getMessage());
                enableCheckVersionButton(true, getString(R.string.check_version));
            }
        });
    }
}
