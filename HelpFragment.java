package com.jio.jiotalkie.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
public class HelpFragment extends Fragment {
    private DashboardActivity mActivity;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        WebView webView = view.findViewById(R.id.help_txt);
        webView.loadUrl("file:///android_asset/help.html");
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
        mActivity.showToolWithBack(mActivity.getResources().getString(R.string.help));
    }

}