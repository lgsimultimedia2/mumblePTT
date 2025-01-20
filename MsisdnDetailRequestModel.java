package com.jio.jiotalkie.model.api;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
public class MsisdnDetailRequestModel {
    @SerializedName("mssisdn")
    @Expose
    private String mssisdn;
    @SerializedName("ssoToken")
    @Expose
    private String ssoToken;
    public String getMssisdn() {
        return mssisdn;
    }
    public void setMssisdn(String mssisdn) {
        this.mssisdn = mssisdn;
    }
    public String getSsoToken() {
        return ssoToken;
    }
    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }
}
