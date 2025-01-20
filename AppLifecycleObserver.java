package com.jio.jiotalkie;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

public class AppLifecycleObserver implements LifecycleObserver {
    private boolean isAppInForeground = false;

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        isAppInForeground = true;
        // App enters foreground
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        isAppInForeground = false;
        // App enters background
    }

    public boolean isAppInForeground() {
        return isAppInForeground;
    }
}
