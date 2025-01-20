package com.jio.jiotalkie.fragment;

import static android.app.Activity.RESULT_OK;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.View.GONE;
import static android.view.inputmethod.EditorInfo.IME_NULL;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.emoji2.widget.EmojiEditText;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.customservice.common.JioPttEnums;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.ChatAdapter;
import com.jio.jiotalkie.adapter.ChatViewHolder;
import com.jio.jiotalkie.adapter.provider.ConnectionStateProvider;
import com.jio.jiotalkie.adapter.provider.ImageEditorResultListener;
import com.jio.jiotalkie.dataclass.AudioDownloadState;
import com.jio.jiotalkie.dataclass.DocumentDownloadState;
import com.jio.jiotalkie.dataclass.VideoDownloadState;
import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.media.AudioRecordUtility;
import com.jio.jiotalkie.media.ImageCaptureUtility;
import com.jio.jiotalkie.model.JioTalkieChats;
import com.jio.jiotalkie.model.PaginatedPersonalChat;
import com.jio.jiotalkie.model.api.MediaUploadResponse;
import com.jio.jiotalkie.model.api.MessageRequestModel;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.service.JioTalkieService;
import com.jio.jiotalkie.util.ADCInfoUtils;
import com.jio.jiotalkie.util.BitmapUtils;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.DateUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.util.FileUtils;
import com.jio.jiotalkie.util.GpsUtils;
import com.jio.jiotalkie.util.ImageEditUtils;
import com.jio.jiotalkie.util.MessageIdUtils;
import com.jio.jiotalkie.util.ServerConstant;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.application.customservice.wrapper.Constants;
import com.application.customservice.wrapper.IJioPttSession;
import com.application.customservice.wrapper.IMediaMessage;
import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.dataManagment.models.UserModel;
import com.application.customservice.Mumble;
import com.yalantis.ucrop.UCropActivity;


@TrackPerformance(threshold = 300)
public class PersonalChatFragment extends Fragment implements ChatAdapter.ChatAdapterProvider, ImageEditorResultListener {

    private static final String TAG = PersonalChatFragment.class.getName();
    public static final String IS_PTT_CALL = "is_ptt_call";
    private static final Pattern LINK_PATTERN = Pattern.compile("(https?://\\S+)");
    private static final int CAMERA_REQUEST_PERMISSIONS = 10;
    private String mTargetUserName;
    private int mTargetUserId;
    private boolean isPttCall;
    private DashboardViewModel mViewModel;
    private IJioPttSession mHumlaSession;
    private ChatAdapter mPersonalChatAdapter;

    private ImageView actionBack;
    private String mCaptureFilePath = null;
    private String mImageMimeType = null;
    private boolean isItemSelected = false;
    private String mMsgId;
    private int mSelectedPosition;

    private boolean isUserScrolling = false;
    private String mTextMessage;
    private boolean isKeyBoardVisibleFirstTime = false;
    private LiveData<PaginatedPersonalChat> mPaginatedPersonalChat;
    private boolean isLoadingDataIntoUI;
    private boolean isLast;

