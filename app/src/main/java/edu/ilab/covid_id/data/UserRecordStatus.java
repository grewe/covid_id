package edu.ilab.covid_id.data;

import android.location.Location;

import com.google.firebase.Timestamp;

/**
 * THis class deals with determining the status of the User relative to records made in the various Covid ID databases.
 * It contains mostly a series of static utility methods.
 */
public class UserRecordStatus {






    public static boolean readyForNewRecordStorageBasedOnLocation(FirestoreHelper myFirestoreHelper, String userID,  String collectionName, Location location){
        boolean ready = false;

        //using MapsActivity.myFirebaseHelper query the collection =collectionName to retrieve for this userID the latest record storage
        //to determine if enough time has elapsed to store a new record
             //NOTE would be faster to simply store the last timestamp of when did a storage for appropriate database but, this would mean
             // the user could turn off the app and restart quickly and add new entries without the time between record storages being elapsed.


        //

        return ready;

    }



}
