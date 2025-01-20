package com.jio.jiotalkie.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;

public class DispatcherStatusFragment extends Fragment {

    private DashboardActivity mActivity;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dispatcher_status, container, false);
        mActivity = (DashboardActivity) getActivity();
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        updateToolBar();
    }
    private void updateToolBar() {
        mActivity.showHomeToolsBar();
        mActivity.needBottomNavigation(true);
        mActivity.needSOSButton(true);
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar_main);
        toolbar.findViewById(R.id.actionSearch).setVisibility(View.GONE);
        toolbar.findViewById(R.id.actionCalender).setVisibility(View.VISIBLE);
    }
}
