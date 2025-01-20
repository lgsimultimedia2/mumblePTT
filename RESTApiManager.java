package com.jio.jiotalkie.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.model.HistoricalRequestModel;
import com.jio.jiotalkie.model.HistoricalResponseModel;
import com.jio.jiotalkie.model.api.MediaUploadModel;
import com.jio.jiotalkie.model.api.MediaUploadResponse;
import com.jio.jiotalkie.model.api.MessageRequestModel;
import com.jio.jiotalkie.model.api.MessageResponseModel;
import com.jio.jiotalkie.model.api.UserSniResponseModel;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.util.ServerConstant;

import java.net.URI;
import java.net.URISyntaxException;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Multipart;
import retrofit2.http.POST;

public class RESTApiManager {
    final static String TAG = RESTApiManager.class.getSimpleName();
    final String CONTENT_TYPE = "Content-Type";
    final String AUTHORIZATION = "Authorization";
    final String CONTENT_TYPE_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    final String DOWNLOAD_MODE_VALUE = "base64";
    final String CLIENT_VALUE = "android";
    final String CLIENT_TYPE_VALUE = "webApp";
    final String CLIENT = "client";
    final String CLIENT_TYPE = "clientType";
    final String MSISDN = "msisdn";

    private static RESTApiManager mInstance;
    private String mEncryptedMsisdn, mMsisdn;
    private String mSSOToken;
    private ApiService mApiService;
    private JioTalkieSettings mJioTalkieSettings;

    private RESTApiManager() {

    }

    public static RESTApiManager getInstance() {
        synchronized (RESTApiManager.class) {
            if (mInstance == null) {
                mInstance = new RESTApiManager();
            }
            return mInstance;
        }
    }

    public void setUp(Context context, String encryptedMsisdn, String ssoToken) {
        mEncryptedMsisdn = encryptedMsisdn;
        mSSOToken = ssoToken;
        mApiService = RetrofitClient.getRetrofitClient(context);
        mJioTalkieSettings = JioTalkieSettings.getInstance(context.getApplicationContext());
        Log.d(TAG, "MSISDN setUpV2 :" + mJioTalkieSettings.getMSISDN());
    }


    private String formattedSsoToken() {
        return "Bearer " + mSSOToken;
    }

    private boolean isNewREST() {
        return BuildConfig.NEW_REST_API;
    }

    public @POST("/historical_data")
    Call<HistoricalResponseModel[]> fetchUserHistoricalData(HistoricalRequestModel historicalRequestModel) {
        if (isNewREST()) {
            return mApiService.getUserHistoricalDataV2(historicalRequestModel, mJioTalkieSettings.getMSISDN(), CLIENT_TYPE_VALUE, formattedSsoToken());
        } else {
            return mApiService.getUserHistoricalData(historicalRequestModel);
        }
    }

    public @POST("messages")
    Call<MessageResponseModel> fetchMessagesData(MessageRequestModel messageRequestModel) {
        if (isNewREST()) {
            Log.d(TAG, "MSISDN fetchMessagesData");
            return mApiService.callMessageListFilterApiV2(messageRequestModel, mJioTalkieSettings.getMSISDN(), CLIENT_TYPE_VALUE, formattedSsoToken());
        } else {
            return mApiService.callMessageListFilterApi(messageRequestModel);
        }
    }

    public @GET("downloadFile?")
    Call<ResponseBody> downloadFile(String msgId) {
        if (isNewREST()) {
            Log.d(TAG, "downloadFile - msgId::" + msgId + "::DOWNLOAD_MODE_VALUE::" + DOWNLOAD_MODE_VALUE + "::CLIENT_VALUE::" + CLIENT_VALUE + "::formattedSsoToken()::" + formattedSsoToken());
            return mApiService.downloadFileV2(mJioTalkieSettings.getMSISDN(), CLIENT_TYPE_VALUE, msgId, CLIENT_VALUE, formattedSsoToken());
        } else {
            return mApiService.downloadFile(msgId);
        }
    }

    public @Multipart
    @POST("uploadfile")
    Call<MediaUploadResponse> uploadImage(MultipartBody.Part inputFile, MediaUploadModel mediaUploadModel) {
        if (isNewREST()) {
            return mApiService.uploadImageV2(inputFile, mediaUploadModel, mJioTalkieSettings.getMSISDN(), CLIENT_TYPE_VALUE, formattedSsoToken());
        } else {
            return mApiService.uploadImage(inputFile, mediaUploadModel);
        }
    }

    public @GET("/getUserSni")
    retrofit2.Call<UserSniResponseModel> callUserSni() {
        return RetrofitClient.getRetrofitClient(RetrofitClient.BaseUrlType.UserSni).callUserSni(
                mJioTalkieSettings.getMSISDN(), CLIENT_TYPE_VALUE, formattedSsoToken());
    }
    public @HEAD("downloadFile?")
    Call<Void> fetchMediaFileSize(String msgId){
        return mApiService.fetchMediaFileSize(msgId,mJioTalkieSettings.getMSISDN(), CLIENT_TYPE_VALUE,CLIENT_VALUE);
    }

    public GlideUrl getGlideMediaUrl(String msgId) {
        GlideUrl glideUrl = null;
        try {
            Uri baseUri = Uri.parse(ServerConstant.getDownloadAWSServer() + msgId);
            if (isNewREST()) {
                Uri.Builder builder = baseUri.buildUpon();
                builder.appendQueryParameter(MSISDN, mJioTalkieSettings.getMSISDN());
                builder.appendQueryParameter(CLIENT_TYPE, CLIENT_TYPE_VALUE);
                builder.appendQueryParameter(CLIENT, CLIENT_VALUE);
                Uri finalUri = builder.build();
                glideUrl = new GlideUrl(finalUri.toString(), new LazyHeaders.Builder()
                        .addHeader(CONTENT_TYPE, CONTENT_TYPE_FORM_URL_ENCODED)
                        .addHeader(AUTHORIZATION, formattedSsoToken())
                        .build());
            } else {
                glideUrl = new GlideUrl(baseUri.toString(), new LazyHeaders.Builder().build());
            }
            return glideUrl;
        } catch (Exception e) {
            return glideUrl;
        }

    }
}
