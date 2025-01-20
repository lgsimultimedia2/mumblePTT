package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.SerializedName;

public class OtpResponseModel {

    @SerializedName("sucess")
    private String success;

    @SerializedName("jToken")
    private String jToken;

    @SerializedName("ssoToken")
    private String ssoToken;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getjToken() {
        return jToken;
    }

    public void setjToken(String jToken) {
        this.jToken = jToken;
    }

    public String getSsoToken() {
        return ssoToken;
    }

    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }
}
