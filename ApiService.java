package com.jio.jiotalkie.network;

import com.jio.jiotalkie.model.HistoricalRequestModel;
import com.jio.jiotalkie.model.HistoricalResponseModel;
import com.jio.jiotalkie.model.api.ApkResponseModel;
import com.jio.jiotalkie.model.api.AuthtokenVerifyModel;
import com.jio.jiotalkie.model.api.MediaUploadModel;
import com.jio.jiotalkie.model.api.MediaUploadResponse;
import com.jio.jiotalkie.model.api.MessageListResponseModel;
import com.jio.jiotalkie.model.api.MessageRequestModel;
import com.jio.jiotalkie.model.api.MessageResponseModel;
import com.jio.jiotalkie.model.api.OtpResponseModel;
import com.jio.jiotalkie.model.api.UserSniResponseModel;
import com.jio.jiotalkie.model.api.ZLAResponseModel;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {

    @GET("/historical_data")
    Call<HistoricalResponseModel[]> getAllHistoricalData();

    @POST("/historical_data")
    Call<HistoricalResponseModel[]> getUserHistoricalData(@Body HistoricalRequestModel historicalRequestModel);

    @POST("/historical_data")
    Call<HistoricalResponseModel[]> getUserHistoricalDataV2(@Body HistoricalRequestModel historicalRequestModel,
                                                            @Query("msisdn") String msisdn,
                                                            @Query("clientType") String clientType,
                                                            @Header("Authorization") String ssoToken);

    //  Add Message POST API call here.
    @GET("messages")
    Call<List<MessageListResponseModel>> callAllMessageListApi();

    @POST("messages")
    Call<MessageResponseModel> callMessageListFilterApi(
            @Body MessageRequestModel messageRequestModel
    );

    @POST("messages")
    Call<MessageResponseModel> callMessageListFilterApiV2(@Body MessageRequestModel messageRequestModel,
                                                          @Query("msisdn") String msisdn,
                                                          @Query("clientType") String clientType,
                                                          @Header("Authorization") String ssoToken);

    //  Message Audio Download Call
    @GET("downloadFile?")
    Call<ResponseBody> downloadFile(@Query("msg_id") String msgId);

//    @GET("downloadFile?")
//    Call<ResponseBody> downloadFileV2(@Query("msg_id") String msgId,
//                                      @Query("downloadmode") String downloadmode,
//                                      @Query("client") String client,
//                                      @Header("Authorization") String ssoToken);
    @GET("downloadFile?")
    Call<ResponseBody> downloadFileV2( @Query("msisdn") String msisdn,
                                       @Query("clientType") String clientType,
                                       @Query("msg_id") String msgId,
                                      @Query("client") String client,
                                      @Header("Authorization") String ssoToken);

    @Multipart
    @POST("uploadfile")
    Call<MediaUploadResponse> uploadImage(
            @Part MultipartBody.Part InputFile,
            @Part("MetaData") MediaUploadModel mediaUploadModel
    );

    @Multipart
    @POST("uploadfile")
    Call<MediaUploadResponse> uploadImageV2(
            @Part MultipartBody.Part InputFile,
            @Part("MetaData") MediaUploadModel mediaUploadModel,
            @Query("msisdn") String msisdn,
            @Query("clientType") String clientType,
            @Header("Authorization") String ssoToken
    );

    // Check latest app version.
    @GET("updateApkVersion?dispatcher=true")
    Call<ApkResponseModel> updateApkVersion();

    @GET("updateApk?dispatcher=true")
    Call<ApkResponseModel> updateApk();

    @GET("v2/users/me/advance")
    retrofit2.Call<ZLAResponseModel> callZLAApi(
            @Header("x-api-key") String ApiKey,
            @Header("app-name") String appName,
            @Header("x-imsi") String imsi,
            @Header("x-msisdn") String msisdn,
            @Header("x-device-name") String deviceName,
            @Header("x-consumption-device-name") String consumptionDeviceName,
            @Header("x-device-type") String deviceType,
            @Header("x-android-id") String androidId
    );

    // ZLA for PROD Server.
    @GET("v2/users/me/advance")
    retrofit2.Call<ZLAResponseModel> callZLAApiProd(
            @Header("x-api-key") String ApiKey,
            @Header("app-name") String appName,
            @Header("x-device-name") String deviceName,
            @Header("x-consumption-device-name") String consumptionDeviceName,
            @Header("x-device-type") String deviceType,
            @Header("x-android-id") String androidId
    );
    @GET("/getUserSni")
    retrofit2.Call<UserSniResponseModel> callUserSni (
            @Query("msisdn") String msisdn,
            @Query("clientType") String clientType,
            @Header("Authorization") String authorization
    );
    @POST("v3/dip/user/authtoken/verify")
    retrofit2.Call<OtpResponseModel> callAuthtokenVerifyApi(
            @Header("x-api-key") String ApiKey,
            @Header("app-name") String appName,
            @Body AuthtokenVerifyModel authtokenVerifyModel
    );

    @HEAD("downloadFile?")
    Call<Void> fetchMediaFileSize(  @Query("msg_id") String msgId,
                           @Query("msisdn") String msisdn,
                           @Query("clientType") String clientType,
                           @Query("client") String client);
}
