package com.jio.jiotalkie.dataclass;

import java.io.Serializable;

public class UserLocationTimelineData implements Serializable {

    String address;
    private String receivedTime;
    private int batteryLevel;

    public UserLocationTimelineData(String address, String receivedTime, int batteryLevel) {
        this.address = address;
        this.receivedTime = receivedTime;
        this.batteryLevel = batteryLevel;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }
}
