package com.jio.jiotalkie.fragment;

import static com.jio.jiotalkie.fragment.LocationTimelineFragment.USER_LOCATION_TIMELINE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.dataclass.UserLocationTimelineData;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.HistoricalRequestModel;
import com.jio.jiotalkie.model.HistoricalResponseModel;
import com.jio.jiotalkie.network.RESTApiManager;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.util.BitmapUtils;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.DateUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.application.customservice.dataManagment.imodels.IUserModel;

@TrackPerformance(threshold = 300)
public class LocationHistoryFragment extends Fragment {
    public static final String TAG = LocationHistoryFragment.class.getName();
    private static final double METERS_PER_DEGREE_LATITUDE = 111320.0; // Approximate value at equator
    private static final double METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR = 111320.0; // Approximate value at equator
    private static final double LOCATION_CHANGE_THRESHOLD_METERS = 10.0;
    private static final double LOCATION_CHANGE_THRESHOLD = 0.000898; // Adjust as needed
    private static final long TIME_GAP_THRESHOLD = 10 * 60 * 1000; // 10 minutes in milliseconds
    public static final String USER_ID = "user_id";
    public static final String USER_NAME = "user_name";
    public static final String USER_BATTERY = "user_battery";
    public static final String USER_NETWORK = "user_network";
    public static final String USER_TIME = "user_time";
    public static final String USER_LATITUDE = "user_latitude";
    public static final String USER_LONGITUDE = "user_longitude";
    public static final String USER_MSISDN = "user_msisdn";

    public static final int DEFAULT_INT_VALUE = -1;
    public static final String DEFAULT_STRING_VALUE = "";
    private GoogleMap mGoogleMap;
    private Marker mMarker;
    private LatLng mUserLatLng;
    private int mUserId;
    private String mUserName, mUserBattery, mUserNetwork,mReceivedTime, mUserTime, mUserLatitude, mUserLongitude,mMobilenumber;
    private DashboardViewModel mViewModel;
    private DashboardActivity mActivity;
    private List<RegisteredUser> mRegUsersList;
    private double lastLatitude, newLatitude;
    private double lastLongitude, newLongitude;
    private List<? extends IUserModel> mOnlineUserList;
    private final List<LatLng> latLngList = new ArrayList<>();
    private ArrayList<UserLocationTimelineData> mUserLocationTimelineData = new ArrayList<>();
    private String lastTime, mFormattedReceivedTime;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private boolean mIsOnline = false;
    private TextView statusInfo;
    ImageView expandeViewIndication;
    byte[] textureData = null;
    String mDurationFrom;
    private View mLoaderLayout;
    private TextView pttUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        Bundle bundle = getArguments();
        assert bundle != null;

        mViewModel = new ViewModelProvider(mActivity).get(DashboardViewModel.class);

