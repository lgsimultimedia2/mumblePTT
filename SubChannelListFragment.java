package com.jio.jiotalkie.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.application.customservice.dataManagment.imodels.IChannelModel;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.SubChannelListAdapter;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SubChannelListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SubChannelListFragment extends Fragment {

    private RecyclerView mSubChannelListView;

    private SubChannelListAdapter channelListAdapter;

    ArrayList<IChannelModel> mSubChannellList;

    private DashboardViewModel mViewModel;

    private DashboardActivity mActivity;


    public SubChannelListFragment() {
    }

    public static SubChannelListFragment newInstance(String param1, String param2) {
        SubChannelListFragment fragment = new SubChannelListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        mViewModel = new ViewModelProvider(mActivity).get(DashboardViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subchannel_list, container, false);
        mSubChannelListView = view.findViewById(R.id.subChannel_list);
        mSubChannellList = new ArrayList<IChannelModel>();
        fetchSubChannelList();
        initRecyclerView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivity.showToolWithBack(getString(R.string.sub_channel));
        mActivity.needSOSButton(false);
        mActivity.needBottomNavigation(false);
    }

    private void fetchSubChannelList() {
        // Not providing ModelHandler as already get calls are available

//            Map<Integer,Channel> channelList = mViewModel.getJioTalkieService().getJioModelHandler().getChannels();
        int parentChannelId = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID();
        IChannelModel parentChannel = mViewModel.getJioTalkieService().getJioPttSession().fetchPttChannel(parentChannelId);
        mSubChannellList = parentChannel.getChildChannels();

    }

    private void initRecyclerView() {
        channelListAdapter = new SubChannelListAdapter(mSubChannellList);
        mSubChannelListView.setAdapter(channelListAdapter);

    }
}