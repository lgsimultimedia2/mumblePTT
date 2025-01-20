package com.jio.jiotalkie.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.HistoricalRequestModel;
import com.jio.jiotalkie.model.HistoricalResponseModel;
import com.jio.jiotalkie.network.RESTApiManager;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.DeviceInfoUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.application.customservice.dataManagment.imodels.IUserModel;

public class DispatcherLocationFragment extends Fragment {
    public static final String TAG = DispatcherLocationFragment.class.getName();
    public static final int DEFAULT_ZOOM_LEVEL = 10;
    private static final int CLUSTER_ZOOM_LEVEL = 15;
    private List<? extends IUserModel> mOnlineUserList;
    private GoogleMap mGoogleMap;
    private Marker mMarker;
    private LatLng mUserLatLng;
    private String mLastUserLatitude, mLastUserLongitude, mBatteryLevel, mReceivedTime, mNetworkLevel,mPhonenumber;
    private DashboardViewModel mViewModel;
    private DashboardActivity mActivity;
    private ClusterManager<MapItem> clusterManager;
    private LayoutInflater mLayoutInflater;
    private String currentLocation;

    private void setUpClusterer() {
        // Initialize the manager with the context and the map
        clusterManager = new ClusterManager<MapItem>(getActivity().getApplicationContext(), mGoogleMap);
        MyClusterRenderer renderer = new MyClusterRenderer(getActivity(), mGoogleMap, clusterManager, mLayoutInflater);
        clusterManager.setRenderer(renderer);
        // Set listeners for cluster managergi tdiff
        mGoogleMap.setOnCameraIdleListener(clusterManager);
        LatLng defaultLocation;
        if (!currentLocation.isEmpty() && currentLocation.contains(";")) {
            String[] coordinates = currentLocation.split(";");
            defaultLocation = new LatLng(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]));
        } else {
            defaultLocation = new LatLng(12.9212102, 77.6617891);
        }

        clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MapItem>() {
            @Override
            public boolean onClusterClick(final Cluster<MapItem> cluster) {
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                cluster.getPosition(), (float) Math.floor(CLUSTER_ZOOM_LEVEL)), 300,
                        null);
                return true;
            }
        });
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM_LEVEL));
        clusterManager.setOnClusterItemClickListener(item -> {
            // Handle click event for individual markers
            Object tag = item.getuserID();
            if (tag != null) {
                Integer userId = (Integer) tag;
//                fetchLocation(userId, item.getTitle(), false);
                launchLocationHistoryFragment(userId, item.getTitle());
                return true; // Consume the event
            }
            return true; // Return true to indicate the event was handled
        });
    }

    private void addItemsToCluster(LatLng latlng, String name, Boolean online, int userid) {
        MapItem item = new MapItem(latlng.latitude, latlng.longitude, name, "", online, userid);
        clusterManager.addItem(item);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        Bundle bundle = getArguments();
        assert bundle != null;
        mViewModel = ViewModelProviders.of(requireActivity()).get(DashboardViewModel.class);
        currentLocation = DeviceInfoUtils.getCurrentLocation();
        mOnlineUserList =fetchOnlineUserLists();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mLayoutInflater = inflater;
        View view = inflater.inflate(R.layout.fragment_dispatcher_location, container, false);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.user_map);
        mViewModel = ViewModelProviders.of(requireActivity()).get(DashboardViewModel.class);

        assert supportMapFragment != null;
        supportMapFragment.getMapAsync(googleMap -> {
            mGoogleMap = googleMap;
        });
        return view;
    }

    public void onResume() {
        super.onResume();
        if (clusterManager != null) {
            clusterManager.clearItems(); // Clear previous markers
            mGoogleMap.clear();
        }
        updateToolBar();
        registerViewModelObserver();
        setUpClusterer();
    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        if (clusterManager != null) {
//            clusterManager.clearItems(); // Clear previous markers
//            mGoogleMap.clear();
//        }
//    }

    private void registerViewModelObserver() {
        mViewModel.observeRegisterUserData().observe(this, registeredUsers -> {
            for (RegisteredUser user : registeredUsers) {
                if (user.getUserId() != mViewModel.getSelfUserId()) {
                    Boolean online=false;
                    int index = CommonUtils.getSearchItemIndex(mOnlineUserList, user.getName());
                    if (index >= 0) {
                        online = true;
                        mPhonenumber = mOnlineUserList.get(index).getMsisdn();
                    }
                    fetchLocation(user.getUserId(), user.getName(), online);
                }
            }
        });
    }

    private void updateToolBar() {
        mActivity.showHomeToolsBar();
        mActivity.needBottomNavigation(true);
        mActivity.needSOSButton(true);
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar_main);
        toolbar.findViewById(R.id.actionSearch).setVisibility(View.GONE);
        toolbar.findViewById(R.id.actionCalender).setVisibility(View.GONE);
    }

    private void drawMarker(String name, boolean online, int userId) {

        if (mUserLatLng != null && mGoogleMap != null) {
            addItemsToCluster(mUserLatLng, name, online, userId);
            clusterManager.cluster();
        }
    }


    // method definition
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private List<? extends IUserModel> fetchOnlineUserLists() {
        return mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getUserList();
    }

    private void fetchLocation(int userId, String userName, boolean online) {
        Log.d(TAG, "fetchLocation for user ID :  " + userId);
        HistoricalRequestModel historicalRequestModel = new HistoricalRequestModel();
        historicalRequestModel.setUserId(userId);
        historicalRequestModel.setNeedLastLocation(true);
        historicalRequestModel.setNeedLastNetworkStrength(true);
        historicalRequestModel.setNeedLastBatteryStrength(true);
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
                            String[] parts = location.split("[;,]");
                            mLastUserLatitude = parts[0].trim();
                            mLastUserLongitude = parts[1].trim();
                            if (!TextUtils.isEmpty(mLastUserLatitude) && !TextUtils.isEmpty(mLastUserLongitude)) {
                                mUserLatLng = new LatLng(Double.parseDouble(mLastUserLatitude), Double.parseDouble(mLastUserLongitude));
                            }
                        }
                        if (!TextUtils.isEmpty(historicalResponseModels[0].getBatteryLevel())) {
                            mBatteryLevel = historicalResponseModels[0].getBatteryLevel();
                        }
                        if (!TextUtils.isEmpty(historicalResponseModels[0].getNetworkLevel())) {
                            mNetworkLevel = historicalResponseModels[0].getNetworkLevel();
                        }
                        drawMarker(userName, online, userId);
                        //fetchBatteryStatus(userId);
                    } else {
                        Log.e(TAG, "Location not found for " + userName);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<HistoricalResponseModel[]> call, Throwable t) {
                Log.e(TAG, "callLocation error :  " + t.getMessage());
                Toast.makeText(mActivity, "Server not responding !!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchBatteryStatus(int mUserId) {
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
                                fetchNetworkStatus(mUserId);
                            }
                        } else {
                            // if battery status not found try for network status
                            fetchNetworkStatus(mUserId);
                            Log.e(TAG, "Battery status not found from Server userID : " + mUserId);
                            //Toast.makeText(mActivity, "Battery status not found from Server", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull Call<HistoricalResponseModel[]> call, Throwable t) {
                    Toast.makeText(mActivity, "Server not responding !!", Toast.LENGTH_LONG).show();
                    // if battery status not found try for network status
                    fetchNetworkStatus(mUserId);
                }
            });
        } else {
            fetchNetworkStatus(mUserId);
        }
    }

    private void fetchNetworkStatus(int mUserId ) {
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
                            }
                        } else {
                            Log.e(TAG, "Network status not found from Server userID : " + mUserId);
                           // Toast.makeText(mActivity, "Network status not found from Server", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull Call<HistoricalResponseModel[]> call, Throwable t) {
                    Toast.makeText(mActivity, "Server not responding !!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void launchLocationHistoryFragment(int userID, String name) {
        Bundle bundle = new Bundle();
        bundle.putInt(LocationHistoryFragment.USER_ID, userID);
        bundle.putString(LocationHistoryFragment.USER_NAME, name);
        bundle.putString(LocationHistoryFragment.USER_BATTERY, mBatteryLevel);
        bundle.putString(LocationHistoryFragment.USER_NETWORK, mNetworkLevel);
        bundle.putString(LocationHistoryFragment.USER_TIME, mReceivedTime);
        bundle.putString(LocationHistoryFragment.USER_LATITUDE, mLastUserLatitude);
        bundle.putString(LocationHistoryFragment.USER_LONGITUDE, mLastUserLongitude);
        bundle.putString(LocationHistoryFragment.USER_MSISDN, mPhonenumber);
        mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.LOCATION_HISTORY_FRAGMENT.ordinal(), bundle);
    }


    public class MyClusterRenderer extends DefaultClusterRenderer<MapItem> {
        private final LayoutInflater mLayoutInflater;

        public MyClusterRenderer(Context context, GoogleMap map, ClusterManager<MapItem> clusterManager, LayoutInflater layoutInflater) {
            super(context, map, clusterManager);
            this.mLayoutInflater = layoutInflater;
        }

        public Bitmap getIcon(Boolean online, String name, int zoomLevel) {
            View markerView = mLayoutInflater.inflate(R.layout.marker_layout_online, null);
            TextView textView = markerView.findViewById(R.id.marker_name);
            ImageView imageView = markerView.findViewById(R.id.marker_image);
            textView.setText(name);

            if (zoomLevel >= 15) {
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setVisibility(View.GONE);
            }

            if (online) {
                imageView.setImageResource(R.drawable.ic_blue_marker);
            } else {
                imageView.setImageResource(R.drawable.ic_grey_marker);
            }

            // Measure and layout the view
            markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

            // Create a bitmap and draw the view onto it
            Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            markerView.draw(canvas);

            return bitmap;
        }

        @Override
        protected void onClusterItemRendered(MapItem clusterItem, Marker marker) {
            super.onClusterItemRendered(clusterItem, marker);
            int zoomLevel =  (int) mGoogleMap.getCameraPosition().zoom;
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(getIcon(clusterItem.getOnline(), clusterItem.getTitle(), zoomLevel)));
        }
    }
}
