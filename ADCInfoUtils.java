package com.jio.jiotalkie.util;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.jio.adc.ADC;
import com.jio.adc.core.model.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class ADCInfoUtils {
    private static final String TAG = ADCInfoUtils.class.getSimpleName();

    private ADCInfoUtils() {
    }

    public static void calculateImageSize(String filePath, boolean isSelfChat, String mimetype,
                                          int userID, int channelID, String msgCategory,
                                          int targetUserId) {
        long duration = 0;
        if (mimetype.equals("video/mp4")) {
            duration = getVideoDuration(filePath);
        }
        File file = new File(filePath);
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                int bytesRead = fis.read(data);
                sendInfoToAdc(mimetype, data.length / 1024.0, duration, 0,
                        "", isSelfChat, userID, channelID, msgCategory, targetUserId);
            } catch (Exception e) {
                Log.e(TAG, "calculateImageSize Exception " + e.getMessage());
            } finally {
                if (fis != null) {
                    safeClose(fis);
                }
            }

        } else {
            Log.d(TAG, "calculateImageSize: File does not exist!");

        }
    }

    private static long getVideoDuration(String filepath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        long durationInSeconds = 0;
        retriever.setDataSource(filepath);
        String time = null;
        time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long durationInMilliseconds = Long.parseLong(time);
        durationInSeconds = durationInMilliseconds / 1000;
        try {
            retriever.release();
        } catch (IOException e) {
            Log.e(TAG, "getVideoDuration Exception " + e.getMessage());
        }
        return durationInSeconds;
    }

    public static void calculateTextSize(String message, boolean isSelfChat, int userId, int channelId, String msgCategory, int targetUserid) {
        int sizeInBytes = message.getBytes().length;
        double sizeInKB = sizeInBytes / 1024.0;
        sendInfoToAdc(EnumConstant.MessageType.TEXT.toString(), sizeInKB, 0, 0, "", isSelfChat, userId, channelId, msgCategory, targetUserid);
    }

    public static void calculatePTTSize(String filePath, double duration, boolean isSelfChat, String msgType,
                                        int userId, int channelId, String msgCategory, int targetUserid) {
        File file = new File(filePath);
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                int bytesRead = fis.read(data);
                sendInfoToAdc(msgType, data.length / 1024,
                        duration, 0, "", isSelfChat,
                        userId, channelId, msgCategory, targetUserid);
            } catch (Exception e) {
                Log.e(TAG, "calculatePTTSize Exception " + e.getMessage());
            } finally {
                if (fis != null) {
                    safeClose(fis);
                }
            }

        }
    }

    public static void loginInfo(int connectionValue, int errorCode,
                                 String errorMessage, int userId, int channelId, String msisdnNum) {
        Parameters parameters = new Parameters()
                .addParameter("XJJT06", connectionValue)
                .addParameter("XJJT04", errorCode)
                .addParameter("XJJT05", errorMessage)
                .addParameter("XJJT09", userId)
                .addParameter("XJJT10", channelId)
                .addParameter("XJJT13", msisdnNum);
        ADC.writeEvent("XJJTE3", parameters);
    }

    public static void serverConnectionInfo(int connectionValue, int errorCode,
                                            String errorMessage, int userId, int channelId) {
        Parameters parameters = new Parameters()
                .addParameter("XJJT08", connectionValue)
                .addParameter("XJJT04", errorCode)
                .addParameter("XJJT05", errorMessage)
                .addParameter("XJJT09", userId)
                .addParameter("XJJT10", channelId);
        ADC.writeEvent("XJJTE7", parameters);

    }

    public static void sendInfoToAdc(String msgType, double size,
                                     double duration, int errorCode, String errorMessage,
                                     boolean isSelfChat, int userID, int channelId,
                                     String msgCategory, int targetUserId) {
        Parameters parameters = new Parameters()
                .addParameter("XJJT01", msgType)
                .addParameter("XJJT02", size)
                .addParameter("XJJT03", duration)
                .addParameter("XJJT04", errorCode)
                .addParameter("XJJT05", errorMessage)
                .addParameter("XJJT09", userID)
                .addParameter("XJJT10", channelId)
                .addParameter("XJJT11", msgCategory)
                .addParameter("XJJT12", targetUserId);
        if (isSelfChat) {
            ADC.writeEvent("XJJTE1", parameters);
        } else {
            ADC.writeEvent("XJJTE2", parameters);
        }
    }

    public static void logOutInfo(int connectionValue, int errorCode, String errorMessage,
                                  int userId, int channelId) {
        Parameters parameters = new Parameters()
                .addParameter("XJJT06", connectionValue)
                .addParameter("XJJT04", errorCode)
                .addParameter("XJJT05", errorMessage)
                .addParameter("XJJT09", userId)
                .addParameter("XJJT10", channelId);
        ADC.writeEvent("XJJTE4", parameters);
    }

    public static void floorGrantedInfo(boolean floorValue, int errorCode, String errorMessage,
                                        int userId, int channelID, String floorCategory) {
        Parameters parameters = new Parameters()
                .addParameter("XJJT07", floorValue)
                .addParameter("XJJT04", errorCode)
                .addParameter("XJJT05", errorMessage)
                .addParameter("XJJT09", userId)
                .addParameter("XJJT10", channelID)
                .addParameter("XJJT14", floorCategory);
        if (floorValue) {
            ADC.writeEvent("XJJTE5", parameters);
        } else {
            ADC.writeEvent("XJJTE6", parameters);
        }
    }

    public static void safeClose(FileInputStream fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                Log.d(TAG, "safeClose Exception : " + e.getMessage());
            }
        }
    }
}