    private final ActivityResultLauncher<Intent> mCameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            if (!mViewModel.getJioTalkieService().isBoundToPttServer() || !mViewModel.getJioTalkieService().isPttConnectionActive()
                    && mViewModel.getJioTalkieService().isConnectionSynchronized()) {
                return;
            }
            Uri imageUri = CommonUtils.getUriFromAbsolutePath(getContext(), mCaptureFilePath);
            onMediaFilePicked(imageUri);
        }
    });
    private final ActivityResultLauncher<Intent> mGalleyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {                // Handle the selected image or video URI here
            Uri selectedMediaUri = result.getData().getData();
            onMediaFilePicked(selectedMediaUri);
        }
    });

    public String getTargetUserName() {
        return mTargetUserName;
    }


    private void startImageEdit(Uri sourceUri) {
        croppedDestinationFile = new File(mActivity.getCacheDir(), System.currentTimeMillis()+"cropped_image.jpg");
        Uri destinationUri = Uri.fromFile(croppedDestinationFile);

        Bundle uCropBundle = ImageEditUtils.getImageEditBundle(mActivity, sourceUri, destinationUri);
        mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.IMAGE_EDIT_FRAGMENT.ordinal(),uCropBundle);
    }

    private void startImageEditing(Uri imageUri) {
        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.setDataAndType(imageUri, "image/*");
        editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            imageEditLauncher.launch(editIntent);
        } catch (Exception e) {
            trySpecificImageEditors(imageUri);
        }
    }

    private void trySpecificImageEditors(Uri imageUri) {
        String[] editorPackages = {
                "com.google.android.apps.photos",  // Google Photos
                "com.adobe.lightroom",             // Adobe Lightroom
                "com.cyberlink.photodirector",     // PhotoDirector
                "com.PicsArt.studio"               // PicsArt
        };
        for (String packageName : editorPackages) {
            Intent editIntent = new Intent(Intent.ACTION_EDIT);
            editIntent.setDataAndType(imageUri, "image/*");
            editIntent.setPackage(packageName);
            editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                imageEditLauncher.launch(editIntent);
                return;
            } catch (Exception e) {
                Log.d(TAG,"trySpecificImageEditors "+Log.getStackTraceString(e));
            }
        }
        Toast.makeText(mActivity, "No image editor found", Toast.LENGTH_SHORT).show();
    }

    private ActivityResultLauncher<Intent> imageEditLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri editedImageUri = data.getData();
                        onMediaFilePicked(editedImageUri);
                    }
                }
            }
    );
    private final ActivityResultLauncher<String> mDocumentPicker  = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onMediaFilePicked);
    private RecyclerView mRecyclerView;
    private EmojiEditText mEditText;
    private ImageView mSendTextButton;
    private ImageView mSendPTTButton;
    private ImageView mScrollBtn;
    private LinearLayout mEmptyLinearLayout;
    private LinearLayout mEditTextLinearLayout;
    private LinearLayout mOfflineStatusLL;

    private LiveData<List<JioTalkieChats>> mPersonalChatObserver;

    private LiveData<List<JioTalkieChats>> mFilteredPersonalChatObserver;
    private LiveData<JioTalkieChats> mChatByMsgId;

    private List<JioTalkieChats> mChatMessageList;
    private LiveData<JioTalkieChats> mChatDBUpdateObserver;
    private LiveData<MediaUploadResponse> mMediaUploadStateObserver;
    private LiveData<AudioDownloadState> mAudioDownloadObserver;
    private LiveData<VideoDownloadState> mVideoDownloadObserver;
    private LiveData<DocumentDownloadState> mDocumentDownloadObserver;
    private LiveData<Boolean> oneToOneBusyObserver;
    private DashboardActivity mActivity;
    private boolean mAllChatsLoaded = false;
    private TextView floatingDate;
    private final Handler handler = new Handler();
    private RelativeLayout mPcfMainContainerRL;
    private View mAttachmentPopup;
    private AlertDialog  mOptionDialog = null;
    private PopupWindow mAttachmentPopupWindow;
    private TextView channelName;
    private Toolbar mToolbar;
    private BottomNavigationView bottomNavigationPanel;
    private int loadedMessageCount;
    private boolean isMessageLoaded=false;
    private View mLoaderLayout;

    private boolean isTargetUserStatusReceived = false;
    private boolean isTargetUserBusy = false;
    private boolean isTalkingStarted = false;
    Handler mHandler;
    private int targetSessionId = -1;
    private ConnectionStateProvider mConnectionStateListener;

    private long mChatHistorySelectedDate;
    private TextView onlineUserCountView;
    private boolean isOnCreateCalled = false;

    private String mCallId;

    private int mDensityMetric;
    private List<? extends IUserModel> mOnlineUserList;

    private boolean isAudioRecordUri =false;

    private boolean isInternalImageCapture = false;

    private File croppedDestinationFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        mTargetUserName = getArguments().getString(EnumConstant.TARGET_USER_NAME);
        mTargetUserId = getArguments().getInt(EnumConstant.TARGET_USER_ID);
        mViewModel = mActivity.getViewModel();
        mViewModel.setTargetUserName(mTargetUserName);
        mViewModel.setOneToOneTargetId(mTargetUserId);
        isPttCall = getArguments().getBoolean(IS_PTT_CALL,false);
        isOnCreateCalled = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isOnCreateCalled) {
            initRecyclerView();
            registerOneToOneBusyObserver();
            registerChatMessageObserver();
            //Setting Voice target user.
            registerVoiceTargetUser();
            registerViewModelObserver();
        }
        isOnCreateCalled = false;

        if (mViewModel.getJioTalkieService() != null && mViewModel.getJioTalkieService().isPttConnectionActive()) {
            int index = CommonUtils.getSearchItemIndex(mOnlineUserList, mTargetUserName);
            if (index == -1) {
                mOfflineStatusLL.setVisibility(View.VISIBLE);
                onlineUserCountView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.offline_icon,0,0,0);
                onlineUserCountView.setText(getResources().getString(R.string.offline));
            }
        }
        // Clear all the one to one chat notification from notification central
        mViewModel.getJioTalkieService().getJioTalkieNotification().clearOneToOneNotification(mTargetUserId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_chat, container, false);
        mViewModel = ViewModelProviders.of(requireActivity()).get(DashboardViewModel.class);
        mViewModel.setTargetUserName(mTargetUserName);
        if (mViewModel != null && mViewModel.getJioTalkieService().isBoundToPttServer()
                && mViewModel.getJioTalkieService().isPttConnectionActive()
                && mViewModel.getJioTalkieService().isConnectionSynchronized()
                && mViewModel.getJioTalkieService().getJioPttSession() != null) {
            mHumlaSession = mViewModel.getJioTalkieService().getJioPttSession();
        }
        mOnlineUserList = mHumlaSession.fetchSessionPttChannel().getUserList();
        mActivity = (DashboardActivity) getActivity();
        DisplayMetrics metrics = mActivity.getResources().getDisplayMetrics();
        mDensityMetric = metrics.densityDpi;
        mHandler = new Handler(getActivity().getMainLooper());
        mAttachmentPopup = inflater.inflate(R.layout.attachment_popup_window,container,false);
        actionBack = mActivity.findViewById(R.id.actionBack);
        actionBack.setOnClickListener(view1 -> {
            unregisterVoiceTargetUser();
            if(mActivity!=null){
                mActivity.handleOnBackPress();
            }
        });
        mConnectionStateListener = connected -> {
            if(!connected) {
                Log.d(TAG,"handlePTTCallUIOnDisconnection ");
                mActivity.getPTTCallView().setVisibility(View.GONE);
                resetPTTCallLayoutBottomMargin();
            }
        };
        mActivity.setConnectionStateListeners(mConnectionStateListener);
        final View rootView = view.findViewById(R.id.fragmentPersonalChat);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int visibleHeight = r.height();
            int heightDiff = screenHeight - visibleHeight;
            boolean isKeyboardShowing = heightDiff > (screenHeight * 0.15); // 15% of the screen height, adjust as needed
            if (isKeyboardShowing && !isKeyBoardVisibleFirstTime) {
                isKeyBoardVisibleFirstTime=true;
                if(!isUserScrolling && mPersonalChatAdapter != null && mPersonalChatAdapter.getItemCount()>=1) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
                    int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition()+3;
                    int totalItems = mPersonalChatAdapter.getItemCount()-1;
                    if(lastVisiblePosition > totalItems || Math.abs(totalItems-lastVisiblePosition) <= 2)
                        lastVisiblePosition = totalItems;
                    mRecyclerView.scrollToPosition(lastVisiblePosition);
                }
            }else if(!isKeyboardShowing){
                isKeyBoardVisibleFirstTime=false;
            }
        });
        initViews(view);
        mLoaderLayout.setVisibility(View.VISIBLE);
        isOnCreateCalled = true;

        mActivity.getViewModel().getImageEditResult().observe(getViewLifecycleOwner(), uCropResult -> {
            if (uCropResult.mResultCode == RESULT_OK && uCropResult.mResultData!=null) {
                // Cropped image URI
                Uri croppedImageUri = UCrop.getOutput(uCropResult.mResultData);
                if (croppedImageUri != null && croppedDestinationFile != null) {
                    Uri croppedContentUri=FileProvider.getUriForFile(getContext(), "com.jio.jiotalkie.dispatch.provider", croppedDestinationFile);
                    onMediaFilePicked(croppedContentUri, true);
                }
            } else if (uCropResult.mResultCode == UCrop.RESULT_ERROR) {
                Throwable cropError = UCrop.getError(uCropResult.mResultData);
                Log.e(TAG, "Crop error: " + cropError);
                Toast.makeText(mActivity, "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pauseAudio();
        mViewModel.setTargetUserName("");
        if(mHandler!=null){
            mHandler.removeCallbacks(mPTTStatusRunnable);
        }
        unregisterVoiceTargetUser();
        isMessageLoaded=false;
        unRegisterOneToOneBusyObserver();
        mChatDBUpdateObserver.removeObserver(observeChatDBUpdates);
        mAllChatsLoaded = false;
        handler.removeCallbacksAndMessages(null);
        if (mAttachmentPopupWindow != null && mAttachmentPopupWindow.isShowing()) {
            mAttachmentPopupWindow.dismiss();
        }
        if (mOptionDialog != null && mOptionDialog.isShowing()){
            mOptionDialog.dismiss();
        }
        mPaginatedPersonalChat.removeObserver(observerPaginatedChat);
    }

    private void initViews(View view) {
        mLoaderLayout = view.findViewById(R.id.loading_layout);
        mRecyclerView = view.findViewById(R.id.personal_chat_recyclerview);
        mEditText = view.findViewById(R.id.chatTextEdit);
        mSendTextButton = view.findViewById(R.id.iv_send_btn);
        mSendPTTButton = view.findViewById(R.id.iv_ptt_btn);
        mEditTextLinearLayout = view.findViewById(R.id.personal_chat_bottom);
        mEmptyLinearLayout = view.findViewById(R.id.ll_empty_message);
        mOfflineStatusLL = view.findViewById(R.id.ll_offline_status);
        onlineUserCountView = view.findViewById(R.id.online_user_count_view);
        mScrollBtn = view.findViewById(R.id.iv_scroll_btn);
        floatingDate = view.findViewById(R.id.floating_date);
        mPcfMainContainerRL = view.findViewById(R.id.fragmentPersonalChat);
        TextView cameraAttachmentButton = mAttachmentPopup.findViewById(R.id.cameraAttachmentButton);
        cameraAttachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()) {
                    openCamera();
                } else {
                    requestAppPermissions();
                }
                mAttachmentPopupWindow.dismiss();
            }
        });
        channelName = view.findViewById(R.id.channelName);
        bottomNavigationPanel = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationPanel.setVisibility(GONE);
        channelName.setText(mTargetUserName);
        enablePTTCallButton(isTargetUserOnline(mTargetUserName));
        if (isTargetUserOnline(mTargetUserName)) {
            onlineUserCountView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.online_icon,0,0,0);
            onlineUserCountView.setText(getResources().getString(R.string.status_online));
        } else {
            onlineUserCountView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.offline_icon,0,0,0);
            onlineUserCountView.setText(getResources().getString(R.string.status_offline));
        }
        mActivity.showToolWithBack(mTargetUserName);
        mActivity.needSOSButton(false);
        mToolbar = getActivity().findViewById(R.id.toolbar_main);
        mToolbar.findViewById(R.id.actionCalender).setVisibility(View.VISIBLE);

        mToolbar.findViewById(R.id.actionDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int result = mViewModel.deleteJioTalkieByMsgId(mMsgId);
                if (result > 0) {
                    mPersonalChatAdapter.removeItem(mSelectedPosition);
                    Toast.makeText(mActivity, "Selected message deleted successfully", Toast.LENGTH_SHORT).show();
                }
                onItemDeselected();
            }
        });
        mToolbar.findViewById(R.id.actionCopy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemDeselected();
                mActivity.copyTextToClipboard(mTextMessage);
            }
        });
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        mEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == IME_NULL && event != null && event.getKeyCode() == KEYCODE_ENTER) {
                sendMessageFromEditor();
                return true;
            }
            onItemDeselected();
            return false;
        });

        mEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
                if (linearLayoutManager != null && mScrollBtn != null ) {
                    setScrollView(linearLayoutManager, mScrollBtn, 0);
                }
                return false;
            }
        });
        mScrollBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPersonalChatAdapter != null && mPersonalChatAdapter.getItemCount()>=1) {
                    mRecyclerView.scrollToPosition(mPersonalChatAdapter.getItemCount() - 1);
                }
            }
        });
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                assert layoutManager != null;
                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                if (!mChatMessageList.isEmpty() && mChatMessageList.size() > firstVisible && mChatMessageList.size() > 10 && !isLoadingDataIntoUI) {
                    long firstVisibleItemTime = mChatMessageList.get(firstVisible).getReceived_time();
                    String firstVisibleItemDate = DateUtils.getStringDateFromLong(firstVisibleItemTime, DateUtils.dateFormatDateDivider);
                    floatingDate.setVisibility(View.VISIBLE);
                    floatingDate.setText(DateUtils.CompareDate(firstVisibleItemDate));
                }
                setScrollView(layoutManager, mScrollBtn, -1);

                if (!isLast && isUserScrolling && !isLoadingDataIntoUI && layoutManager != null && layoutManager.findFirstVisibleItemPosition() == 0) {
                    isLoadingDataIntoUI = true;
                    floatingDate.setText(getResources().getString(R.string.load_chat_data));
                    mPaginatedPersonalChat.observe(getActivity(), observerPaginatedChat);
                    MessageRequestModel messageRequestModel = new MessageRequestModel(mViewModel.getSelfUserId(), mTargetUserId, true, null, null, null, true, 30, mPersonalChatAdapter.getItemCount());
                    mViewModel.downloadMessagesWithPagination(messageRequestModel);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                isUserScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }
        });
        mSendTextButton.setOnClickListener(view1 -> {
            if (mEditText.getText().length() > 0) {
                sendMessageFromEditor();
            }
        });
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                DashboardActivity mActivity = (DashboardActivity) getActivity();
                assert mActivity != null;
                unregisterVoiceTargetUser();
                mActivity.handleOnBackPress();
                return true;
            }
            return false;
        });

        mSendPTTButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mActivity.handleChatFragment();
                        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()) {
                            int selfSessionId =  mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getSessionID();
                            UserModel self = (UserModel)  mViewModel.getJioTalkieService().getJioPttSession().fetchPttUser(selfSessionId);
                            if (self.isMute() || self.isUserLocalMute() || self.isUserSuppressed()) {
                                Toast.makeText(getContext(), mActivity.getResources().getString(R.string.mute_message_alert), Toast.LENGTH_SHORT).show();
                                return true;
                            }

                            int index = CommonUtils.getSearchItemIndex(mOnlineUserList, mTargetUserName);
                            if (index >= 0) {
                                if (mOnlineUserList.get(index).isDeaf()) {
                                    Toast.makeText(getContext(), mActivity.getResources().getString(R.string.defean_user_alert, mTargetUserName), Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                            }
                        }
                        //Setting Voice target user.
                        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()
                                && mViewModel.getJioTalkieService().isConnectionSynchronized()) {
                            boolean onlineStatus = CommonUtils.checkOnlineStatus(mOnlineUserList, mTargetUserName);
                            if (onlineStatus) {
                                // UserModel is online
                                registerVoiceTargetUser();
                                setPTTCallUserState(true);
                                pauseAudio();
//                                try {
//                                    mViewModel.getJioTalkieService().getJioModelHandler().setUserStateProvider(busy -> {
//                                        isTargetUserStatusReceived = true;
//                                        isTargetUserBusy= busy;
//                                    });
//                                }catch(Exception e){
////                            Log.e(TAG,"Exception in model handler "+Log.getStackTraceString(e));
//                                }
                                mHandler.post(mPTTStatusRunnable);
                                //TODO: Integration: Find Turn around
                                isTargetUserStatusReceived = true;
                            } else {
                                // UserModel is offline
                                Toast.makeText(getActivity(),  mActivity.getString(R.string.ptt_to_offline_user_alert_meesage),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Service not connected, cannot send PTT");
                            Toast.makeText(getActivity(), "Service not connected",
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        handlePTTButtonActionUp();
                        break;
                }
                return true;
            }
        });

        mEditText.setOnKeyListener((v, keyCode, event) -> {
            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (mEditText.getText().length() > 0) {
                    sendMessageFromEditor();
                }
                return true;
            }
            return false;
        });

        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int lines = mEditText.getLineCount();
                if (lines > 1) {
                    ViewGroup.LayoutParams params = mEditText.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    mEditText.setLayoutParams(params);
                }
                if (mEditText.getText().length() > 0) {
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

        //Added code for AttachmentPopupUI and LocationSend MapsFragment
        TextView locationAttachmentButton = mAttachmentPopup.findViewById(R.id.locationAttachmentButton);
        TextView galleryAttachmentButton = mAttachmentPopup.findViewById(R.id.galleryAttachmentButton);
        TextView documentAttachmentButton = mAttachmentPopup.findViewById(R.id.documentAttachmentButton);

        locationAttachmentButton.setOnClickListener(v -> {
            if(!GpsUtils.isLocationEnabled(mActivity)){
                Toast.makeText(mActivity,mActivity.getResources().getString(R.string.enable_location),Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle args=new Bundle();
            args.putString("selectedUser",mTargetUserName);
            args.putBoolean("isGroupChat",false);
            Fragment fragment = new MapsFragment();
            fragment.setArguments(args);

            Class<? extends Fragment> fragmentName = MapsFragment.class;

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment,fragmentName.getName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(fragmentName.getName())
                    .commitAllowingStateLoss();
            mAttachmentPopupWindow.dismiss();
        });
        galleryAttachmentButton.setOnClickListener(v -> {
            // Create an intent to select images and videos
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*"); // Allow both images ,videos and audio
            String[] mimeTypes = {"image/*", "video/*", "audio/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            mGalleyLauncher.launch(intent);
            mAttachmentPopupWindow.dismiss();
            mAttachmentPopupWindow.dismiss();
        });
        documentAttachmentButton.setOnClickListener(v -> {
            mDocumentPicker.launch("*/*");
            mAttachmentPopupWindow.dismiss();
        });
        mAttachmentPopupWindow = new PopupWindow(mAttachmentPopup, ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,true);
        mAttachmentPopup.setOnClickListener(v -> mAttachmentPopupWindow.dismiss());

        ImageButton attachIcon = view.findViewById(R.id.attachmentButton);
        attachIcon.setOnClickListener(v -> {
            hideKeyboard();
            onItemDeselected();
            if (mActivity != null) {
                int offsetY = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 160, mActivity.getResources().getDisplayMetrics());
                mAttachmentPopupWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, offsetY);
            }
        });
    }

    private void enablePTTCallButton(boolean isEnable) {
        mSendPTTButton.setEnabled(isEnable);
        mSendPTTButton.setAlpha(isEnable ? 1f : 0.5f);
    }

    private void openCamera() {
        File imageFile = null;
        try {
            imageFile = FileUtils.createImageFile(getContext());
            mCaptureFilePath = imageFile.getAbsolutePath();
        } catch (IOException e) {
            mCaptureFilePath = null;
            e.printStackTrace();
        }
        Uri imageUri = FileProvider.getUriForFile(getContext(), EnumConstant.AUTHORITY_PROVIDER_NAME, imageFile);
        ContentResolver cR = requireContext().getContentResolver();
        mImageMimeType = cR.getType(imageUri);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        mCameraLauncher.launch(intent);
        mAttachmentPopupWindow.dismiss();
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAppPermissions() {
        requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST_PERMISSIONS);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_PERMISSIONS) {
            if (!isInternalImageCapture && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
            if(isInternalImageCapture && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                isInternalImageCapture=false;
                captureImageInternally();
            }
        }
    }

    private boolean isTargetUserOnline(String userName){
        boolean onlineStatus =false;
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()
                && mViewModel.getJioTalkieService().isConnectionSynchronized()) {
            onlineStatus = CommonUtils.checkOnlineStatus(mOnlineUserList, userName);
        }
        return onlineStatus;
    }

    private void handlePTTButtonActionUp() {
        isTalkingStarted=false;
        mActivity.updateState(EnumConstant.pttAnimationState.USER_STATE_DEFAULT.name()); // Update views
        setPTTCallUserState(false);
        try {
//            mViewModel.getJioTalkieService().getJioModelHandler().setUserStateProvider(null);
            mViewModel.getJioTalkieService().onTalkKeyUp();
        } catch(Exception e) {
        }

        mPcfMainContainerRL.setBackgroundColor(getResources().getColor(R.color.pcf_main_containerrl_white));
        mActivity.getPTTCallView().setVisibility(View.GONE);
        resetPTTCallLayoutBottomMargin();
        if (mPersonalChatAdapter.getItemCount() > 0) {
            mEmptyLinearLayout.setVisibility(GONE);
        } else {
            mEmptyLinearLayout.setVisibility(View.VISIBLE);
        }
        mEditTextLinearLayout.setVisibility(View.VISIBLE);
        mSendPTTButton.setVisibility(View.VISIBLE);
        unregisterVoiceTargetUser();
        if(mHandler!=null)mHandler.removeCallbacks(mPTTStatusRunnable);
    }

    Runnable mPTTStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if(isTargetUserStatusReceived){
                isTargetUserStatusReceived=false;
                if(isTargetUserBusy){
//                    Log.d("Sheetal_TAG", "PTT user is busy can not talk ");
                    isTalkingStarted = false;
                    Toast.makeText(mActivity,mActivity.getResources().getString(R.string.target_user_busy,mTargetUserName),Toast.LENGTH_SHORT).show();
                    mActivity.runOnUiThread(() -> handlePTTButtonActionUp());
                }else{
                    isTalkingStarted = true;
//                    Log.d("Sheetal_TAG", "PTT user is not busy lets talk ");
                    Toast.makeText(mActivity,mActivity.getResources().getString(R.string.target_user_available,mTargetUserName),Toast.LENGTH_SHORT).show();
                    if (mViewModel.getJioTalkieService().isBoundToPttServer()) {
                        mViewModel.getJioTalkieService().onTalkKeyDown();
                    }
                    setPTTCallLayoutBottomMargin();
                    mActivity.getPTTCallView().setVisibility(View.VISIBLE);
                    mEmptyLinearLayout.setVisibility(GONE);
                    mEditTextLinearLayout.setVisibility(GONE);
                    mActivity.runOnUiThread(() -> mActivity.updateState(EnumConstant.pttAnimationState.USER_STATE_SPEAKER.name()));
                }
            }else{
                mHandler.postDelayed(this,10);
            }
        }
    };

    private void setPTTCallLayoutBottomMargin(){
        RelativeLayout pttCallLayout = (RelativeLayout) mActivity.getPTTCallView();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pttCallLayout.getLayoutParams();
        if(mDensityMetric >= DisplayMetrics.DENSITY_450) {
            params.bottomMargin = 170;
        }else if(mDensityMetric >= DisplayMetrics.DENSITY_400){
            params.bottomMargin = 150;
        }else if (mDensityMetric >= DisplayMetrics.DENSITY_XHIGH) {
            params.bottomMargin = 110;
        }else if (mDensityMetric >= DisplayMetrics.DENSITY_280){
            params.bottomMargin = 100;
        }
        pttCallLayout.setLayoutParams(params);
    }

    private void resetPTTCallLayoutBottomMargin(){
        RelativeLayout pttCallLayout = (RelativeLayout) mActivity.getPTTCallView();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pttCallLayout.getLayoutParams();
        if(mDensityMetric >= DisplayMetrics.DENSITY_450) {
            params.bottomMargin = 350;
        }else if(mDensityMetric >= DisplayMetrics.DENSITY_400){
            params.bottomMargin = 310;
        }else if (mDensityMetric >= DisplayMetrics.DENSITY_XHIGH) {
            params.bottomMargin = 230;
        }else if (mDensityMetric >= DisplayMetrics.DENSITY_280){
            params.bottomMargin = 210;
        }
        pttCallLayout.setLayoutParams(params);
    }

    private void initRecyclerView() {
        mPersonalChatAdapter = new ChatAdapter(getContext(), mViewModel.getJioTalkieService(),this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public void onLayoutCompleted(final RecyclerView.State state) {
                super.onLayoutCompleted(state);
                if(state.getItemCount() == loadedMessageCount && isMessageLoaded) {
                    long delay = (loadedMessageCount/300)* 2000L;
                    isMessageLoaded=false;
                    new Handler(getActivity().getMainLooper()).postDelayed(() -> {
                        mLoaderLayout.setVisibility(GONE);
                        if (isPttCall) {
                            Toast.makeText(mActivity.getApplicationContext() ,getString(R.string.hold_ptt_info), Toast.LENGTH_LONG).show();
                        }
                    }, delay);
                }
            }
        });
        mRecyclerView.setAdapter(mPersonalChatAdapter);
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (mPersonalChatAdapter != null && mPersonalChatAdapter.getItemCount()>=1) {
                    mRecyclerView.scrollToPosition(mPersonalChatAdapter.getItemCount() - 1);
                }
            }
        });
    }
    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void filterChatMessages(long receivedTime){
        mChatHistorySelectedDate = receivedTime;
        mLoaderLayout.setVisibility(View.VISIBLE);
        mFilteredPersonalChatObserver = mViewModel.observeFilteredPersonalChatFromDB(mTargetUserName,receivedTime);
        mFilteredPersonalChatObserver.observe(this,observeFilteredPersonalChatData);
    }

    private void registerOneToOneBusyObserver(){
        oneToOneBusyObserver = mViewModel.observeOneToOneTargetBusy();
        oneToOneBusyObserver.observe(this , observeOneToOneBusy);
    }

    private void unRegisterOneToOneBusyObserver(){
        oneToOneBusyObserver.removeObserver(  observeOneToOneBusy);
    }

    private void registerChatMessageObserver() {
        mPaginatedPersonalChat = mViewModel.observePaginatedPersonalChat();
        mPersonalChatObserver = mViewModel.observePersonalChatFromDB(mTargetUserName); //return only personal chat data of requested user
        mPersonalChatObserver.observe(this, observePersonalChatData);

        mChatDBUpdateObserver = mViewModel.observeChatMessageData();
        mChatDBUpdateObserver.observe(this, observeChatDBUpdates);
        mAudioDownloadObserver = mViewModel.observeDownloadAudio();
        mAudioDownloadObserver.observe(this,observeDownloadAudio);
//        Log.d("AudioWork","registerChatMessageObserver called!");

        mVideoDownloadObserver = mViewModel.observeDownloadVideo();
        mVideoDownloadObserver.observe(this,observeDownloadVideo);
        mDocumentDownloadObserver = mViewModel.observeDownloadDocument();
        mDocumentDownloadObserver.observe(this,observeDownloadDocument);

        mViewModel.observeSelfUserTalkState().observe(this, userTalkState -> {
//            Log.d(TAG, "jio: registerStateObserver: observeSelfUserTalkState called talkState self user =" + userTalkState.isSelfUser());
            if(userTalkState.getUserTalkState() == EnumConstant.userTalkState.PASSIVE){
                mHandler.postDelayed(() -> mPersonalChatAdapter.notifyDataSetChanged(),800);
            }
            if (userTalkState.isSelfUser()) {
                switch (userTalkState.getUserTalkState()) {
                    case TALKING:
                        break;
                    case PASSIVE:
                        break;
                }
            } else {
//                Log.d(TAG, "jio: userTalkState.getUserTalkState() =" + userTalkState.getUserTalkState());
                switch (userTalkState.getUserTalkState()) {
                    case TALKING:
                        if (mAttachmentPopupWindow !=null && mAttachmentPopupWindow.isShowing())
                            mAttachmentPopupWindow.dismiss();
                        pauseAudio();
                        mActivity.dismissCalendarDialog();
                        break;
                    case PASSIVE:
                        break;
                }
            }
        });

        mViewModel.observeMsgDeliveryReport().observe(this, deliverystatus ->{
            mPersonalChatAdapter.updateMessageStatus(deliverystatus.getMsgId(),deliverystatus.getStatus().ordinal(),null,null);
        });
    }

    private final Observer<JioTalkieChats> observeChatByMsgId = new Observer<JioTalkieChats>() {
        @Override
        public void onChanged(JioTalkieChats message) {
            if (!message.getFile_upload_status().equals(getResources().getString(R.string.uploading_pending))) {
                mPersonalChatAdapter.refreshData(message);
                mChatByMsgId.removeObserver(observeChatByMsgId);
            }
        }
    };

    private final Observer<Boolean> observeOneToOneBusy = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean oneToOneBusy) {
            if(oneToOneBusy == null)
                return;

            isTargetUserStatusReceived = true;
            isTargetUserBusy= oneToOneBusy;

            //Reset to null , once we use it.
            mViewModel.resetOneToOneTargetBusy();
        }
    };

    private final Observer<JioTalkieChats> observeChatDBUpdates = new Observer<JioTalkieChats>() {
        @Override
        public void onChanged(JioTalkieChats message) {
            // Log.d(TAG,"observeChatDBUpdates");
            // Check if this is called even before we loaded the chats from DB.
            // If so, ignore the message to avoid duplicate message in adapter. We have observed
            // that sometimes this observer is invoked for the last message in DB and in such case
            // last message is seen twice in chat adapter. This issue does not occur in Group Chat
            // because they initialize chat adapter in observePersonalChatData observer hence this
            // duplicate message is not entered in chat adapter because it is null.
            if (mAllChatsLoaded == false) {
                // Log.d(TAG,"observeChatDBUpdates mAllChatsLoaded false, returning");
                return;
            }

            // update chat adapter if the incoming message is from selected user
            if (!message.getIs_group_chat()) {
                //Log.d(TAG,"Sender: " + message.getUser_name() + " Receiver: "
                //        + message.getTarget_user() + "Message: " + message.getMessage());
                // add message if sender is selected user or the receiver is selected user
                if (message.getUser_name().equals(mTargetUserName)
                        || (message.getTarget_user().equals(mTargetUserName))) {
                    // Log.d(TAG,"observeChatDBUpdates: Message added to adapter");
                    if (BuildConfig.BUILD_TYPE.contains("devpreprod")) {
                        if (message.getIs_self_chat() && message.getMessage().equals("recording")) {
                            AudioRecordUtility.getInstance(mActivity, audioRecordUri -> {
                                isAudioRecordUri = true;
                                onMediaFilePicked(audioRecordUri);
                            }).startRecording();
                        }
                        if (message.getIs_self_chat() && message.getMessage().equals("capture")) {
                            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                isInternalImageCapture = true;
                                requestAppPermissions();
                            } else {
                                captureImageInternally();
                            }
                        }
                    }
                    addChatMessage(message, true);
                    if (mPersonalChatAdapter.getItemCount() == 1) {
                        mEmptyLinearLayout.setVisibility(GONE);
                    }
                }
            }
        }
    };

    private void captureImageInternally() {
        ImageCaptureUtility.getInstance().init(mActivity, uri -> onMediaFilePicked(uri));
    }

    private final Observer<List<JioTalkieChats>> observePersonalChatData = new Observer<List<JioTalkieChats>>() {
        @Override
        public void onChanged(List<JioTalkieChats> message) {
//            Log.d(TAG, "onChanged: getPersonalChatFromDB called size =" + message.size());
            mChatMessageList = message;
            mPersonalChatAdapter.clear();
            loadedMessageCount = message.size();
            isMessageLoaded=true;
            if (message.size() > 0) {
                // add chats loaded from DB to adapter
                for (JioTalkieChats chat : message) {
                    addChatMessage(chat, false);
                }
                // once all chats are added to adapter, scroll to the last
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mPersonalChatAdapter != null && mPersonalChatAdapter.getItemCount()>=1) {
                            mRecyclerView.smoothScrollToPosition(mPersonalChatAdapter.getItemCount() - 1);
                        }
                    }
                });
            } else {
                getPersonalChatHistory();
                mLoaderLayout.setVisibility(View.VISIBLE);
            }
            mPersonalChatObserver.removeObserver(observePersonalChatData);
            mAllChatsLoaded = true;
        }
    };

    private final Observer<List<JioTalkieChats>> observeFilteredPersonalChatData = new Observer<List<JioTalkieChats>>() {
        @Override
        public void onChanged(List<JioTalkieChats> message) {
            Date selectedDate = new Date(mChatHistorySelectedDate);
            if (!message.isEmpty()) {
                int counter=0;
                Date firstMessageDate = null;
                long firstDateTime=System.currentTimeMillis();
                mChatMessageList = message;
                mPersonalChatAdapter.clear();
                loadedMessageCount = message.size();
                isMessageLoaded=true;
                for (JioTalkieChats chat : message) {
                    Date chatDate = new Date(chat.getReceived_time());
                    if(firstMessageDate==null){
                        firstMessageDate=chatDate;
                        firstDateTime= chat.getReceived_time();
                    }
                    if(!CommonUtils.isSameDay(selectedDate,chatDate)){
                        counter++;
                    }
                    addChatMessage(chat, false);
                }
                if(counter==loadedMessageCount){
                    Toast.makeText(mActivity,mActivity.getResources().getString(R.string.no_chat_history_selected_date,
                            CommonUtils.getSimpleDateFormatForToast(firstMessageDate)),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(mActivity,mActivity.getResources().getString(R.string.chat_history_in_range,CommonUtils.getSimpleDateFormatForToast(selectedDate)),Toast.LENGTH_LONG).show();
                }
                mRecyclerView.post(() -> {
                    if (mPersonalChatAdapter != null && mPersonalChatAdapter.getItemCount() >= 1) {
                        mRecyclerView.smoothScrollToPosition(0);
                    }
                });
                String firstVisibleItemDate = DateUtils.getStringDateFromLong(firstDateTime, DateUtils.dateFormatDateDivider);
                floatingDate.setVisibility(View.VISIBLE);
                floatingDate.setText(DateUtils.CompareDate(firstVisibleItemDate));
            }else{
                mLoaderLayout.setVisibility(GONE);
                Toast.makeText(mActivity,mActivity.getResources().getString(R.string.no_chat_history_in_range,CommonUtils.getSimpleDateFormatForToast(selectedDate)),Toast.LENGTH_SHORT).show();
            }
            mFilteredPersonalChatObserver.removeObserver(observeFilteredPersonalChatData);
            mAllChatsLoaded = true;
        }
    };

    private final Observer<VideoDownloadState> observeDownloadVideo = new Observer<VideoDownloadState>() {
        @Override
        public void onChanged(VideoDownloadState videoDownloadState) {
            ChatViewHolder viewHolderForCurrentPosition = (ChatViewHolder) mRecyclerView.findViewHolderForAdapterPosition(videoDownloadState.getPosition());
            if (mPersonalChatAdapter!=null && videoDownloadState.getPosition() != -1){
                mPersonalChatAdapter.updateVideoUI(videoDownloadState.getPosition(), videoDownloadState.getFilePath(),videoDownloadState.isLeft(),videoDownloadState.isSos(),(ChatViewHolder) viewHolderForCurrentPosition);
                mViewModel.resetVideoDownloadObserver();
            }
        }
    };

    private final Observer<DocumentDownloadState> observeDownloadDocument = new Observer<DocumentDownloadState>() {
        @Override
        public void onChanged(DocumentDownloadState documentDownloadState) {
            ChatViewHolder viewHolderForCurrentPosition = (ChatViewHolder) mRecyclerView.findViewHolderForAdapterPosition(documentDownloadState.getPosition());
            if (mPersonalChatAdapter!=null && documentDownloadState.getPosition() != -1) {
                mPersonalChatAdapter.updateDocumentUI(documentDownloadState.getPosition(), documentDownloadState.getFilePath(), documentDownloadState.isLeft(), (ChatViewHolder) viewHolderForCurrentPosition);
                mViewModel.resetDocumentDownloadObserver();
            }
        }
    };

    private final Observer<MediaUploadResponse> observeFileUploadState = new Observer<MediaUploadResponse>() {
        @Override
        public void onChanged(MediaUploadResponse mediaUploadResponse) {
            Log.d(TAG,"jioPtt: Personal:observeFileUploadState: upload status is: " + mediaUploadResponse.getFileUploadSuccess() +
                    "file type:" + mediaUploadResponse.getFileType() + ", msgId:" + mediaUploadResponse.getMsgId());
            try {
                if (croppedDestinationFile != null && croppedDestinationFile.exists()) {
                    croppedDestinationFile.delete();
                    croppedDestinationFile=null;
                }
            }catch (Exception e){
                Log.d(TAG,"On File upload status, delete cropped Destination file exception "+Log.getStackTraceString(e));
            }
            if(mediaUploadResponse.getFileUploadSuccess()) {
                if(mediaUploadResponse.getFileType()==EnumConstant.FILE_TYPE_IMAGE){
                    sendMessage(EnumConstant.IMAGE_MSG,mediaUploadResponse.getMsgId(),EnumConstant.MessageType.IMAGE.name(),mediaUploadResponse.getMimeType(),"");
                }
                else if(mediaUploadResponse.getFileType()==EnumConstant.FILE_TYPE_AUDIO){
                    sendMessage(EnumConstant.AUDIO_MSG,mediaUploadResponse.getMsgId(),EnumConstant.MessageType.AUDIO.name(), mediaUploadResponse.getMimeType(),mediaUploadResponse.getMediaPath());
                } else if (mediaUploadResponse.getFileType() == EnumConstant.FILE_TYPE_DOC) {
                    if (mediaUploadResponse.getMediaPath() != null) {
                        String[] filename = mediaUploadResponse.getMediaPath().split("/Documents/");
                        sendMessage(filename[1], mediaUploadResponse.getMsgId(), EnumConstant.MessageType.DOCUMENT.name(),
                                mediaUploadResponse.getMimeType(), mediaUploadResponse.getMediaPath());
                    }
                }
                else if (mediaUploadResponse.getFileType()==EnumConstant.FILE_TYPE_VIDEO){
                    sendMessage(EnumConstant.VIDEO_MSG,mediaUploadResponse.getMsgId(),EnumConstant.MessageType.VIDEO.name(), mediaUploadResponse.getMimeType(),mediaUploadResponse.getMediaPath());
                }
                ADCInfoUtils.calculateImageSize(mediaUploadResponse.getMediaPath(),true,mediaUploadResponse.getMimeType(),mViewModel.getUserId(),mViewModel.getChannelId(),"OneToOne",mTargetUserId);
                mViewModel.resetMediaUploadStateObserver();
            }
            else{
                if(!mediaUploadResponse.getUploadStatus().isEmpty())
                    Toast.makeText(mActivity,mediaUploadResponse.getUploadStatus(),Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final Observer<AudioDownloadState> observeDownloadAudio = new Observer<AudioDownloadState>() {
        @Override
        public void onChanged(AudioDownloadState audioDownloadState) {
            ChatViewHolder viewHolderForCurrentPosition = (ChatViewHolder) mRecyclerView.findViewHolderForAdapterPosition(audioDownloadState.getPosition());
            if (mPersonalChatAdapter!=null && audioDownloadState.getPosition() != -1) {
                mPersonalChatAdapter.updateAudioUI(audioDownloadState.getPosition(), audioDownloadState.getFilePath(), audioDownloadState.isLeft(), audioDownloadState.isSos(), (ChatViewHolder) viewHolderForCurrentPosition);
                mViewModel.resetAudioDownloadObserver();
            }
        }
    };


    private void sendMessageFromEditor() {
        if (mEditText.length() == 0) {
            return;
        }
        String message = mEditText.getText().toString();
        String msgId = MessageIdUtils.generateUUID();
        sendMessage(message, msgId, EnumConstant.MessageType.TEXT.name(), "", "");
        ADCInfoUtils.calculateTextSize(message,true,mViewModel.getUserId(),mViewModel.getChannelId(),"OneToOne",mTargetUserId);
        mEditText.setText("");


    }

    private void sendMessage(String message, String msgId, String msgType, String mimeType, String mediaPath)  {
        Log.d(TAG, "jioPtt::called sendMessage():message:" + message + ",msgId:" + msgId + ",msgType:" + msgType + ",mimeType:" + mimeType + ",mediaPath:" + mediaPath );
        if (!mViewModel.getJioTalkieService().isBoundToPttServer()) {
//            Log.d(TAG, "Service not connected");
            return;
        }

        String formattedMessage = markupOutgoingMessage(message);
        IMediaMessage responseMessage = null;

        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()) {
            IJioPttSession hs = mViewModel.getJioTalkieService().getJioPttSession();
            Mumble.TextMessage.MsgType messageType = Mumble.TextMessage.MsgType.TextMessageType;
            if (msgType.equals(EnumConstant.MessageType.IMAGE.name())) {
                messageType = Mumble.TextMessage.MsgType.ImageMessageType;
            } else if(msgType.equals(EnumConstant.MessageType.AUDIO.name())){
                messageType = Mumble.TextMessage.MsgType.VoiceMessageType;
            } else if(msgType.equals(EnumConstant.MessageType.DOCUMENT.name())){
                messageType = Mumble.TextMessage.MsgType.DocMessageType;
            } else if(msgType.equals(EnumConstant.MessageType.VIDEO.name())){
                messageType = Mumble.TextMessage.MsgType.VideoMessageType;
            }
            int index = CommonUtils.getSearchItemIndex(mOnlineUserList, mTargetUserName);
            if (index != -1) {
                responseMessage = hs.sendTextMsgToPttUser(mOnlineUserList.get(index).getSessionID(),
                        formattedMessage,messageType, msgId,mimeType,false);
            } else {
                responseMessage = hs.sendTextMsgToPttUser(Constants.OFFLINE_USER_SESSION_ID, mTargetUserId, mTargetUserName,
                        formattedMessage, messageType, msgId, mimeType,false);
            }
            // Update upload status to database
            if (msgType.equals(EnumConstant.MessageType.VIDEO.name()) || msgType.equals(EnumConstant.MessageType.DOCUMENT.name()) || msgType.equals(EnumConstant.MessageType.AUDIO.name()) || msgType.equals(EnumConstant.MessageType.IMAGE.name())) {
                if (msgType.equals(EnumConstant.MessageType.IMAGE.name())) {
                   mViewModel.updateJioTalkieChats(getResources().getString(R.string.uploading_done), responseMessage.getMessageId(),  ServerConstant.getDownloadAWSServer() + responseMessage.getMessageId());
                } else {
                    mViewModel.updateJioTalkieChats(getResources().getString(R.string.uploading_done), responseMessage.getMessageId(), responseMessage.getMessageContent());
                }
                mChatByMsgId = mViewModel.observeChatByMsgId(responseMessage.getMessageId());
                mChatByMsgId.observe(getActivity(), observeChatByMsgId);
                Toast.makeText(mActivity, getString(R.string.file_upload_success), Toast.LENGTH_SHORT).show();
                return;
            }
            // Add message to database
            mViewModel.storeMessageDataInDB(responseMessage, true, false,msgType
                    , mediaPath,EnumConstant.MsgStatus.Undelivered.ordinal());
        } else {
            Log.e(TAG, "Service not connected, cannot send message");
            Toast.makeText(getActivity(), "Service not connected",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void pauseAudio(){
        if (mPersonalChatAdapter != null)
            mPersonalChatAdapter.pauseAudio();
    }

    private void addChatMessage(JioTalkieChats message, boolean scroll) {
        if (mPersonalChatAdapter == null) {
            Log.v("MSG", "mPersonalChatAdapter is null");
            return;
        }
        mPersonalChatAdapter.add(message);
        JioTalkieService mService = (JioTalkieService) mViewModel.getJioTalkieService();
        if (mService.isBoundToPttServer() && !message.getIs_self_chat()) {
            mService.getJioPttSession().updateMsgStatus(message.getMsg_id(), Mumble.MessageDelivery.MsgStatus.Read);
        }
        if (scroll) {
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    if (mPersonalChatAdapter != null && mPersonalChatAdapter.getItemCount()>=1) {
                        mRecyclerView.smoothScrollToPosition(mPersonalChatAdapter.getItemCount() - 1);
                    }
                }
            });
        }
    }

    private String markupOutgoingMessage(String message) {
        String formattedBody = message;
        Matcher matcher = LINK_PATTERN.matcher(formattedBody);
        formattedBody = matcher.replaceAll("<a href=\"$1\">$1</a>")
                .replaceAll("\n", "<br>");
        return formattedBody;
    }


    private void onMediaFilePicked(Uri uri) {
        onMediaFilePicked(uri, false);
    }

    private void onMediaFilePicked(Uri uri, Boolean isImageEdited) {
        if (uri == null) {
            return;
        }
        if (!mViewModel.getJioTalkieService().isBoundToPttServer() ||
                !mViewModel.getJioTalkieService().isPttConnectionActive()) {
            return;
        }
        ContentResolver cR = requireContext().getContentResolver();
        String type = cR.getType(uri);
        if (isAudioRecordUri && type.equals("video/3gpp")) {
            type = "audio/3gpp";
            isAudioRecordUri = false;
        }
        if (CommonUtils.isImageMimeType(type)) {
            onImagePicked(uri, type, isImageEdited);
        } else if (type.contains(EnumConstant.AUDIO)) {
            String filePath = FileUtils.copyFileToInternal(this.getContext(), uri, type);
            onMediaConfirmed(filePath, EnumConstant.MIME_TYPE_AUDIO_OGG, EnumConstant.FILE_TYPE_AUDIO);
        } else if (type.contains(EnumConstant.VIDEO)) {
            String filePath = FileUtils.copyFileToInternal(this.getContext(), uri, type);
            onMediaConfirmed(filePath, EnumConstant.MIME_TYPE_VIDEO_MP4, EnumConstant.FILE_TYPE_VIDEO);
        } else if (CommonUtils.isDocumentMimeType(type)) {
            String filePath = FileUtils.copyFileToInternal(this.getContext(), uri, type);
            onMediaConfirmed(filePath, type, EnumConstant.FILE_TYPE_DOC);
        } else {
            Toast.makeText(this.getContext(), getString(R.string.unspported_file), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        pauseAudio();
    }

    private void onImagePicked(Uri uri, String type, boolean isImageEdited) {
        boolean needCompress = mViewModel.getSettings().isImageCompressEnable();
        String filePath = FileUtils.copyFileToInternal(this.getContext(), uri,type,needCompress);
        // We don't fail on errors when getting orientation
        boolean flipped = false;
        int rotationDeg = 0;
        try (InputStream imageStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (imageStream == null) {
                Log.w(TAG, "openInputStream(uri) failed for orientation");
            } else {
                ExifInterface exif = new ExifInterface(imageStream);
                flipped = exif.isFlipped();
                rotationDeg = exif.getRotationDegrees();
            }
        } catch (IOException e) {
            Log.w(TAG, "exception when getting orientation: " + e);
        }
        InputStream imageStream;
        try {
            imageStream = requireContext().getContentResolver().openInputStream(uri);
            if (imageStream == null) {
                Log.w(TAG, "openInputStream(uri) failed");
                return;
            }
        } catch (IOException e) {
            Log.w(TAG, "exception when opening stream: " + e);
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
        if (bitmap == null) {
            Log.w(TAG, "decode to bitmap failed");
            return;
        }
        if (flipped || rotationDeg > 0) {
            Matrix matrix = new Matrix();
            if (flipped) {
                // first flip horizontally, following {@link ExifInterface#getRotationDegrees()}
                matrix.postScale(-1, 1, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
            }
            if (rotationDeg > 0) {
                matrix.postRotate(rotationDeg);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, false);
        }

        Bitmap resized = BitmapUtils.resizeKeepingAspect(bitmap, 600, 400);

        // Added handler because Internal image capture is running on background thread.
        // But this image view is on main thread. So need to draw the image on imageview
        // we need to shift the image data to main thread using looper inside the handler.
        mHandler.post(() -> {
            ImageView preview = new ImageView(mActivity);
            preview.setImageBitmap(resized);
            preview.setAdjustViewBounds(true);
            preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preview.setMaxHeight(Resources.getSystem().getDisplayMetrics().heightPixels / 3);
            AlertDialog.Builder adb = new AlertDialog.Builder(mActivity)
                    .setMessage(R.string.prompt_confirm_image_send)
                    .setPositiveButton(android.R.string.ok, (dlg, which) -> onMediaConfirmed(filePath, type,EnumConstant.FILE_TYPE_IMAGE))
                    .setNegativeButton(android.R.string.cancel, null )
                    .setView(preview);
            if(!isImageEdited)
                adb.setNeutralButton(R.string.image_edit, (dlg, which) -> loadImageEditorFragment(uri));
            mOptionDialog = adb.create();
            mOptionDialog.show();
        });
    }

    private void onMediaConfirmed(String filePath, String mimeType, int fileType) {
        String messageId = MessageIdUtils.generateUUID();
        mMediaUploadStateObserver = mViewModel.observeFileUploadStateData();
        mMediaUploadStateObserver.observe(getActivity(),observeFileUploadState);
        getResources().getString(R.string.uploading_pending);
        File file = new File(filePath);
        if (mActivity.isFileSizeAllow(file, fileType)) {
            mViewModel.uploadMediaToServer(filePath, mimeType, messageId, fileType, mTargetUserName, false, getResources().getString(R.string.uploading_pending));
        }
    }

    private void registerVoiceTargetUser() {
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()) {
            IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
            int index = CommonUtils.getSearchItemIndex(mOnlineUserList, mTargetUserName);
            if (index != -1) {
                if (session.getCurrentVoiceTargetMode() == JioPttEnums.AudioTargetMode.WHISPER_TARGET) {
                    session.removeWhisperTarget(session.getCurrentVoiceTargetId());
                }
                String userName = mOnlineUserList.get(index).getUserName();
//                WhisperTargetUsers userTarget = new WhisperTargetUsers(mOnlineUserList.get(index).getSessionID(), userName);

                //To register a new Whisper , only userModel is required
                byte id = session.addWhisperTarget(mOnlineUserList.get(index));
                if (id > 0) {
                    session.assignVoiceTargetId(id);
//                    Log.d(TAG, "register VoiceTargetUser");
                } else {
//                    Log.d(TAG, "setVoiceTargetUser: unable to set voice target for user " + userName);
                }
            }
        }
    }

    @Override
    public void onNavigateButtonClick(double latitude, double longitude, View view) {

    }

    private void setPTTCallUserState(boolean isTalking) {
        mCallId =  MessageIdUtils.generateUUID();
        int index = CommonUtils.getSearchItemIndex(mOnlineUserList, mTargetUserName);
        if (index >= 0) {
            targetSessionId = mOnlineUserList.get(index).getSessionID();
            JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
            service.getJioPttSession().updatePersonalChatUserState(isTalking, targetSessionId, mTargetUserId, mCallId);
            ADCInfoUtils.floorGrantedInfo(true, 0, "", mViewModel.getUserId(), mViewModel.getChannelId(), "OneToOne");
        } else if (!isTalking) {
            JioTalkieService service = (JioTalkieService) mViewModel.getJioTalkieService();
            if (service.isPttConnectionActive() && mTargetUserId != -1) {
                service.getJioPttSession().updatePersonalChatUserState(false, targetSessionId, mTargetUserId, mCallId);
                ADCInfoUtils.floorGrantedInfo(false, 0, "", mViewModel.getUserId(), mViewModel.getChannelId(), "OneToOne");
            }
        }
    }

    private void getPersonalChatHistory() {
        mPaginatedPersonalChat.observe(getActivity(), observerPaginatedChat);
        MessageRequestModel messageRequestModel = new MessageRequestModel(mViewModel.getSelfUserId(), mTargetUserId, true, null, null, null, true, 30, 0);
        mViewModel.downloadMessagesWithPagination(messageRequestModel);
    }

    public static String getCalculatedDate(String dateFormat, int days) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat s = new SimpleDateFormat(dateFormat);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return s.format(new Date(cal.getTimeInMillis()));
    }

    private void setScrollView(LinearLayoutManager layoutManager, ImageView scrollBtn, int lastPosition) {
        int firstVisibleItemPosition;
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        if (lastPosition == 0)
            firstVisibleItemPosition = lastPosition;
        else
            firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        //Disable scroll view once we have reach the end to the recyclerView else enable.
        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0)
            scrollBtn.setVisibility(GONE);
        else
            scrollBtn.setVisibility(View.VISIBLE);
    }

    private void unregisterVoiceTargetUser() {
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()) {
            IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
            if (session.getCurrentVoiceTargetMode() == JioPttEnums.AudioTargetMode.WHISPER_TARGET) {
                byte target = session.getCurrentVoiceTargetId();
                session.assignVoiceTargetId((byte) 0);
                session.removeWhisperTarget(target);
            }
        }
    }
    @Override
    public void downloadMediaFile(int position, JioTalkieChats currMessage, boolean isLeft, boolean isSos, String mimeType, String messageType) {
        mViewModel.downloadMediaAndDocumentFile(position,currMessage,isLeft,isSos,mimeType,messageType);
    }

    @Override
    public void updateImageSize(String msgId, long imageSize) {
        mViewModel.updateImageSizeInDB(msgId,imageSize);
    }

    @Override
    public void playFullScreen(String mediaPath, boolean isVideo) {
        Bundle args=new Bundle();
        args.putString(EnumConstant.MEDIA_PATH,mediaPath);
        args.putBoolean(EnumConstant.IS_VIDEO,isVideo);
        mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.MEDIAPLAYER_FRAGMENT.ordinal(),args);
    }

    private void registerViewModelObserver() {

        mViewModel.observeDisablePttCall().observe(this, pttCallUserState -> {
            if (pttCallUserState.getmCallId().equals(mCallId) && !pttCallUserState.isGroupCall()) {
                if (isTalkingStarted) {
                    handlePTTButtonActionUp();
                }
            }
        });

        mViewModel.observerUserStateData().observe(this, userState -> {
            if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive() && mViewModel.getJioTalkieService().getJioPttSession() != null) {
                if (userState.getUser().getUserChannel().getChannelID() != mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID()) {
                    return;
                }
            }
            switch (userState.getUserState()) {
                case USER_CONNECTED:
                 if (userState.getUser().getUserID() == mTargetUserId) {
                     mOfflineStatusLL.setVisibility(View.INVISIBLE);
                     onlineUserCountView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.online_icon,0,0,0);
                     onlineUserCountView.setText(getResources().getString(R.string.status_online));
                     enablePTTCallButton(true);
                 }
                 break;
                case USER_REMOVED:
                    if (userState.getUser().getUserID() == mTargetUserId) {
                        mOfflineStatusLL.setVisibility(View.VISIBLE);
                        onlineUserCountView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.offline_icon,0,0,0);
                        onlineUserCountView.setText(getResources().getString(R.string.status_offline));
                        enablePTTCallButton(false);
                        if(isTalkingStarted) {
                            Toast.makeText(mActivity, mActivity.getResources().getString(R.string.receiver_offline, mTargetUserName), Toast.LENGTH_LONG).show();
                            handlePTTButtonActionUp();
                        }
                    }
                    break;
            }
        });
    }

    private final Observer<PaginatedPersonalChat> observerPaginatedChat = new Observer<PaginatedPersonalChat>() {
        @Override
        public void onChanged(PaginatedPersonalChat paginatedPersonalChat) {
            List<JioTalkieChats> paginatedMessageList = paginatedPersonalChat.getChats();
            isLast = paginatedPersonalChat.isLast();
            if (!paginatedMessageList.isEmpty() && loadedMessageCount < paginatedMessageList.size() && paginatedPersonalChat.getUserIdReceiver() == mTargetUserId) {
                mChatMessageList = paginatedMessageList;
                int newItemCount = paginatedMessageList.size() - loadedMessageCount;
                loadedMessageCount = paginatedMessageList.size();
                isMessageLoaded = true;
                mPersonalChatAdapter.clear();
                if (paginatedMessageList.size() > 0) {
                    for (JioTalkieChats chat : paginatedMessageList) {
                        addChatMessage(chat, false);
                    }
                    mRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mPersonalChatAdapter != null) {
                                mRecyclerView.smoothScrollToPosition(newItemCount);
                            }
                        }
                    });
                } else {
                    mEmptyLinearLayout.setVisibility(View.VISIBLE);
                }
                mLoaderLayout.setVisibility(GONE);
                isLoadingDataIntoUI = false;
                mPaginatedPersonalChat.removeObserver(observerPaginatedChat);
            }
        }
    };

    @Override
    public void onItemSelected(String msgId, int selectedPosition, boolean isSelfChat, boolean isTextMessage, String textMessage) {
        mMsgId = msgId;
        mSelectedPosition = selectedPosition;
        isItemSelected = true;
        mTextMessage = textMessage;
        mToolbar.findViewById(R.id.actionCalender).setVisibility(GONE);
        mToolbar.findViewById(R.id.actionDelete).setVisibility(View.VISIBLE);
        if (isTextMessage) {
            mToolbar.findViewById(R.id.actionCopy).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemDeselected() {
        isItemSelected = false; // Mark item as deselected
        mToolbar.findViewById(R.id.actionCalender).setVisibility(View.VISIBLE);
        mToolbar.findViewById(R.id.actionDelete).setVisibility(GONE);
        mToolbar.findViewById(R.id.actionCopy).setVisibility(GONE);
        mPersonalChatAdapter.onItemDeselected();
    }

    public boolean isItemSelected() {
        return isItemSelected;
    }

    @Override
    public void croppedImageUri(Uri uri) {
        onMediaFilePicked(uri, true);
    }

    private void loadImageEditorFragment(Uri imageUri){
        Bundle args=new Bundle();
        args.putParcelable("image_uri", imageUri);
        ImageEditorFragment editorFragment = new ImageEditorFragment();
        editorFragment.setArguments(args);
        editorFragment.setListener(this);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.content_frame, editorFragment)
                .addToBackStack(null)
                .commit();
    }
}