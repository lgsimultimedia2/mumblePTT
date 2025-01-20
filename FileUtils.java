package com.jio.jiotalkie.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.text.SimpleDateFormat;

public class FileUtils {

    private static final String TAG = FileUtils.class.getName();
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 720;
    private static final int MAX_COMPRESSION_QUALITY = 100;

    private FileUtils() {

    }

    public static File createImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    public static String copyFileToInternal(Context context, Uri fileUri, String mineType) {
        return copyFileToInternal(context, fileUri, mineType, false);
    }

    public static String copyFileToInternal(Context context, Uri fileUri, String mineType, boolean needCompress) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Cursor cursor = context.getContentResolver().query(fileUri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
            } else {
                Log.d(TAG, "copyFileToInternal cursor is null");
                return null;
            }

            @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            @SuppressLint("Range") long size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
            cursor.close();
            File file;
            if (CommonUtils.isDocumentMimeType(mineType)) {
                file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + displayName);
            } else {
                file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + displayName);
            }
            FileOutputStream fileOutputStream = null;
            try {
                if (needCompress && CommonUtils.isImageMimeType(mineType)) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), fileUri);
                    Bitmap bitmap1;
                    bitmap1 = resize(bitmap);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap1.compress(Bitmap.CompressFormat.JPEG, MAX_COMPRESSION_QUALITY, bytes);
                    byte[] bitmapData = bytes.toByteArray();
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(bitmapData);
                    fos.flush();
                    fos.close();
                    return Uri.fromFile(file).getPath();

                }
                fileOutputStream = new FileOutputStream(file);
                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                byte buffers[] = new byte[1024];
                int read;
                while ((read = inputStream.read(buffers)) != -1) {
                    fileOutputStream.write(buffers, 0, read);
                }
                inputStream.close();
                fileOutputStream.close();
                return file.getPath();
            } catch (IOException e) {
                Log.d(TAG, "copyFileToInternal Exception : " + e.getMessage());
            } finally {
                if (fileOutputStream != null) {
                    safeClose(fileOutputStream);
                }
            }
        }
        return null;
    }

    private static void safeClose(FileOutputStream fileOutputStream) {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                Log.d(TAG, "safeClose Exception : " + e.getMessage());
            }
        }
    }


    private static Bitmap resize(Bitmap image) {
        if (MAX_HEIGHT > 0 && MAX_WIDTH > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) MAX_WIDTH / (float) MAX_HEIGHT;
            int finalWidth = MAX_WIDTH;
            int finalHeight = MAX_HEIGHT;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) (MAX_HEIGHT * ratioBitmap);
            } else {
                finalHeight = (int) (MAX_WIDTH / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }
}
