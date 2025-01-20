package com.jio.jiotalkie.model;

import com.google.gson.annotations.SerializedName;

public class HistoricalRequestModel {
    @SerializedName("userId")
    private int userId;
    @SerializedName("needLastLocation")
    private boolean needLastLocation;
    @SerializedName("needLastBatteryStrength")
    private boolean needLastBatteryStrength;
    @SerializedName("needLastNetworkStrength")
    private boolean needLastNetworkStrength;
    @SerializedName("durationFrom")
    private String durationFrom;
    @SerializedName("durationTill")
    private String durationTill;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isNeedLastLocation() {
        return needLastLocation;
    }

    public void setNeedLastLocation(boolean needLastLocation) {
        this.needLastLocation = needLastLocation;
    }

    public boolean isNeedLastBatteryStrength() {
        return needLastBatteryStrength;
    }

    public void setNeedLastBatteryStrength(boolean needLastBatterystrength) {
        this.needLastBatteryStrength = needLastBatterystrength;
    }

    public boolean isNeedLastNetworkStrength() {
        return needLastNetworkStrength;
    }

    public void setNeedLastNetworkStrength(boolean needLastNetworkStrength) {
        this.needLastNetworkStrength = needLastNetworkStrength;
    }

    public String getDurationFrom() {
        return durationFrom;
    }

    public void setDurationFrom(String durationFrom) {
        this.durationFrom = durationFrom;
    }

    public String getDurationTill() {
        return durationTill;
    }

    public void setDurationTill(String durationTill) {
        this.durationTill = durationTill;
    }
}