        mUserId = bundle.getInt(USER_ID, DEFAULT_INT_VALUE);
        mUserName = bundle.getString(USER_NAME, DEFAULT_STRING_VALUE);
        mUserBattery = bundle.getString(USER_BATTERY, DEFAULT_STRING_VALUE);
        mUserNetwork = bundle.getString(USER_NETWORK, DEFAULT_STRING_VALUE);
        mUserTime = bundle.getString(USER_TIME, DEFAULT_STRING_VALUE);
        mUserLatitude = bundle.getString(USER_LATITUDE, DEFAULT_STRING_VALUE);
        mUserLongitude = bundle.getString(USER_LONGITUDE, DEFAULT_STRING_VALUE);
        mOnlineUserList = fetchOnlineUserLists();
        mMobilenumber= bundle.getString(USER_MSISDN, DEFAULT_STRING_VALUE);
        int index = CommonUtils.getSearchItemIndex(mOnlineUserList, mUserName);
        if (index >= 0) {
            mIsOnline = true;
            textureData = mOnlineUserList.get(index).getUserTexture();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        registerViewModelObserver();
        View view = inflater.inflate(R.layout.location_history_fragment, container, false);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.location_map);
        mLoaderLayout = view.findViewById(R.id.loading_layout);
        View slidePanelView = view.findViewById(R.id.slide_panel_view);
        TextView userName = view.findViewById(R.id.user_name);
        pttUser = view.findViewById(R.id.ptt_user);
        ImageView statusIcon = view.findViewById(R.id.status_icon);
        statusInfo = view.findViewById(R.id.status_info);
        TextView userAddress = view.findViewById(R.id.address);
        Button sendMessage = view.findViewById(R.id.send_message);
        Button pttCall = view.findViewById(R.id.ptt_call);
        TextView batteryCapacity = view.findViewById(R.id.battery_capacity_info);
        ImageView userImage =  view.findViewById(R.id.user_image);
        TextView userIcon = view.findViewById(R.id.user_icon);
        TextView phoneSignal = view.findViewById(R.id.phone_signal_info);
        if (textureData != null) {
            userImage.setVisibility(View.VISIBLE);
            userIcon.setVisibility(View.GONE);
            Bitmap profilepic = BitmapFactory.decodeByteArray(textureData, 0, textureData.length);
            userImage.setImageBitmap(BitmapUtils.getCircularBitmap(profilepic));
        } else {
            userIcon.setVisibility(View.VISIBLE);
            userImage.setVisibility(View.GONE);
            userIcon.setText(String.valueOf(mUserName.charAt(0)));
            BitmapUtils.setRandomBgColor(mActivity, userIcon, mIsOnline);
        }
        expandeViewIndication = view.findViewById(R.id.expande_view_indication);
        userName.setText(mUserName);
        phoneSignal.setText(mUserNetwork);
        batteryCapacity.setText(mActivity.getResources().getString(R.string.battery_capacity, mUserBattery));
        userAddress.setText(getUserAddress(Double.parseDouble(mUserLatitude),Double.parseDouble(mUserLongitude)));
        sendMessage.setOnClickListener(v -> mActivity.launchPersonalChat(mUserName, mUserId, false));
        pttCall.setOnClickListener(v -> {
            if (mIsOnline) {
                mActivity.launchPersonalChat(mUserName, mUserId, true);
            } else {
                Toast.makeText(getActivity(), mActivity.getString(R.string.ptt_to_offline_user_alert_meesage), Toast.LENGTH_SHORT).show();
            }
        });

