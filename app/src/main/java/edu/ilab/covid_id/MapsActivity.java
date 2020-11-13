package edu.ilab.covid_id;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;

import edu.ilab.covid_id.auth.LoginActivity;
import edu.ilab.covid_id.data.CovidRecord;
import edu.ilab.covid_id.data.FirestoreHelper;


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
     * Handle to the mask activity launching button
     */
    private Button maskButton;

    /**
     * Handle to the social distancing activity launching button
     */
    private Button socDistButton;

    /**
     * Handle to the crowd activity launching button
     */
    private Button crowdButton;

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

    /**
     * list populated by firestore query in populateMap() method
     */
    private ArrayList<CovidRecord> queryRecords = null;

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
     * variables representing risk thresholds from 0 to 100 for the three stages of high, caution,
     * and low, read in from integers.xml. NOTE: everything below 'caution' value will be considered
     * low, and between low and high will be 'caution', everything above 'high' is considered high
     * risk. NOTE: this will influence the choice of icons used in visualization
     */
    public static int riskThresholdHigh_IR;
    public static int riskThresholdCaution_IR;

    public static int riskThresholdHigh_Crowd;
    public static int riskThresholdCaution_Crowd;

    public static int riskThresholdHigh_SocDist;
    public static int riskThresholdCaution_SocDist;

    public static int riskThresholdHigh_Mask;
    public static int riskThresholdCaution_Mask;

    public static int riskThresholdHigh_Covid;
    public static int riskThresholdCaution_Covid;

    /**
     * width of earth in meters (used to calculate size of map display area in meters)
     */
    public static final double EARTH_WIDTH_M = 12742000.0;

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

    /**
     * limit for max number of results query from firebase can return
     */
    public static int QUERY_LIMIT = 1000;

    /**
     * threshold for how long back to search in time when querying for covid records in milliseconds
     *  currently: 1 hour = 1000 ms * 60 s * 60 min
     */
    public static long timeOffsetMS = 1000 * 60 * 60;

    /**
     * threshold for how long back to search in time when querying for covid records in milliseconds
     *  currently: 15 days = 1000 ms * 60 s * 60 min * 24 hrs * 15 days
     */
    public static long timeOffsetMSDiagnostic = 1000 * 60 * 60 * 24 * 15;

    /**
     * flag for whether we want to run in diagnostic mode or not
     */
    public static boolean DIAGNOSTIC_MODE = true;

    // TODO: calculate these values rather than hard coding
    /**
     * stores height of map in meters
     */
    private double mapHeightM = 200;

    /**
     * stores height of map in meters
     */
    private double mapWidthM = 200;

    /**
     * stores height of map in pixels
     */
    private int mapHeightPx = -1;

    /**
     * stores width of map in pixels
     */
    private int mapWidthPx = -1;

    /**
     * stores height of map in digital pixels
     */
    private int mapHeightDP = -1;

    /**
     * stores width of map in digital pixels
     */
    private int mapWidthDP = -1;

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

        // get fragment width and height
        Display display = mapFragment.getActivity().getWindowManager().getDefaultDisplay();
        mapHeightPx = display.getHeight();
        mapWidthPx = display.getWidth();

        // TODO: convert to digital pixels
        mapHeightDP = convertPixelsToDp(mapHeightPx, MapsActivity.this);
        mapWidthDP = convertPixelsToDp(mapWidthPx, MapsActivity.this);

        Log.d("LOCATION_QUERY", "mapheightpx: " + mapHeightPx + ", mapwidthpx: " + mapWidthPx);

        //need fusedLocationProviderClient to utilize Location services from device.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return An int value to represent dp equivalent to px value
     */
    public static int convertPixelsToDp(float px, Context context){
        Log.d("LOCATION_QUERY", "pixels: " + px);
        int dp = Math.round(px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
        Log.d("LOCATION_QUERY", "dp: " + dp);
        return dp;

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

        //retrieve from integers.xml the hard coded values for the risk thresholds for each record type
        riskThresholdCaution_IR = getApplicationContext().getResources().getInteger(R.integer.riskThresholdCaution_IR);
        riskThresholdHigh_IR = getApplicationContext().getResources().getInteger(R.integer.riskThresholdHigh_IR);
        riskThresholdCaution_Crowd = getApplicationContext().getResources().getInteger(R.integer.riskThresholdCaution_Crowd);
        riskThresholdHigh_Crowd = getApplicationContext().getResources().getInteger(R.integer.riskThresholdHigh_Crowd);
        riskThresholdCaution_Mask = getApplicationContext().getResources().getInteger(R.integer.riskThresholdCaution_Mask);
        riskThresholdHigh_Mask = getApplicationContext().getResources().getInteger(R.integer.riskThresholdHigh_Mask);
        riskThresholdCaution_SocDist = getApplicationContext().getResources().getInteger(R.integer.riskThresholdCaution_SocDist);
        riskThresholdHigh_SocDist = getApplicationContext().getResources().getInteger(R.integer.riskThresholdHigh_SocDist);
        riskThresholdCaution_Covid = getApplicationContext().getResources().getInteger(R.integer.riskThresholdCaution_Covid);
        riskThresholdHigh_Covid = getApplicationContext().getResources().getInteger(R.integer.riskThresholdHigh_Covid);


                //retrieve any of the following fields if present: maskRecordLastStoreTimestamp, crowdReocrdLastStoreTimestamp, socDistRecordLastStoreTimestamp, feverRecordLastStoreTimestamp
        // will be -1 if not yet set
        covidRecordLastStoreTimestamp = appPrefs.getLong("covidRecordLastStoreTimestamp", -1);
        maskRecordLastStoreTimestamp = appPrefs.getLong("maskRecordLastStoreTimestamp", -1);
        feverRecordLastStoreTimestamp = appPrefs.getLong("feverRecordLastStoreTimestamp", -1);
        crowdRecordLastStoreTimestamp = appPrefs.getLong("crowdRecordLastStoreTimestamp", -1);
        socDistRecordLastStoreTimestamp = appPrefs.getLong("socDistRecordLastStoreTimestamp", -1);
    }
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
        //grab handle to the crowd activity button
        this.crowdButton = findViewById(R.id.crowdButton);
        //grab handle to the mask activity button
        this.maskButton = findViewById(R.id.maskButton);
        //grab handle to the social distancing activity button
        this.socDistButton = findViewById(R.id.socDistButton);
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
        // TODO: start crowd button activity
        crowdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MapsActivity.this, "Crowd Button Pressed", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent("edu.ilab.covid_id.crowd.ClassifierActivity");
                startActivity(intent);
            }
        });
        // TODO: start mask button activity
        maskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MapsActivity.this, "Mask Button Pressed", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent("edu.ilab.covid_id.mask.MaskActivity");
                startActivity(intent);
            }
        });
        // TODO: start social distancing button activity
        socDistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MapsActivity.this, "Social Distancing Button Pressed", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent("edu.ilab.covid_id.socDist.DetectorActivity");
                startActivity(intent);
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
                    MapsActivity.currentLocation = location;

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


        /**
         * setup custom windowInfoAdapter so when user clicks on a CovidRecord marker it will display
         * appropriate information
         */
        /*
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(final Marker marker) {
               return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });

         */

    }

    /**
     * pulls records from the firebase and uses them to populate map
     */
    private void populateMap() {
        // pull records from firebase according to certain conditions
        FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

        // calculate time after which we want to pull records (prior)
        Date now = new Date();
        Date prior = new Date(now.getTime() - (DIAGNOSTIC_MODE ? timeOffsetMSDiagnostic : timeOffsetMS));

        Log.d("LOCATION_QUERY", "Now: " + now.toString());
        Log.d("LOCATION_QUERY", "Prior: " + prior.toString());

        // pull up to QUERY_LIMIT (1000 currently) records posted after 'prior' date
        mFirestore.collection("CovidRecord")
                .whereGreaterThan("timestamp", prior)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(QUERY_LIMIT)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // log the number of records returned
                Log.d("LOCATION_QUERY", "size of returned documents list: " +queryDocumentSnapshots.size());
                // initialize new local list of CovidRecords to process
                queryRecords = getVisibleRecords(queryDocumentSnapshots);
                // TODO: complete method to erase the current markers and add new markers to the map
                //  based on query
                setMarkers(queryRecords, mMap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("LOCATION_QUERY", e.toString());
            }
        });
    }

    /**
     * from a QuerySnapshot of CovidRecords from the firebase, returns all records that were
     * generated on the visible portion of the screen
     * @param queryDocumentSnapshots - snapshot of records pulled from database
     * @return - ArrayList of CovidRecord objects that were made from locations currently visible
     */
    private ArrayList<CovidRecord> getVisibleRecords(QuerySnapshot queryDocumentSnapshots) {
        // new list of records to parse from query
        ArrayList<CovidRecord> queryRecords = new ArrayList<>();

        /*
         Get current centered position, zoom level, and map width/height in meters:
         */
        CameraPosition currentPosition = mMap.getCameraPosition();  // get current camera position
        Location centeredLoc = new Location("");    // make temp location
        centeredLoc.setLatitude(currentPosition.target.latitude);   // set lat
        centeredLoc.setLongitude(currentPosition.target.longitude); // set long

        // calculate width of earth in digital pixels based on current zoom level
        //  see: https://developers.google.com/maps/documentation/android-sdk/views
        //  for documentation into where this equation comes from
        double EARTH_WIDTH_DP = 256 * Math.pow(2.0, currentPosition.zoom);

        // calculate how many meters each digital pixel represents given the zoom level
        double metersPerDP = EARTH_WIDTH_M / EARTH_WIDTH_DP;

        // calculate height & width of map in meters based on map fragment dimensions
        mapHeightM = mapHeightDP * metersPerDP;
        mapWidthM =  mapWidthDP * metersPerDP;

        Log.d("LOCATION_QUERY", "map height in meters: " + mapHeightM);

        for(DocumentSnapshot doc : queryDocumentSnapshots) {
            // convert document to CovidRecord class object
            CovidRecord x = doc.toObject(CovidRecord.class);

            // compute distance in meters from record to center of map
            double distanceToM = centeredLoc.distanceTo(geoPointToLocation(x.getLocation()));

            // check if map height/width have been calculated and if record is within distance to current location
            if( mapHeightPx != -1 && mapWidthPx != -1 && (distanceToM <= mapHeightM || distanceToM <= mapWidthM)) {
                queryRecords.add(x);
            }

        }
        Log.d("LOCATION_QUERY", "size of added records: " + queryRecords.size());

        return queryRecords;    // return populated list of records
    }

    /**
     * TODO: flesh this method out to do more than simply add a marker for each record found
     * currently clears map and add new markers to map, 1 basic marker per record
     * @param records - list of records to build markers from
     * @param map - map to draw records on
     */
    private void setMarkers(ArrayList<CovidRecord> records, GoogleMap map) {
        map.clear();   // remove all markers, overlays, polylines, etc from map
        // iterate through records and add one marker per record at the corresponding location
        BitmapDescriptor icon;

        String marker_info = "";


        for(CovidRecord record : records) {
            marker_info = "risk: " + record.getRisk() +  "  ";
            marker_info += "certainty:" + String.format("%.1f", record.getCertainty()) + " ";


            if(record.getRecordType().equals("ir")) {
                if(record.getRisk() > riskThresholdHigh_IR) {   // high
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_ir_high);
                } else if(record.getRisk() > riskThresholdCaution_IR) { // caution
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_ir_caution);
                } else { // low
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_ir_low);
                }
                marker_info += "temperature:" + record.getInfo() + " ";
            }
            else if(record.getRecordType().equals("mask")) {
                if(record.getRisk() > riskThresholdHigh_Mask) {   // high
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_mask_high);
                } else if(record.getRisk() > riskThresholdCaution_Mask) { // caution
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_mask_caution);
                } else { // low
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_mask_low);
                }
                marker_info += "type:" + record.getInfo() + " ";
            }
            else if(record.getRecordType().equals("crowd")) {
                if(record.getRisk() > riskThresholdHigh_Crowd) {   // high
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_crowd_high);
                } else if(record.getRisk() > riskThresholdCaution_Crowd) { // caution
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_crowd_caution);
                } else { // low
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_crowd_low);
                }
                marker_info += "type:" + record.getInfo() + " ";
            }
            else if(record.getRecordType().equals("socDist")) {
                if(record.getRisk() > riskThresholdHigh_SocDist) {   // high
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_socdist_high);
                } else if(record.getRisk() > riskThresholdCaution_IR) { // caution
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_socdist_caution);
                } else { // low
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_socdist_low);
                }
                marker_info += "type:" + record.getInfo() + " ";
            }
            else { // default covid record
//                if(record.getRisk() > riskThresholdHigh_Covid) {   // high
//                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_covid_high);
//                } else if(record.getRisk() > riskThresholdCaution_Covid) { // caution
//                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_covid_caution);
//                } else { // low
//                    icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_covid_low);
//                }
                marker_info += "type:" + record.getInfo() + " ";
                // TODO: delete this and replace with commented code
                icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_covid_high);
            }


            map.addMarker(new MarkerOptions()
                    .icon(icon)
                    .position(new LatLng(record.getLocation().getLatitude(), record.getLocation().getLongitude()))
                    .title(record.getRecordType())
                    .snippet(marker_info)
            );
        }
    }

    /**
     * Takes a geopoint and returns the equivalent location
     * @param geoPoint
     * @return
     */
    public static Location geoPointToLocation(GeoPoint geoPoint) {
        Location location = new Location("");
        double latitude = geoPoint.getLatitude();
        double longitude = geoPoint.getLongitude();

        location.setLatitude(latitude);
        location.setLongitude(longitude);

        return location;
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
