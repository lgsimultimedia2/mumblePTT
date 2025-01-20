package com.jio.jiotalkie.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import com.jio.jiotalkie.dataclass.SubChannelCreationData;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.util.BitmapUtils;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;


public class SubChannelFragment extends Fragment {

    private static String TAG = SubChannelFragment.class.getName();
    private DashboardActivity mActivity;
    private DashboardViewModel mViewModel;
    private EditText mEditChannel;
    private EditText mEditDescription;
    private Button mNextBtn;


    private RelativeLayout rootLayout;
    private LiveData<SubChannelCreationData> subChannelIDLiveData;
    private ImageChooserDialogFragment imageChooserDialogFragment;

    private ImageView channelImageView;

    private Bitmap resizedBitmap;

    private ImageView editChannelPhoto;

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            enableNextButton(!charSequence.toString().trim().isEmpty());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void enableNextButton(boolean enable) {
        mNextBtn.setEnabled(enable);
        if (enable) {
            mNextBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
            mNextBtn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.btn_bg_enable_color));
        } else {
            mNextBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.card_outline_color));
            mNextBtn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.btn_bg_disble_color));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        mViewModel = new ViewModelProvider(mActivity).get(DashboardViewModel.class);
        registerSubChannelCreationObserver();
    }

    private void createChannel() {
        // API tested working fine for create channel
        String chanelName = mEditChannel.getText().toString().trim();
        String description = mEditDescription.getText().toString().trim();
        int channelId = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserChannel().getChannelID();
        mViewModel.getJioTalkieService().getJioPttSession().createPttChannel(channelId, chanelName, description, 0, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sub_channel, container, false);
        rootLayout = view.findViewById(R.id.sub_channel_root);
        channelImageView = view.findViewById(R.id.channel_photo);
        editChannelPhoto = view.findViewById(R.id.edit_channel_photo);
        editChannelPhoto.setOnClickListener(view12 -> openProfilePhotoChooser());
        mEditChannel = view.findViewById(R.id.channel_edit);
        mEditChannel.addTextChangedListener(mTextWatcher);
        mEditDescription = view.findViewById(R.id.description_edit);
        mNextBtn = view.findViewById(R.id.btnNext);
        enableNextButton(false);
        mNextBtn.setOnClickListener(view1 -> {
            //Toast.makeText(getContext(), "Create ChannelModel API yet to call", Toast.LENGTH_SHORT).show();
            // Once Resume this feature will un-commented
            createChannel();

        });
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootLayout.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootLayout.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) { // If keyboard is visible
                mNextBtn.setVisibility(View.GONE); // Hide the button or move it
            } else {
                mNextBtn.setVisibility(View.VISIBLE); // Show the button when keyboard is hidden
            }
        });

        //            mViewModel.getJioTalkieService().getJioModelHandler().setChannelStateProvider(mChannelStateProvider);
        return view;
    }

    private void openProfilePhotoChooser() {
        imageChooserDialogFragment = new ImageChooserDialogFragment(mActivity, (path, isCameraImage) -> setPic(path, isCameraImage));
        imageChooserDialogFragment.show(mActivity.getSupportFragmentManager(), imageChooserDialogFragment.getTag());
    }

    public void dismissPopUp() {
        if (imageChooserDialogFragment != null && imageChooserDialogFragment.getDialog() != null && imageChooserDialogFragment.getDialog().isShowing()) {
            imageChooserDialogFragment.getDialog().dismiss();
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

    @Override
    public void onResume() {
        super.onResume();
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
        mActivity.showToolWithBack(mActivity.getResources().getString(R.string.create_sub_channel));
        registerReceiveSOSandPTT();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiveSOSandPTT();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterSubChannelCreationObserver();
    }

    private void registerSubChannelCreationObserver(){
        subChannelIDLiveData = mViewModel.observeSubChannelCreation();
        subChannelIDLiveData.observe(this , observerSubChanelCreation);
    }

    private void unRegisterSubChannelCreationObserver(){
        subChannelIDLiveData.removeObserver( observerSubChanelCreation);
    }

    private final Observer<SubChannelCreationData> observerSubChanelCreation = new Observer<SubChannelCreationData>() {
        @Override
        public void onChanged(SubChannelCreationData subChannelData) {
            if(subChannelData == null)
                return;
            if(subChannelData.isSuccess()){
                Toast.makeText(mActivity,  subChannelData.getSubChannel().getChannelName() + " sub channel created", Toast.LENGTH_SHORT).show();
                if(mActivity!=null){
                    mActivity.handleOnBackPress();
                }
            }
            mViewModel.resetObserveSubChannelCreation();
        }
    };

    private void setPic(Object path, boolean isCameraImage) {
        String cameraImagePath = "";
        Uri galleryImageUri = null;
        Bitmap bitmap;
        if (isCameraImage) {
            cameraImagePath = (String) path;
        } else {
            galleryImageUri = (Uri) path;
        }
        int targetW = channelImageView.getWidth();
        int targetH = channelImageView.getHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        try {
            if (isCameraImage) {
                bitmap = BitmapFactory.decodeFile(cameraImagePath, bmOptions);
                bitmap = CommonUtils.rotateBitmapIfNeeded(cameraImagePath, bitmap);
            } else {
                bitmap = CommonUtils.decodeBitmapFromUri(mActivity,galleryImageUri, bmOptions);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setChannelPic(bitmap);
    }

    private void setChannelPic(Bitmap image) {
        if (image != null) {
            int targetW = channelImageView.getWidth();
            int targetH = channelImageView.getHeight();
            resizedBitmap = BitmapUtils.resizeKeepingAspect(image, targetW, targetH);
            Glide.with(this)
                    .load(resizedBitmap)
                    .transform(new CircleCrop())
                    .into(channelImageView);
        }
    }

    private void unregisterReceiveSOSandPTT() {
        mViewModel.observeSelfUserTalkState().removeObservers(this);
        mViewModel.observeSOSStateLiveData().removeObservers(this);
    }
}