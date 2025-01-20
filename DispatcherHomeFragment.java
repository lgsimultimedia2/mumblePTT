package com.jio.jiotalkie.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.customservice.dataManagment.imodels.IChannelModel;
import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.DispatcherAdapter;
import com.jio.jiotalkie.adapter.provider.UserAdapterProvider;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.DispatcherItemModel;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.service.JioTalkieService;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.DateUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.util.UserPinnedStateHandler;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.application.customservice.wrapper.Constants;
import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.Mumble;

@TrackPerformance(threshold = 300)
public class DispatcherHomeFragment extends Fragment {

    private static final String TAG = DispatcherHomeFragment.class.getName();

    private DashboardActivity mActivity;

    private RecyclerView mHomeScreenListView;

    private DispatcherAdapter mDispatcherHomeScreenAdapter;

    private DashboardViewModel mViewModel;

    private List<? extends IUserModel> mOnlineUserList;
    private List<RegisteredUser> mRegUsersList;
    private List<RegisteredUser> mServerRegUserList = new ArrayList<>();
    private List<DispatcherItemModel> mItemModelList;
    private String dispatcherChannelName;
    private int dispatcherChannelId;
    private boolean searchVisible;
    private boolean isContextMenuVisible = false;
    private AlertDialog mKillDialog;
    private final DashboardActivity.SearchQueryCallBack mSearchQueryCallBack = new DashboardActivity.SearchQueryCallBack() {

        @Override
        public void onTextChanged(String queryText, boolean isSearchVisible) {
            Log.d(TAG, "onTextChanged : " + queryText);
            searchVisible = isSearchVisible;
            mDispatcherHomeScreenAdapter.applyFilter(queryText.trim());
            // Now search view hide then refresh the user list from server
            if (!searchVisible) {
                refreshUserList();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        mViewModel = new ViewModelProvider(mActivity).get(DashboardViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dispatcher_home, container, false);
        mViewModel.getSettings().setAudioMode(JioTalkieSettings.AUDIO_MODE_PUSH_TO_TALK);
        mHomeScreenListView = view.findViewById(R.id.homeScreenList);
        mOnlineUserList = fetchOnlineUserLists();
        return view;
    }

    private List<? extends IUserModel> fetchOnlineUserLists() {
        if (isJioTalkieServiceActive()) {
            return mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getUserList();
        }
        return Collections.emptyList();
    }

    private void refreshUserList() {
        mOnlineUserList = fetchOnlineUserLists();
        if (isJioTalkieServiceActive() && mRegUsersList != null) {
            refreshUserListAdapter();
        }
    }

    public boolean isSubChannelAvailable(){
        // Not providing ModelHandler as already get calls are available

        //            Map<Integer, ChannelModel> channels = mViewModel.getJioTalkieService().getJioModelHandler().getChannels();
        if(mViewModel.getJioTalkieService().isPttConnectionActive()) {
            List<IChannelModel> subChannels = mViewModel.getJioTalkieService().getJioPttSession().fetchPttChannel(mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID()).getChildChannels();
            return (!subChannels.isEmpty());
        }
        return  false;
    }

    private boolean isJioTalkieServiceActive() {
        return mViewModel != null
                && mViewModel.getJioTalkieService().isBoundToPttServer()
                && mViewModel.getJioTalkieService().isPttConnectionActive()
                && mViewModel.getJioTalkieService().getJioPttSession() != null;
    }

    public void initRecyclerView() {
        mItemModelList = new ArrayList<>();
        mDispatcherHomeScreenAdapter = new DispatcherAdapter(mActivity,mUserAdapterProvider, mItemModelList,isSubChannelAvailable());
        addCreateChannelAndUserItem();
        mHomeScreenListView.setAdapter(mDispatcherHomeScreenAdapter);
        mActivity.needSOSButton(true);
        mActivity.needBottomNavigation(true);
    }

    private void refreshUserListAdapter() {
        mServerRegUserList.clear();
        mDispatcherHomeScreenAdapter.clear();
        // Iterate through all users and add to adapter
        String dispatcherUserName = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserName();
        addCreateChannelAndUserItem();
        addDispatcherChannelInfo();
        for (RegisteredUser user : mRegUsersList) {
            boolean isOnline = false;
            boolean isMute = false;
            boolean isDeafen = false;
             byte[] textureData = null;
            int sessionId = -1;
            mServerRegUserList.add(user);
            int index = CommonUtils.getSearchItemIndex(mOnlineUserList, user.getName());
            if (index >= 0) {
                isOnline = true;
                isMute = mOnlineUserList.get(index).isMute();
                isDeafen = mOnlineUserList.get(index).isDeaf();
                sessionId = mOnlineUserList.get(index).getSessionID();
                textureData=mOnlineUserList.get(index).getUserTexture();
            }
            boolean isDispatcherUser = dispatcherUserName.equals(user.getName());
            DispatcherItemModel userItem = new DispatcherItemModel(DispatcherItemModel.ItemType.USER, user.getName());
            userItem.setUserId(user.getUserId());
            userItem.setDispatcherUser(isDispatcherUser);
            userItem.setHasOnline(isOnline);
            userItem.setLastSeenMsg(user.getLastSeen());
            userItem.setSessionId(sessionId);
            userItem.setMute(isMute);
            userItem.setDeafen(isDeafen);
            userItem.setUserRole(user.getUserRole());
            userItem.setTexture(textureData);
            userItem.setPinned(UserPinnedStateHandler.getInstance().isPinned(user.getUserId()));
            Log.d(TAG, "UserModel Name : " + user.getName() + "Id "+user.getUserId() + " and role: " + user.getUserRole());
            mDispatcherHomeScreenAdapter.add(userItem);
            mActivity.setProfilePic();
        }

        mActivity.updateChannelInfo(dispatcherChannelName, mRegUsersList.size(), mOnlineUserList.size());
        mActivity.setCurrentChannelName(dispatcherChannelName);
        mDispatcherHomeScreenAdapter.setOnlineRegUsers(mOnlineUserList.size());
        mDispatcherHomeScreenAdapter.setTotalRegUsers(mRegUsersList.size());
        mDispatcherHomeScreenAdapter.setChannel(dispatcherChannelName, dispatcherChannelId);
        mDispatcherHomeScreenAdapter.refreshUserList();

        Log.i("Karthik" , "refreshUserListAdapter: "+mOnlineUserList.size() +" "+mRegUsersList.size());
    }

    private void registerViewModelObserver() {
        // Once app launch then request the updated list for render user lists
        if (isJioTalkieServiceActive()) {
            mViewModel.getJioTalkieService().getJioPttSession().fetchPttUserList();
        }
        mViewModel.observeUserAddedOrDeleted().observe(this,regUserState -> {
            Log.d(TAG, "observeUserAddedOrDeleted: = " + regUserState.toString());
            // if Someone Add or Delete members from sever, need to refresh channel members.
            resetAndFetchNewList();
        });
        mViewModel.observeRegisterUserData().observe(this, registeredUsers -> {
            Log.d(TAG, "observeRegisterUserData no of users: " + registeredUsers.size());
            // if first time fetch the list the refresh the UX, other case update the local lists.
            if (mRegUsersList == null) {
                mRegUsersList = registeredUsers;
                refreshUserList();
            } else {
                mRegUsersList = registeredUsers;
            }
        });

        mViewModel.observeUserStateUpdate().observe(this, user -> {
            Log.e(TAG,"observeUserStateUpdate");
            // if current list reset than request for updated sheet.
            if (mRegUsersList == null) {
                if (isJioTalkieServiceActive()) {
                    mViewModel.getJioTalkieService().getJioPttSession().fetchPttUserList();
                }
            } else {
                // When Fragment is visible and context menu & search filter hide,
                if (isVisible() && !searchVisible && !isContextMenuVisible) {
                    refreshUserList();
                }
            }
        });

        mViewModel.observerUserStateData().observe(this, userState -> {
            Log.d(TAG, "userState: " + userState.getUserState() + " name: " + userState.getUser().getUserName() + " channelID: " + userState.getUser().getUserChannel().getChannelID());

            // Ideally, we do not want server   to send updates of channel Id other than ours. But,
            // till server implements this, we are going to check if update is from our channel.
            if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive() && mViewModel.getJioTalkieService().getJioPttSession() != null) {
                int selfChannelId = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID();
                if (userState.getUser().getUserChannel().getChannelID() != selfChannelId) {
                    Log.d(TAG, "observerUserStateData, ignoring update of user not in our channel: " + userState.getUser().getUserName());
                    return;
                }
            }

            switch (userState.getUserState()) {
                case USER_JOINED:
                case USER_CONNECTED:
                    // Reset current user list and request to fetch updated list
                    mRegUsersList = null;
                    if (isJioTalkieServiceActive()) {
                        mViewModel.getJioTalkieService().getJioPttSession().fetchPttUserList();
                        Log.d(TAG, "observerUserStateData, ignoring update of user not in our channel: " + userState.getUser().getUserName());
                        refreshUserList();
                    }
                    break;
                case USER_REMOVED:
                    if (userState.getUserState() == EnumConstant.userState.USER_REMOVED) {
                        // update master list with last seen of disconnected user
                        int index = CommonUtils.getSearchItemIndexUser(mRegUsersList, userState.getUser().getUserName());
                        if (index >= 0) {
                            mRegUsersList.get(index).setLastSeen(DateUtils.getLastSeenDateFormat(System.currentTimeMillis()));
                        }
                    }
                    refreshUserList();
                    break;
            }
        });
        mViewModel.observeChannelState().observe(this,channel -> {
            // if Someone add or kick  members from your channels, need to refresh channel members.
            Log.d(TAG, "observeChannelState: " + channel.getChannelName());
            resetAndFetchNewList();
        });
    }

    private void addDispatcherChannelInfo() {
        if (isJioTalkieServiceActive() && mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser() != null) {
            dispatcherChannelName = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserChannel().getChannelName();
            dispatcherChannelId = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserChannel().getChannelID();
            Log.d(TAG, "ChannelModel Name is " + dispatcherChannelName);
            DispatcherItemModel dispatcherChannel = new DispatcherItemModel(DispatcherItemModel.ItemType.CHANNEL, dispatcherChannelName);
            mDispatcherHomeScreenAdapter.add(dispatcherChannel);
        }
    }

    private void addCreateChannelAndUserItem() {
        DispatcherItemModel addChannel = new DispatcherItemModel(DispatcherItemModel.ItemType.ADD_SUB_CHANNEL, getResources().getString(R.string.create_sub_channel));
        mDispatcherHomeScreenAdapter.add(addChannel);
        // Hide Add user option from top level item , it's move to context menu of channel.
        /*
        DispatcherItemModel addUser = new DispatcherItemModel(DispatcherItemModel.ItemType.ADD_USER, getResources().getString(R.string.dispatcher_add_user));
         mDispatcherHomeScreenAdapter.add(addUser);*/
    }
    @Override
    public void onResume() {
        super.onResume();
        initRecyclerView();
        registerViewModelObserver();
        showReceiverAnimationForPTTCall(); // this is for scenario when network resume and launch this fragment with group PTT Call ongoing already on channel
        mActivity.showHomeToolsBar();
        mActivity.needBottomNavigation(true);
        searchVisible = false;
        isContextMenuVisible = false;
        mActivity.setSearchQueryCallBack(mSearchQueryCallBack);
        // Reset current user list and request to fetch updated list
        mRegUsersList = null;
        if (isJioTalkieServiceActive()) {
            mViewModel.getJioTalkieService().getJioPttSession().fetchPttUserList();
        }
        refreshUserList();
        registerReceiveSOSandPTT();
      
    }

    @Override
    public void onStop() {
        super.onStop();
        mActivity.setSearchQueryCallBack(null);
        unregisterReceiveSOSandPTT();
        if (mKillDialog != null && mKillDialog.isShowing()) {
            mKillDialog.dismiss();
        }
        if (mDispatcherHomeScreenAdapter != null) {
            mDispatcherHomeScreenAdapter.dismissUserPopupMenu();
        }
    }

    private void unregisterReceiveSOSandPTT() {
        mViewModel.observeSelfUserTalkState().removeObservers(this);
        mViewModel.observeSOSStateLiveData().removeObservers(this);
    }
    private void dismissPopUp() {
        if (mKillDialog != null && mKillDialog.isShowing()) {
            mKillDialog.dismiss();
        }
        if(mDispatcherHomeScreenAdapter != null){
            mDispatcherHomeScreenAdapter.dismissUserPopupMenu();
        }
    }
    private void registerReceiveSOSandPTT() {
        mViewModel.observeSOSStateLiveData().observe(this, sosState -> {
            switch (sosState.getSosState()) {
                case RECEIVER:
                    dismissPopUp();
                    break;
                case SENDER:
                case DEFAULT:
                    break;
            }
        });
        mViewModel.observeSelfUserTalkState().observe(this, userTalkState -> {
            if (!userTalkState.isSelfUser()) {
                switch (userTalkState.getUserTalkState()) {
                    case TALKING:
                        dismissPopUp();
                        break;
                    case PASSIVE:
                        break;
                }
            }
        });
    }

    private void launchUserMapLocation(DispatcherItemModel model) {
        Bundle bundle = new Bundle();
        bundle.putInt(UserMapLocation.USER_ID, model.getUserId());
        bundle.putString(UserMapLocation.USER_NAME, model.getName());
        mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_USER_MAP_FRAGMENT.ordinal(), bundle);
    }

