package com.jio.jiotalkie.performance;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
public class AdvancedPerformanceTracker {
    private static final String TAG = "PerformanceTracker";
    private static volatile AdvancedPerformanceTracker instance;

    private final Map<String, PerformanceMetric> metrics = new HashMap<>();

    public static class PerformanceMetric {
        String componentName;
        long startTime;
        String type;
        long threshold;
        boolean isCritical;

        public PerformanceMetric(String componentName, long startTime,
                                 String type, long threshold, boolean isCritical) {
            this.componentName = componentName;
            this.startTime = startTime;
            this.type = type;
            this.threshold = threshold;
            this.isCritical = isCritical;
        }
    }

    private final Application.ActivityLifecycleCallbacks activityCallbacks =
            new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                    TrackPerformance annotation = activity.getClass().getAnnotation(TrackPerformance.class);
                    if (annotation != null) {
                        trackStart(
                                activity.getClass().getSimpleName(),
                                "Activity",
                                annotation.threshold(),
                                annotation.critical()
                        );
                    }
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    trackEnd(activity.getClass().getSimpleName());
                }

                // Other methods omitted for brevity
                @Override public void onActivityStarted(@NonNull Activity activity) {}
                @Override public void onActivityPaused(@NonNull Activity activity) {}
                @Override public void onActivityStopped(@NonNull Activity activity) {}
                @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
                @Override public void onActivityDestroyed(@NonNull Activity activity) {}
            };

    private final FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks =
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentPreAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
                    super.onFragmentPreAttached(fm, f, context);
                    TrackPerformance annotation = f.getClass().getAnnotation(TrackPerformance.class);
                    if (annotation != null) {
                        trackStart(
                                f.getClass().getSimpleName(),
                                "Fragment",
                                annotation.threshold(),
                                annotation.critical()
                        );
                    }
                }

                @Override
                public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                    trackEnd(f.getClass().getSimpleName());
                }
            };

    private AdvancedPerformanceTracker() {}

    public static AdvancedPerformanceTracker getInstance() {
        if (instance == null) {
            synchronized (AdvancedPerformanceTracker.class) {
                if (instance == null) {
                    instance = new AdvancedPerformanceTracker();
                }
            }
        }
        return instance;
    }

    private void trackStart(String name, String type, long threshold, boolean isCritical) {
        long startTime = SystemClock.elapsedRealtime();
        metrics.put(name, new PerformanceMetric(name, startTime, type, threshold, isCritical));
        Log.d(TAG, type + " " + name + " tracking started");
    }

    private void trackEnd(String name) {
        PerformanceMetric metric = metrics.get(name);
        if (metric != null) {
            long endTime = SystemClock.elapsedRealtime();
            long duration = endTime - metric.startTime;

            logPerformanceResult(metric, duration);
            metrics.remove(name);
        }
    }

    private void logPerformanceResult(PerformanceMetric metric, long duration) {
        long timeInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        long remainingMillis = duration % 1000;

        String performance;
        if (duration <= metric.threshold / 2) {
            performance = "🟢 FAST";
        } else if (duration <= metric.threshold) {
            performance = "🟡 ACCEPTABLE";
        } else {
            performance = "🔴 SLOW";
        }

        String criticalTag = metric.isCritical ? "⚠️ CRITICAL PATH" : "";

        Log.d(TAG, String.format(
                "┌────────────────────────────────────────\n" +
                        "│ Performance Results\n" +
                        "│ Component: %s\n" +
                        "│ Type: %s\n" +
                        "│ Status: %s %s\n" +
                        "│ Duration: %d.%d s (%d ms)\n" +
                        "│ Threshold: %d ms\n" +
                        "└────────────────────────────────────────",
                metric.componentName, metric.type, performance, criticalTag,
                timeInSeconds, remainingMillis, duration, metric.threshold
        ));

        // Report to analytics if critical path or exceeds threshold
        if (metric.isCritical || duration > metric.threshold) {
            reportPerformanceIssue(metric, duration);
        }
    }

    private void reportPerformanceIssue(PerformanceMetric metric, long duration) {
        // Implement your analytics reporting here
        Log.w(TAG, "Performance issue detected: " + metric.componentName + " took " + duration + " ms");
    }

    public void attachToApplication(Application application) {
        application.registerActivityLifecycleCallbacks(activityCallbacks);
    }

    public FragmentManager.FragmentLifecycleCallbacks getFragmentLifecycleCallbacks() {
        return fragmentCallbacks;
    }
}