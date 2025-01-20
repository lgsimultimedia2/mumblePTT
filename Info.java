package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Info {

    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("platform")
    @Expose
    private Platform platform;
    @SerializedName("build")
    @Expose
    private BuildInfo build;
    @SerializedName("imei")
    @Expose
    private String imei;
    @SerializedName("imsi")
    @Expose
    private String imsi;
    @SerializedName("msisdn")
    @Expose
    private String msisdn;
    @SerializedName("androidId")
    @Expose
    private String androidId;
    @SerializedName("mac")
    @Expose
    private String mac;
    @SerializedName("bluetoothAddress")
    @Expose
    private String bluetoothAddress;
    @SerializedName("latitude")
    @Expose
    private String latitude;
    @SerializedName("longitude")
    @Expose
    private String longitude;

    public Info() {
        platform = new Platform();
        build = new BuildInfo();
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public BuildInfo getBuild() {
        return build;
    }

    public void setBuild(BuildInfo build) {
        this.build = build;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getmsisdn() {
        return msisdn;
    }
    public void setmsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getAndroidId() {
        return androidId;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

}