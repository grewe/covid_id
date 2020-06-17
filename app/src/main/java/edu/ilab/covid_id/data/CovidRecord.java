package edu.ilab.covid_id.data;

import android.location.Location;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Map;

import edu.ilab.covid_id.MapsActivity;

/**
 * Base class for all Covid recognition modules to report Covid instance records
 * Known Subclasses:
 */
public class CovidRecord {
    /**
     * Represents the potential risk involved for this record from 0-100
     * (0: no risk, 100: high risk)
     */
    private float risk;

    /**
     * Represents certainty in risk measurement from 0-100
     * (0: completely uncertain, 100: completely certain)
     */
    private float certainty;

    /**
     * Represents a GeoPoint (latitude and longitude record) for a particular point in time
     */
    private GeoPoint location;

    /**
     * Represents the time that this record was taken
     */
    private Timestamp timestamp;

    /**
     * Contains URL to cloud storage containing image associated with this record
     */
    private String filenameURL;

    /**
     * Represents the estimated altitude (height) of the device when record was taken
     * -1.0: unknown altitude
     */
    private float altitude;

    /**
     * Represents the three rotational angles from a sensor measurement on the device
     * orientationAngles[0] = azimuth (z-axis)
     * orientationAngles[1] = pitch (x-axis)
     * orientationAngles[2] = roll (y-axis)
     */
    private ArrayList<Float> orientationAngles;

    /**
     * Represents the four corners of the bounding box
     * 0: x, upper left
     * 1: y, upper left
     * 2: x, lower right
     * 3: y, lower right
     */
    private ArrayList<Float> boundingBox;

    /**
     * For storing any misc info we might want to tack onto an object
     */
    private String info;


    /**
     * UserID retrieved after a Firebase Authentication of user
     */
    private String userIDFirebase;

    /**
     * User Email retrieved after a Firebase Authentication of user
     */
    private String userEmailFirebase;


    /**
     * Default constructor with null or impossible values.
     */
    public CovidRecord() {
        this.risk = -1.0f;
        this.certainty = -1.0f;
        this.location = null;
        this.timestamp = null;
        this.orientationAngles = null;
        this.altitude = -1.0f;
        this.filenameURL = null;
        this.info = null;
        this.boundingBox = null;
        this.userEmailFirebase = null;
        this.userIDFirebase = null;
    }

    /**
     *  constructor with all values given
     */
    public CovidRecord(float risk, float certainty, GeoPoint location, Timestamp timestamp,
                       String filenameURL, String info, ArrayList<Float> boundingBox, ArrayList<Float> orientationAngles, float altitude, String userEmailFirebase, String userIDFirebase) {
        this.risk = risk;
        this.certainty = certainty;
        this.location = location;
        this.timestamp = timestamp;
        this.orientationAngles = orientationAngles;
        this.altitude = altitude;
        this.filenameURL = filenameURL;
        this.info = info;
        this.boundingBox = boundingBox;
        this.userIDFirebase = userIDFirebase;
        this.userEmailFirebase = userEmailFirebase;
    }
    /**
     *  constructor with all values except user info given --ANONYMOUS storage
     */
    public CovidRecord(float risk, float certainty, GeoPoint location, Timestamp timestamp,
                       String filenameURL, String info, ArrayList<Float> boundingBox, ArrayList<Float> orientationAngles, float altitude) {
        this.risk = risk;
        this.certainty = certainty;
        this.location = location;
        this.timestamp = timestamp;
        this.orientationAngles = orientationAngles;
        this.altitude = altitude;
        this.filenameURL = filenameURL;
        this.info = info;
        this.boundingBox = boundingBox;
    }

    /**
     * Default constructor with default values for orientation angles and altitude
     */

    public CovidRecord(float risk, float certainty, GeoPoint location, Timestamp timestamp,
                       String filenameURL, String info, ArrayList<Float> boundingBox, String userEmailFirebase, String userIDFirebase) {
        this.risk = risk;
        this.certainty = certainty;
        this.location = location;
        this.timestamp = timestamp;
        this.orientationAngles = new ArrayList<Float>();
        this.orientationAngles.add(0, 0.0f);
        this.orientationAngles.add(1, 0.0f);
        this.orientationAngles.add(2, 0.0f);
        this.altitude = -1.0f;
        this.filenameURL = filenameURL;
        this.info = info;
        this.boundingBox = boundingBox;
        this.userEmailFirebase = userEmailFirebase;
        this.userIDFirebase = userIDFirebase;
    }

    /**
     * Default constructor with default values for orientation angles and altitude - ANONYMOUS
     */

