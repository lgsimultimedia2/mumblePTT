package com.jio.jiotalkie;



import static com.jio.jiotalkie.dispatch.BuildConfig.ADC_KEY;

import android.app.Application;

import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.BuildConfig;

import com.google.firebase.FirebaseApp;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.jio.adc.ADC;
import com.jio.adc.core.model.ADCOptions;
import com.jio.jiotalkie.performance.AdvancedPerformanceTracker;
import com.jio.logging.LogFactory;


public class JioTalkieApplication extends Application {
    private static AppLifecycleObserver lifecycleObserver;
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        LogFactory.setLogLevel(LogFactory.VERBOSE);
        ADCOptions adcOptions = new ADCOptions.Builder()
                .withRunMode(BuildConfig.DEBUG ? ADCOptions.RunMode.DEVELOPMENT : ADCOptions.RunMode.PRODUCTION)
                .withApiKey(ADC_KEY)
                .withUploadDisabled(false)
                .build();
        ADC.init(this, adcOptions);
        FirebaseApp.initializeApp(this);
        AdvancedPerformanceTracker.getInstance().attachToApplication(this);
        lifecycleObserver = new AppLifecycleObserver();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
    }

    public static boolean isAppInForeground() {
        return lifecycleObserver.isAppInForeground();
    }
}