        if (mIsOnline) {
            statusIcon.setImageResource(R.drawable.online_icon);
            statusInfo.setText(getResources().getString(R.string.status_online));
        } else {
            statusIcon.setImageResource(R.drawable.offline_icon);
        }
        bottomSheetBehavior = BottomSheetBehavior.from(slidePanelView);
        bottomSheetBehavior.setPeekHeight(getResources().getInteger(R.integer.bottom_sheet_slider_height));
        bottomSheetBehavior.setDraggable(false);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        if (expandeViewIndication.getVisibility() == View.VISIBLE) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            launchLocationTimelineFragment();
                        }
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        fetchLocation();
        assert supportMapFragment != null;
        supportMapFragment.getMapAsync(googleMap -> {
            mGoogleMap = googleMap;
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateToolBar();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mUserLocationTimelineData.clear();
    }

    private void registerViewModelObserver() {
        mViewModel.observeRegisterUserData().observe(this, registeredUsers -> {
            mRegUsersList = registeredUsers;
            int index = CommonUtils.getSearchItemIndexUser(mRegUsersList, mUserName);
            if (index >= 0) {
                mActivity.getResources().getString(R.string.mobile_no);
                pttUser.setText(mActivity.getResources().getString(R.string.mobile_no, mRegUsersList.get(index).getMsisdn()));
                if (!mIsOnline) {
                    if (mRegUsersList.get(index).getLastSeen() == null || mRegUsersList.get(index).getLastSeen().isEmpty()) {
                        statusInfo.setText(getResources().getString(R.string.status_offline));
                    } else {
                        statusInfo.setText(getResources().getString(R.string.user_last_seen_info, CommonUtils.getFormattedLastSeen(mRegUsersList.get(index).getLastSeen())));
                    }
                }
            }
        });
    }

    public void filterLocationHistory(long durationFrom) {
        mDurationFrom = DateUtils.getStringDateFromLong(durationFrom, DateUtils.dateFormatServer);
        mGoogleMap.clear();
        mUserLocationTimelineData.clear();
        latLngList.clear();
        fetchLocation();
    }

    private void fetchLocation() {
        mLoaderLayout.setVisibility(View.VISIBLE);
        HistoricalRequestModel historicalRequestModel = new HistoricalRequestModel();
        historicalRequestModel.setUserId(mUserId);
        historicalRequestModel.setNeedLastLocation(true);
        historicalRequestModel.setNeedLastNetworkStrength(true);
        historicalRequestModel.setNeedLastBatteryStrength(true);
        long durationTillLong = System.currentTimeMillis();
        String durationTill = DateUtils.getStringDateFromLong(durationTillLong, DateUtils.dateFormatServer);
        if (TextUtils.isEmpty(mDurationFrom)) {
            mDurationFrom = getDurationFrom(durationTill);
        }
        historicalRequestModel.setDurationFrom(mDurationFrom);
        historicalRequestModel.setDurationTill(durationTill);
        Call<HistoricalResponseModel[]> callLocation = RESTApiManager.getInstance().fetchUserHistoricalData(historicalRequestModel);
        callLocation.enqueue(new Callback<HistoricalResponseModel[]>() {
            @Override
            public void onResponse(@NonNull Call<HistoricalResponseModel[]> call, @NonNull Response<HistoricalResponseModel[]> response) {
                mLoaderLayout.setVisibility(View.GONE);
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    if (response.body() != null && response.body().length > 0) {
                        // TODO: will handle this case in better way.
                        mUserLocationTimelineData.clear();
                        latLngList.clear();
                        HistoricalResponseModel[] historicalResponseModels = response.body();
                        int batteryLevel = -1;
                        for (int i = historicalResponseModels.length - 1; i >= 0; i--) {
                            if (!TextUtils.isEmpty(historicalResponseModels[i].getLocation())) {
                                String location = historicalResponseModels[i].getLocation();
                                mReceivedTime = historicalResponseModels[i].getReceivedTime().trim();
                                mFormattedReceivedTime = DateUtils.dateFormat(mReceivedTime);
                                if (!TextUtils.isEmpty(historicalResponseModels[i].getBatteryLevel())) {
                                    batteryLevel = Integer.valueOf(historicalResponseModels[i].getBatteryLevel().trim());
                                }

                                String[] parts = location.split("[;,]");
                                String userLatitude = parts[0].trim();
                                String userLongitude = parts[1].trim();
                                if (!TextUtils.isEmpty(userLatitude) && !userLatitude.contains("null") && !TextUtils.isEmpty(userLongitude) && !userLongitude.contains("null")) {
                                    newLatitude = Double.parseDouble(userLatitude);
                                    newLongitude = Double.parseDouble(userLongitude);
                                    mUserLatLng = new LatLng(Double.parseDouble(userLatitude), Double.parseDouble(userLongitude));
                                }
                            }
                            if (shouldPlotLocation(newLatitude, newLongitude, mFormattedReceivedTime)) {
                                mUserLocationTimelineData.add(new UserLocationTimelineData(getUserAddress(newLatitude, newLongitude), mFormattedReceivedTime, batteryLevel));
                                latLngList.add(new LatLng(newLatitude, newLongitude));
                                drawMarker(mUserName,latLngList.size());
                                lastLatitude = newLatitude;
                                lastLongitude = newLongitude;
                                lastTime = mFormattedReceivedTime;
                            }
                        }
                        expandeViewIndication.setVisibility(View.VISIBLE);
                        bottomSheetBehavior.setDraggable(true);
                        PolylineOptions polylineOptions = new PolylineOptions().addAll(latLngList).color(Color.BLUE).width(2); // Width of polyline
                        Polyline polyline = mGoogleMap.addPolyline(polylineOptions);
                    } else {
                        Log.e(TAG, "Location not found for " + mUserName);
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


    private boolean shouldPlotLocation(double newLatitude, double newLongitude, String newTime) {
        double distance = calculateDistance(lastLatitude, lastLongitude, newLatitude, newLongitude);
        return distance > LOCATION_CHANGE_THRESHOLD_METERS;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Calculate distance using Haversine formula or other methods
        // Example calculation (not precise for large distances):
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = METERS_PER_DEGREE_LATITUDE * c;
        return distance;
    }

    private Bitmap getMarkerIconFromDrawable(String number) {
        // Inflate custom marker view
        View customMarkerView = LayoutInflater.from(requireContext()).inflate(R.layout.view_custom_marker, null);
        TextView markerText = customMarkerView.findViewById(R.id.marker_text);
        markerText.setText(number);

        // Measure and layout view
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());

        // Create bitmap from view
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        customMarkerView.draw(canvas);

        return returnedBitmap;
    }

    private String getDurationFrom(String durationTillStr) {
        String durationFrom = new String();
        android.icu.text.SimpleDateFormat dateFormat = new android.icu.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = dateFormat.parse(durationTillStr);
        } catch (android.net.ParseException | java.text.ParseException e) {
            e.printStackTrace();
        }
        long threeDaysInMillis = 100L * 24 * 60 * 60 * 1000;
        Date threeDaysBefore = new Date(date.getTime() - threeDaysInMillis);
        durationFrom = dateFormat.format(threeDaysBefore);
        return durationFrom;
    }

    private List<? extends IUserModel> fetchOnlineUserLists() {
        if (isJioTalkieServiceActive()) {
            return mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getUserList();
        }
        return Collections.emptyList();
    }

    private boolean isJioTalkieServiceActive() {
        return mViewModel != null
                && mViewModel.getJioTalkieService().isBoundToPttServer()
                && mViewModel.getJioTalkieService().isPttConnectionActive()
                && mViewModel.getJioTalkieService().getJioPttSession() != null;
    }

    private String getUserInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bat: ").append(TextUtils.isEmpty(mUserBattery) ? "NA" : mUserBattery);
        stringBuilder.append(", Nw: ").append(TextUtils.isEmpty(mUserNetwork) ? "NA" : mUserNetwork);
        stringBuilder.append(", Time: ").append(TextUtils.isEmpty(mUserTime) ? "NA" : DateUtils.dateFormat(mUserTime));
        return stringBuilder.toString();
    }

    private void drawMarker(String name,  int number) {
        if (mUserLatLng != null && mGoogleMap != null) {
            mMarker = mGoogleMap.addMarker(new MarkerOptions().position(mUserLatLng).title(name).snippet(getUserInfo()).icon(BitmapDescriptorFactory.fromBitmap(getMarkerIconFromDrawable(String.valueOf(number))))); // Use custom icon with number
            mMarker.showInfoWindow();
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(mUserLatLng));
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mUserLatLng, 10));
        }
    }

    // method definition
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void updateToolBar() {
        mActivity.showHomeToolsBar();
        mActivity.needBottomNavigation(true);
        mActivity.needChannelDetailsView(false);
        mActivity.needSOSButton(false);
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar_main);
        toolbar.findViewById(R.id.actionSearch).setVisibility(View.GONE);
        toolbar.findViewById(R.id.actionCalender).setVisibility(View.VISIBLE);
    }

    private String getUserAddress(Double userLatitude, Double userLongitude) {
        Geocoder geocoder = new Geocoder(mActivity, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(userLatitude, userLongitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getActivity().getString(R.string.address_not_found);
    }

    private void launchLocationTimelineFragment() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(USER_LOCATION_TIMELINE, mUserLocationTimelineData);
        bundle.putString(USER_NAME, mUserName);
        bundle.putString(USER_BATTERY, mUserBattery);
        bundle.putString(USER_NETWORK, mUserNetwork);
        bundle.putInt(USER_ID, mUserId);
        mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.LOCATION_TIMELINE_FRAGMENT.ordinal(), bundle);
    }
}