    public CovidRecord(float risk, float certainty, GeoPoint location, Timestamp timestamp,
                       String filenameURL, String info, ArrayList<Float> boundingBox) {
        this.risk = risk;
        this.certainty = certainty;
        this.location = location;
        this.timestamp = timestamp;
        this.orientationAngles = new ArrayList<Float>();
        this.orientationAngles.add(0, 0.0f);
        this.orientationAngles.add(1, 0.0f);
        this.orientationAngles.add(2, 0.0f);
        this.altitude = -1.0f;
        this.filenameURL = filenameURL;
        this.info = info;
        this.boundingBox = boundingBox;
    }
    /**
     * Constructor that may be used for classification only
     * (default values for bounding box -1 on all coordinates)
     */
    public CovidRecord(float risk, float certainty, GeoPoint location, Timestamp timestamp,
                       String filenameURL, String info, ArrayList<Float> orientationAngles, float altitude,String userEmailFirebase, String userIDFirebase) {
        this.risk = risk;
        this.certainty = certainty;
        this.location = location;
        this.timestamp = timestamp;
        this.orientationAngles = orientationAngles;
        this.altitude = altitude;
        this.filenameURL = filenameURL;
        this.info = info;
        this.boundingBox = new ArrayList<Float>();
        this.boundingBox.add(0, -1.0f);
        this.boundingBox.add(1, -1.0f);
        this.boundingBox.add(2, -1.0f);
        this.boundingBox.add(3, -1.0f);
        this.userIDFirebase = userIDFirebase;
        this.userEmailFirebase = userEmailFirebase;
    }
    /**
     * Constructor that may be used for classification only
     * (default values for bounding box -1 on all coordinates)  -- ANONYMOUS
     */
    public CovidRecord(float risk, float certainty, GeoPoint location, Timestamp timestamp,
                       String filenameURL, String info, ArrayList<Float> orientationAngles, float altitude) {
        this.risk = risk;
        this.certainty = certainty;
        this.location = location;
        this.timestamp = timestamp;
        this.orientationAngles = orientationAngles;
        this.altitude = altitude;
        this.filenameURL = filenameURL;
        this.info = info;
        this.boundingBox = new ArrayList<Float>();
        this.boundingBox.add(0, -1.0f);
        this.boundingBox.add(1, -1.0f);
        this.boundingBox.add(2, -1.0f);
        this.boundingBox.add(3, -1.0f);
    }

    public float getRisk() {
        return risk;
    }

    public void setRisk(float risk) {
        this.risk = risk;
    }

    public float getCertainty() {
        return certainty;
    }

    public void setCertainty(float certainty) {
        this.certainty = certainty;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getFilenameURL() {
        return filenameURL;
    }

    public void setFilenameURL(String filenameURL) {
        this.filenameURL = filenameURL;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public ArrayList<Float> getOrientationAngles() {
        return orientationAngles;
    }

    public void setOrientationAngles(ArrayList<Float> orientationAngles) {
        this.orientationAngles = orientationAngles;
    }

    public ArrayList<Float> getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(ArrayList<Float> boundingBox) {
        this.boundingBox = boundingBox;
    }



    public String getUserIDFirebase(){ return userIDFirebase; }

    public void setUserIDFirebase(String userIDFirebase) { this.userIDFirebase = userIDFirebase;}

    public String getUserEmailFirebase() { return userEmailFirebase;}

    public void setUserEmailFirebase(String userEmailFirebase) { this.userEmailFirebase = userEmailFirebase;}


    /**
     * determines if we are ready to store a new record based on either location moved or time duration since last record storage is enough
     * @return true if ready to store next record, false otherwise
     * @param lastStoredTimeMS time in MS of last stored record of this type
     * @param deltaTimeMS delta time needed to have elapsed since last storage before ready to store next record
     * @param lastStoredLocation This is location of the last stored record of this type
     * @param currentLocation This is the lcoation that is updated and represents the current location of this device (should be same as MapsActivity.currentLcoation
     */
    public static boolean readyStoreRecord(long lastStoredTimeMS, long deltaTimeMS, Location lastStoredLocation, Location currentLocation, long deltaLocationM ){

        //first test if ANY CovidRecord has been stored, if not then say yes!
        if( lastStoredTimeMS == -1) //nothing has been store yet
            return true;

        //based on time we are ready to store new record
        if (Math.abs(lastStoredTimeMS- System.currentTimeMillis()) > deltaTimeMS)
            return true;


        //SUBHANGI, DIVYA, ROHAN - add test for location change being large enough- OR ENOUGH TIME elapsed
        /*
        replace ABOVE
        //Distance is calcuated on the globe using 2 Lat/Long values
        if (Math.abs(lastStoredTimeMS- System.currentTimeMillis()) > deltaTimeMS  || (Distance(lastStoredLcation, currentLocation) > deltaLocationM)
           return true;

         */


        return false;

    }



    /**
     * determines if we are ready to store a new record based on ONLY time duration since last record storage is enough
     * @return true if ready to store next record, false otherwise
     * @param lastStoredTimeMS time in MS of last stored record of this type
     * @param deltaTimeMS delta time needed to have elapsed since last storage before ready to store next record
     */
    public static boolean readyStoreRecord(long lastStoredTimeMS, long deltaTimeMS){

        //first test if ANY CovidRecord has been stored, if not then say yes!
        if( lastStoredTimeMS == -1) //nothing has been store yet
            return true;

        //based on time we are ready to store new record
        if (Math.abs(lastStoredTimeMS- System.currentTimeMillis()) > deltaTimeMS)
            return true;


        //SUBHANGI, DIVYA, ROHAN - add test for location change being large enough---do like time.
        /*
        replace ABOVE
        //Distance is calcuated on the globe using 2 Lat/Long values
        if (Math.abs(lastStoredTimeMS- System.currentTimeMillis()) > deltaTimeMS  && (Distance(lastStoredLcation, currentLocation) > deltaLocationM)
           return true;

         */


        return false;

    }
}
