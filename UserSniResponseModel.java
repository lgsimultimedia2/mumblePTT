package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.SerializedName;

public class UserSniResponseModel {
    @SerializedName("entId")

    private Integer entId;
    @SerializedName("mssidn")

    private String mssidn;
    @SerializedName("entName")

    private String entName;
    @SerializedName("ip")

    private String ip;

    @SerializedName("hash")

    private String hash;
    @SerializedName("dPort")

    private String dPort;
    @SerializedName("port")

    private String port;

    public Integer getEntId() {
        return entId;
    }

    public void setEntId(Integer entId) {
        this.entId = entId;
    }

    public String getMssidn() {
        return mssidn;
    }

    public void setMssidn(String mssidn) {
        this.mssidn = mssidn;
    }

    public String getEntName() {
        return entName;
    }

    public void setEntName(String entName) {
        this.entName = entName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getdPort() {
        return dPort;
    }

    public void setdPort(String dPort) {
        this.dPort = dPort;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "UserSniResponseModel{" +
                "entId=" + entId +
                ", mssidn='" + mssidn + '\'' +
                ", entName='" + entName + '\'' +
                ", ip='" + ip + '\'' +
                ", hash='" + hash + '\'' +
                ", dPort='" + dPort + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}
