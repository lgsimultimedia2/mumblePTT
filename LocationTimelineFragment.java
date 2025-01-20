package com.jio.jiotalkie.fragment;

import static android.view.View.GONE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.customservice.dataManagment.imodels.IUserModel;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.LocationTimelineAdapter;
import com.jio.jiotalkie.dataclass.UserLocationTimelineData;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@TrackPerformance(threshold = 300)
public class LocationTimelineFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private ArrayList<UserLocationTimelineData> mLocationTimelineList = new ArrayList<>();
    private DashboardActivity mActivity;
    private String mUserName, mUserBattery, mUserNetwork;
    private int mUserId;
    private DashboardViewModel mViewModel;
    private List<? extends IUserModel> mOnlineUserList;
    private boolean mIsOnline = false;
    private TextView statusInfo;
    public static final String USER_LOCATION_TIMELINE = "user_location_timeline";
    public static final String USER_NAME = "user_name";
    public static final String USER_BATTERY = "user_battery";
    public static final String USER_NETWORK = "user_network";
    public static final String USER_ID = "user_id";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        mViewModel = new ViewModelProvider(mActivity).get(DashboardViewModel.class);
        Bundle args = getArguments();
        if (args != null) {
            mLocationTimelineList = (ArrayList<UserLocationTimelineData>) args.getSerializable(USER_LOCATION_TIMELINE);
            mUserName = args.getString(USER_NAME);
            mUserBattery = args.getString(USER_BATTERY);
            mUserNetwork =  args.getString(USER_NETWORK);
            mUserId = args.getInt(USER_ID);
        }

        mOnlineUserList = fetchOnlineUserLists();
        int index = CommonUtils.getSearchItemIndex(mOnlineUserList, mUserName);
        if (index >= 0) {
            mIsOnline = true;
        }
        mActivity.showToolWithBack(mUserName);
        mActivity.needBottomNavigation(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        registerViewModelObserver();
        View view = inflater.inflate(R.layout.location_timeline_fragment, container, false);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        statusInfo = view.findViewById(R.id.status_info);
        TextView batteryCapacity = view.findViewById(R.id.battery_capacity_info);
        ImageView statusIcon = view.findViewById(R.id.status_icon);
        TextView phoneSignalInfo = view.findViewById(R.id.phone_signal_info);
        phoneSignalInfo.setText(mUserNetwork);
        if (mIsOnline) {
            statusIcon.setImageResource(R.drawable.online_icon);
            statusInfo.setText(getResources().getString(R.string.status_online));
        } else {
            statusIcon.setImageResource(R.drawable.offline_icon);
        }
        batteryCapacity.setText(mActivity.getResources().getString(R.string.battery_capacity, mUserBattery));
        LocationTimelineAdapter locationTimelineAdapter = new LocationTimelineAdapter(mActivity, mLocationTimelineList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(locationTimelineAdapter);
        return view;
    }

    private void registerViewModelObserver() {
        mViewModel.observeRegisterUserData().observe(this, registeredUsers -> {
            int index = CommonUtils.getSearchItemIndexUser(registeredUsers, mUserName);
            if (index>= 0) {
                mActivity.showSubTitle(mActivity.getResources().getString(R.string.mobile_no, registeredUsers.get(index).getMsisdn()));
                if (!mIsOnline) {
                    if (registeredUsers.get(index).getLastSeen() == null || registeredUsers.get(index).getLastSeen().isEmpty()) {
                        statusInfo.setText(getResources().getString(R.string.status_offline));
                    } else {
                        statusInfo.setText(getResources().getString(R.string.user_last_seen_info, CommonUtils.getFormattedLastSeen(registeredUsers.get(index).getLastSeen())));
                    }
                }
            }
        });
    }

    private List<? extends IUserModel> fetchOnlineUserLists() {
        if (isJioTalkieServiceActive()) {
            return mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getUserList();
        }
        return Collections.emptyList();
    }

    private boolean isJioTalkieServiceActive() {
        return mViewModel != null
                && mViewModel.getJioTalkieService().isBoundToPttServer()
                && mViewModel.getJioTalkieService().isPttConnectionActive()
                && mViewModel.getJioTalkieService().getJioPttSession() != null;
    }
}
