package com.jio.jiotalkie.fragment;

import static android.view.View.GONE;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.provider.ImageEditorResultListener;
import com.jio.jiotalkie.dispatch.R;

import com.jio.jiotalkie.util.CropOverlayView;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageEditorFragment extends Fragment {

    private static final String TAG = ImageEditorFragment.class.getName();

    private ImageView imageView;
    private CropOverlayView cropOverlayView;
    private Bitmap originalBitmap;

    private Uri imageUri;

    DashboardActivity mActivity;

    private ImageEditorResultListener mImageEditorListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        mActivity.showToolWithBack(getActivity().getString(R.string.image_edit));
        Toolbar mToolbar = mActivity.findViewById(R.id.toolbar_main);
        mToolbar.findViewById(R.id.actionCalender).setVisibility(GONE);
        mToolbar.findViewById(R.id.actionProfile).setVisibility(GONE);
        mActivity.needBottomNavigation(false);
        if (getArguments() != null) {
            imageUri = getArguments().getParcelable("image_uri");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_editor, container, false);

        imageView = view.findViewById(R.id.imageView);
        cropOverlayView = view.findViewById(R.id.cropOverlayView);
        Button btnRotate = view.findViewById(R.id.btnRotate);
        Button btnCrop = view.findViewById(R.id.btnCrop);

        // Load Image
        try {
            ContentResolver resolver = getContext().getContentResolver();
            InputStream inputStream = resolver.openInputStream(imageUri);
            if (inputStream != null) {
                originalBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        imageView.setImageBitmap(originalBitmap);
        cropOverlayView.setImageBitmap(originalBitmap);

        // Rotate Button
        btnRotate.setOnClickListener(v -> {
            originalBitmap = rotateBitmap(originalBitmap, 90);
            imageView.setImageBitmap(originalBitmap);
            cropOverlayView.setImageBitmap(originalBitmap);
        });

        // Crop Button
        btnCrop.setOnClickListener(v -> {
            RectF cropRect = cropOverlayView.getCropRect();
            // Ensure crop rect is within image bounds
            cropRect.left = Math.max(0, cropRect.left);
            cropRect.top = Math.max(0, cropRect.top);
            cropRect.right = Math.min(originalBitmap.getWidth(), cropRect.right);
            cropRect.bottom = Math.min(originalBitmap.getHeight(), cropRect.bottom);
            // Ensure valid dimensions
            if (cropRect.width() <= 0 || cropRect.height() <= 0) return;

            Bitmap croppedBitmap = cropImage(originalBitmap, cropRect);
            if (croppedBitmap != null) {
//                originalBitmap=croppedBitmap;
//                imageView.setImageBitmap(croppedBitmap);
//                cropOverlayView.setImageBitmap(croppedBitmap);
                File croppedDestinationFile = new File(mActivity.getCacheDir(), System.currentTimeMillis()+"cropped_image.jpg");
                Uri croppedContentUri= FileProvider.getUriForFile(requireContext(), "com.jio.jiotalkie.dispatch.provider", croppedDestinationFile);
                Uri uri = saveBitmapAsJpegOverwrite(croppedBitmap,croppedContentUri);
                mImageEditorListener.croppedImageUri(uri);
                mActivity.handleOnBackPress();
            }
        });

        return view;
    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Bitmap cropImage(Bitmap bitmap, RectF cropRect) {
        try {
            return Bitmap.createBitmap(
                    bitmap,
                    Math.round(cropRect.left),
                    Math.round(cropRect.top),
                    Math.round(cropRect.width()),
                    Math.round(cropRect.height())
            );
        } catch (Exception e) {
            Log.e(TAG,"Exception while cropping "+ Log.getStackTraceString(e));
            return null;
        }
    }

    private Uri saveBitmapAsJpegOverwrite(Bitmap bitmap, Uri uri) {
        OutputStream outputStream = null;
        try {
            ContentResolver contentResolver = requireContext().getContentResolver();
            outputStream = contentResolver.openOutputStream(uri, "w");
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            }
            return uri;
        } catch (Exception e) {
            Log.d(TAG,"Exception in saving edited image "+Log.getStackTraceString(e));
            return null;
        } finally {
            // Close the OutputStream to release resources
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    Log.d(TAG,"Finally Exception in saving edited image "+Log.getStackTraceString(e));
                }
            }
        }
    }

    public void setListener(ImageEditorResultListener listener){
        mImageEditorListener = listener;
    }
}
