package com.jio.jiotalkie.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LocationHelperUtils {

    public static String LOCALITY = "locality";
    public static String ADDRESS = "address";
    public static String getLocationFromCoordinates(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        String result = "";
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i)).append("\n");
                }
                result = sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getLocalityFromCoordinates(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static HashMap<String,String> getLocalityAddress(Context context, double latitude, double longitude){
        HashMap<String,String> localityAddress = new HashMap<>();
       localityAddress.put(LOCALITY,getLocalityFromCoordinates(context,latitude,longitude));
       localityAddress.put(ADDRESS,getLocationFromCoordinates(context,latitude,longitude));
       return localityAddress;
    }
}
