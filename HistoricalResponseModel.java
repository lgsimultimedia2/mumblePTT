package com.jio.jiotalkie.model;

import com.google.gson.annotations.SerializedName;

public class HistoricalResponseModel {
    @SerializedName("battery_level")
    private String batteryLevel;
    @SerializedName("location")
    private String location;
    @SerializedName("network_level")
    private String networkLevel;
    @SerializedName("received_time")
    private String receivedTime;
    @SerializedName("server_id")
    private String serverId;
    @SerializedName("user_id")
    private String userId;

    public String getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(String batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNetworkLevel() {
        return networkLevel;
    }

    public void setNetworkLevel(String networkLevel) {
        this.networkLevel = networkLevel;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
