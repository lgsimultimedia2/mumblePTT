package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.SerializedName;

public class ApkResponseModel {

    public ApkResponseModel(String androidApkVersion, String androidApkFile){
        this.androidApkVersion = androidApkVersion;
        this.androidApkFile = androidApkFile;
    }

    @SerializedName("android_apk_version")
    private String androidApkVersion;

    @SerializedName("android_apk_file")
    private String androidApkFile;

    public String getAndroidApkVersion() {
        return androidApkVersion;
    }
    public String getAndroidApkFile() {
        return androidApkFile;
    }
}
