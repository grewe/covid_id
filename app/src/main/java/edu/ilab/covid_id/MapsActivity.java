package edu.ilab.covid_id;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import edu.ilab.covid_id.auth.LoginActivity;
import edu.ilab.covid_id.classification.ClassifierActivity;
import edu.ilab.covid_id.data.FirestoreHelper;
import edu.ilab.covid_id.localize.DetectorActivity;


/**
 * Main Launched Activity that contains our Map for Covid ID project
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    /**
     * Initializes a single helper to be used by all activities
     */
    public static FirestoreHelper myFirestoreHelper;

    /**
     * map used in display
     */
    private GoogleMap mMap;


    /**
     * Firebase Storage used to store image files
     */
    public static FirebaseStorage storageFirebase;


    /**
     * Create a storage reference from our app
     */
    public static StorageReference storageFirebaseRef;

    /**
     * Create a child reference ---imagesRef now points to "images"
     */
    public static StorageReference imagesFirebaseRef;

    /**
     * current location of the user (Lat, long)
     */
    public static Location currentLocation;

    /**
     * object to handle Location updates/changes from user
     */
    FusedLocationProviderClient fusedLocationProviderClient;

    /**
     * Buttons used to Launch Classification Activities
     */
    Button flowersClassificationActivityButton;

    /**
     * Handle to the detector activity button
     */
    Button exampleDetectorActivityButton;

    /**
     * Handle to the login/logout button
     */
    Button loginButton;

    /**
     * Handle to the IR activity launching button
     */
    Button IRButton;

    /**
     * Activities to perform different kinds of Classification
     */
    ClassifierActivity flowersClassifierActivity;

    /**
     * Activity to perform localition (detection)
     */
    DetectorActivity exampleDetectorActivity;

    private static final int REQUEST_CODE = 101;



    /**
     * used to store any app preferences and persistent data like the last record storage timestamp
     * for MaskRecord, FeverRecord, CrowdRecord and SocDistRecord
     */
    public static SharedPreferences appPrefs;

    /**
     * following Timestamps (represented using System.getCurrentTimeInMillis()) indicate the last time that kind of record (i.e. maskRecord) was stored to FireStore
     */
    public static long maskRecordLastStoreTimestamp;
    public static long crowdRecordLastStoreTimestamp;
    public static long socDistRecordLastStoreTimestamp;
    public static long feverRecordLastStoreTimestamp;
    public static long covidRecordLastStoreTimestamp;  //for generic CovidRecord


    /**
     * variables representing deltas in time necessary to allow a new related record (i.e. MaskRecord) to be stored
     * represented in milliseconds
     */
    public static long deltaCovidRecordStoreTimeMS;
    public static long deltaMaskRecordStoreTimeMS;
    public static long deltaFeverRecordStoreTimeMS;
    public static long deltaCrowdRecordStoreTimeMS;
    public static long deltaSocDistRecordStoreTimeMS;



    /**
     * following Locations indicate the last Location that kind of record (i.e. maskRecord) was stored to FireStore
     */
    public static Location maskRecordLastStoreLocation;
    public static Location crowdRecordLastStoreLocation;
    public static Location socDistRecordLastStoreLocation;
    public static Location feverRecordLastStoreLocation;
    public static Location covidRecordLastStoreLocation;  //for generic CovidRecord


    /**
     * variables representing deltas in location distance necessary to allow a new related record (i.e. MaskRecord) to be stored
     * represented in ????
     */
    public static long deltaCovidRecordStoreLocationM;
    public static long deltaMaskRecordStoreLocationM;
    public static long deltaFeverRecordStoreLocationM;
    public static long deltaCrowdRecordStoreLocationM;
    public static long deltaSocDistRecordStoreLocationM;

    /**
     * flag to indicate that system is ready to store a new recognition result based on enough time elapsed
     * since last stored record OR user movement is great enough to have the record indicate it is in a new location
     */
    public static boolean flagStoreRecognitionResults =true;


    /**
     * flag to indicate if should store images to Firebase Storage
     */
    public static boolean flagStoreImageFiles = true;

    /**
     * email as specified when logging into Firebase by user
     */
    public static String userEmailFirebase;
    /**
     * ID associated with user in Firebase
     */
    public static String userIdFirebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //need fusedLocationProviderClient to utilize Location services from device.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //grab handles to the various buttons to launch different classification activities
        this.flowersClassificationActivityButton = (Button) findViewById(R.id.flowersClassificationButton);

        //create event handler for each classification button to launch the corresponding activity
        flowersClassificationActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Launch classifier --here stupid default flowers classifier
                Intent intent = new Intent("edu.ilab.covid_id.classification.ClassifierActivity");
                startActivity(intent);
            }
        });

        //grab handle to the example detector/localization Activity
        this.exampleDetectorActivityButton  = (Button) findViewById(R.id.objectDetectButton);
        //create event handler for the object Detetor to launch DetectorActvity
        exampleDetectorActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Launch classifier --here stupid default flowers classifier
                Intent intent = new Intent("edu.ilab.covid_id.localize.DetectorActivity");
                startActivity(intent);
            }
        });


        //grab handle to the launch the IRStaticDataExploreActivity
        //IMPORTANT:  CHANGE: later will chante to launch the runtime IRActivity
        this.IRButton  = (Button) findViewById(R.id.IRButton);
        //create event handler for the object Detetor to launch DetectorActvity
        IRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Launch classifier --here stupid default flowers classifier
                Intent intent = new Intent("edu.ilab.covid_id.ir.IRStaticDataExploreActivity");
                startActivity(intent);
            }
        });



        // initialize our db helper object
        myFirestoreHelper = new FirestoreHelper();


        //grab shared preferences associated with this app
        appPrefs =  getSharedPreferences("appPreferences", MODE_PRIVATE);  //associate storage with name "appPreferences"


        //retrieve for integers.xml the hard coded values for the delta distances  between record storage necessary
        deltaCovidRecordStoreLocationM = getApplicationContext().getResources().getInteger(R.integer.deltaCovidRecordStoreLocationM);
        deltaCrowdRecordStoreLocationM = getApplicationContext().getResources().getInteger(R.integer.deltaCrowdRecordStoreLocationM);
        deltaMaskRecordStoreLocationM = getApplicationContext().getResources().getInteger(R.integer.deltaMaskRecordStoreLocationM);
        deltaFeverRecordStoreLocationM = getApplicationContext().getResources().getInteger(R.integer.deltaFeverRecordStoreLocationM);
        deltaSocDistRecordStoreLocationM = getApplicationContext().getResources().getInteger(R.integer.deltaSocDistRecordStoreLocationM);

        //retrieve for integers.xml the hard coded values for the delta times between record storage necessary
        deltaCovidRecordStoreTimeMS = getApplicationContext().getResources().getInteger(R.integer.deltaCovidRecordStoreTimeMS);
        deltaCrowdRecordStoreTimeMS = getApplicationContext().getResources().getInteger(R.integer.deltaCrowdRecordStoreTimeMS);
        deltaMaskRecordStoreTimeMS = getApplicationContext().getResources().getInteger(R.integer.deltaMaskRecordStoreTimeMS);
        deltaFeverRecordStoreTimeMS = getApplicationContext().getResources().getInteger(R.integer.deltaFeverRecordStoreTimeMS);
        deltaSocDistRecordStoreTimeMS = getApplicationContext().getResources().getInteger(R.integer.deltaSocDistRecordStoreTimeMS);


        //retrieve any of the following fields if present: maskRecordLastStoreTimestamp, crowdReocrdLastStoreTimestamp, socDistRecordLastStoreTimestamp, feverRecordLastStoreTimestamp
        // will be -1 if not yet set
        covidRecordLastStoreTimestamp = appPrefs.getLong("covidRecordLastStoreTimestamp", -1);
        maskRecordLastStoreTimestamp = appPrefs.getLong("maskRecordLastStoreTimestamp", -1);
        feverRecordLastStoreTimestamp = appPrefs.getLong("feverRecordLastStoreTimestamp", -1);
        crowdRecordLastStoreTimestamp = appPrefs.getLong("crowdRecordLastStoreTimestamp", -1);
        socDistRecordLastStoreTimestamp = appPrefs.getLong("socDistRecordLastStoreTimestamp", -1);


        // connect to get instance of FirebaseStorage used to store image files
        storageFirebase = FirebaseStorage.getInstance();

        // Create a storage reference from our app
        storageFirebaseRef = storageFirebase.getReference();

        //create reference to images location
        imagesFirebaseRef = storageFirebaseRef.child("images");

    }

    /**
     * on start, set login button view
     */
    @Override
    protected void onStart() {
        super.onStart();
        setLoginButtonUI();
    }

    /**
     * Sets up the login/logout button to take user to google authentication service
     * ALso, if user already logged in grab their Firebase email and ID
     */
    private void setLoginButtonUI() {
        loginButton = findViewById(R.id.loginButton);
        // check if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // if the user is logged in, set button text to logout
        if (user != null) {
            //grab users Firebase email and userID previously entered
            MapsActivity.userEmailFirebase = user.getEmail();
            MapsActivity.userIdFirebase = user.getUid();
            loginButton.setText(R.string.logout);
        }
        else {
            loginButton.setText(R.string.login);
        }

        // set the listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // if the user is logged in, sign them out, set button text to login
                if (user != null) {
                    FirebaseAuth.getInstance().signOut();
                    loginButton.setText(R.string.login);
                }   // else take them to the login activity
                else {
                    goToLogin();
                }
            }
        });
    }




    /**
     * takes user to login activity (for use in login button onClick listener)
     */
    private void goToLogin() {
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
    }


    /**
     * before destropying app update the shared preferences with last stored record timestamps for each kind of record (i.e. maskRecord)
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //make sure to update the SharedPreferences so when app restarts it will now the last timestamp
        appPrefs.edit().putLong("maskRecordLastStoreTimestamp", maskRecordLastStoreTimestamp)
                .putLong("feverRecordLastStoreTimestamp",feverRecordLastStoreTimestamp)
                .putLong("crowdRecordLastStoreTimestamp",crowdRecordLastStoreTimestamp)
                .putLong("socDistRecordLastStoreTimestamp",socDistRecordLastStoreTimestamp)
                .putLong("covidRecordLastStoreTimestamp", covidRecordLastStoreTimestamp).apply();
    }

    public void updateMapLocation(Location location ){
        if (location != null) {

            Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            //make a marker for user's current location and set map to this location
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("You");
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5));
            mMap.addMarker(markerOptions);
            Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        /**
         * SUBHANGI, DIVYA, ROHAN
         * you will have a update callback for location and the ONLY thing you do in it is to set
         * this.currentLocation = newLocaiton you will retrieve from the LocationResults
         *
         * NOTE: ***investigate why a LocationResults recieves more than one location (getLocations() method)...wierd --should be only 1???
         * */

        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    MapsActivity.currentLocation = location;
                    updateMapLocation(location);

                }
            }

        };

        // confirm that either Fine or Coarse permissions are set for app and if not return --nothing can do.
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }

        //Permissions are granted so we can now grab the last known location to initialize the currentLocation and use it to
        // locate our map display
        // note: call to get last known location is asynchronous so response is done in the onSucces callback method
        //Task<Location> task = fusedLocationProviderClient.getLastLocation();
        fusedLocationProviderClient.requestLocationUpdates(getLocationRequest(), mLocationCallback,
                null /* Looper */);
        /*
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                    LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    //make a marker for user's current location and set map to this location
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("You");
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5));
                    mMap.addMarker(markerOptions);
                    Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_LONG).show();
                }
            }
        });*/
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }



}
