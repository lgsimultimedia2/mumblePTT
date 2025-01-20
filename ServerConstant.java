package com.jio.jiotalkie.util;

import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.dispatch.R;

public class ServerConstant {
    private static final String HTTPS = "https://";
    private static final String HTTP = "http://";
    private static final String AWS_DOWNLOAD_URL_POSTFIX = "downloadFile?msg_id=";

    private ServerConstant() {

    }

    public static String getAWSServer() {
        StringBuilder sb = new StringBuilder();
        if (BuildConfig.BUILD_TYPE.equals("sit")) {
            sb.append(HTTPS).append(BuildConfig.SERVER_IP).append(":").append(BuildConfig.AWS_SERVER_PORT).append("/");
        } else {
            sb.append(HTTP).append(BuildConfig.SERVER_IP).append(":").append(BuildConfig.AWS_SERVER_PORT).append("/");
        }

        return sb.toString();
    }

    public static String getDownloadAWSServer() {
        return getAWSServer() + AWS_DOWNLOAD_URL_POSTFIX;
    }

    public static String getMumbleServer() {
        return BuildConfig.SERVER_IP;
    }

    public static int getMumblePort() {
        return BuildConfig.SERVER_PORT;
    }

    public static int getHttpsCertificate() {
        return BuildConfig.TESTING_BUILD ? R.raw.certificate_13_235_8_160 : R.raw.certificate_13_235_8_160;
    }

    public static boolean isTestingBuild() {
        return BuildConfig.TESTING_BUILD;
    }

}
