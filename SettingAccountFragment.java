package com.jio.jiotalkie.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.performance.TrackPerformance;

@TrackPerformance(threshold = 300)
public class SettingAccountFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_setting_up, container, false);
        DashboardActivity activity = (DashboardActivity) getActivity();
        assert activity != null;
        activity.hideShowToolbar(false);
        activity.needSOSButton(false);
        activity.needBottomNavigation(false);
        return view;
    }
}
