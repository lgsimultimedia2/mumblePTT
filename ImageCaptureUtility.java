package com.jio.jiotalkie.media;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.util.Log;
import android.widget.Toast;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.provider.MediaDataProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

public class ImageCaptureUtility {

    private static String TAG = ImageCaptureUtility.class.getName();
    private String cameraId;
    private CameraDevice cameraDevice;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private ImageReader imageReader;

    private Context mContext;

    private CameraCaptureSession cameraCaptureSession;

    private DashboardActivity mActivity;

    private static ImageCaptureUtility mImageCaptureUtility = null;

    private MediaDataProvider mMediaDataProvider;

    private File mImageFile;

    private ImageCaptureUtility() {

    }

    public static ImageCaptureUtility getInstance() {
        if (mImageCaptureUtility == null) {
            mImageCaptureUtility = new ImageCaptureUtility();
        }
        return mImageCaptureUtility;
    }

    public void init(Activity activity, MediaDataProvider mediaDataProvider) {
        mContext = activity;
        mActivity = (DashboardActivity) activity;
        mMediaDataProvider = mediaDataProvider;
        startBackgroundThread();
        openCamera();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    this.cameraId = cameraId;
                    break;
                }
            }
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Handle permission here.
            }else {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.d(TAG,"openCamera Camera Access Exception "+ Log.getStackTraceString(e));
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    public void createCameraPreviewSession() {
        try {
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                saveImageToFile(image);
                image.close();
                Uri imageUri = FileProvider.getUriForFile(mContext, "com.jio.jiotalkie.dispatch.provider", mImageFile);
                mMediaDataProvider.getMediaUri(imageUri);
                closeCameraUtility();
            }, backgroundHandler);

            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            cameraDevice.createCaptureSession(Collections.singletonList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        cameraCaptureSession = session;
                        session.capture(captureBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.d(TAG,"createCameraPreviewSession createCaptureSession Exception "+ Log.getStackTraceString(e));
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG,"on Configuration failed ");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.d(TAG,"createCameraPreviewSession Camera Access Exception "+ Log.getStackTraceString(e));
        }
    }

    private void saveImageToFile(Image image) {
        File imageCaptureDirectory = new File("/sdcard/Download");
        if(!imageCaptureDirectory.exists()){
            imageCaptureDirectory.mkdirs();
        }
        mImageFile = new File(imageCaptureDirectory, System.currentTimeMillis()+"_captured_image.jpg");
        if(!mImageFile.exists()){
            try {
                mImageFile.createNewFile();
            }catch (Exception e){
                Log.d(TAG,"Start Recording create file exception "+Log.getStackTraceString(e));
            }
        }
        FileOutputStream outputStream = null;
        try {

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            outputStream = new FileOutputStream(mImageFile);
            outputStream.write(bytes);
            Toast.makeText(mContext,"Image saved at path: "+mImageFile.getAbsolutePath(),Toast.LENGTH_LONG).show();
            Log.d(TAG, "Image saved successfully: " + mImageFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save the image: " + e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream: " + e.getMessage());
                }
            }
        }
    }


    public void closeCameraUtility() {
        // Close the CameraCaptureSession
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        // Close the CameraDevice
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        // Close the ImageReader
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        stopBackgroundThread();
    }

    public void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG,"stop background thread "+Log.getStackTraceString(e));
            }
        }
    }
}

