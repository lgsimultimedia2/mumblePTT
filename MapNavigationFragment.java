package com.jio.jiotalkie.fragment;


import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;
import java.util.Objects;

import com.application.customservice.wrapper.IJioPttSession;

@TrackPerformance(threshold = 300)
public class MapNavigationFragment extends Fragment {
    private GoogleMap mMap;
    private DashboardActivity mActivity;
    private Toolbar mToolbar;
    //private ImageView mFilterMenuView;
    private DashboardViewModel mViewModel;

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            Bundle bundle = getArguments();
            Double latitude = bundle.getDouble("latitude");
            Double longitude = bundle.getDouble("longitude");
            LatLng location = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(location));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));

            return;
        }

    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        mViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(DashboardViewModel.class);
        mActivity = (DashboardActivity) getActivity();
        initView(view);
        return view;

    }

    private void initView(View view) {
        Button sendLocButton = view.findViewById(R.id.sendLocButton);
        sendLocButton.setVisibility(View.GONE);
//        mToolbar = getActivity().findViewById(R.id.toolbar_main);
//        mFilterMenuView = mToolbar.findViewById(R.id.iv_three_dot);
        getChannelInfoFromSession();
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    mActivity.handleOnBackPress();
                    return true;
                }
                return false;
            }
        });
    }

    private void getChannelInfoFromSession() {
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive()) {
            try {
                IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
                if (session != null && session.fetchSessionPttChannel() != null &&
                        session.fetchSessionPttChannel().getChannelName() != null) {
                    String channelName = session.fetchSessionPttChannel().getChannelName();
                   // mActivity.showDashboardToolbar(false, channelName);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

}