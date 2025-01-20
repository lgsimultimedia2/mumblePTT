package com.jio.jiotalkie.network;

import android.content.Context;
import android.util.Log;

import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.util.ServerConstant;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public enum BaseUrlType {
        LoginUrlHttp,
        LoginUrlHttps,
        BaseUrlInternal,
        ApkDownloadURL,
        UserSni

    }

    public static final String TAG = RetrofitClient.class.getName();
    private static final String LOGIN_URL_HTTP = BuildConfig.LOGIN_URL_HTTP;
    private static final String LOGIN_URL_HTTPS = BuildConfig.LOGIN_URL_HTTPS;

    private static final String APK_URL = BuildConfig.APK_URL_HTTP;
    private static final int NETWORK_TIMEOUT = 300; // 5 * 60 (sec) i.e 5 min for connection and Write operation
    private static final int READ_TIMEOUT = 30; // 30 sec for read operation
    private static Retrofit rfLoginHttp = null; // used for HTTP login
    private static ApiService asLoginHttp = null; // used for HTTP login

    private static Retrofit rfapkHttp = null; // used for HTTP login
    private static ApiService asapkHttp = null; // used for HTTP login
    private static Retrofit rfLoginHttps = null; // used for SSO token refresh
    private static ApiService asLoginHttps = null; // used for SSO token refresh
    private static Retrofit rfBaseUrlInternal = null; // used for other REST APIs
    private static ApiService asBaseUrlInternal = null; // used for other REST APIs
    private static Retrofit retrofit = null; // used for others
    private static ApiService apiService = null; // user for others
    private static final String USER_SNI_URL = BuildConfig.USER_SNI_URL;
    private static Retrofit rfUserSni = null; // used for user SNI API
    private static ApiService asUserSni = null; // used for user SNI API

    private RetrofitClient() {

    }

    /**
     * For AWS network call (e.g. Message, History Data,etc)
     *
     * @param context :
     * @return :@ApiService
     */
    public static ApiService getRetrofitClient(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(ServerConstant.getAWSServer())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(getOKHttpClient(context))
                    .build();
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    /**
     * For AWS network call (e.g. Message, History Data,etc)
     *
     * @param urlType :
     * @return :@ApiService
     */
    public static ApiService getRetrofitClient(BaseUrlType urlType) {
        switch (urlType) {
            case LoginUrlHttp:
                if (rfLoginHttp == null) {
                    rfLoginHttp = new Retrofit.Builder()
                            .baseUrl(LOGIN_URL_HTTP)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(getOKHttpClient())
                            .build();
                    asLoginHttp = rfLoginHttp.create(ApiService.class);
                }
                return asLoginHttp;
            case LoginUrlHttps:
                if (rfLoginHttps == null) {
                    rfLoginHttps = new Retrofit.Builder()
                            .baseUrl(LOGIN_URL_HTTPS)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(getOKHttpClient())
                            .build();
                    asLoginHttps = rfLoginHttps.create(ApiService.class);
                }
                return asLoginHttps;
            case ApkDownloadURL:
                if (rfapkHttp == null) {
                    rfapkHttp = new Retrofit.Builder()
                            .baseUrl(APK_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(getOKHttpClient())
                            .build();
                    asapkHttp = rfapkHttp.create(ApiService.class);
                }
                return asapkHttp;
            case UserSni:
                if (rfUserSni == null) {
                    rfUserSni = new Retrofit.Builder()
                            .baseUrl(USER_SNI_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(getOKHttpClient())
                            .build();
                    asUserSni= rfUserSni.create(ApiService.class);
                }
                return asUserSni;
            default:
                break;
        }
        return null;
    }

    private static OkHttpClient getOKHttpClientZLA() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        return new OkHttpClient.Builder().addInterceptor(interceptor)
                .connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                // Support both HTTP/1.1 and HTTP/2
                .protocols(Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_2))
                .build();
    }

    private static OkHttpClient getOKHttpClient(Context context) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.addInterceptor(interceptor);
        okHttpClientBuilder.connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS);
        okHttpClientBuilder.writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS);
        okHttpClientBuilder.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        // Support both HTTP/1.1 and HTTP/2
        okHttpClientBuilder.protocols(Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_2));
        // Validate SSL certificate for Https network calls.
        // TODO : below check will remove once https deploy in 13.232.90.124 server.
        // Disable https call for now
        if (false && !ServerConstant.isTestingBuild()) {
            SSLContext sslContext = getSSLCertificate(context);
            if (sslContext != null) {
                okHttpClientBuilder.hostnameVerifier((hostname, session) -> true);
                okHttpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), systemDefaultTrustManager());
            }
        }
        return okHttpClientBuilder.build();
    }

    private static SSLContext getSSLCertificate(Context context) {
        SSLContext sslContext = null;
        try {
            Certificate ca;
            try (InputStream trustedCertificateIS = context.getResources().openRawResource(ServerConstant.getHttpsCertificate())) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                try (trustedCertificateIS) {
                    ca = cf.generateCertificate(trustedCertificateIS);
                }
            }
            // Creating a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Creating a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Creating an SSLSocketFactory that uses our TrustManager
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
        } catch (CertificateException | IOException | KeyStoreException | KeyManagementException |
                 NoSuchAlgorithmException exception) {
            Log.e(TAG, "Exception while getSSLCertificate : " + exception.getMessage());
        }
        return sslContext;

    }

    private static X509TrustManager systemDefaultTrustManager() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            return (X509TrustManager) trustManagers[0];
        } catch (GeneralSecurityException e) {
            // The system has no TLS. Just give up.
            throw new AssertionError();
        }
    }

    private static OkHttpClient getOKHttpClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder().addInterceptor(interceptor).build();
    }

}
