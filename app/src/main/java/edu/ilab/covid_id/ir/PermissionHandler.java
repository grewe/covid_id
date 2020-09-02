package edu.ilab.covid_id.ir;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Process;

import com.flir.thermalsdk.log.ThermalLog;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Encapsulated the asking of permission in Android, the class has to be used by an Activity.
 * Support handling both permissions for Network, Bluetooth, Storage
 *
 * The Activity registered with this utility class has to implement "onRequestPermissionsResult(..)" and call this handlers "onRequestPermissionsResult(..)"
 *
 * For network usage add these in the Android manifest:
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
 * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
 * <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE"/>
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 * <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>
 * <uses-permission android:name="android.permission.INTERNET"/>
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
 *
 * For Storage usage add these in the Android manifest:
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
 *
 * For Bluetooth usage add these in the Android manifest:
 *   <uses-permission android:name="android.permission.BLUETOOTH" />
 *   <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
 *   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 *
 */
public class PermissionHandler {

    private static final String TAG = "PermissionHandler";
    private final ConnectFlirActivity connectFlirActivity;

    ConnectFlirActivity.ShowMessage showMessage;

    @VisibleForTesting
    static String[] PERMISSIONS_FOR_NW_DISCOVERY = {
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @VisibleForTesting
    static String[] PERMISSIONS_FOR_STORAGE_DISCOVERY = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @VisibleForTesting
    static String[] PERMISSIONS_FOR_BLUETOOTH = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    public PermissionHandler(ConnectFlirActivity.ShowMessage showMessage, ConnectFlirActivity connectFlirActivity) {
        this.showMessage = showMessage;
        this.connectFlirActivity = connectFlirActivity;
    }

    /**
     * Check if we have Network permission, if not it shows a dialog requesting the permission and "onRequestPermissionsResult(..)" is called with the result
     *
     * @return TRUE if all permission was given else FALSE
     */
    public boolean checkForNetworkPermission(){
        return checkForPermission(PERMISSIONS_FOR_NW_DISCOVERY);
    }

    /**
     * Check if we have Storage permission, if not it shows a dialog requesting the permission and "onRequestPermissionsResult(..)" is called with the result
     *
     * @return TRUE if all permission was given else FALSE
     */
    public boolean checkForStoragePermission(){
        return checkForPermission(PERMISSIONS_FOR_STORAGE_DISCOVERY);
    }

    /**
     * Check if we have Bluetooth permission, if not it shows a dialog requesting the permission and "onRequestPermissionsResult(..)" is called with the result
     *
     * @return TRUE if all permission was given else FALSE
     */
    public boolean checkForBluetoothPermission(){
        return checkForPermission(PERMISSIONS_FOR_BLUETOOTH);
    }

    /**
     * Handles the information from a request Permission dialog, has to be called by the associated PermissionHandler Activity eg:
     *
     * Activity:onRequestPermissionsResult(...) { permissionHandler.onRequestPermissionsResult(...); }
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length <= 0) {
            showMessage.show("Permission request was canceled");
            return;
        }
        boolean permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

        String requestPermissionName = getRequestPermissionName(requestCode);

        // If request is cancelled, the result arrays are empty.
        if (permissionGranted) {
            // permission was granted, jippie!
            showMessage.show(requestPermissionName + " permission was granted");
        } else {
            // permission denied,
            showMessage.show(requestPermissionName + " permission was denied");
        }
        return;
    }

    /**
     * Check if we have permissions, if not it shows a dialog requesting the permission and "onRequestPermissionsResult(..)" is called with the result
     *
     * @return TRUE if all permission was given else FALSE
     */
    private boolean checkForPermission(String[] permissions){
        for (String permission : permissions) {
            if ( ! checkPermission(permission) ) {
                requestPermission(permission);
                return false;
            }
        }
        return true;
    }

    /**
     * Request permission show a dialog of the permission was not already granted
     * */
    private void requestPermission(final String permission) {
        ThermalLog.d(TAG,"requestPermission(), permission:"+permission);
        boolean permissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(connectFlirActivity, permission);
        if (permissionRationale) {
            showMessage.show("Please provide permission:"+permission);
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
        } else {
            // No explanation needed; request the permission
            int requestCode = getRequestCode(permission);
            String[] permissions = {permission};
            ActivityCompat.requestPermissions(connectFlirActivity, permissions, requestCode);
        }
    }

    /**
     * Get the permission request code for a permission
     *
     * @return -1 if permission can't be found otherwise a unique nr for the permission
     * */
    @VisibleForTesting
    int getRequestCode(String permission) {
        int nwLength = PERMISSIONS_FOR_NW_DISCOVERY.length;
        int storageLength = PERMISSIONS_FOR_STORAGE_DISCOVERY.length;
        int btLength = PERMISSIONS_FOR_BLUETOOTH.length;

        //Network
        for (int i = 0; i < nwLength; i++) {
            if (permission.equals(PERMISSIONS_FOR_NW_DISCOVERY[i])) {
                return i;
            }
        }

        //Network + Storage
        for (int i = 0; i < storageLength; i++) {
            if (permission.equals(PERMISSIONS_FOR_STORAGE_DISCOVERY[i])) {
                return i+nwLength;
            }
        }

        //Network + Storage + BT
        for (int i = 0; i < btLength; i++) {
            if (permission.equals(PERMISSIONS_FOR_BLUETOOTH[i])) {
                return i+nwLength+storageLength;
            }
        }
        return -1;
    }

    /**
     * Get the permission name matching the request permissionCode
     *
     * @return null if permission can't be found otherwise the name of the permission is returned
     * */
    @VisibleForTesting
    String getRequestPermissionName(int permissionCode) {
        int nwLength = PERMISSIONS_FOR_NW_DISCOVERY.length;
        int storageLength = PERMISSIONS_FOR_STORAGE_DISCOVERY.length;
        int btLength = PERMISSIONS_FOR_BLUETOOTH.length;

        if (permissionCode < nwLength) {
            return PERMISSIONS_FOR_NW_DISCOVERY[permissionCode];
        }else if (permissionCode < nwLength + storageLength) {
            return PERMISSIONS_FOR_STORAGE_DISCOVERY[permissionCode-nwLength];
        }else if (permissionCode <= nwLength + storageLength + btLength) {
            return PERMISSIONS_FOR_BLUETOOTH[permissionCode-nwLength-storageLength];
        }
        return null;
    }

    private boolean checkPermission(final String permission){
        ThermalLog.d(TAG,"checkPermission(), permission: "+permission);
        int checkPermission = connectFlirActivity.checkPermission(permission, Process.myPid(), Process.myUid());

        if (checkPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ThermalLog.d(TAG,"checkPermission Not granted "+permission);
        return false;
    }


}
