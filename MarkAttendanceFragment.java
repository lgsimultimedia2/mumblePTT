package com.jio.jiotalkie.fragment;

import static android.view.View.GONE;

import static com.jio.jiotalkie.util.Constants.LATITUDE;
import static com.jio.jiotalkie.util.Constants.LONGITUDE;
import static com.jio.jiotalkie.util.Constants.RADIUS;
import static com.jio.jiotalkie.util.Constants.USER_GEOFENCE_PREFERENCE;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.util.DeviceInfoUtils;

public class MarkAttendanceFragment extends Fragment {

    private DashboardActivity mActivity;
    private GoogleMap mGoogleMap;
    private static final long NO_VALUE = -1;
    private LatLng mGeofenceLocation = null;
    private LatLng mCurrentLocation = null;
    private String currentCoordinates;
    private int radius;
    private SharedPreferences sharedPreferences;

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mGoogleMap = googleMap;
            if (mGeofenceLocation != null) {
                drawMarkerAndCircle(mGeofenceLocation);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(mGeofenceLocation));
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mGeofenceLocation, 15));
            }

            if (mCurrentLocation != null) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                drawMarker(mCurrentLocation);
            }

            mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    moveToCurrentLocation();
                    return true;
                }
            });
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        sharedPreferences = mActivity.getSharedPreferences(USER_GEOFENCE_PREFERENCE, Context.MODE_PRIVATE);
        long lat = sharedPreferences.getLong(LATITUDE, NO_VALUE);
        long lnt = sharedPreferences.getLong(LONGITUDE, NO_VALUE);
        radius = sharedPreferences.getInt(RADIUS,500);
        if (lat != NO_VALUE && lnt != NO_VALUE) {
            mGeofenceLocation = new LatLng(Double.longBitsToDouble(lat), Double.longBitsToDouble(lnt));
        }

        currentCoordinates = DeviceInfoUtils.getCurrentLocation();
        if (!currentCoordinates.isEmpty() && currentCoordinates.contains(";")) {
            String[] coordinates = currentCoordinates.split(";");
            mCurrentLocation = new LatLng(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mark_attendance_layout, container, false);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        Button proceedButton = view.findViewById(R.id.proceed);
        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(callback);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivity.showToolWithBack(mActivity.getResources().getString(R.string.mark_attendance));
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
    }

    private void moveToCurrentLocation() {
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLocation));
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, 15));
    }

    private void drawMarkerAndCircle(LatLng latLng) {
        drawMarker(latLng);
        drawCircle(latLng, radius);
    }

    private void drawMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mGoogleMap.addMarker(markerOptions);
    }

    private void drawCircle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0));
        circleOptions.fillColor(Color.argb(64, 255, 0, 0));
        circleOptions.strokeWidth(4);
        mGoogleMap.addCircle(circleOptions);
    }
}
