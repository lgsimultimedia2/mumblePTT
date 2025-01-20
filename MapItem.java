package com.jio.jiotalkie.fragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MapItem implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final Boolean online;
    private final int userid;

    public MapItem(double lat, double lng, String title, String snippet, Boolean online, int userid) {
        this.online = online;
        position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
        this.userid=userid;
    }


    @Override
    public LatLng getPosition() {
        return position;
    }


    public Boolean getOnline() {
        return online;
    }

    public int getuserID() {
        return userid;
    }
    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }


    @Override
    public Float getZIndex() {
        return 0f;
    }
}


