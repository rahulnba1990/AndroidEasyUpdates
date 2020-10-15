
package com.androideasyupdates;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import android.graphics.Color;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.common.IntentSenderForResultStarter;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class RNAndroidEasyUpdatesModule extends ReactContextBaseJavaModule implements InstallStateUpdatedListener, LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private AppUpdateManager appUpdateManager;
    private final int DAYS_FOR_FLEXIBLE_UPDATE = 10;
    private final int UPDATE_REQUEST_CODE = 985;
    private Promise mPromise;
    private int updateType;

    private ActivityEventListener activityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (mPromise != null) {
                try {
                    if (requestCode == UPDATE_REQUEST_CODE) {
                        if (resultCode != RESULT_OK) {
                            mPromise.resolve("FAILED");
                        } else {
                            mPromise.resolve("COMPLETED");
                        }
                    }
                } catch (Exception e) {
                    mPromise.resolve("UPDATE_FAILED");
                    e.printStackTrace();
                }
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
    public void checkUpdateAvailability(String type, Promise promise) {
        // Creates instance of the manager.
        mPromise = promise;
        updateType = ("IMMEDIATE".equalsIgnoreCase(type)) ? AppUpdateType.IMMEDIATE : AppUpdateType.FLEXIBLE;
        appUpdateManager = AppUpdateManagerFactory.create(reactContext);
        appUpdateManager.registerListener(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.clientVersionStalenessDays() != null
                    && appUpdateInfo.clientVersionStalenessDays() > DAYS_FOR_FLEXIBLE_UPDATE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    IntentSenderForResultStarter intentSenderForResultStarter = new IntentSenderForResultStarter() {
                        @Override
                        public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {

                        }
                    };
                    /*appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE,
                            this.getCurrentActivity(),
                            UPDATE_REQUEST_CODE);*/
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE,
                            intentSenderForResultStarter,
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            } else {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE,
                                this.getCurrentActivity(), UPDATE_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        /*appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateType,
                                this.getCurrentActivity(), UPDATE_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            }
        });*/
    }

    @ReactMethod
    public void completeUpdate() {
        Toast.makeText(reactContext, "completeUpdate called", Toast.LENGTH_LONG).show();
        if (appUpdateManager != null) {
            appUpdateManager.completeUpdate();
        }
    }

    @Override
    public void onStateUpdate(InstallState state) {
        if (state.installStatus() == InstallStatus.DOWNLOADING) {
            long bytesDownloaded = state.bytesDownloaded();
            long totalBytesToDownload = state.totalBytesToDownload();
            //long percentage = (bytesDownloaded / totalBytesToDownload) * 100;
            WritableMap params = Arguments.createMap();
            String data = Long.valueOf(bytesDownloaded).toString() + "/" + Long.valueOf(totalBytesToDownload).toString();
            params.putString("updateProgress", data);
            sendEvent(reactContext, "APP_UPDATE", params);

        }
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate();
            WritableMap params = Arguments.createMap();
            params.putString("UPDATE_COMPLETED", "");
            sendEvent(reactContext, "APP_UPDATE", params);
        }
    }

    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar = Snackbar.make(Objects.requireNonNull(this.getCurrentActivity())
                .findViewById(android.R.id.content), "An update has just been downloaded.", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
        snackbar.setTextColor(Color.WHITE);
        snackbar.setActionTextColor(Color.YELLOW);
        final View snackBarView = snackbar.getView();
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackBarView.getLayoutParams();
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, 50);
        snackBarView.setLayoutParams(params);
        snackbar.show();
    }

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @Override
    public void onHostResume() {
        if (appUpdateManager != null) {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate();
                    mPromise.resolve("DOWNLOAD_COMPLETED");
                } else if (appUpdateInfo.installStatus() == InstallStatus.INSTALLED) {
                    if (appUpdateManager != null) {
                        appUpdateManager.unregisterListener(this);
                    }
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