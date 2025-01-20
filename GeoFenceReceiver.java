package com.jio.jiotalkie.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeoFenceReceiver extends BroadcastReceiver {

    private static String TAG = GeoFenceReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String message;
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        assert geofencingEvent != null;
        if (geofencingEvent.hasError()) {
            Log.d(TAG, "onReceive: Error receiving geofence event...");
            return;
        }
        List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
        for (Geofence geofence : geofenceList) {
            Log.d(TAG, "onReceive: " + geofence.getRequestId());
        }
        int transitionType = geofencingEvent.getGeofenceTransition();
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Log.d(TAG, "onReceive: GEOFENCE_TRANSITION_ENTER");
                message = "UserModel Enter into Geofence location";
                MessageEngine.getInstance().msgToChannel(message);
                MessageEngine.getInstance().msgToCompanyAdmin(message);
                Toast.makeText(context, "UserModel Enter into Geofence location", Toast.LENGTH_SHORT).show();
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Log.d(TAG, "onReceive: GEOFENCE_TRANSITION_EXIT");
                message = "UserModel Exit from Geofence location";
                MessageEngine.getInstance().msgToChannel(message);
                MessageEngine.getInstance().msgToCompanyAdmin(message);
                Toast.makeText(context, "UserModel Exit from Geofence location", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
