package com.jio.jiotalkie.model.api;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
public class MsisdnDetailResponseModel {
    @SerializedName("userId")
    @Expose
    private Integer userId;
    @SerializedName("entId")
    @Expose
    private Integer entId;
    @SerializedName("serverId")
    @Expose
    private Integer serverId;
    @SerializedName("pld")
    @Expose
    private String pld;
    @SerializedName("userName")
    @Expose
    private String userName;
    @SerializedName("mssidn")
    @Expose
    private String mssidn;
    @SerializedName("entName")
    @Expose
    private String entName;
    @SerializedName("ip")
    @Expose
    private String ip;
    @SerializedName("port")
    @Expose
    private Integer port;
    @SerializedName("entUsername")
    @Expose
    private String entUsername;
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("phone")
    @Expose
    private String phone;
    public Integer getUserId() {
        return userId;
    }
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    public Integer getEntId() {
        return entId;
    }
    public void setEntId(Integer entId) {
        this.entId = entId;
    }
    public Integer getServerId() {
        return serverId;
    }
    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }
    public String getPld() {
        return pld;
    }
    public void setPld(String pld) {
        this.pld = pld;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
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
    public Integer getPort() {
        return port;
    }
    public void setPort(Integer port) {
        this.port = port;
    }
    public String getEntUsername() {
        return entUsername;
    }
    public void setEntUsername(String entUsername) {
        this.entUsername = entUsername;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
}
