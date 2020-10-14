
package com.reactlibrary;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

import static android.app.Activity.RESULT_OK;

public class RNAndroidEasyUpdatesModule extends ReactContextBaseJavaModule implements InstallStateUpdatedListener, LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private AppUpdateManager appUpdateManager;
    private final int DAYS_FOR_FLEXIBLE_UPDATE = 1;
    private final int UPDATE_REQUEST_CODE = 985;
    private Promise mPromise;
    private int updateType;

    private ActivityEventListener activityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            try {
                if (requestCode == UPDATE_REQUEST_CODE) {
                    if (resultCode != RESULT_OK) {
                        mPromise.resolve("FAILED");
                        System.out.println("InAppUpdate Update flow failed! Result code: " + resultCode);
                        // If the update is cancelled or fails,
                        // you can request to start the update again.
                    } else {
                        mPromise.resolve("COMPLETED");
                    }
                }
            } catch (Exception e) {
                if (mPromise != null) {
                    mPromise.resolve("UPDATE_FAILED");
                }
                e.printStackTrace();
            }
        }
    };

    public RNAndroidEasyUpdatesModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(activityEventListener);
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return "RNAndroidEasyUpdates";
    }

    @ReactMethod
    public void checkUpdateAvailability(String updateType, Promise promise) {
        // Creates instance of the manager.
        mPromise = promise;
        this.updateType = (updateType.equalsIgnoreCase("IMMEDIATE")) ? AppUpdateType.IMMEDIATE : AppUpdateType.FLEXIBLE;
        appUpdateManager = AppUpdateManagerFactory.create(this.reactContext);
        appUpdateManager.registerListener(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (appUpdateInfo.isUpdateTypeAllowed(this.updateType)) {
                    //Request immediate update
                    try {
                        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, this.updateType,
                                this.getCurrentActivity(), UPDATE_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    if (appUpdateInfo.clientVersionStalenessDays() != null
                            && appUpdateInfo.clientVersionStalenessDays() >= DAYS_FOR_FLEXIBLE_UPDATE) {
                        //Request flexible update
                        InstallStateUpdatedListener listener = state -> {
                            // (Optional) Provide a download progress bar.
                            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                                long bytesDownloaded = state.bytesDownloaded();
                                long totalBytesToDownload = state.totalBytesToDownload();
                                // Implement progress bar.
                            }
                            // Log state or install the update.
                        };
                        appUpdateManager.registerListener(this);
                    }
                }
            }
        });
    }

    @ReactMethod
    public void completeUpdate() {
        Toast.makeText(this.reactContext, "completeUpdate called", Toast.LENGTH_LONG).show();
        if (appUpdateManager != null) {
            appUpdateManager.completeUpdate();
        }
    }

    @Override
    public void onStateUpdate(InstallState state) {
        if (state.installStatus() == InstallStatus.DOWNLOADING) {
            long bytesDownloaded = state.bytesDownloaded();
            long totalBytesToDownload = state.totalBytesToDownload();
            // Implement progress bar.
            long percentage = (bytesDownloaded/totalBytesToDownload)*100;
            Toast.makeText(this.reactContext, "downloading: "+percentage+"%", Toast.LENGTH_LONG).show();
        }
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Toast.makeText(this.reactContext, "update installed restart app", Toast.LENGTH_LONG).show();
            mPromise.resolve("COMPLETED");
        }
    }

    @Override
    public void onHostResume() {
        if (appUpdateManager != null) {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    mPromise.resolve("COMPLETED");
                } else if (appUpdateInfo.installStatus() == InstallStatus.INSTALLED) {
                    if (appUpdateManager != null) {
                        appUpdateManager.unregisterListener(this);
                    }
                } else {
                    System.out.println("current state: " + appUpdateInfo.installStatus());
                }
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE,
                                this.getCurrentActivity(), UPDATE_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        if (appUpdateManager != null) {
            appUpdateManager.unregisterListener(this);
        }
    }
}