package edu.ilab.covid_id;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import edu.ilab.covid_id.auth.LoginActivity;
import edu.ilab.covid_id.classification.ClassifierActivity;
import edu.ilab.covid_id.data.FirestoreHelper;
import edu.ilab.covid_id.localize.DetectorActivity;


/**
 * Main Launched Activity that contains our Map for Covid ID project --
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MAPS_ACTIVITY";
    /**
     * helper for fire store access
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
    private FusedLocationProviderClient fusedLocationProviderClient;

    /**
     * Buttons used to Launch Classification Activities
     */
    private Button flowersClassificationActivityButton;

    /**
     * Handle to the detector activity button
     */
    private Button exampleDetectorActivityButton;

    /**
     * Handle to the login/logout button
     */
    private Button loginButton;

    /**
     * Handle to track location button
     */
    private Button trackLocationButton;

    /**
     * Handle to the IR activity launching button
     */
    private Button IRButton;

    /**
     * Handle to the button which refreshes markers
     */
    private Button refreshMarkersButton;

    /**
     * button to expand / collapse settings
     */
    private FloatingActionButton expandCollapseSettingsButton;

    /**
     * layout where settings buttons live
     */
    private LinearLayout settingsLayout;


    //private ArrayList<MarkerData> markersData;

    /**
     * for firebase (I think)
     */
    private static final int REQUEST_CODE = 101;

    /**
     * used to store any app preferences and persistent data like the last record storage timestamp
     * for CovidRecord
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
     * variables representing deltas in time necessary to allow a new related CovidRecord  related
     * to the corresponding module (Mask, Crowd, IR, Soc Dist)to be stored
     * represented in milliseconds
     */
    public static long deltaCovidRecordStoreTimeMS;
    public static long deltaMaskRecordStoreTimeMS;
    public static long deltaFeverRecordStoreTimeMS;
    public static long deltaCrowdRecordStoreTimeMS;
    public static long deltaSocDistRecordStoreTimeMS;

    /**
     * following Locations indicate the last Location the CovidRecord related to the corresponding
     * module (Mask, Crowd, IR, Soc Dist) was stored to FireStore
     */
    public static Location maskRecordLastStoreLocation;
    public static Location crowdRecordLastStoreLocation;
    public static Location socDistRecordLastStoreLocation;
    public static Location feverRecordLastStoreLocation;
    public static Location covidRecordLastStoreLocation;  //for generic CovidRecord

    /**
     * variables representing deltas in location distance necessary to allow a new related
     * CovidRecord (i.e. type: "mask") to be stored represented in meters
     */
    public static long deltaCovidRecordStoreLocationM;
    public static long deltaMaskRecordStoreLocationM;
    public static long deltaFeverRecordStoreLocationM;
    public static long deltaCrowdRecordStoreLocationM;
    public static long deltaSocDistRecordStoreLocationM;

    /**
     * if user is in live tracking mode, how zoomed in do we want to be (larger is more zoomed in)
     */
    public static float trackingZoomSize = 20.0f;

    /**
     * flag to indicate that system is ready to store a new recognition result based on enough time elapsed
     * since last stored record OR user movement is great enough to have the record indicate it is in a new location
     */
    public static boolean flagStoreRecognitionResults = true;

    /**
     * flag to indicate if map should track user's location live (false by default)
     */
    public static boolean trackLocation = false;

    /**
     * flag to indicate if should store images to Firebase Storage
     */
    public static boolean flagStoreImageFiles = true;

    /**
     * flag to indicate if location should be toasted to user whenever new location is detected
     */
    public static boolean TOAST_LOCATION = false;

    /**
     * flag for if settings are expanded or not (false by default)
     */
    public static boolean expand_settings = false;

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

        initFirebase(); // initialize various objects for communicating with the firebase backend
        initMap();  // initialize map fragment and fusedLocationProviderClient
        initViewHooks(); // initialize hooks to views on screen
        initButtonListeners();  // initialize listeners for all buttons on screen
        initConstants();    // initialize constants from values/integers.xml and shared preferences
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
     * before destroying app update the shared preferences with last stored record timestamps for each kind of record (i.e. maskRecord)
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //make sure to update the SharedPreferences so when app restarts it will now the last timestamp
        appPrefs.edit().putLong("maskRecordLastStoreTimestamp", maskRecordLastStoreTimestamp)
                .putLong("feverRecordLastStoreTimestamp", feverRecordLastStoreTimestamp)
                .putLong("crowdRecordLastStoreTimestamp", crowdRecordLastStoreTimestamp)
                .putLong("socDistRecordLastStoreTimestamp", socDistRecordLastStoreTimestamp)
                .putLong("covidRecordLastStoreTimestamp", covidRecordLastStoreTimestamp).apply();
    }

    /**
     * initialize map fragment and fusedLocationProviderClient
     */
    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        //need fusedLocationProviderClient to utilize Location services from device.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /**
     * initialize various objects for communicating with the firebase backend
     */
    private void initFirebase() {
        // init firebase helper
        myFirestoreHelper = new FirestoreHelper();
        // connect to get instance of FirebaseStorage used to store image files
        storageFirebase = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        storageFirebaseRef = storageFirebase.getReference();
        //create reference to images location
        imagesFirebaseRef = storageFirebaseRef.child("images");
    }

    /**
     * initialize constants like maskRecordLastStoreTimestamp and deltaCrowdRecordStoredTimeMS
     * by querying res/values/integers.xml as well as shared preferences
     */
    private void initConstants() {
        //grab shared preferences associated with this app
        appPrefs = getSharedPreferences("appPreferences", MODE_PRIVATE);  //associate storage with name "appPreferences"

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
    }

    /**
     * initialize local hooks to all views on the screen we may need to mutate/utilize
     */
    private void initViewHooks() {
        //grab handles to the various buttons to launch different classification activities
        this.flowersClassificationActivityButton = (Button) findViewById(R.id.flowersClassificationButton);
        //grab handle to the example detector/localization Activity
        this.exampleDetectorActivityButton = (Button) findViewById(R.id.objectDetectButton);
        //grab handle to the launch the IRStaticDataExploreActivity
        this.IRButton = (Button) findViewById(R.id.IRButton);
        //grab handle to the FAB for expanding the settings
        this.expandCollapseSettingsButton = findViewById(R.id.expand_settings_button);
        //grab handle to expandible settings layout
        this.settingsLayout = findViewById(R.id.collapsible_button_layout);
        //grab handle to track location button
        this.trackLocationButton = findViewById(R.id.track_location_button);
        //grab handle to the refresh markers button
        this.refreshMarkersButton = findViewById(R.id.refresh_markers_button);
    }

    /**
     * initialize on click listeners to all the buttons on screen
     */
    private void initButtonListeners() {
        //create event handler for each classification button to launch the corresponding activity
        flowersClassificationActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Launch classifier --here stupid default flowers classifier
                Intent intent = new Intent("edu.ilab.covid_id.classification.ClassifierActivity");
                startActivity(intent);
            }
        });
        //create event handler for the object Detetor to launch DetectorActvity
        exampleDetectorActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Launch classifier --here stupid default flowers classifier
                Intent intent = new Intent("edu.ilab.covid_id.localize.DetectorActivity");
                startActivity(intent);
            }
        });
        //create event handler for the object Detetor to launch DetectorActvity
        IRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Launch classifier --here stupid default flowers classifier
                Intent intent = new Intent("edu.ilab.covid_id.ir.ConnectFlirActivity");
                startActivity(intent);
            }
        });
        // create event handler for FAB settings button
        expandCollapseSettingsButton.setAlpha(0.85f);
        settingsLayout.setVisibility(expand_settings ? View.VISIBLE : View.GONE);
        expandCollapseSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expand_settings = !expand_settings;
                settingsLayout.setVisibility(expand_settings ? View.VISIBLE : View.GONE);
            }
        });
        // create event handler for tracking button
        trackLocationButton.setText("Track");
        trackLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trackLocation = !trackLocation;
                trackLocationButton.setText(trackLocation ? "Stop Track" : "Track");
            }
        });
        // create event handler for refresh markers button to populate map with markers
        refreshMarkersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                populateMap();
            }
        });
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
        } else {
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
     * used to update the map location
     * @param location
     */
    public void updateMapLocation(Location location) {
        if (location != null) {
            // toast location if flag is set to do so
            if (TOAST_LOCATION) {
                Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            }
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            //make a marker for user's current location and set map to this location
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("You");
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5));
            mMap.addMarker(markerOptions);
            // toast location if flag is set to do so
            if (TOAST_LOCATION) {
                Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_LONG).show();
            }
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
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;   // init handle to google map object
        mMap.setMyLocationEnabled(true);    // set location enabled
        UiSettings mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);

        /*
         * SUBHANGI, DIVYA, ROHAN
         * you will have a update callback for location and the ONLY thing you do in it is to set
         * this.currentLocation = newLocaiton you will retrieve from the LocationResults
         *
         * NOTE: ***investigate why a LocationResults receives more than one location (getLocations() method)...weird --should be only 1???
         */

        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // if user is live tracking, we reset to track user
                    if(trackLocation) {
                        Log.d("MY_MAP", "Updating map location");
                        // MapsActivity.currentLocation = location;
                        // updateMapLocation(location);

                        //Move the camera to the user's location and zoom in!
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), trackingZoomSize));
                    }
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

    /**
     * pulls records from the firebase and uses them to populate map
     */
    private void populateMap() {
        // pull records from firebase according to certain conditions

        // pass those records into a method which returns a list of google map markers

        // erase the current markers and add these new markers to the map

    }

    /**
     * create listener to pass to query completion for populating map
     * @return
     */
    private OnCompleteListener<QuerySnapshot> getPopulateMapListener() {
        OnCompleteListener<QuerySnapshot> listener = new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(TAG, document.getId() + " => " + document.getData());

                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        };
        return listener;
    }

    /**
     * used to get a location request
     * @return - location request
     */
    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }
}
