package edu.ilab.covid_id.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

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
    private float[] orientationAngles;

    /**
     * Default constructor with all values given
     */
    public CovidRecord(float risk, float certainty,
                       GeoPoint location, Timestamp timestamp, float[] orientationAngles, float altitude) {
        this.risk = risk;
        this.certainty = certainty;
        this.location = location;
        this.timestamp = timestamp;
        this.orientationAngles = orientationAngles;
        this.altitude = altitude;
    }

    /**
     * Default constructor with default values for orientation angles and altitude
     */
    public CovidRecord(float risk, float certainty, GeoPoint location, Timestamp timestamp) {
        this.risk = risk;
        this.certainty = certainty;
        this.location = location;
        this.timestamp = timestamp;
        this.orientationAngles = new float[3];
        this.orientationAngles[0] = 0.0f;
        this.orientationAngles[1] = 0.0f;
        this.orientationAngles[2] = 0.0f;
        this.altitude = -1.0f;
    }





    /*
    x Time Stamp
    x GeoPoint
    Altitude ?
    xRoll
    xPitch
    xYaw (Azimuth)
    x Certainty (0-100)
    x Risk (0-100)
    */

}
