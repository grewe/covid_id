package edu.ilab.covid_id;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

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
    public static FirestoreHelper myHelper;

    /**
     * map used in display
     */
    private GoogleMap mMap;

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

    Button exampleDetectorActivityButton;

    /**
     * Activities to perform different kinds of Classification
     */
    ClassifierActivity flowersClassifierActivity;

    /**
     * Activity to perform localition (detection)
     */
    DetectorActivity exampleDetectorActivity;

    private static final int REQUEST_CODE = 101;

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


        // initialize our db helper object
        myHelper = new FirestoreHelper();

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
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
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
        });
    }
}
