package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ZLAResponseModel {
    @SerializedName("imsi")
    @Expose
    private String imsi;
    @SerializedName("msisdn")
    @Expose
    private String msisdn;
    @SerializedName("jToken")
    @Expose
    private String jToken;
    @SerializedName("ssoLevel")
    @Expose
    private String ssoLevel;
    @SerializedName("ssoToken")
    @Expose
    private String ssoToken;
    @SerializedName("sessionAttributes")
    @Expose
    private SessionAttributes sessionAttributes;
    @SerializedName("lbCookie")
    @Expose
    private String lbCookie;

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getmsisdn() {
        return msisdn;
    }
    public void setmsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getjToken() {
        return jToken;
    }

    public void setjToken(String jToken) {
        this.jToken = jToken;
    }

    public String getSsoLevel() {
        return ssoLevel;
    }

    public void setSsoLevel(String ssoLevel) {
        this.ssoLevel = ssoLevel;
    }

    public String getSsoToken() {
        return ssoToken;
    }

    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }

    public SessionAttributes getSessionAttributes() {
        return sessionAttributes;
    }

    public void setSessionAttributes(SessionAttributes sessionAttributes) {
        this.sessionAttributes = sessionAttributes;
    }

    public String getLbCookie() {
        return lbCookie;
    }

    public void setLbCookie(String lbCookie) {
        this.lbCookie = lbCookie;
    }

    public class SessionAttributes {

        @SerializedName("user")
        @Expose
        private User user;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

    }

    public class User {

        @SerializedName("subscriberId")
        @Expose
        private String subscriberId;
        @SerializedName("preferredLocale")
        @Expose
        private String preferredLocale;
        @SerializedName("ssoLevel")
        @Expose
        private String ssoLevel;
        @SerializedName("unique")
        @Expose
        private String unique;
        @SerializedName("commonName")
        @Expose
        private String commonName;

        public String getSubscriberId() {
            return subscriberId;
        }

        public void setSubscriberId(String subscriberId) {
            this.subscriberId = subscriberId;
        }

        public String getPreferredLocale() {
            return preferredLocale;
        }

        public void setPreferredLocale(String preferredLocale) {
            this.preferredLocale = preferredLocale;
        }

        public String getSsoLevel() {
            return ssoLevel;
        }

        public void setSsoLevel(String ssoLevel) {
            this.ssoLevel = ssoLevel;
        }

        public String getUnique() {
            return unique;
        }

        public void setUnique(String unique) {
            this.unique = unique;
        }

        public String getCommonName() {
            return commonName;
        }

        public void setCommonName(String commonName) {
            this.commonName = commonName;
        }
    }
}
