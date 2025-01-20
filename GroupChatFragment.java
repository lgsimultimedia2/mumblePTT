package com.jio.jiotalkie.fragment;

import static android.app.Activity.RESULT_OK;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.View.GONE;
import static android.view.inputmethod.EditorInfo.IME_NULL;

import android.Manifest;
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
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.emoji2.widget.EmojiEditText;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.customservice.wrapper.IMediaMessage;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.ChatAdapter;
import com.jio.jiotalkie.adapter.ChatViewHolder;
import com.jio.jiotalkie.adapter.provider.ImageEditorResultListener;
import com.jio.jiotalkie.dataclass.AudioDownloadState;
import com.jio.jiotalkie.dataclass.DocumentDownloadState;
import com.jio.jiotalkie.dataclass.VideoDownloadState;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.FilterChatMessageList;
import com.jio.jiotalkie.model.JioTalkieChats;
import com.jio.jiotalkie.model.PaginatedGroupChat;
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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.application.customservice.wrapper.IJioPttSession;
import com.application.customservice.Mumble;
import com.yalantis.ucrop.UCropActivity;


@TrackPerformance(threshold = 300)
public class GroupChatFragment extends Fragment implements ChatAdapter.ChatAdapterProvider , ImageEditorResultListener {

    private DashboardViewModel mViewModel;
    private static final int CAMERA_REQUEST_PERMISSIONS = 10;

    private static final String TAG = GroupChatFragment.class.getName();
    private static final Pattern LINK_PATTERN = Pattern.compile("(https?://\\S+)");

    private RecyclerView mRecyclerView;
    private EmojiEditText mEditText;
    private String mCaptureFilePath = "";
    private String mImageMimeType = null;

    private ChatAdapter mChatAdapter;

    private boolean isUserScrolling = false;

    private boolean isKeyBoardVisibleFirstTime = false;

    private Handler mHandler;
    private int mSelectedPosition;
    private boolean isItemSelected = false;
    private String mMsgId;
    private boolean mIsSelfChat;
    private boolean mIsTextMessage;
    private String mTextMessage;
    private Date selectedDate;
    private Date firstMessageDate;
    private long firstDateTime;
    private boolean isLast;
    private final ActivityResultLauncher<Intent> mCameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            if (!mViewModel.getJioTalkieService().isBoundToPttServer()  || !mViewModel.getJioTalkieService().isPttConnectionActive()) {
                return;
            }
            Uri imageUri = CommonUtils.getUriFromAbsolutePath(getContext(), mCaptureFilePath);
            onMediaFilePicked(imageUri);
        }
    });
    private final ActivityResultLauncher<Intent> mGalleyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri selectedMediaUri = result.getData().getData();
            onMediaFilePicked(selectedMediaUri);
        }
    });

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
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri editedImageUri = data.getData();
                        onMediaFilePicked(editedImageUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> mDocumentPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onMediaFilePicked);
    private ImageView mSendTextButton;
    private ImageView mScrollBtn;
    private TextView floatingDate;
    private DashboardActivity mActivity;
    private List<JioTalkieChats> mChatMessageList;
    private LiveData<List<JioTalkieChats>> mGroupChatObserver;

    private LiveData<FilterChatMessageList> mFilteredGroupChatFromServer;
    private LiveData<List<JioTalkieChats>> mFilteredGroupChatFromDB;
    private LiveData<PaginatedGroupChat> mPaginatedGroupChat;

    private LiveData<JioTalkieChats> mChatMessageDataObserver;
    private LiveData<AudioDownloadState> mAudioDownloadObserver;
    private LiveData<VideoDownloadState> mVideoDownloadObserver;
    private LiveData<DocumentDownloadState> mDocumentDownloadObserver;
    private LiveData<MediaUploadResponse>mMediaUploadStateObserver;
    private LiveData<JioTalkieChats> mChatByMsgId;
    private Toolbar mToolbar;
    private View mAttachmentPopup;
    private AlertDialog  mOptionDialog = null;
    private PopupWindow mAttachmentPopupWindow;
    private boolean isMessageLoaded=false;
    private int loadedMessageCount;
    private View mLoaderLayout;

    private ImageView actionBack;

    private ImageView actionHome;

    private long mChatHistorySelectedDate;
    private boolean isOnCreateCalled = false;

    private File croppedDestinationFile;
    private boolean isLoadingDataIntoUI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        isOnCreateCalled = true;
    }

    private void updateToolBar() {
        mActivity.showHomeToolsBar();
        actionBack.setVisibility(View.VISIBLE);
        actionHome.setVisibility(View.GONE);
        mActivity.needSOSButton(false);
        mActivity.needBottomNavigation(true);
        mToolbar = getActivity().findViewById(R.id.toolbar_main);
        mToolbar.findViewById(R.id.actionSearch).setVisibility(View.GONE);
        mToolbar.findViewById(R.id.actionCalender).setVisibility(View.VISIBLE);

        if (isItemSelected()) {
            updateToolBar(mIsSelfChat);
        }
        mToolbar.findViewById(R.id.actionDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int result = mViewModel.deleteJioTalkieByMsgId(mMsgId);
                if (result > 0) {
                    mChatAdapter.removeItem(mSelectedPosition);
                    Toast.makeText(mActivity, "Selected message deleted successfully", Toast.LENGTH_SHORT).show();
                }
                onItemDeselected();
            }
        });

        mToolbar.findViewById(R.id.actionInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChatAdapter.showDeliveryStatus();
            }
        });
        mToolbar.findViewById(R.id.actionCopy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemDeselected();
                mActivity.copyTextToClipboard(mTextMessage);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isOnCreateCalled) {
            initRecyclerView();
            registerViewModeObserver();
            registerUserTalkStateObserver();
        }
        isOnCreateCalled = false;
        updateToolBar();
