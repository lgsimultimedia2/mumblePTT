package com.jio.jiotalkie.fragment;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.provider.ImageDataProvider;
import com.jio.jiotalkie.dispatch.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ImageChooserDialogFragment extends BottomSheetDialogFragment {

    View view;

    private String currentPhotoPath;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_PERMISSIONS = 3;

    private static final int REQUEST_IMAGE_SELECT = 4;

    DashboardActivity mActivity;

    ImageDataProvider mImageDataProvider;

    public ImageChooserDialogFragment(DashboardActivity activity, ImageDataProvider dataProvider){
        mActivity = activity;
        mImageDataProvider= dataProvider;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.JioBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.image_chooser_layout, container, false);
        Objects.requireNonNull(getDialog()).setCanceledOnTouchOutside(false);
        initView(view);
        return view;
    }

    private void initView(View view){
        LinearLayout cameraButton = view.findViewById(R.id.camera_chooser);
        LinearLayout galleryButton = view.findViewById(R.id.gallery_chooser);
        LinearLayout filesButton = view.findViewById(R.id.fileChooser);
        Button cancelButton = view.findViewById(R.id.dialogCancel);
        cameraButton.setOnClickListener(view1 -> {
            if (checkPermissions()) {
                dispatchTakePictureIntent();
            } else {
                requestAppPermissions();
            }
        });
        galleryButton.setOnClickListener(v -> dispatchSelectPictureIntent());
        filesButton.setOnClickListener(view13 -> dispatchSelectPictureIntentFromFiles());
        cancelButton.setOnClickListener(view12 -> Objects.requireNonNull(getDialog()).dismiss());
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAppPermissions() {
        requestPermissions(new String[]{Manifest.permission.CAMERA},
                REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(mActivity,
                        "com.jio.jiotalkie.dispatch.provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchSelectPictureIntent() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (pickPhoto.resolveActivity(mActivity.getPackageManager()) != null) {
            startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
        } else {
            // Show a toast message to the user
            Toast.makeText(mActivity, getString(R.string.no_app_for_pick_photo), Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchSelectPictureIntentFromFiles(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE_PICK);
        } else {
            // Show a toast message to the user
            Toast.makeText(mActivity, getString(R.string.no_app_for_pick_photo), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            mImageDataProvider.setProfilePic(currentPhotoPath,true);
        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if(selectedImage!=null) {
                mImageDataProvider.setProfilePic(selectedImage,false);
            }
        }
        getDialog().dismiss();
    }
}
