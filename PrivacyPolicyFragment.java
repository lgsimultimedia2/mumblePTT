package com.jio.jiotalkie.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;


public class PrivacyPolicyFragment extends Fragment {

    private DashboardActivity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privacy_policy, container, false);
        WebView webView = view.findViewById(R.id.privacy_txt);
        webView.loadUrl("file:///android_asset/privacy.html");
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
        mActivity.showToolWithBack(mActivity.getResources().getString(R.string.privacy_policy));
    }
}