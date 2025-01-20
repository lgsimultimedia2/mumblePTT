package com.jio.jiotalkie.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.application.customservice.wrapper.IMediaMessage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.util.GpsUtils;
import com.jio.jiotalkie.util.MessageIdUtils;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.application.customservice.dataManagment.models.ChannelModel;
import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.wrapper.MediaMessage;
import com.application.customservice.dataManagment.models.UserModel;
import com.application.customservice.Mumble;

@TrackPerformance(threshold = 300)
public class MapsFragment extends Fragment {

    private GoogleMap mMap;
    ArrayList markerPoints = new ArrayList();
    double mLattitude;
    double mLongitude;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SupportMapFragment mapFragment;


    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**Priyanshu.Vijay
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Avana Office, Bangalore.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            if ((ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mLattitude = location.getLatitude();
                            mLongitude = location.getLongitude();
                            Log.v("MapsFragment", "Last known location : " + mLattitude + " ; " + mLongitude);
                            LatLng currLoc = new LatLng(mLattitude, mLongitude);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(currLoc));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currLoc, 15));
                        }
                    }
                });
                mMap.setMyLocationEnabled(true);
            }
//            mMap.addMarker(new MarkerOptions().position(sydney).title(sydney.latitude+" : "+sydney.longitude));
//            markerPoints.add(sydney);

        }
    };

    private DashboardViewModel mViewModel;

    private DashboardActivity mActivity;

    private void getLatestLocation() {
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    mLattitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                    Log.v("MapsFragment", "Last known location : " + mLattitude + " ; " + mLongitude);
                    LatLng currLoc = new LatLng(mLattitude, mLongitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currLoc));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currLoc, 15));
                }
            });
            if (mapFragment != null) {
                mapFragment.getMapAsync(callback);
            }
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        mActivity = (DashboardActivity)getActivity();
        mViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(DashboardViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mActivity);
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        Bundle bundle=getArguments();
        boolean isGroupChat=bundle.getBoolean("isGroupChat");


        Button sendLocButton = view.findViewById(R.id.sendLocButton);
        sendLocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("MapsFragment","Button Onclicked "+mLattitude+" "+mLongitude);
                if(!GpsUtils.isLocationEnabled(mActivity)){
                    Toast.makeText(mActivity,mActivity.getResources().getString(R.string.enable_location),Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mLattitude == 0.0 || mLongitude == 0.0){
                    getLatestLocation();
                }
                int actor = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserID();
                String actorName = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttUser().getUserName();
                String locMessage = EnumConstant.MESSAGE_TYPE_LOCATION+mLattitude+"/"+mLongitude;
                Mumble.TextMessage.MsgType messageType = Mumble.TextMessage.MsgType.TextMessageType;
                String msgId = MessageIdUtils.generateUUID();
                String mimeType = "";
                if (isGroupChat) {
                    mViewModel.getJioTalkieService().getJioPttSession().sendTextMsgToPttChannel(mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getChannelID(), locMessage, false,messageType,msgId,mimeType,false);
                    IMediaMessage message = new MediaMessage(actor,actorName,new ArrayList<ChannelModel>(),new ArrayList<ChannelModel>(),new ArrayList<UserModel>(), locMessage,messageType,msgId,mimeType,false);
                    mViewModel.storeMessageDataInDB(message,true,true,EnumConstant.MessageType.LOCATION.name(),"",EnumConstant.MsgStatus.Undelivered.ordinal());
                }
                else {
                    String selectedUser=bundle.getString("selectedUser");
                    List<? extends IUserModel> userList = mViewModel.getJioTalkieService().getJioPttSession().fetchSessionPttChannel().getUserList();
                    int index= CommonUtils.getSearchItemIndex(userList,selectedUser);
                    if (index == -1) Toast.makeText(getActivity(),"UserModel is Offline : Can't Send Location",Toast.LENGTH_LONG).show();
                    else {
                        mViewModel.getJioTalkieService().getJioPttSession().sendTextMsgToPttUser(userList.get(index).getSessionID(), locMessage, messageType,msgId,mimeType,false);
                        Log.v("MapsFragment", "Location sent to user " + userList.get(index).getUserName());
                        ArrayList<UserModel> user = new ArrayList<>();
                        user.add((UserModel) userList.get(index));
                        MediaMessage message = new MediaMessage(actor, actorName, new ArrayList<ChannelModel>(), new ArrayList<ChannelModel>(), user, locMessage,messageType,msgId,mimeType,false);
                        mViewModel.storeMessageDataInDB(message, true, false, EnumConstant.MessageType.LOCATION.name(),"",EnumConstant.MsgStatus.Undelivered.ordinal());
                    }
                }
                getParentFragmentManager().popBackStack();
            }
        });

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }
}


