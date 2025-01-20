package com.jio.jiotalkie.util;

import android.app.Activity;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.camera2.CameraManager;
import android.icu.text.SimpleDateFormat;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.Vibrator;
import android.util.Log;

import com.application.customservice.dataManagment.imodels.IUserModel;
import androidx.core.content.FileProvider;

import com.google.protobuf.ByteString;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.dispatch.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class CommonUtils {

    private static final String TAG = CommonUtils.class.getSimpleName();

    private static final int BLINK_DURATION = 500; // Duration of each blink in milliseconds
    private static final int TOTAL_BLINKS = 10; // Number of times to blink

    private static final int MAX_SIZE = 128;
    private CommonUtils() {
    }

    // Returns true if user is online else false
    public static boolean checkOnlineStatus(List<? extends IUserModel> userList, String username) {
        int index = getSearchItemIndex(userList, username);
        return index != -1;
    }

    // Searches the username in the userlist. Returns -1 if username not found, else returns the
    // index of username
    public static int getSearchItemIndex(List<? extends IUserModel> userList, String username) {
        if (userList == null || userList.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getUserName().equals(username)) {
                return i;
            }
        }
        return -1; // Not found
    }

    public static int getSearchItemIndexUser(List<RegisteredUser> userList, String username) {
        if (userList == null || userList.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getName().equals(username)) {
                return i;
            }
        }
        return -1; // Not found
    }

    public static void flashLightAndVibrate(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable blinkRunnable = new Runnable() {
            private int blinkCount = 0;
            private boolean isLightOn = false;

            @Override
            public void run() {
                try {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    if (isLightOn) {
                        cameraManager.setTorchMode(cameraId, false); // Turn off the flashlight
                        if (vibrator != null) {
                            vibrator.cancel(); // Stop vibration
                        }
                    } else {
                        cameraManager.setTorchMode(cameraId, true); // Turn on the flashlight
                        if (vibrator != null) {
                            vibrator.vibrate(BLINK_DURATION); // Vibrate for BLINK_DURATION
                        }
                    }
                    isLightOn = !isLightOn;
                    blinkCount++;
                    if (blinkCount < TOTAL_BLINKS * 2) {
                        handler.postDelayed(this, BLINK_DURATION);
                    } else {
                        cameraManager.setTorchMode(cameraId, false); // Ensure flashlight is off
                        if (vibrator != null) {
                            vibrator.cancel(); // Ensure vibration is off
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        handler.post(blinkRunnable);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));

    }

    public static int getNetworkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return -1;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        if(actNw != null) {
            if(actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                return NetworkCapabilities.TRANSPORT_WIFI;
            }else if(actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                return NetworkCapabilities.TRANSPORT_CELLULAR;
            }else if(actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                return NetworkCapabilities.TRANSPORT_ETHERNET;
            }
        }
        return -1;
    }

    public static String getSimpleDateFormat(Date date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMMM yyyy", Locale.ENGLISH);
        String formattedDate = dateFormat.format(date);
        return formattedDate;
    }
    public static String getSimpleDateFormatForToast(Date date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        String formattedDate = dateFormat.format(date);
        return formattedDate;
    }

    public static boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();

        cal1.setTime(date1);
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    public static String getFormattedLastSeen(String date) {
        String oldDate = date.replaceAll("[TZ]", "");
        String currentDate = new java.text.SimpleDateFormat("yyyy-MM-ddHH:mm:ss")
                .format(Calendar.getInstance().getTime());
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
        Date date1 = null;
        Date date2 = null;
        try {
            date1 = format.parse(oldDate);
            date2 = format.parse(currentDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        long diff = date2.getTime() - date1.getTime();
        long months = diff / (24 * 60 * 60 * 1000 * 31);
        long weeks = diff / (24 * 60 * 60 * 1000 * 7);
        long days = diff / (24 * 60 * 60 * 1000);
        long hours = diff / (60 * 60 * 1000) % 24;
        long minutes = diff / (60 * 1000) % 60;
        long seconds = diff / 1000 % 60;
        if (months != 0) {
            if (months > 1) {
                return months + " months";
            } else {
                return months + " month";
            }
        } else if (weeks != 0) {
            if (weeks > 1) {
                return weeks + " weeks";
            } else {
                return weeks + " week";
            }
        } else if (days != 0) {
            if (days > 1) {
                return days + " days";
            } else {
                return days + " day";
            }
        } else if (hours != 0) {
            if (hours > 1) {
                return hours + " hours";
            } else {
                return hours + " hour";
            }
        } else if (minutes != 0) {
            if (minutes > 1) {
                return minutes + " mins";
            } else {
                return minutes + " min";
            }
        } else if (seconds < 60) {
            return "few sec";
        }

        return "";
    }
    public static String getFormattedDateTime(long receivedTime) {
        Calendar calendar = Calendar.getInstance(new Locale("en","IN"));
        calendar.setTimeInMillis(receivedTime);
        String dayNumberSuffix = getDayNumberSuffix(calendar.get(Calendar.DAY_OF_MONTH));
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("d'"+ dayNumberSuffix+" " +"'MMM, h:mm a",
                new Locale("en","IN"));
        return formatter.format(calendar.getTime());
    }

    private static String getDayNumberSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    public static Bitmap rotateBitmapIfNeeded(String filePath, Bitmap bitmap) {
        // Read the image's EXIF data
        int rotation = getRotationFromExif(filePath);
        if (rotation != 0) {
            // Rotate the bitmap if needed
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    public static int getRotationFromExif(String filePath) {
        // Default rotation
        int rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rotation;
    }

    public static Bitmap decodeBitmapFromUri(Activity mActivity, Uri uri, BitmapFactory.Options bmOptions) {
        ContentResolver contentResolver = mActivity.getContentResolver();
        InputStream inputStream = null;
        try {
            inputStream = contentResolver.openInputStream(uri);
            if (inputStream != null) {
                return BitmapFactory.decodeStream(inputStream, null, bmOptions);
            }
        } catch (IOException e) {
            Log.e(TAG, "decodeBitmapFromURI " + Log.getStackTraceString(e));
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "decodeBitmapFromURI " + Log.getStackTraceString(e));
                }
            }
        }
        return null;
    }

    public static ByteString getResizedImageByteString(String filePath) throws IOException {
        Bitmap bitmap = decodeBitmapFromFilePath(filePath);
        bitmap = rotateBitmapIfNeeded(filePath, bitmap);
        if (bitmap == null) throw new IOException("Failed to decode image.");
        // Resize the bitmap to ensure it's no larger than MAX_SIZE x MAX_SIZE
        Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_SIZE, MAX_SIZE);
        // Convert the bitmap to ByteString
        return bitmapToByteString(resizedBitmap);
    }

    private static Bitmap decodeBitmapFromFilePath(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return null;
        // Decode the image file
        return BitmapFactory.decodeFile(filePath);
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(ratio * width);
        int newHeight = Math.round(ratio * height);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    public static ByteString bitmapToByteString(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return ByteString.copyFrom(outputStream.toByteArray());
    }

    public static ByteString getResizedImageByteString(Context context, Uri contentUri) throws IOException {
        Bitmap bitmap = decodeBitmapFromContentUri(context, contentUri);
        if (bitmap == null) throw new IOException("Failed to decode image.");
        // Resize the bitmap to ensure it's no larger than MAX_SIZE x MAX_SIZE
        Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_SIZE, MAX_SIZE);
        // Convert the bitmap to ByteString
        return bitmapToByteString(resizedBitmap);
    }

    private static Bitmap decodeBitmapFromContentUri(Context context, Uri contentUri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        InputStream inputStream = null;
        Bitmap bitmap = null;
        try {
            inputStream = contentResolver.openInputStream(contentUri);
            if (inputStream == null) throw new IOException("Failed to open input stream.");
            // Decode the image stream
            bitmap = BitmapFactory.decodeStream(inputStream);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace(); // Handle the error as needed
                }
            }
        }
        return bitmap;
    }

    public static boolean isSleepModeActive(Context context){
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return !powerManager.isInteractive();
    }
    public static String getUserChatSize(Context context) {
        long dataSize = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            dataSize = getAppStorageSize(context);
        } else {
            dataSize = getAppStorageSizeLegacy(context);
        }
        return convertByteToFileSize(dataSize);
    }

    private static long getAppStorageSize(Context context) {
        long dataSize = 0;
        try {
            StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
            String packageName = context.getPackageName();
            final UserHandle user = android.os.Process.myUserHandle();
            // Retrieve the package info
            UUID uuid = StorageManager.UUID_DEFAULT;
            // Get storage stats for the app
            StorageStats storageStats = storageStatsManager.queryStatsForPackage(uuid, packageName, user);
            long cacheSize = storageStats.getCacheBytes();
            dataSize = storageStats.getDataBytes() - cacheSize;
            long appSize = storageStats.getAppBytes();
            Log.d(TAG,"Cache Size: " + cacheSize);
            Log.d(TAG,"User Data Size: " + dataSize);
            Log.d(TAG,"App Size: " + appSize);
        } catch (Exception e) {
            Log.e(TAG, "getAppStorageSize " + e.getMessage());
        }
        return dataSize;
    }

    private static long getAppStorageSizeLegacy(Context context) {
        File cacheDir = context.getCacheDir();
        File dataDir = new File(context.getFilesDir().getParent());
        long cacheSize = getFolderSize(cacheDir);
        long dataSize = getFolderSize(dataDir) - cacheSize;
        Log.d(TAG,"Cache Size: " + cacheSize);
        Log.d(TAG,"User Data Size: " + dataSize);
        return dataSize;
    }

    private static String convertByteToFileSize(long sizeInBytes) {
        final double KB = 1024.0;
        final double MB = KB * 1024;
        final double GB = MB * 1024;
        if (sizeInBytes >= GB) {
            return String.format("%.2f GB", sizeInBytes / GB);
        } else if (sizeInBytes >= MB) {
            return String.format("%.2f MB", sizeInBytes / MB);
        } else if (sizeInBytes >= KB) {
            return String.format("%.2f KB", sizeInBytes / KB);
        } else {
            return sizeInBytes + " Bytes";
        }
    }

    private static long getFolderSize(File folder) {
        long size = 0;
        if (folder != null && folder.isDirectory()) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getFolderSize(file);
                }
            }
        }
        return size;
    }
    /**
     * Method to check Mine type belongs to Document
     * @param mimeType
     * @return
     */
    public static boolean isDocumentMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false; // Invalid or empty MIME type
        }
        return switch (mimeType) {
            case EnumConstant.MIME_TYPE_TXT, EnumConstant.MIME_TYPE_DOC,
                 EnumConstant.MIME_TYPE_DOCX, EnumConstant.MIME_TYPE_PPT,
                 EnumConstant.MIME_TYPE_PPTX, EnumConstant.MIME_TYPE_XLS,
                 EnumConstant.MIME_TYPE_XLSX, EnumConstant.MIME_TYPE_PDF ->
                    true; // MIME type matches a valid document type
            default -> false; // MIME type doesn't match any predefined types
        };
    }

    /**
     *  Return icon map with mine type for UX layer
     * @param mimeType
     * @return
     */
    public static int getIconMineType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return R.drawable.ic_txt; // Invalid or empty MIME type
        }
        switch (mimeType) {
            case EnumConstant.MIME_TYPE_TXT:
                return R.drawable.ic_txt; // Icon for text files
            case EnumConstant.MIME_TYPE_DOC:
                return R.drawable.ic_doc;
            case EnumConstant.MIME_TYPE_DOCX:
                return R.drawable.ic_docx; // Icon for Word documents
            case EnumConstant.MIME_TYPE_PPT:
                return R.drawable.ic_ppt;
            case EnumConstant.MIME_TYPE_PPTX:
                return R.drawable.ic_pptx; // Icon for PowerPoint presentations
            case EnumConstant.MIME_TYPE_XLS:
                return R.drawable.ic_xls;
            case EnumConstant.MIME_TYPE_XLSX:
                return R.drawable.ic_xlsx; // Icon for Excel spreadsheets
            case EnumConstant.MIME_TYPE_PDF:
                return R.drawable.doc_file_pdf_icon_red; // Icon for PDF files
            default:
                return R.drawable.ic_txt; // Default icon for unknown MIME types
        }
    }

    /**
     *  Return Documents type based on Mine Type.
     * @param mimeType
     * @return
     */
    public static String getMineTypeText(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return EnumConstant.FILE_TEXT; // Invalid or empty MIME type
        }
        switch (mimeType) {
            case EnumConstant.MIME_TYPE_TXT:
                return EnumConstant.TXT_TEXT;
            case EnumConstant.MIME_TYPE_DOC:
                return EnumConstant.DOC_TEXT;
            case EnumConstant.MIME_TYPE_DOCX:
                return EnumConstant.DOCX_TEXT;
            case EnumConstant.MIME_TYPE_PPT:
                return EnumConstant.PPT_TEXT;
            case EnumConstant.MIME_TYPE_PPTX:
                return EnumConstant.PPTX_TEXT;
            case EnumConstant.MIME_TYPE_XLS:
                return EnumConstant.XLS_TEXT;
            case EnumConstant.MIME_TYPE_XLSX:
                return EnumConstant.XLSX_TEXT;
            case EnumConstant.MIME_TYPE_PDF:
                return EnumConstant.PDF_TEXT;
            default:
                return EnumConstant.FILE_TEXT; // Default Text for unknown MIME types
        }

    }
    /**
     * Method to check Mine type belongs to Image
     * @param mimeType
     * @return
     */
    public static boolean isImageMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false; // Invalid or empty MIME type
        }
        return switch (mimeType) {
            case EnumConstant.MIME_TYPE_IMAGE_JPEG, EnumConstant.MIME_TYPE_IMAGE_JPG,
                 EnumConstant.MIME_TYPE_IMAGE_PNG ->
                    true; // MIME type matches a valid Image type
            default -> false; // MIME type doesn't match any predefined types
        };
    }

    /**
     * return content uri using absolute path
     * @param context
     * @param filePath
     * @return
     */
    public static Uri getUriFromAbsolutePath(Context context,String filePath) {
        File file = new File(filePath);
        if (file == null)
            return null;
        return FileProvider.getUriForFile(context, EnumConstant.AUTHORITY_PROVIDER_NAME, file);
    }

}
