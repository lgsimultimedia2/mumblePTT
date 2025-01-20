package com.jio.jiotalkie.util;

import android.Manifest;
import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.telephony.TelephonyManager.UNKNOWN_CARRIER_ID;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Looper;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoUtils {

    private static final String TAG = DeviceInfoUtils.class.getName();
    private  static FusedLocationProviderClient fusedLocationProviderClient;
    private  static String mLatitude;
    private  static String mLongitude;

    public static int getBatteryPercentage(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        if (level != -1 && scale != -1) {
            double batteryPct = level / (double) scale;
            return (int) (batteryPct * 100);
        } else {
            return -1;
        }
    }

    public static String getSignalStrength(Context context) {
        String signalStrength = null;
        String extractedInfo = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            signalStrength = String.valueOf(telephonyManager.getSignalStrength());
        }

        Pattern pattern;
        String networkType = DeviceInfoUtils.getNetworkClass(context);

        if (networkType == "WIFI") {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            int wifiStrength = 0;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                wifiStrength = wifiInfo.getRssi();
            }
            extractedInfo =String.valueOf(wifiStrength) ;
        } else if (networkType == "2G") {
            pattern = Pattern.compile("mGsm=CellSignalStrengthGsm:.*?rssi=(-?\\d+)");
            Matcher matcher = pattern.matcher(signalStrength);
            if (matcher.find()) {
                extractedInfo = matcher.group(1);
            }
        } else if (networkType == "3G") {
            pattern = Pattern.compile("mCdma=CellSignalStrengthCdma:.*?cdmaDbm=(-?\\d+)");
            Matcher matcher = pattern.matcher(signalStrength);
            if (matcher.find()) {
                extractedInfo = matcher.group(1);
            }
        } else if (networkType == "4G") {
            pattern = Pattern.compile("mLte=CellSignalStrengthLte:.*?rsrp=(-?\\d+)");
            Matcher matcher = pattern.matcher(signalStrength);
            if (matcher.find()) {
                extractedInfo = matcher.group(1);
            }
        } else if (networkType == "5G") {
            pattern = Pattern.compile("mNr=CellSignalStrengthNr:.*?csiRsrp=(-?\\d+)");
            Matcher matcher = pattern.matcher(signalStrength);
            if (matcher.find()) {
                extractedInfo = matcher.group(1);
            }
        }
        if (TextUtils.isEmpty(networkType) && TextUtils.isEmpty(extractedInfo)) {
            return "";
        } else {
            return networkType + " ;" + extractedInfo;
        }
    }

    public static void getLocation(Context context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).setMinUpdateIntervalMillis(5000).build();
            LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
            SettingsClient settingsClient = LocationServices.getSettingsClient(context);
            settingsClient.checkLocationSettings(locationSettingsRequest).addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                @Override
                public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                    if (task.isSuccessful()) {
                        if ((ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                                && (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                            Log.v(TAG, " Have Location Permission");
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                        }
                    } else {
                        Log.v(TAG, "Task unsuccessful called");
                    }
                }
            });
        }
    }

    public static String getCurrentLocation() {
        if (mLatitude != null && mLongitude != null) {
            return mLatitude+ " ; " +mLongitude;
        }
        else {
            return "";
        }
    }

    static LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.v(TAG, "LocationCallback():" + String.valueOf(locationResult));
            mLatitude = String.valueOf(locationResult.getLastLocation().getLatitude());
            mLongitude = String.valueOf(locationResult.getLastLocation().getLongitude());
        }
    };

    public static String getNetworkClass(Context context) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mInfo == null || !mInfo.isConnected()) {
            return "-";
        }

        // If Connected to Wifi
        if (mInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return "WIFI";
        }

        // If Connected to Mobile
        if (mInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            switch (mInfo.getSubtype()) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                case TelephonyManager.NETWORK_TYPE_GSM:
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                case TelephonyManager.NETWORK_TYPE_IWLAN:
                case 19:
                    return "4G";
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "5G";
                default:
                    return "?";
            }
        }
        return "?";
    }

    public static String GetMCCandMNC(Context context){
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = telephonyManager.getNetworkOperator();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            telephonyManager.getSimCarrierId();
            telephonyManager.getSimCarrierIdName();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            telephonyManager.getCarrierIdFromSimMccMnc();
            Log.d(TAG,"TelphonyValues of CarrierID=" + telephonyManager.getCarrierIdFromSimMccMnc());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG,"TelphonyValues are " +telephonyManager.getSimCarrierId()+ ";" + (telephonyManager.getSimCarrierIdName()));
        }
        String mcc = "";
        String mnc = "";
        if (!TextUtils.isEmpty(networkOperator)) {
            mcc = networkOperator.substring(0, 3);
            mnc = networkOperator.substring(3);
        }
        String result = mcc+" "+ mnc ;
        Log.d(TAG,"value of mcc and mnc is " + result);
        return result ;
    }

    public static String GetOperatorName(Context context){
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String Sim_Operator = telephonyManager.getSimOperatorName();
        return Sim_Operator;
    }

    public static boolean isJioSIM(Context context) {
        TelephonyManager telephonyManager
                = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        int carrierId = -1;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            carrierId = telephonyManager.getSimCarrierId();
            if (carrierId == UNKNOWN_CARRIER_ID) {
                // fallback
                carrierId = telephonyManager.getCarrierIdFromSimMccMnc();
            }
        }
        // FIXME check solution for devices < Q

        return true;

        //return carrierId == EnumConstant.JIO_CARRIER_ID;

        /*SubscriptionManager sManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            sManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

            SubscriptionInfo infoSim1 = sManager.getActiveSubscriptionInfoForSimSlotIndex(0);
            SubscriptionInfo infoSim2 = sManager.getActiveSubscriptionInfoForSimSlotIndex(1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                infoSim2.getCarrierId();
            }
        } Reference code, to be removed later. */

    }
}