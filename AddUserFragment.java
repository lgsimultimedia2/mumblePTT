package com.jio.jiotalkie.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.AddUsersAdapter;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.AddUserModel;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.Mumble;

public class AddUserFragment extends Fragment implements AddUsersAdapter.AdapterInterface {
    private static final String TAG = AddUserFragment.class.getName();
    public static final String CHANNEL_NAME = "channel_name";
    public static final String CHANNEL_ID = "channel_id";
    private DashboardActivity mActivity;
    private View mLoaderLayout;
    private View mEmptyView;
    private RecyclerView mRecyclerView;
    private String mChannelName;
    private int mChannelId;

    private DashboardViewModel mViewModel;
    private List<AddUserModel> mAddUserModelList = new ArrayList<>();
    private final HashMap<Integer, AddUserModel> mModelHashMap = new HashMap<>();
    private TextView actionSubTitle;
    private ImageView actionCross;
    private ImageView actionPlus;
    private ImageView actionBack;
    private ImageView actionSearch;
    private AddUsersAdapter mAddUsersAdapter;
    private AlertDialog mAddDialog;
    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener onClickListener = view -> {
        switch (view.getId()) {
            case R.id.actionCross:
                clearSelection();
                break;
            case R.id.actionPlus:
                showAddUserConfirmation();
                break;
            default:
                break;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        assert mActivity != null;
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
        Bundle bundle = getArguments();
        assert bundle != null;
        mChannelName = bundle.getString(CHANNEL_NAME, "");
        mChannelId = bundle.getInt(CHANNEL_ID, -1);
        mActivity.showToolWithBack(getString(R.string.add_users));
        mViewModel = new ViewModelProvider(mActivity).get(DashboardViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_user_channel, container, false);
        mLoaderLayout = view.findViewById(R.id.loading_layout);
        mEmptyView = view.findViewById(R.id.empty_layout);
        mRecyclerView = view.findViewById(R.id.add_users);
        actionBack = mActivity.findViewById(R.id.actionBack);
        actionSubTitle = mActivity.findViewById(R.id.actionSubTitle);
        actionCross = mActivity.findViewById(R.id.actionCross);
        actionPlus = mActivity.findViewById(R.id.actionPlus);
        actionSearch = mActivity.findViewById(R.id.actionSearch);
        actionCross.setOnClickListener(onClickListener);
        actionPlus.setOnClickListener(onClickListener);
        mActivity.setSearchQueryCallBack((queryText, isSearchVisible) -> {
            Log.d(TAG, "onTextChanged : " + queryText);
            mAddUsersAdapter.applyFilter(queryText.trim());
        });

        fetchAddUserList();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceivePTTCall();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceivePTTCall();
    }

    private void clearSelection() {
        mAddUsersAdapter.clearSelection();
    }
    private void showAddUserConfirmation() {
        int count = mAddUsersAdapter.getSelectedUsers().size();
        String adduserMsg = getActivity().getString(R.string.add_user_msg, mChannelName);
        if (count == 1) {
            AddUserModel addUserModel = mAddUsersAdapter.getSelectedUsers().get(0);
            adduserMsg = getActivity().getString(R.string.add_user_msg_single, addUserModel.getUserName(), mChannelName);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.add_users))
                .setMessage(adduserMsg)
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.action_add), (dialog, id) -> {
                    dialog.dismiss();
                    callAddUserAPIs();
                })
                .setNegativeButton(getResources().getString(R.string.cancel), (dialog, id) -> dialog.dismiss());
        mAddDialog = builder.create();
        mAddDialog.show();
    }
    private void unregisterReceivePTTCall(){
        mViewModel.observeSelfUserTalkState().removeObservers(this);
    }

    private void registerReceivePTTCall() {
        mViewModel.observeSelfUserTalkState().observe(this, userTalkState -> {
            if (!userTalkState.isSelfUser()) {
                switch (userTalkState.getUserTalkState()) {
                    case TALKING:
                        if (mAddDialog != null && mAddDialog.isShowing()) {
                            mAddDialog.dismiss();
                        }
                        break;
                    case PASSIVE:
                        break;
                }
            }
        });
    }

    private void callAddUserAPIs() {
        List<AddUserModel> addUserModels = mAddUsersAdapter.getSelectedUsers();
        int channelId = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserChannel().getChannelID();
        int session = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getSessionID();
        // Request to get ACL for current channel
        mViewModel.getJioTalkieService().getJioPttSession().requestACL(channelId);
        // Observer when ACL fetched
        mViewModel.observeACLReport().observe(this, acl -> {
            Log.d(TAG, "Swarn -AddUser - ACLReport --- " + acl);
            Log.d(TAG, "Total selected users " + addUserModels.size());
            // Make userid list for update ACL.
            List<Integer> addUserIds = new ArrayList<>();
            for (AddUserModel userModel : addUserModels) {
                addUserIds.add(userModel.getUserId());
            }
            // update the ACL
            mViewModel.getJioTalkieService().getJioPttSession().addUpdateACL(channelId, addUserIds,acl);
            // Now Move one by one to current channel.
            for (AddUserModel addUserModel : addUserModels) {
                if (addUserModel.isOnline()) {
                    mViewModel.getJioTalkieService().getJioPttSession().addUserToChannel(addUserModel.getSession(), channelId, session,addUserModel.getUserRole());
                } else {
                    mViewModel.getJioTalkieService().getJioPttSession().addOfflineUserToChannel(addUserModel.getUserId(), channelId, addUserModel.getUserRole());
                }
            }
            mViewModel.observeACLReport().removeObservers(this);
            showAddMessage(addUserModels );
            mActivity.handleOnBackPress();
        });
    }

    private void showAddMessage(List<AddUserModel> addUserModels) {
        int count = addUserModels.size();
        String successMsg = getActivity().getString(R.string.user_added_msg, addUserModels.get(0).getUserName(), mChannelName);
        if (count > 1) {
            successMsg = getActivity().getString(R.string.multiple_user_added_msg, count, mChannelName);
        }
        Snackbar snackbar = Snackbar.make(mRecyclerView, successMsg, BaseTransientBottomBar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.setMargins(30, 0, 30, 40);
        snackbarView.setLayoutParams(params);
        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(mActivity.getColor(R.color.add_users_bg_color));
        TextView snackTV = snackBarView.findViewById(R.id.snackbar_text);
        snackTV.setTextSize(14);
        snackTV.setTextColor(mActivity.getColor(R.color.black));
        snackTV.setTypeface(snackTV.getTypeface(), Typeface.BOLD);
        snackbar.show();
    }

    private void fetchAddUserList() {
        if (mViewModel.getJioTalkieService().isBoundToPttServer()
                && mViewModel.getJioTalkieService().isPttConnectionActive()
                && mViewModel.getJioTalkieService().getJioPttSession() != null) {
            mLoaderLayout.setVisibility(View.VISIBLE);
            mViewModel.getJioTalkieService().getJioPttSession().fetchPttUserList();
            // Get Online user of ChannelModel = 0
            List<? extends IUserModel> onlineUserList = mViewModel.getJioTalkieService().getJioPttSession().fetchPttChannel(0).getUserList();
            // Get all users of ChannelModel = 0
            mViewModel.observeAllRegisterUser().observe(this, registeredUsers -> {
                Log.d(TAG, "Total register users in server : " + registeredUsers.size());
                mModelHashMap.clear();
                // filter out channel 0  registered list
                for (Mumble.UserList.User user : registeredUsers) {
                    // Check channel id 0 means root and also user role "normal"
                    if (user.getLastChannel() == EnumConstant.ROOT_CHANNEL_ID ) {
                        // add user those have user role = Mumble.UserState.UserRole.Normal. (normal) or there is no any user role
                        Log.d(TAG, "UserModel has role : " +user.hasUserRole() +"and role : "+user.getUserRole() );
                        if(!user.hasUserRole() || (user.hasUserRole() && user.getUserRole() == Mumble.UserState.UserRole.Normal.getNumber())) {
                            AddUserModel addUserModel = new AddUserModel(user.getName(), user.getUserId(), 0, false);
                            // update user role if present.
                            if (user.hasUserRole()) {
                                addUserModel.setUserRole(user.getUserRole());
                            }
                            mModelHashMap.put(user.getUserId(), addUserModel);
                        }
                    }
                }
                Log.d(TAG, "ChannelModel id 0 user count :  " + mModelHashMap.size());
                Log.d(TAG, "ChannelModel id 0 Online user count :  " + onlineUserList.size());
                // Now update the online status of user
                for (IUserModel iUser : onlineUserList) {
                    int userId = iUser.getUserID();
                    if (mModelHashMap.containsKey(userId)) {
                        AddUserModel addUserModel = new AddUserModel(iUser.getUserName(), iUser.getUserID(), iUser.getSessionID(), true);
                        mModelHashMap.put(userId, addUserModel);
                    }
                }
                mAddUserModelList = new ArrayList<>(mModelHashMap.values());
                initAdapter();
            });
        }
    }

    private void initAdapter() {
        if (mAddUserModelList != null && !mAddUserModelList.isEmpty()) {
            mAddUsersAdapter = new AddUsersAdapter(mAddUserModelList, this);
            mLoaderLayout.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            mRecyclerView.setAdapter(mAddUsersAdapter);
            actionSubTitle.setVisibility(View.VISIBLE);
        } else {
            mLoaderLayout.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.GONE);
            actionSubTitle.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void refreshSelected(int totalCount, int selected, List<AddUserModel> selectedUserList) {
        if (selected >= 1) {
            actionSubTitle.setText(getString(R.string.add_user_selected, selected, totalCount));
            actionPlus.setVisibility(View.VISIBLE);
            actionCross.setVisibility(View.VISIBLE);
            actionSearch.setVisibility(View.VISIBLE);
            actionBack.setVisibility(View.GONE);
        } else {
            actionPlus.setVisibility(View.GONE);
            actionCross.setVisibility(View.GONE);
            actionSearch.setVisibility(View.VISIBLE);
            actionBack.setVisibility(View.VISIBLE);
            actionSubTitle.setText(getResources().getQuantityString(R.plurals.channel_user_count, totalCount, totalCount));
        }
    }
}
