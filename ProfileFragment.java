package com.jio.jiotalkie.fragment;

import static android.view.View.GONE;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.protobuf.ByteString;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.ProfileAdapter;
import com.jio.jiotalkie.adapter.provider.ProfileAdapterProvider;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.util.BitmapUtils;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@TrackPerformance(threshold = 300)
public class ProfileFragment extends Fragment {
    private static final String TAG = ProfileFragment.class.getSimpleName();

    private RecyclerView mProfileOptionsView;

    private ProfileAdapter mProfileAdapter;

    private DashboardActivity mActivity;
    private TextView mDispatcherName;

    private DashboardViewModel mViewModel;

    private ImageView editProfilePhoto;

    private ImageView profileImageView;

    private static final int MAX_SIZE = 128; // Maximum size in pixels
    private byte[] textureData;
    private Bitmap resizedBitmap, profilepic;
    private AlertDialog mLogoutDialog;
    private ImageChooserDialogFragment imageChooserDialogFragment;

    private Switch mPowerSaverToggle;
    private Switch mImageCompressToggle;


    public ProfileFragment() {
        // Required empty public constructor
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
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mPowerSaverToggle = view.findViewById(R.id.power_saver_toggle);
        mImageCompressToggle = view.findViewById(R.id.image_toggle);
        mProfileOptionsView = view.findViewById(R.id.profile_options);
        mDispatcherName = view.findViewById(R.id.dispatcher_name);
        editProfilePhoto = view.findViewById(R.id.edit_profile_photo);
        profileImageView = view.findViewById(R.id.profile_photo);
        editProfilePhoto.setOnClickListener(view1 -> openProfilePhotoChooser());
        if (mViewModel.isJioTalkieServiceActive() && mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser() != null) {
            mDispatcherName.setText(mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserName());
            textureData = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserTexture();
        }
        if (textureData != null) {
            profilepic = BitmapFactory.decodeByteArray(textureData, 0, textureData.length);
        }
        // Set default value from shared preference
        mPowerSaverToggle.setChecked(mViewModel.getSettings().isPowerSaverEnable());
        mImageCompressToggle.setChecked(mViewModel.getSettings().isImageCompressEnable());
        mPowerSaverToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mViewModel.getSettings().setPowerSaverEnable(isChecked);
        });
        mImageCompressToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!isChecked){
                mImageCompressToggle.setChecked(true);
                mViewModel.getSettings().setImageCompressEnable(true);
                Toast.makeText(mActivity, getString(R.string.image_compress_disable_note), Toast.LENGTH_SHORT).show();
            }else{
                mViewModel.getSettings().setImageCompressEnable(isChecked);
            }
        });
        initRecyclerView();
        return view;
    }

    private void setPic(Object path, boolean isCameraImage) {
        String cameraImagePath = "";
        ByteString textureData = null;
        Uri galleryImageUri = null;
        Bitmap bitmap;
        if (isCameraImage) {
            cameraImagePath = (String) path;
        } else {
            galleryImageUri = (Uri) path;
        }
        int targetW = profileImageView.getWidth();
        int targetH = profileImageView.getHeight();
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
                textureData = CommonUtils.getResizedImageByteString(cameraImagePath);
            } else {
                bitmap = CommonUtils.decodeBitmapFromUri(mActivity,galleryImageUri, bmOptions);
                textureData = CommonUtils.getResizedImageByteString(getActivity().getApplicationContext(), galleryImageUri);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        profilepic = bitmap;
        if (mViewModel.isJioTalkieServiceActive()) {
            mViewModel.getJioTalkieService().getJioPttSession().applyAvatarForPttUser(mViewModel.getJioTalkieService().getJioPttSession().fetchSessionId(), textureData);
        }
        setProfilePic(profilepic);
    }

    private void openProfilePhotoChooser() {
        imageChooserDialogFragment = new ImageChooserDialogFragment(mActivity, (path, isCameraImage) -> setPic(path, isCameraImage));
        imageChooserDialogFragment.show(mActivity.getSupportFragmentManager(), imageChooserDialogFragment.getTag());
    }

    private void setProfilePic(Bitmap image) {
        if (image != null) {
            int targetW = profileImageView.getWidth();
            int targetH = profileImageView.getHeight();
            resizedBitmap = BitmapUtils.resizeKeepingAspect(image, targetW, targetH);
            Glide.with(this)
                    .load(resizedBitmap)
                    .transform(new CircleCrop())
                    .into(profileImageView);
            mActivity.setTempProfilePic(resizedBitmap);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
        mActivity.showToolWithBack(mActivity.getResources().getString(R.string.profile));
        setProfilePic(profilepic);
        registerReceiveSOSandPTT();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiveSOSandPTT();
    }

    private void unregisterReceiveSOSandPTT() {
        mViewModel.observeSelfUserTalkState().removeObservers(this);
        mViewModel.observeSOSStateLiveData().removeObservers(this);
    }

    public void dismissPopUp() {
        if (mLogoutDialog != null && mLogoutDialog.isShowing()) {
            mLogoutDialog.dismiss();
        }
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


    public ProfileAdapterProvider adapterProvider = new ProfileAdapterProvider() {
        @Override
        public void onItemClick(int id) {
            if (id == EnumConstant.LOGOUT) {
                showLogoutDialog();
            } else {
                mActivity.loadInnerFragment(id, null);
            }
        }
    };

    private void initRecyclerView() {
        List<String> profileList = Arrays.asList(mActivity.getResources().getStringArray(R.array.dispatcher_profile_items));
        Log.d(TAG, "List of items " + profileList);
        mProfileAdapter = new ProfileAdapter(profileList, adapterProvider);
        mProfileOptionsView.setAdapter(mProfileAdapter);
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getResources().getString(R.string.logout))
                .setMessage(mActivity.getResources().getString(R.string.logout_confirm))
                .setCancelable(false)
                .setPositiveButton(mActivity.getResources().getString(R.string.action_confirm), (dialog, id) -> {
                    Toast.makeText(mActivity, R.string.logout_successful, Toast.LENGTH_LONG).show();
                    mActivity.setLogoutStatus(true);
                    mActivity.logoutDispatcherUser();
                    dialog.dismiss();
                })
                .setNegativeButton(mActivity.getResources().getString(R.string.cancel), (dialog, i) -> dialog.dismiss());
        mLogoutDialog = builder.create();
        mLogoutDialog.show();
    }


}

