package com.jio.jiotalkie.fragment;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.HistoricalRequestModel;
import com.jio.jiotalkie.model.HistoricalResponseModel;
import com.jio.jiotalkie.network.RESTApiManager;

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserMapLocation extends Fragment {
    public static final String TAG = UserMapLocation.class.getName();
    public static final String USER_NAME = "user_name";
    public static final String USER_ID = "user_id";
    private GoogleMap mGoogleMap;
    private Marker mMarker;
    private LatLng mUserLatLng;
    private String mBatteryLevel;
    private String mNetworkLevel;
    private String mReceivedTime;
    private String mUserName;
    private int mUserId;
    private DashboardActivity mActivity;

    private String getUserInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bat: ").append(TextUtils.isEmpty(mBatteryLevel) ? "NA" : mBatteryLevel);
        stringBuilder.append(", Nw: ").append(TextUtils.isEmpty(mNetworkLevel) ? "NA" : mNetworkLevel);
        stringBuilder.append(", Time: ").append(TextUtils.isEmpty(mReceivedTime) ? "NA" : dateFormat(mReceivedTime));
        return stringBuilder.toString();
    }

    private String dateFormat(String receivedTime) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            Date date = simpleDateFormat.parse(receivedTime);
            return simpleDateFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Date format " + e.getMessage());
        }
        return receivedTime;
    }

    private void updateMarker() {
        if (mMarker != null) {
            mMarker.setSnippet(getUserInfo());
        }
    }

    private void drawMarker() {
        if (mUserLatLng != null && mGoogleMap != null) {
            mMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .position(mUserLatLng)
                    .title(mUserName)
                    .snippet(getUserInfo())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(mUserLatLng));
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mUserLatLng, 17));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        assert mActivity != null;
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
        Bundle bundle = getArguments();
        assert bundle != null;
        mUserName = bundle.getString(USER_NAME, "");
        mUserId = bundle.getInt(USER_ID, -1);
        mActivity.showToolWithBack(mUserName);
        fetchLocation();
    }


    private void fetchLocation() {
        Log.d(TAG, "fetchLocation for user ID :  " + mUserId);
        HistoricalRequestModel historicalRequestModel = new HistoricalRequestModel();
        historicalRequestModel.setUserId(mUserId);
        historicalRequestModel.setNeedLastLocation(true);
        historicalRequestModel.setNeedLastBatteryStrength(true);
        historicalRequestModel.setNeedLastNetworkStrength(true);
        Call<HistoricalResponseModel[]> callLocation = RESTApiManager.getInstance().fetchUserHistoricalData(historicalRequestModel);
        callLocation.enqueue(new Callback<HistoricalResponseModel[]>() {
            @Override
            public void onResponse(@NonNull Call<HistoricalResponseModel[]> call, @NonNull Response<HistoricalResponseModel[]> response) {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    if (response.body() != null && response.body().length > 0) {
                        HistoricalResponseModel[] historicalResponseModels = response.body();
                        if (!TextUtils.isEmpty(historicalResponseModels[0].getLocation())) {
                            String location = historicalResponseModels[0].getLocation();
                            mReceivedTime = historicalResponseModels[0].getReceivedTime().trim();
                            // Split location with either ',' or ';'
                            // Since the server returns both characters as separators
                            String[] parts = location.split("[;,]");
                            String userLatitude = parts[0].trim();
                            String userLongitude = parts[1].trim();
                            if (!TextUtils.isEmpty(userLatitude) && !TextUtils.isEmpty(userLongitude)) {
                                mUserLatLng = new LatLng(Double.parseDouble(userLatitude), Double.parseDouble(userLongitude));
                            }
                        }
                        if (!TextUtils.isEmpty(historicalResponseModels[0].getBatteryLevel())) {
                            mBatteryLevel = historicalResponseModels[0].getBatteryLevel();
                        }
                        if (!TextUtils.isEmpty(historicalResponseModels[0].getNetworkLevel())) {
                            mNetworkLevel = historicalResponseModels[0].getNetworkLevel();
                        }
                        drawMarker();
                        // if require than call
                        //fetchBatteryStatus();
                    } else {
                        Log.e(TAG, "Location not found for " + mUserName);
                        Toast.makeText(mActivity,"Location not found for " + mUserName,Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<HistoricalResponseModel[]> call, Throwable t) {
                Log.e(TAG, "fetchLocation error :  " + t.getMessage());
                Toast.makeText(mActivity, "Server not responding !!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchBatteryStatus() {
        // if Battery value not fetch before
        if (TextUtils.isEmpty(mBatteryLevel)) {
            HistoricalRequestModel historicalRequestModel = new HistoricalRequestModel();
            historicalRequestModel.setUserId(mUserId);
            historicalRequestModel.setNeedLastBatteryStrength(true);
            Call<HistoricalResponseModel[]> callBattery = RESTApiManager.getInstance().fetchUserHistoricalData(historicalRequestModel);
            callBattery.enqueue(new Callback<HistoricalResponseModel[]>() {
                @Override
                public void onResponse(@NonNull Call<HistoricalResponseModel[]> call, @NonNull Response<HistoricalResponseModel[]> response) {
                    if (response.code() == HttpURLConnection.HTTP_OK) {
                        if (response.body() != null && response.body().length > 0) {
                            HistoricalResponseModel[] historicalResponseModels = response.body();
                            if (!TextUtils.isEmpty(historicalResponseModels[0].getBatteryLevel())) {
                                mBatteryLevel = historicalResponseModels[0].getBatteryLevel();
                                fetchNetworkStatus();
                                updateMarker();
                            }
                        } else {
                            // if battery status not found try for network status
                            fetchNetworkStatus();
                            Log.d(TAG, "Battery status not found for " + mUserName);
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull Call<HistoricalResponseModel[]> call, Throwable t) {
                    Log.e(TAG, "fetchBatteryStatus error :  " + t.getMessage());
                    Toast.makeText(mActivity, "Server not responding !!", Toast.LENGTH_SHORT).show();
                    // if battery status not found try for network status
                    fetchNetworkStatus();
                }
            });
        } else {
            fetchNetworkStatus();
        }
    }

    private void fetchNetworkStatus() {
        // if network value not fetch before
        if (TextUtils.isEmpty(mNetworkLevel)) {
            HistoricalRequestModel historicalRequestModel = new HistoricalRequestModel();
            historicalRequestModel.setUserId(mUserId);
            historicalRequestModel.setNeedLastNetworkStrength(true);
            Call<HistoricalResponseModel[]> callNetwork = RESTApiManager.getInstance().fetchUserHistoricalData(historicalRequestModel);
            callNetwork.enqueue(new Callback<HistoricalResponseModel[]>() {
                @Override
                public void onResponse(@NonNull Call<HistoricalResponseModel[]> call, @NonNull Response<HistoricalResponseModel[]> response) {
                    if (response.code() == HttpURLConnection.HTTP_OK) {
                        if (response.body() != null && response.body().length > 0) {
                            HistoricalResponseModel[] historicalResponseModels = response.body();
                            if (!TextUtils.isEmpty(historicalResponseModels[0].getNetworkLevel())) {
                                mNetworkLevel = historicalResponseModels[0].getNetworkLevel();
                                updateMarker();
                            }
                        } else {
                            Log.d(TAG, "Network status not found for " + mUserName);
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull Call<HistoricalResponseModel[]> call, Throwable t) {
                    Log.e(TAG, "fetchNetworkStatus error :  " + t.getMessage());
                    Toast.makeText(mActivity, "Server not responding !!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dispatcher_user_location, container, false);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        assert supportMapFragment != null;
        supportMapFragment.getMapAsync(googleMap -> {
            mGoogleMap = googleMap;
            drawMarker();
        });
        return view;
    }


}
