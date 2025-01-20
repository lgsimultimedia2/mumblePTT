package com.jio.jiotalkie.fragment;

import static android.view.View.GONE;

import static com.jio.jiotalkie.util.Constants.LATITUDE;
import static com.jio.jiotalkie.util.Constants.LONGITUDE;
import static com.jio.jiotalkie.util.Constants.RADIUS;
import static com.jio.jiotalkie.util.Constants.USER_GEOFENCE_PREFERENCE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.util.DeviceInfoUtils;
import com.jio.jiotalkie.util.MessageEngine;

public class GeoFenceFragment extends Fragment {

    private GoogleMap mGoogleMap;
    private int radius = 500;
    private DashboardActivity mActivity;
    private Button mSubmit;
    private LatLng mLatLng = null;
    private SharedPreferences sharedPreferences;
    private static final long NO_VALUE = -1;
    private boolean isReset = false;
    private Circle drawnCircle;
    private ConstraintLayout seekBarLayout;
    private SeekBar seekBarView;
    private String currentLocation;
    View thumbView;

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mGoogleMap = googleMap;
            seekBarLayout.setVisibility(View.VISIBLE);
            mSubmit.setVisibility(View.VISIBLE);
            long lat = sharedPreferences.getLong(LATITUDE, NO_VALUE);
            long lnt = sharedPreferences.getLong(LONGITUDE, NO_VALUE);
            radius = sharedPreferences.getInt(RADIUS,500);
            if (lat != NO_VALUE && lnt != NO_VALUE) {
                isReset = true;
                mLatLng = new LatLng(Double.longBitsToDouble(lat), Double.longBitsToDouble(lnt));
            } else if (!currentLocation.isEmpty() && currentLocation.contains(";")) {
                String[] coordinates = currentLocation.split(";");
                mLatLng = new LatLng(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]));
            } else {
                mLatLng = new LatLng(12.971599,77.594566);
            }
            seekBarView.setProgress(radius);
            drawMarkerAndCircle(mLatLng);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(mLatLng));
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 15));

            mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    handleMapLongClick(latLng);
                }
            });
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DashboardActivity) getActivity();
        sharedPreferences = mActivity.getSharedPreferences(USER_GEOFENCE_PREFERENCE, Context.MODE_PRIVATE);
        currentLocation = DeviceInfoUtils.getCurrentLocation();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.geo_fence_fragment, container, false);
        thumbView = LayoutInflater.from(getActivity()).inflate(R.layout.custom_seekbar_thumb, null, false);
        mSubmit = view.findViewById(R.id.submit);
        seekBarLayout = view.findViewById(R.id.seek_bar_layout);
        seekBarView = view.findViewById(R.id.seek_bar_view);
        seekBarView.setThumb(getThumb(radius));
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(callback);
        }

        seekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (drawnCircle != null) {
                    seekBar.setThumb(getThumb(progress));
                    radius = progress;
                    drawnCircle.setRadius(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mSubmit.setOnClickListener(view1 -> {
            if (mLatLng != null) {
                SharedPreferences.Editor mPreferencesEditor = sharedPreferences.edit();
                mPreferencesEditor.putLong(LATITUDE, Double.doubleToRawLongBits(mLatLng.latitude));
                mPreferencesEditor.putLong(LONGITUDE, Double.doubleToRawLongBits(mLatLng.longitude));
                mPreferencesEditor.putInt(RADIUS, radius);
                mPreferencesEditor.apply();
                mActivity.addGeofence(mLatLng, radius);
                String message;
                if (isReset) {
                     message = getResources().getString(R.string.geofence_updated);
                } else {
                    message = getResources().getString(R.string.geofence_setup);
                }
                MessageEngine.getInstance().msgToChannel(message);
                MessageEngine.getInstance().msgToCompanyAdmin(message);
                if (isReset) {
                    Toast.makeText(mActivity, R.string.geo_fence_update_success_message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, R.string.geo_fence_set_success_message, Toast.LENGTH_SHORT).show();
                }
                mActivity.handleOnBackPress();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isReset) {
            mActivity.showToolWithBack(mActivity.getResources().getString(R.string.update_geo_fence));
        } else {
            mActivity.showToolWithBack(mActivity.getResources().getString(R.string.set_geo_fence));
        }
        mActivity.needBottomNavigation(false);
        mActivity.needSOSButton(false);
    }

    private void handleMapLongClick(LatLng latLng) {
        mLatLng = latLng;
        mGoogleMap.clear();
        radius = 500;
        drawMarkerAndCircle(latLng);
        seekBarView.setProgress(radius);
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
        drawnCircle = mGoogleMap.addCircle(circleOptions);
    }

    public Drawable getThumb(int progress) {
        ((TextView) thumbView.findViewById(R.id.tvProgress)).setText(progress+"");

        thumbView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Bitmap bitmap = Bitmap.createBitmap(thumbView.getMeasuredWidth(), thumbView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        thumbView.layout(0, 0, thumbView.getMeasuredWidth(), thumbView.getMeasuredHeight());
        thumbView.draw(canvas);

        return new BitmapDrawable(getResources(), bitmap);
    }
}
