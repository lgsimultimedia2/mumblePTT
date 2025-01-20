package com.jio.jiotalkie.fragment;

import static android.view.View.GONE;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji2.widget.EmojiEditText;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.AddUsersAdapter;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.AddUserModel;
import com.jio.jiotalkie.util.MessageEngine;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.application.customservice.dataManagment.imodels.IUserModel;

public class MultipleMessageFragment extends Fragment implements AddUsersAdapter.AdapterInterface {

    private DashboardActivity mActivity;
    private DashboardViewModel mViewModel;
    private List<AddUserModel> mAddUserModelList = new ArrayList<>();
    private List<AddUserModel> mSelectedUserList = new ArrayList<>();
    private final HashMap<Integer, AddUserModel> mModelHashMap = new HashMap<>();
    private AddUsersAdapter mAddUsersAdapter;
    private View mLoaderLayout;
    private RecyclerView mRecyclerView;
    private EmojiEditText mEditText;
    private ImageView mSendTextButton;
    private int mSelectedUserCount;
    private TextView actionSubTitle;
    private ImageButton attachmentButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        assert mActivity != null;
        mViewModel = new ViewModelProvider(mActivity).get(DashboardViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivity.showToolWithBack(mActivity.getResources().getString(R.string.message_multiple_users));
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.multiple_message_fragment, container, false);
        mLoaderLayout = view.findViewById(R.id.loading_layout);
        mRecyclerView = view.findViewById(R.id.user_list);
        mEditText = view.findViewById(R.id.chatTextEdit);
        attachmentButton = view.findViewById(R.id.attachmentButton);
        mSendTextButton = view.findViewById(R.id.iv_send_btn);
        actionSubTitle = mActivity.findViewById(R.id.actionSubTitle);
        registerViewModelObserver();
        mSendTextButton.setOnClickListener(view12 -> sendMessageFromEditor());
        attachmentButton.setVisibility(GONE);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int lines = mEditText.getLineCount();
                if (lines > 1) {
                    ViewGroup.LayoutParams params = mEditText.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    mEditText.setLayoutParams(params);
                }
                if (mEditText.getText().length() > 0 && mSelectedUserCount > 0) {
                    mSendTextButton.setEnabled(true);
                    mSendTextButton.setImageResource(R.drawable.bt_send_enable);
                } else {
                    mSendTextButton.setEnabled(false);
                    mSendTextButton.setImageResource(R.drawable.bt_send_disable);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return view;
    }

    private void registerViewModelObserver() {
        if (mViewModel.isJioTalkieServiceActive()) {
            mLoaderLayout.setVisibility(View.VISIBLE);
            mViewModel.observeRegisterUserData().observe(this, registeredUsers -> {
                for (RegisteredUser user : registeredUsers) {
                    if (mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserID() != user.getUserId()) {
                        AddUserModel addUserModel = new AddUserModel(user.getName(), user.getUserId(), mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID(), false);
                        mModelHashMap.put(user.getUserId(), addUserModel);
                    }
                }
                List<? extends IUserModel> onlineUserList = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getUserList();
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
            mRecyclerView.setVisibility(View.VISIBLE);
            mRecyclerView.setAdapter(mAddUsersAdapter);
            actionSubTitle.setVisibility(View.VISIBLE);
        } else {
            mLoaderLayout.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.GONE);
            actionSubTitle.setVisibility(GONE);
        }
    }

    private void sendMessageFromEditor() {
        if (mEditText.length() == 0) {
            return;
        }
        String message = mEditText.getText().toString();
        for (AddUserModel user : mSelectedUserList) {
            if (user.isOnline()) {
                MessageEngine.getInstance().msgToOnlineUser(user.getUserId(), user.getSession(), message);
            } else {
                MessageEngine.getInstance().msgToOfflineUser(user.getUserId(), user.getUserName(), message);
            }
        }
        mEditText.setText("");
        mAddUsersAdapter.clearSelection();
        Toast.makeText(mActivity, R.string.msg_sent_success, Toast.LENGTH_SHORT).show();
        mActivity.handleOnBackPress();
    }

    @Override
    public void refreshSelected(int totalCount, int selected, List<AddUserModel> selectedUserList) {
        mSelectedUserList = selectedUserList;
        mSelectedUserCount = selected;
        if (selected > 0) {
            actionSubTitle.setText(getString(R.string.add_user_selected, selected, totalCount));
        } else {
            actionSubTitle.setText(getResources().getQuantityString(R.plurals.channel_user_count, totalCount, totalCount));
        }
        if (mEditText.getText().length() > 0 && mSelectedUserCount > 0) {
            mSendTextButton.setEnabled(true);
            mSendTextButton.setImageResource(R.drawable.bt_send_enable);
        } else {
            mSendTextButton.setEnabled(false);
            mSendTextButton.setImageResource(R.drawable.bt_send_disable);
        }
    }

}
