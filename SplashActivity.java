package com.jio.jiotalkie.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.db.JioTalkieDatabaseRepository;
import com.jio.jiotalkie.performance.TrackPerformance;
import com.jio.jiotalkie.util.CommonUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

@TrackPerformance(threshold = 500, critical = false)
public class SplashActivity extends AppCompatActivity {
    private static String TAG = SplashActivity.class.getName();

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 2;
    private static final int PERMISSIONS_REQUEST_LOCATION = 3;
    private static final int PERMISSIONS_REQUEST_STORAGE = 4;
    private static final int PERMISSIONS_REQUEST_PHONE_STATE = 5;
    private static final int PERMISSIONS_REQUEST_GEOFENCES = 6;

    private boolean mPermPostNotificationsAsked = false;
    private boolean isNeedToRequestPermissionAgain = false;
    private JioTalkieDatabaseRepository mDatabase;
    private JioTalkieSettings mJioTalkieSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mDatabase = new JioTalkieDatabaseRepository(this.getApplication()); // TODO add support for cloud storage
        //mDatabase.open();
        mJioTalkieSettings = JioTalkieSettings.getInstance(this.getApplication());
        checkRequiredPermission();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNeedToRequestPermissionAgain) {
            isNeedToRequestPermissionAgain = false;
            checkRequiredPermission();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private void checkRequiredPermission() {
        if (checkPhonePermission() && checkAudioPermission() && checkReadWriteStoragePermission() && checkNotificationPermission() && checkLocationPermission() && checkGeofencesPermission()) {
            new Handler().postDelayed(this::startNextActivity, 1000);
        }
    }

    private boolean checkPhonePermission() {
        if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST_PHONE_STATE);
            return false;
        } else {
            return true;
        }
    }

    private boolean checkLocationPermission() {
        boolean granted = false;
        if ((ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
        } else {
            granted = true;
        }
        return granted;
    }

    private boolean checkGeofencesPermission() {
            boolean granted = false;
            if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.background_location_permission_message, Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSIONS_REQUEST_GEOFENCES);
            } else {
                granted = true;
            }
            return granted;
    }

    private boolean checkNotificationPermission() {
        boolean granted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!mPermPostNotificationsAsked) {
                if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
                } else {
                    granted = true;
                }
            } else {
                granted = true;
            }
        } else {
            granted = true;
        }
        return granted;
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return false;
        } else {
            return true;
        }
    }

    private boolean checkReadWriteStoragePermission() {
        String[] permission = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else permission = new String[]{Manifest.permission.READ_MEDIA_AUDIO};
        } else {
            if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }

        ActivityCompat.requestPermissions(SplashActivity.this, permission, PERMISSIONS_REQUEST_STORAGE);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0) {
            return;
        }

        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO:
            case PERMISSIONS_REQUEST_PHONE_STATE:
                Log.d(TAG, "PERMISSIONS_REQUEST_RECORD_AUDIO..");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startNextActivity();
                } else {
                    showPermissionAlertDialog();
                }
                break;
            case PERMISSIONS_REQUEST_POST_NOTIFICATIONS:
                Log.d(TAG, "PERMISSIONS_REQUEST_POST_NOTIFICATIONS..");
                mPermPostNotificationsAsked = true;
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    // This is inspired by https://stackoverflow.com/a/34612503
                    if (ActivityCompat.shouldShowRequestPermissionRationale(SplashActivity.this, Manifest.permission.POST_NOTIFICATIONS)) {
                        Toast.makeText(SplashActivity.this, getString(R.string.permission_request_notifications), Toast.LENGTH_LONG).show();
                    }
                }
                startNextActivity();
                break;
            case PERMISSIONS_REQUEST_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startNextActivity();
                } else {
                    showPermissionAlertDialog();
                }
                break;
            case PERMISSIONS_REQUEST_STORAGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        startNextActivity();
                    } else {
                        showPermissionAlertDialog();
                    }
                } else {
                    if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        startNextActivity();
                    } else {
                        showPermissionAlertDialog();
                    }
                }
                break;
            case PERMISSIONS_REQUEST_GEOFENCES:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startNextActivity();
                } else {
                    showBackgroundLocationPermissionDialog();
                }
        }
    }

    private void startNextActivity() {
        Log.d(TAG, "StartNextActivity .. checkAudioPermission() " + checkAudioPermission() + " checkNotificationPermission() =" + checkNotificationPermission());
        if (checkAudioPermission() && checkReadWriteStoragePermission() && (mPermPostNotificationsAsked || checkNotificationPermission()) && checkLocationPermission() && checkGeofencesPermission()) {
            if (CommonUtils.isNetworkAvailable(this)) {
                Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            } else {
                showNoNetworkDialog();
            }
        }
    }

    private void showNoNetworkDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this,R.style.Theme_Alert_Rounded);
        alertBuilder.setTitle(R.string.internet_unavailable);
        alertBuilder.setMessage(R.string.no_internet_msg);
        alertBuilder.setPositiveButton(R.string.ok_got_it, (dialog, which) -> finish());
        alertBuilder.show();
    }

    private void showPermissionAlertDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(R.string.grant_app_permission);

        alertBuilder.setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isNeedToRequestPermissionAgain = true;
                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
            }
        });
        alertBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });

        alertBuilder.show();
    }

    private void showBackgroundLocationPermissionDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(R.string.background_location_access_title);
        alertBuilder.setMessage(R.string.background_location_permission_message);
        alertBuilder.setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isNeedToRequestPermissionAgain = true;
                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
            }
        });
        alertBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });

        alertBuilder.show();
    }
}