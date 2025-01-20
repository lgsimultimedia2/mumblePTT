package com.jio.jiotalkie.dataclass;
import com.application.customservice.Mumble;
public class HistoricalUserData {
    public int getUserId() {
        return userId;
    }
    private Mumble.HistoricalDataList.HistoricalData data;
    private int userId;
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    private String location;
    public HistoricalUserData(String location){
        this.location= location;
    }
    public HistoricalUserData(Mumble.HistoricalDataList.HistoricalData mdata){
        this.data=mdata;
        this.location =mdata.getLocation();
    }
}