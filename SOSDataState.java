package com.jio.jiotalkie.dataclass;

import com.jio.jiotalkie.util.EnumConstant;

public class SOSDataState {

    private EnumConstant.sosState sosState;
    private String userName;

    private int userID;
    private String batteryPercentage;
    private boolean SOSvalue;
    private String location;


    private String signalStrength;
    public SOSDataState(EnumConstant.sosState sosState, String userName,String batteryPercentage,String locationCoordinates,boolean sosSetValue,String signalStrength, int userID) {
        this.sosState = sosState;
        this.userName = userName;
        this.batteryPercentage = batteryPercentage;
        this.location = locationCoordinates;
        this.SOSvalue = sosSetValue;
        this.signalStrength = signalStrength;
        this.userID = userID;
    }

    public EnumConstant.sosState getSosState() {
        return sosState;
    }

    public String getUserName() {
        return userName;
    }

    public int getUserID() {
        return userID;
    }
    public String getBatteryPercentage(){
        return batteryPercentage;
    }
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    public boolean isSOSvalue() {
        return SOSvalue;
    }
    public String getSignalStrength() {
        return signalStrength;
    }



}