    private void launchAddUsers() {
        Bundle bundle = new Bundle();
        bundle.putString(AddUserFragment.CHANNEL_NAME, dispatcherChannelName);
        bundle.putInt(AddUserFragment.CHANNEL_ID, dispatcherChannelId);
        mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_ADD_USER_FRAGMENT.ordinal(), bundle);
    }

    private void launchSubChannel() {
        Bundle bundle = new Bundle();
        mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_SUB_CHANNEL_FRAGMENT.ordinal(), bundle);
    }

    private void processKick(DispatcherItemModel model) {
        int channelId = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserChannel().getChannelID();
        int currentSession = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getSessionID();
        // Request to get ACL for current channel
        mViewModel.getJioTalkieService().getJioPttSession().requestACL(channelId);
        // Observer when ACL fetched
        mViewModel.observeACLReport().observe(this, acl -> {
            Log.d(TAG, "HOME - ACLReport --- " + acl);
            /*
             UserModel need to move root channel i.e ChannelModel ID = 0.
             First update the ACL with remove kicked user from list and update it.
             */
            mViewModel.getJioTalkieService().getJioPttSession().kickUpdateACL(channelId, model.getUserId(), acl);

            // Now move user to root channel.
            if (model.isHasOnline()) {
                mViewModel.getJioTalkieService().getJioPttSession().changePttUserChannel(model.getSessionId(), EnumConstant.ROOT_CHANNEL_ID, currentSession);
            } else {
                mViewModel.getJioTalkieService().getJioPttSession().changeOfflinePttUserChannel(model.getUserId(), EnumConstant.ROOT_CHANNEL_ID);
            }
           resetAndFetchNewList();

        });
    }

    private void resetAndFetchNewList(){
        // Reset current user list and request to fetch updated list
        mRegUsersList = null;
        mViewModel.getJioTalkieService().getJioPttSession().fetchPttUserList();
        mViewModel.observeACLReport().removeObservers(this);
    }

    public void showReceiverAnimationForPTTCall(){
//        if(mActivity.isPTTCallGoing()){
//            mActivity.showPTTReceiverUI();
//        }
    }

    private void initKickProcess(DispatcherItemModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.remove_user_title))
                .setMessage(getResources().getString(R.string.remove_user_msg, dispatcherChannelName))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.action_remove), (dialog, id) -> {
                    dialog.dismiss();
                    if (model.getUserRole() == Mumble.UserState.UserRole.Normal.getNumber()) {
                        processKick(model);
                    } else {
                        Toast.makeText(mActivity, R.string.kick_not_allowed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), (dialog, id) -> dialog.dismiss());
        mKillDialog = builder.create();
        mKillDialog.show();
    }

    UserAdapterProvider mUserAdapterProvider = new UserAdapterProvider() {
        @Override
        public void onUserSelect(String userName, int userId) {
            mActivity.launchPersonalChat(userName, userId, false);
        }

        public void onPushToTalk(String userName, int userId) {
            mActivity.launchPersonalChat(userName, userId, true);
        }

        @Override
        public void onLocateUser(DispatcherItemModel model) {
            launchUserMapLocation(model);
        }

        @Override
        public void onAddUsers() {
            launchAddUsers();
        }
        public void onSubChannel() {
            launchSubChannel();
        }

        @Override
        public void onKick(DispatcherItemModel model) {
            initKickProcess(model);
        }

        @Override
        public void onMute(DispatcherItemModel model) {
            if (mViewModel.getJioTalkieService().isBoundToPttServer()) {
                // if user is deafen state than Unmute is not allowed, User can Unmute by UnDeafen
                if (model.isDeafen()) {
                    Toast.makeText(mActivity, R.string.to_unmute_deafened_user_please_do_undeafen, Toast.LENGTH_SHORT).show();
                } else {
                    JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
                    service.getJioPttSession().updateMuteDeafState(model.getSessionId(), !model.isMute(), model.isDeafen());
                }
            }
        }

        @Override
        public void onDeafen(DispatcherItemModel model) {
            if (mViewModel.getJioTalkieService().isBoundToPttServer()) {
                JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
                boolean mute = model.isMute();
                // if user deafen than it also user will be mute (as per design by server)
                // Now if user undeafen than also remove mute status (This need to handle from app side)
                if (model.isDeafen()) {
                    mute = false;
                }
                service.getJioPttSession().updateMuteDeafState(model.getSessionId(), mute, !model.isDeafen());
            }
        }
        @Override
        public void onUserContextMenu(boolean isShow) {
            isContextMenuVisible = isShow;
        }
    };
}
