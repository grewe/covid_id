package edu.ilab.covid_id.data;

import android.location.Location;

import com.google.firebase.Timestamp;

/**
 * THis class deals with determining the status of the User relative to records made in the various Covid ID databases.
 * It contains mostly a series of static utility methods.  UNDER CONSTRUCTION
 */
public class UserRecordStatus {

/*
 if(maskRecordLastStoreTimestamp == -1){
            maskRecordLastStoreTimestamp = System.currentTimeMillis();

        }
        Long timestamp = System.getCurrentTimeInMillis()
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        prefs.edit().putLong("time", timestamp).commit();
        crowdReocrdLastStoreTimestamp =  socDistRecordLastStoreTimestamp, feverRecordLastStoreTimestamp

sharedPreferences.edit()
                .putLong("YourKey", timestamp)
                .apply();
 */





    public static boolean readyForNewRecordStorageBasedOnLocation(FirestoreHelper myFirestoreHelper, String userID,  String collectionName, Location location){
        boolean ready = false;

        //using MapsActivity.myFirebaseHelper query the collection =collectionName to retrieve for this userID the latest record storage
        //to determine if enough time has elapsed to store a new record
             //NOTE would be faster to simply store the last timestamp of when did a storage for appropriate database but, this would mean
             // the user could turn off the app and restart quickly and add new entries without the time between record storages being elapsed.
             // could SOLVE by doing a pull from database to have the timestamp for each kind of record storage for this USER/DEVICE stored.
             // otherwise need to also have authentication AND a userid associated with each USER.


        //

        return ready;

    }



}