//        mTargetProvider.registerChatTargetListener(this);
        mEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                mEditText.clearFocus();
                mActivity.handleOnBackPress();
                return true;
            }
            return false;
        });
        // Clear all the group chat notification from notification central
        mViewModel.getJioTalkieService().getJioTalkieNotification().clearGroupChatNotification();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // remove the observer to avoid multiple instance running
        Log.d(TAG,"OnDestroy called");
        pauseAudio();
        isMessageLoaded=false;
        if(mChatMessageDataObserver != null) mChatMessageDataObserver.removeObserver(observeChatMessageData);
        if (mAttachmentPopupWindow != null && mAttachmentPopupWindow.isShowing()) {
            mAttachmentPopupWindow.dismiss();
        }
        if (mOptionDialog != null && mOptionDialog.isShowing()){
            mOptionDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        pauseAudio();
    }

    private void openCamera(){
        File imageFile = null;
        try {
            imageFile = FileUtils.createImageFile(getContext());
            mCaptureFilePath = imageFile.getAbsolutePath();
        } catch (IOException e) {
            mCaptureFilePath = null;
            e.printStackTrace();
        }
        Uri imageUri = FileProvider.getUriForFile(getContext(), "com.jio.jiotalkie.dispatch.provider", imageFile);
        ContentResolver cR = requireContext().getContentResolver();
        mImageMimeType = cR.getType(imageUri);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        mCameraLauncher.launch(intent);

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = (DashboardActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);
        mViewModel = new ViewModelProvider(mActivity).get(DashboardViewModel.class);
        mHandler = new Handler(getActivity().getMainLooper());
        mRecyclerView = view.findViewById(R.id.group_chat_recyclerview);
        mEditText = view.findViewById(R.id.chatTextEdit);
        mSendTextButton = view.findViewById(R.id.iv_send_btn);
        mScrollBtn = view.findViewById(R.id.iv_scroll_btn);
        mLoaderLayout = view.findViewById(R.id.loading_layout);
        actionBack = mActivity.findViewById(R.id.actionBack);
        actionHome=mActivity.findViewById(R.id.actionHome);
        mAttachmentPopup = inflater.inflate(R.layout.attachment_popup_window,container,false);
        TextView locationAttachmentButton = mAttachmentPopup.findViewById(R.id.locationAttachmentButton);
        TextView galleryAttachmentButton = mAttachmentPopup.findViewById(R.id.galleryAttachmentButton);
        TextView cameraAttachmentButton = mAttachmentPopup.findViewById(R.id.cameraAttachmentButton);
        TextView documentAttachmentButton = mAttachmentPopup.findViewById(R.id.documentAttachmentButton);

        cameraAttachmentButton.setOnClickListener(view1 -> {
            if (checkPermissions()) {
                openCamera();
            } else {
                requestAppPermissions();
            }
            mAttachmentPopupWindow.dismiss();

        });
        locationAttachmentButton.setOnClickListener(v -> {
            if(!GpsUtils.isLocationEnabled(mActivity)){
              Toast.makeText(mActivity,mActivity.getResources().getString(R.string.enable_location),Toast.LENGTH_SHORT).show();
              return;
            }
            Bundle args=new Bundle();
            args.putBoolean("isGroupChat",true);
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
        });
        documentAttachmentButton.setOnClickListener(v -> {
            mDocumentPicker.launch("*/*");
            mAttachmentPopupWindow.dismiss();
        });
        mAttachmentPopupWindow = new PopupWindow(mAttachmentPopup, ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,true);
        mAttachmentPopup.setOnClickListener(v -> mAttachmentPopupWindow.dismiss());

        floatingDate = view.findViewById(R.id.floating_date);
        ImageButton attachIcon = view.findViewById(R.id.attachmentButton);
        attachIcon.setOnClickListener(v -> {
            hideKeyboard();
            onItemDeselected();
            int offsetY = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 220, getResources().getDisplayMetrics());
            mAttachmentPopupWindow.showAtLocation(view, Gravity.BOTTOM, 0, offsetY);
        });
      //  getChannelInfoFromSession();
        mEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == IME_NULL && event != null && event.getKeyCode() == KEYCODE_ENTER) {
                sendMessageFromEditor();
                return true;
            }
            return false;
        });

        mEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
                if (linearLayoutManager != null && mScrollBtn != null ) {
                    setScrollView(linearLayoutManager, mScrollBtn, 0);
                }
                onItemDeselected();
                return false;
            }
        });
        mScrollBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mChatAdapter != null && mChatAdapter.getItemCount()>=1) {
                    mRecyclerView.scrollToPosition(mChatAdapter.getItemCount() - 1);
                }
            }
        });

        mSendTextButton.setOnClickListener(view12 -> sendMessageFromEditor());
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
//        updateChatTargetText(mTargetProvider.getChatTarget());

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                mActivity.handleOnBackPress();
                return true;
            }
            return false;
        });
        mLoaderLayout.setVisibility(View.VISIBLE);
        final View rootView = view.findViewById(R.id.fragmentGroupChat);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int visibleHeight = r.height();
            int heightDiff = screenHeight - visibleHeight;
            boolean isKeyboardShowing = heightDiff > (screenHeight * 0.15); // 15% of the screen height, adjust as needed
            if (isKeyboardShowing && !isKeyBoardVisibleFirstTime) {
                isKeyBoardVisibleFirstTime=true;
                if(!isUserScrolling && mChatAdapter != null && mChatAdapter.getItemCount()>=1) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
                    int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition()+3;
                    int totalItems = mChatAdapter.getItemCount()-1;
                    if(lastVisiblePosition > totalItems || Math.abs(totalItems-lastVisiblePosition) <= 2)
                        lastVisiblePosition = totalItems;
                    mRecyclerView.scrollToPosition(lastVisiblePosition);
                }
            }else if(!isKeyboardShowing){
                isKeyBoardVisibleFirstTime=false;
            }
        });
        isOnCreateCalled = true;

        mActivity.getViewModel().getImageEditResult().observe(getViewLifecycleOwner(), uCropResult -> {
            if (uCropResult.mResultCode == RESULT_OK && uCropResult.mResultData!=null) {
                // Cropped image URI
                Uri croppedImageUri = UCrop.getOutput(uCropResult.mResultData);
                if (uCropResult.mResultCode == RESULT_OK && uCropResult.mResultData!=null) {
                    Uri croppedContentUri=FileProvider.getUriForFile(getContext(), "com.jio.jiotalkie.dispatch.provider", croppedDestinationFile);
                    onMediaFilePicked(croppedContentUri, true);
                }
            } else if (uCropResult.mResultCode == UCrop.RESULT_ERROR) {
                Throwable cropError = UCrop.getError(uCropResult.mResultData);
                Toast.makeText(mActivity, "Crop error: " + cropError, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    public void pauseAudio(){
        if(mChatAdapter != null) {
            mChatAdapter.pauseAudio();
        }
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
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
        }
    }
    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private String getDispatcherChannelInfo() {
        String dispatcherChannelName ="";
        if (mViewModel != null && mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel
                .getJioTalkieService().getJioPttSession() != null && mViewModel
                .getJioTalkieService().getJioPttSession().fetchSessionPttUser() != null) {
            dispatcherChannelName = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserChannel().getChannelName();
            Log.d(TAG, "ChannelModel Name is " + dispatcherChannelName);
        }
        return dispatcherChannelName;
    }

    private void registerViewModeObserver() {
        mPaginatedGroupChat = mViewModel.observePaginatedGroupChat();
        mFilteredGroupChatFromServer = mViewModel.observeFilteredChatFromServer();
        mChatMessageDataObserver = mViewModel.observeChatMessageData();
        mChatMessageDataObserver.observe(this,observeChatMessageData);
        mGroupChatObserver = mViewModel.observeGroupChatFromDB();
        mGroupChatObserver.observe(this, observeGroupChatData);
        mAudioDownloadObserver = mViewModel.observeDownloadAudio();
        mAudioDownloadObserver.observe(this,observeDownloadAudio);
        mVideoDownloadObserver = mViewModel.observeDownloadVideo();
        mVideoDownloadObserver.observe(this,observeDownloadVideo);
        mDocumentDownloadObserver = mViewModel.observeDownloadDocument();
        mDocumentDownloadObserver.observe(this,observeDownloadDocument);
        mViewModel.observeMsgDeliveryReport().observe(this , messageDelivery -> {
            if(mChatAdapter!=null) {
                 mChatAdapter.updateMessageStatus(messageDelivery.getMsgId(), messageDelivery.getStatus().ordinal(), String.valueOf(messageDelivery.getReceiverDisplayedListList()), String.valueOf(messageDelivery.getReceiverDeliveredListList()));
            }
        });
      
        mViewModel.observeRegisterUserData().observe(this, registeredUsers -> {
            Log.d(TAG, "observeRegisterUserData no of users: " + registeredUsers.size());
            mChatAdapter.updateRegisterUserList(registeredUsers);
        });
    }

    private void registerUserTalkStateObserver() {

        mViewModel.observeSelfUserTalkState().observe(this, userTalkState -> {
            if(userTalkState.getUserTalkState()== EnumConstant.userTalkState.PASSIVE) {
                mHandler.postDelayed(() -> mChatAdapter.notifyDataSetChanged(), 800);
            }
            if(!userTalkState.isSelfUser() && userTalkState.getUserTalkState()== EnumConstant.userTalkState.TALKING){
                if (mAttachmentPopupWindow !=null && mAttachmentPopupWindow.isShowing())
                    mAttachmentPopupWindow.dismiss();
                if (mChatAdapter !=null) mChatAdapter.pauseAudio();
            }
        });
    }

    private final Observer<JioTalkieChats> observeChatByMsgId = new Observer<JioTalkieChats>() {
        @Override
        public void onChanged(JioTalkieChats message) {
            if (!message.getFile_upload_status().equals(getResources().getString(R.string.uploading_pending))) {
                mChatAdapter.refreshData(message);
                mChatByMsgId.removeObserver(observeChatByMsgId);
            }
        }
    };

    private final Observer<JioTalkieChats> observeChatMessageData = new Observer<JioTalkieChats>() {
        @Override
        public void onChanged(JioTalkieChats jioTalkieChats) {
            if (jioTalkieChats.getIs_group_chat()) {
                Log.d(TAG, "registerViewModeObserver: observeChatMessageData from server called ");
                addChatMessage(jioTalkieChats, true);
            }
        }
    };
    private final Observer<List<JioTalkieChats>> observeGroupChatData = new Observer<List<JioTalkieChats>>() {
        @Override
        public void onChanged(List<JioTalkieChats> message) {
            mChatMessageList = message;
            loadedMessageCount = message.size();
            isMessageLoaded=true;
            mChatAdapter.clear();
            for (JioTalkieChats chat : message) {
                addChatMessage(chat, false);
            }
            mGroupChatObserver.removeObserver(observeGroupChatData);
            if(!message.isEmpty()) {
                mRecyclerView.post(() -> mRecyclerView.scrollToPosition(mChatAdapter.getItemCount() - 1));
            }
        }
    };

    private final Observer<List<JioTalkieChats>> observerFilteredChatFromLocalDB = new Observer<List<JioTalkieChats>>() {
        @Override
        public void onChanged(List<JioTalkieChats> message) {
            selectedDate = new Date(mChatHistorySelectedDate);
            if (!message.isEmpty()) {
                if (!isSelectedDateAvailable(message)) {
                    long durationTillLong = System.currentTimeMillis();
                    String durationTill = DateUtils.getStringDateFromLong(durationTillLong, DateUtils.dateFormatServer);
                    String durationFrom = DateUtils.getStringDateFromLong(mChatHistorySelectedDate, DateUtils.dateFormatServer);
                    mViewModel.messagesDownloadApi(-1, -1, false, durationFrom, durationTill, true);
                    mFilteredGroupChatFromServer.observe(getActivity(), observerFilteredChatFromServer);
                } else {
                    updateChatWithFilterMessageList(message);
                    Toast.makeText(mActivity, mActivity.getResources().getString(R.string.chat_history_in_range, CommonUtils.getSimpleDateFormatForToast(selectedDate)), Toast.LENGTH_LONG).show();
                }
            } else {
                mLoaderLayout.setVisibility(GONE);
                Toast.makeText(mActivity,mActivity.getResources().getString(R.string.no_chat_history_in_range,CommonUtils.getSimpleDateFormatForToast(selectedDate)),Toast.LENGTH_SHORT).show();
            }
            mFilteredGroupChatFromDB.removeObserver(observerFilteredChatFromLocalDB);
        }
    };

    private final Observer<FilterChatMessageList> observerFilteredChatFromServer = new Observer<FilterChatMessageList>() {
        @Override
        public void onChanged(FilterChatMessageList filterChatMessageList) {
            if (filterChatMessageList.getDurationFrom().equals(DateUtils.getStringDateFromLong(mChatHistorySelectedDate, DateUtils.dateFormatServer))) {
                selectedDate = new Date(mChatHistorySelectedDate);
                List<JioTalkieChats> messageList = filterChatMessageList.getJioTalkieChats();
                if (!messageList.isEmpty()) {
                    if (!isSelectedDateAvailable(messageList)) {
                        updateChatWithFilterMessageList(messageList);
                        Toast.makeText(mActivity, mActivity.getResources().getString(R.string.no_chat_history_selected_date,
                                CommonUtils.getSimpleDateFormatForToast(firstMessageDate)), Toast.LENGTH_LONG).show();
                    } else {
                        updateChatWithFilterMessageList(messageList);
                        Toast.makeText(mActivity, mActivity.getResources().getString(R.string.chat_history_in_range, CommonUtils.getSimpleDateFormatForToast(selectedDate)), Toast.LENGTH_LONG).show();
                    }
                } else {
                    mLoaderLayout.setVisibility(GONE);
                    Toast.makeText(mActivity, mActivity.getResources().getString(R.string.no_chat_history_in_range, CommonUtils.getSimpleDateFormatForToast(selectedDate)), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private final Observer<PaginatedGroupChat> observerPaginatedChat = new Observer<PaginatedGroupChat>() {
        @Override
        public void onChanged(PaginatedGroupChat paginatedGroupChat) {
            List<JioTalkieChats> paginatedMessageList = paginatedGroupChat.getChats();
            isLast = paginatedGroupChat.isLast();
            if (!paginatedMessageList.isEmpty() && loadedMessageCount < paginatedMessageList.size()) {
                updateChatWithPaginatedMessageList(paginatedMessageList, paginatedMessageList.size() - loadedMessageCount);
            }
        }
    };

    private final Observer<AudioDownloadState> observeDownloadAudio = new Observer<AudioDownloadState>() {
        @Override
        public void onChanged(AudioDownloadState audioDownloadState) {
            ChatViewHolder viewHolderForCurrentPosition = (ChatViewHolder) mRecyclerView.findViewHolderForAdapterPosition(audioDownloadState.getPosition());
            if (mChatAdapter!=null && audioDownloadState.getPosition() != -1){
                mChatAdapter.updateAudioUI(audioDownloadState.getPosition(), audioDownloadState.getFilePath(),audioDownloadState.isLeft(),audioDownloadState.isSos(),(ChatViewHolder) viewHolderForCurrentPosition);
                mViewModel.resetAudioDownloadObserver();
            }
        }
    };

    private final Observer<VideoDownloadState> observeDownloadVideo = new Observer<VideoDownloadState>() {
        @Override
        public void onChanged(VideoDownloadState videoDownloadState) {
            ChatViewHolder viewHolderForCurrentPosition = (ChatViewHolder) mRecyclerView.findViewHolderForAdapterPosition(videoDownloadState.getPosition());
            if (mChatAdapter!=null&& videoDownloadState.getPosition() != -1){
                mChatAdapter.updateVideoUI(videoDownloadState.getPosition(), videoDownloadState.getFilePath(),videoDownloadState.isLeft(),videoDownloadState.isSos(), viewHolderForCurrentPosition);
                mViewModel.resetVideoDownloadObserver();
            }
        }
    };

    private final Observer<DocumentDownloadState> observeDownloadDocument = new Observer<DocumentDownloadState>() {
        @Override
        public void onChanged(DocumentDownloadState documentDownloadState) {
            ChatViewHolder viewHolderForCurrentPosition = (ChatViewHolder) mRecyclerView.findViewHolderForAdapterPosition(documentDownloadState.getPosition());
            if (mChatAdapter!=null && documentDownloadState.getPosition() != -1) {
                mChatAdapter.updateDocumentUI(documentDownloadState.getPosition(), documentDownloadState.getFilePath(), documentDownloadState.isLeft(), (ChatViewHolder) viewHolderForCurrentPosition);
                mViewModel.resetDocumentDownloadObserver();
            }
        }
    };

    private final Observer<MediaUploadResponse> observeFileUploadState = new Observer<MediaUploadResponse>() {
        @Override
        public void onChanged(MediaUploadResponse mediaUploadResponse) {
            Log.d(TAG,"jioPtt: GroupChatFragment:observeFileUploadState: upload status is: " + mediaUploadResponse.getFileUploadSuccess() +
                    "file type:" + mediaUploadResponse.getFileType() + ", msgId:" + mediaUploadResponse.getMsgId());
            try {
                if (croppedDestinationFile != null && croppedDestinationFile.exists()) {
                    croppedDestinationFile.delete();
                    croppedDestinationFile=null;
                }
            }catch (Exception e){
                Log.d(TAG,"On File upload status, delete cropped Destination file exception "+Log.getStackTraceString(e));
            }
            if(mediaUploadResponse.getFileUploadSuccess()){
                if(mediaUploadResponse.getFileType()==EnumConstant.FILE_TYPE_IMAGE){
                    sendMessage(EnumConstant.IMAGE_MSG,mediaUploadResponse.getMsgId(),EnumConstant.MessageType.IMAGE.name(),mediaUploadResponse.getMimeType(),"");
                }
                else if(mediaUploadResponse.getFileType()==EnumConstant.FILE_TYPE_AUDIO){
                    sendMessage(EnumConstant.AUDIO_MSG,mediaUploadResponse.getMsgId(),EnumConstant.MessageType.AUDIO.name(), mediaUploadResponse.getMimeType(),mediaUploadResponse.getMediaPath());
                }
                else if (mediaUploadResponse.getFileType()==EnumConstant.FILE_TYPE_VIDEO){
                    sendMessage(EnumConstant.VIDEO_MSG,mediaUploadResponse.getMsgId(),EnumConstant.MessageType.VIDEO.name(), mediaUploadResponse.getMimeType(),mediaUploadResponse.getMediaPath());
                }
                else if (mediaUploadResponse.getFileType() == EnumConstant.FILE_TYPE_DOC) {
                    if (mediaUploadResponse.getMediaPath() != null) {
                        String[] filename = mediaUploadResponse.getMediaPath().split("/Documents/");
                        sendMessage(filename[1],mediaUploadResponse.getMsgId(),EnumConstant.MessageType.DOCUMENT.name(),
                                mediaUploadResponse.getMimeType(),mediaUploadResponse.getMediaPath());
                    }
                }
                ADCInfoUtils.calculateImageSize(mediaUploadResponse.getMediaPath(),true,mediaUploadResponse.getMimeType(),mViewModel.getUserId(),mViewModel.getChannelId(),"group",-1);
                mViewModel.resetMediaUploadStateObserver();
            } else{
                if(!mediaUploadResponse.getUploadStatus().isEmpty()) {
                    Toast.makeText(mActivity,mediaUploadResponse.getUploadStatus(),Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    public void filterChatMessages(long receivedTime){
        mChatHistorySelectedDate = receivedTime;
        mLoaderLayout.setVisibility(View.VISIBLE);
        floatingDate.setVisibility(View.GONE);
        mFilteredGroupChatFromDB = mViewModel.observeFilteredGroupChatFromDB(receivedTime);
        mFilteredGroupChatFromDB.observe(this,observerFilteredChatFromLocalDB);
    }

    private void getChannelInfoFromSession() {
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()) {
            try {
                IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
                if (session != null && session.fetchSessionPttChannel()!= null &&
                        session.fetchSessionPttChannel().getChannelName()!= null) {
                    String channelName = session.fetchSessionPttChannel().getChannelName();
                    //assert mActivity != null;
                   // mActivity.showDashboardToolbar(false, channelName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void onMediaFilePicked(Uri uri) {
        onMediaFilePicked(uri,false);
    }

    private void onMediaFilePicked(Uri uri, Boolean isImageEditedRequired) {
        if (uri == null) {
            return;
        }
        if (!mViewModel.getJioTalkieService().isBoundToPttServer() ||
                !mViewModel.getJioTalkieService().isPttConnectionActive()) {
            return;
        }
        ContentResolver cR = requireContext().getContentResolver();
        String type = cR.getType(uri);
        if (CommonUtils.isImageMimeType(type)) {
            onImagePicked(uri, type, isImageEditedRequired);
        } else if (type.contains(EnumConstant.AUDIO)) {
            String filePath = FileUtils.copyFileToInternal(this.getContext(), uri, type);
            onMediaConfirmed(filePath, EnumConstant.MIME_TYPE_AUDIO_OGG, EnumConstant.FILE_TYPE_AUDIO);
        } else if (type.contains(EnumConstant.VIDEO)) {
            String filePath = FileUtils.copyFileToInternal(this.getContext(), uri, type);
            Log.v("Video", filePath);
            onMediaConfirmed(filePath, EnumConstant.MIME_TYPE_VIDEO_MP4, EnumConstant.FILE_TYPE_VIDEO);
        } else if (CommonUtils.isDocumentMimeType(type)) {
            String filePath = FileUtils.copyFileToInternal(this.getContext(), uri, type);
            onMediaConfirmed(filePath, type, EnumConstant.FILE_TYPE_DOC);
        } else {
            Toast.makeText(this.getContext(), getString(R.string.unspported_file), Toast.LENGTH_SHORT).show();
        }
    }

    private void onImagePicked(Uri uri, String type, Boolean isImageEdited){
        boolean needCompress = mViewModel.getSettings().isImageCompressEnable();
        String filePath = FileUtils.copyFileToInternal(this.getContext(), uri, type,needCompress);
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
        Log.d(TAG, "flipped:" + flipped + " rotationDeg:" + rotationDeg);

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

        ImageView preview = new ImageView(requireContext());
        preview.setImageBitmap(resized);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setMaxHeight(Resources.getSystem().getDisplayMetrics().heightPixels / 3);
        AlertDialog.Builder adb = new AlertDialog.Builder(requireContext()).setMessage(R.string.prompt_confirm_image_send)
                .setPositiveButton(android.R.string.ok, (dlg, which) -> onMediaConfirmed(filePath, type, EnumConstant.FILE_TYPE_IMAGE))
                .setNegativeButton(android.R.string.cancel, null)
                .setView(preview);

        if (!isImageEdited)
            adb.setNeutralButton(R.string.image_edit, (dlg, which) -> loadImageEditorFragment(uri));

        mOptionDialog = adb.create();
        mOptionDialog.show();
    }

    private void onMediaConfirmed(String filePath, String mimeType, int fileType) {
        String messageId = MessageIdUtils.generateUUID();
        mMediaUploadStateObserver = mViewModel.observeFileUploadStateData();
        mMediaUploadStateObserver.observe(getActivity(),observeFileUploadState);
        Log.d(TAG,"upload: in onMediaConfirmed, just before calling uploadMediaToServer: filePath: "
                + filePath + " mimeType: " + mimeType + "  messageID: " + messageId + "  fileType:  " + fileType);
        File file = new File(filePath);
        if (mActivity.isFileSizeAllow(file, fileType)) {
            mViewModel.uploadMediaToServer(filePath, mimeType, messageId, fileType, null, true, getResources().getString(R.string.uploading_pending));
        }
    }

    private void sendMessageFromEditor() {
        if (mEditText.length() == 0) {
            return;
        }
        String message = mEditText.getText().toString();
        String msgId = MessageIdUtils.generateUUID();
        sendMessage(message, msgId, EnumConstant.MessageType.TEXT.name(), "","");
        mEditText.setText("");
        ADCInfoUtils.calculateTextSize(message,true,mViewModel.getUserId(),mViewModel.getChannelId(),"Group chat",-1);
    }

    private void sendMessage(String message, String msgId, String msgType, String mimeType, String mediaPath)  {
        Log.d(TAG, "jioPtt::called sendMessage():message:" + message + ",msgId:" + msgId + ",msgType:" + msgType + ",mimeType:" + mimeType + ",mediaPath:" + mediaPath );
        if (!mViewModel.getJioTalkieService().isBoundToPttServer()) {
            Log.d(TAG, "getService()==null in sendMessage");
            return;
        }
        IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
        Log.d(TAG, "sendMessage SessionId = "+session.fetchSessionId());

        String formattedMessage = markupOutgoingMessage(message);

        IMediaMessage responseMessage = null;
        Mumble.TextMessage.MsgType messageType = Mumble.TextMessage.MsgType.TextMessageType;
        if(msgType.equals(EnumConstant.MessageType.IMAGE.name())){
            messageType = Mumble.TextMessage.MsgType.ImageMessageType;
        } else if(msgType.equals(EnumConstant.MessageType.AUDIO.name())){
            messageType = Mumble.TextMessage.MsgType.VoiceMessageType;
        } else if(msgType.equals(EnumConstant.MessageType.VIDEO.name())){
            messageType = Mumble.TextMessage.MsgType.VideoMessageType;
        } else if(msgType.equals(EnumConstant.MessageType.DOCUMENT.name())){
            messageType = Mumble.TextMessage.MsgType.DocMessageType;
        }
        responseMessage = session.sendTextMsgToPttChannel(session.fetchSessionPttChannel().getChannelID(),
                formattedMessage, false,messageType,msgId,mimeType,false);

        // Update upload status to database
        if (msgType.equals(EnumConstant.MessageType.VIDEO.name()) || msgType.equals(EnumConstant.MessageType.DOCUMENT.name()) || msgType.equals(EnumConstant.MessageType.AUDIO.name()) || msgType.equals(EnumConstant.MessageType.IMAGE.name())) {
            if (msgType.equals(EnumConstant.MessageType.IMAGE.name())) {
                mViewModel.updateJioTalkieChats(getResources().getString(R.string.uploading_done), responseMessage.getMessageId(),  ServerConstant.getDownloadAWSServer() + responseMessage.getMessageId());
            } else {
                mViewModel.updateJioTalkieChats(getResources().getString(R.string.uploading_done), responseMessage.getMessageId(), responseMessage.getMessageContent());
            }
            mChatByMsgId = mViewModel.observeChatByMsgId(responseMessage.getMessageId());
            mChatByMsgId.observe(getActivity(), observeChatByMsgId);
            Toast.makeText(mActivity,  getString(R.string.file_upload_success), Toast.LENGTH_SHORT).show();
            return;
        }

        // Add message to database
        mViewModel.storeMessageDataInDB(responseMessage, true, true, msgType,mediaPath,EnumConstant.MsgStatus.Undelivered.ordinal());
    }

    private String markupOutgoingMessage(String message) {
        String formattedBody = message;
        Matcher matcher = LINK_PATTERN.matcher(formattedBody);
        formattedBody = matcher.replaceAll("<a href=\"$1\">$1</a>")
                .replaceAll("\n", "<br>");
        return formattedBody;
    }

    public void clear() {
        if (mChatAdapter != null) {
            mChatAdapter.clear();
        }
    }

    private void addChatMessage(JioTalkieChats message, boolean scroll) {
        if (mChatAdapter == null) {
            Log.v(TAG, "mchatadapter is null");
            return;
        }

        mChatAdapter.add(message);
        JioTalkieService mService = (JioTalkieService) mViewModel.getJioTalkieService();

        if (mService.isBoundToPttServer() && !message.getIs_self_chat() &&  message.getMsg_id() != null) {
            mService.getJioPttSession().updateMsgStatus(message.getMsg_id(), Mumble.MessageDelivery.MsgStatus.Read);
        }

        if (scroll) {
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    if (mChatAdapter != null && mChatAdapter.getItemCount() > 0) {
                        mRecyclerView.smoothScrollToPosition(mChatAdapter.getItemCount() - 1);
                    }
                }
            });
        }
    }

    private void initRecyclerView(){
        mChatAdapter = new ChatAdapter(getActivity(), mViewModel.getJioTalkieService(),this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public void onLayoutCompleted(final RecyclerView.State state) {
                super.onLayoutCompleted(state);
                if(state.getItemCount() == loadedMessageCount && isMessageLoaded) {
                    long delay = (loadedMessageCount/300)* 2000L;
                    isMessageLoaded=false;
                    new Handler(getActivity().getMainLooper()).postDelayed(() -> mLoaderLayout.setVisibility(View.GONE), delay);
                }
            }
        });
        mRecyclerView.setAdapter(mChatAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                try {
                    layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && !mChatMessageList.isEmpty() && mChatMessageList.size() > 10 && !isLoadingDataIntoUI) {
                        int firstVisible = layoutManager.findFirstVisibleItemPosition();
                        long firstVisibleItemTime = mChatMessageList.get(firstVisible).getReceived_time();
                        String firstVisibleItemDate = DateUtils.getStringDateFromLong(firstVisibleItemTime,DateUtils.dateFormatDateDivider);
                        floatingDate.setVisibility(View.VISIBLE);
                        floatingDate.setText(DateUtils.CompareDate(firstVisibleItemDate));
                    }
                    setScrollView(layoutManager,mScrollBtn,-1);
                } catch(Exception e) {
                    Log.d(TAG,"Recycler View Scroll Listener exception "+Log.getStackTraceString(e));
                }

                if (!isLast && isUserScrolling && !isLoadingDataIntoUI && layoutManager != null && layoutManager.findFirstVisibleItemPosition() == 0) {
                    mPaginatedGroupChat.observe(getActivity(), observerPaginatedChat);
                    isLoadingDataIntoUI = true;
                    floatingDate.setText(getResources().getString(R.string.load_chat_data));
                    MessageRequestModel messageRequestModel = new MessageRequestModel(null, null, false, mViewModel.getChannelId(), null, null, true, 30, mChatAdapter.getItemCount());
                    mViewModel.downloadMessagesWithPagination(messageRequestModel);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                isUserScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }
        });
    }

    private void updateChatWithFilterMessageList(List<JioTalkieChats> messageList) {
        mChatMessageList = messageList;
        loadedMessageCount = messageList.size();
        isMessageLoaded = true;
        mChatAdapter.clear();
        for (JioTalkieChats chat : messageList) {
            addChatMessage(chat, false);
        }

        mRecyclerView.post(() -> mRecyclerView.scrollToPosition(0));
        String firstVisibleItemDate = DateUtils.getStringDateFromLong(firstDateTime, DateUtils.dateFormatDateDivider);
        floatingDate.setVisibility(View.VISIBLE);
        floatingDate.setText(DateUtils.CompareDate(firstVisibleItemDate));
    }

    private void updateChatWithPaginatedMessageList(List<JioTalkieChats> messageList, int newItemCount) {
        mChatMessageList = messageList;
        loadedMessageCount = messageList.size();
        isMessageLoaded = true;
        mChatAdapter.clear();
        for (JioTalkieChats chat : messageList) {
            addChatMessage(chat, false);
        }

        mRecyclerView.post(() -> mRecyclerView.scrollToPosition(newItemCount));
        isLoadingDataIntoUI = false;
    }


    private boolean isSelectedDateAvailable(List<JioTalkieChats> messageList) {
        boolean isSelectedDateChatAvailable = false;
        firstMessageDate = null;
        firstDateTime = System.currentTimeMillis();
        for (JioTalkieChats chat : messageList) {
            Date chatDate = new Date(chat.getReceived_time());
            if (firstMessageDate == null) {
                firstMessageDate = chatDate;
                firstDateTime = chat.getReceived_time();
            }
            if (CommonUtils.isSameDay(selectedDate, chatDate)) {
                isSelectedDateChatAvailable = true;
            }
        }
        return isSelectedDateChatAvailable;
    }



    @Override
    public void onNavigateButtonClick(double latitude, double longitude, View view) {
        loadMapNavigationFragment(latitude, longitude, view);
    }

    private void loadMapNavigationFragment(double latitude, double longitude, View view) {
        Bundle args = new Bundle();
        args.putDouble("latitude", latitude);
        args.putDouble("longitude", longitude);
        Fragment fragment = new UserLocationFragment();
        fragment.setArguments(args);
        Class<? extends Fragment> fragmentName = UserLocationFragment.class;
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, fragmentName.getName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(fragmentName.getName())
                .commitAllowingStateLoss();
    }

    @Override
    public void downloadMediaFile(int position, JioTalkieChats currMessage, boolean isLeft, boolean isSos, String mimeType, String messageType) {
        Log.d("MSISDN downloadMediaFile", "downloadMediaFile");

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
        mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.MEDIAPLAYER_FRAGMENT.ordinal(), args);
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
            scrollBtn.setVisibility(View.GONE);
        else
            scrollBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemSelected(String msgId, int selectedPosition, boolean isSelfChat, boolean isTextMessage, String textMessage) {
        mMsgId = msgId;
        mSelectedPosition = selectedPosition;
        isItemSelected = true;
        mIsSelfChat = isSelfChat;
        mIsTextMessage = isTextMessage;
        mTextMessage = textMessage;
        updateToolBar(mIsSelfChat);
    }

    @Override
    public void onItemDeselected() {
        isItemSelected = false; // Mark item as deselected
        mToolbar.findViewById(R.id.actionCalender).setVisibility(View.VISIBLE);
        mToolbar.findViewById(R.id.actionProfile).setVisibility(View.VISIBLE);
        mToolbar.findViewById(R.id.actionInfo).setVisibility(GONE);
        mToolbar.findViewById(R.id.actionDelete).setVisibility(GONE);
        mToolbar.findViewById(R.id.actionCopy).setVisibility(GONE);
        mChatAdapter.onItemDeselected();
    }

    public boolean isItemSelected() {
        return isItemSelected;
    }

    private void updateToolBar(boolean isSelfChat) {
        if (isSelfChat) {
            mToolbar.findViewById(R.id.actionCalender).setVisibility(GONE);
            mToolbar.findViewById(R.id.actionProfile).setVisibility(GONE);
            mToolbar.findViewById(R.id.actionInfo).setVisibility(View.VISIBLE);
            mToolbar.findViewById(R.id.actionDelete).setVisibility(View.VISIBLE);
        } else {
            mToolbar.findViewById(R.id.actionCalender).setVisibility(GONE);
            mToolbar.findViewById(R.id.actionProfile).setVisibility(GONE);
            mToolbar.findViewById(R.id.actionInfo).setVisibility(GONE);
            mToolbar.findViewById(R.id.actionDelete).setVisibility(View.VISIBLE);
        }
        if (mIsTextMessage) {
            mToolbar.findViewById(R.id.actionCopy).setVisibility(View.VISIBLE);
        } else {
            mToolbar.findViewById(R.id.actionCopy).setVisibility(GONE);
        }
    }

    @Override
    public void croppedImageUri(Uri uri) {
        onMediaFilePicked(uri, true);
    }
}
