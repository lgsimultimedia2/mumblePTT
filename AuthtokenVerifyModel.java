package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.SerializedName;

public class AuthtokenVerifyModel {

    @SerializedName("deviceInfo")
    private DeviceInfo1 deviceInfo1 = null;

    public class DeviceInfo1 {
        @SerializedName("jToken")
        private String jToken;
        @SerializedName("consumptionDeviceName")
        private String consumptionDeviceName;
        @SerializedName("info")
        private Info info = null;
        public DeviceInfo1() {
            info = new Info();
        }
        public void setToken(String token) { this.jToken = token; }
        public String getToken() { return  jToken; }
        public void setConsumptionDeviceName(String consumptionDeviceName) {
            this.consumptionDeviceName = consumptionDeviceName;
        }
        public String getConsumptionDeviceName() { return consumptionDeviceName; }
        public void setInfo(Info info) { this.info = info; }
        public Info getInfo() { return  info; }
    }

    public void setDeviceInfo(DeviceInfo1 deviceInfo) { this.deviceInfo1 = deviceInfo;}
    public DeviceInfo1 getDeviceInfo1() { return  deviceInfo1; }

    public AuthtokenVerifyModel() {
        deviceInfo1 = new DeviceInfo1();
    }
}
