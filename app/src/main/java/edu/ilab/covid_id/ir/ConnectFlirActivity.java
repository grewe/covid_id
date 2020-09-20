package edu.ilab.covid_id.ir;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import edu.ilab.covid_id.R;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.BuildConfig;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;
import com.flir.thermalsdk.live.Camera;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

//Import necessary for Backend Plus Tensorflow Model Integration
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import edu.ilab.covid_id.MapsActivity;
import edu.ilab.covid_id.data.CovidRecord;
import edu.ilab.covid_id.localize.DetectorActivity;
import edu.ilab.covid_id.localize.customview.OverlayView;
import edu.ilab.covid_id.localize.env.BorderedText;
import edu.ilab.covid_id.localize.env.ImageUtils;
import edu.ilab.covid_id.localize.env.Logger;
import edu.ilab.covid_id.localize.tflite.Classifier;
import edu.ilab.covid_id.localize.tflite.TFLiteObjectDetectionAPIModel;
import edu.ilab.covid_id.localize.tracking.MultiBoxTracker;
import edu.ilab.covid_id.storage.FirebaseStorageUtil;


public class ConnectFlirActivity extends AppCompatActivity {

    //necessary to limit thread generation - one thread per frame gets created --so that do not run out of memory
    private Handler handler;
    private HandlerThread handlerThread;


    //logging tag
    private static final String TAG = "ConnectFlirActivity";

    //Handles Android permission for eg Network
    private PermissionHandler permissionHandler;

    //Handles network camera operations
    private CameraHandler cameraHandler;

    private Identity connectedIdentity = null;
    private TextView connectionStatus;
    private TextView discoveryStatus;

    private ImageView thermalImage;
    private ImageView photoImage;

    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue(21);
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();



    //NECESSARY Variables for doing Backend storage and Tensorflow model processing
    // temporary flag for determining when to create/push records to the db
    int flag = 0;

    private static final Logger LOGGER = new Logger();


    // TODO: fix this value
    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 512;    //this is the wxh of square input size to MODEL
    private static final boolean TF_OD_API_IS_QUANTIZED = true;  //if its quantized or not. MUST be whatever the save tflite model is saved as
    private static final String TF_OD_API_MODEL_FILE = "IRdetect.tflite"; //"IRdetect.tflite";   //name of input file for MODEL must be tflite format
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/IRlabelmap.txt";  //LabelMap file listed classes--same order as training


    private static final DetectorActivity.DetectorMode MODE = DetectorActivity.DetectorMode.TF_OD_API;   //Using Object Detection API

    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;   //a detected prediction must have value > threshold to be displayed
    private static final boolean MAINTAIN_ASPECT = false;  //if you want to keep aspect ration or not --THIS must be same as what is expected in model,done in training
    //PHILLIP below needs to change to adapt to your view size
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480); //for display ONLY specific to THIS activity
    private static final boolean SAVE_PREVIEW_BITMAP = false;  //specific to THIS activity
    private static final float TEXT_SIZE_DIP = 10;  //font size for display of bounding boxes




    private Classifier detector;  //class variable representing the actual model loaded up
    // note this is  edu.ilab.covid_id.localize.tflite.Classifier;

    private long lastProcessingTimeMs;   //last time processed a frame
    private Bitmap rgbFrameBitmap = null;  //various bitmap variables used in code below
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    //specifying the size you want as input to your model...which will be used later in image processing of input images to resize them.
    private int cropSize;

    private MultiBoxTracker tracker; // this class assists with tracking bounding boxes - represents results
    //note this is instance of edu.ilab.covid_id.localize.tracking.MultiBoxTracker;

    private BorderedText borderedText;


    //Variables for Previewing and Overlay
    private int previewWidth; //width of region will dispplay image in and draw on
    private int previewHeight;

    //PHILLIP this will go away and be replaced by your bounding box drawing solution
    OverlayView trackingOverlay;   //boudning box and prediction info is drawn on screen using an OverlayView
    private Integer sensorOrientation;  //this Activity does rotation for different Orientations



    /**
     * Show message on the screen
     */
    public interface ShowMessage {
        void show(String message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_flir);

        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;

        //ThermalSdkAndroid has to be initiated from a Activity with the Application Context to prevent leaking Context,
        // and before ANY using any ThermalSdkAndroid functions
        //ThermalLog will show log from the Thermal SDK in standards android log framework
        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);

        permissionHandler = new PermissionHandler(showMessage, ConnectFlirActivity.this);

        //This creates CameraHandler for our Flir One Pro IR camera and once connected has
        // event handlers to receive incoming IR images from a live stream
        cameraHandler = new CameraHandler();

        setupViews();

        //showSDKversion(ThermalSdkAndroid.getVersion());

        //method to setup for performing ML detection on stream of IR images captured
        setupForDetection();
    }

    public void startDiscovery(View view) {
        startDiscovery();
    }

    public void stopDiscovery(View view) {
        stopDiscovery();
    }


    public void connectFlirOne(View view) {
        connect(cameraHandler.getFlirOne());
    }

    public void connectSimulatorOne(View view) {
        connect(cameraHandler.getCppEmulator());
    }

    public void connectSimulatorTwo(View view) {
        connect(cameraHandler.getFlirOneEmulator());
    }

    public void disconnect(View view) {
        disconnect();
    }

    /**
     * Handle Android permission request response for Bluetooth permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + permissions + "], grantResults = [" + grantResults + "]");
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Connect to a Camera
     */
    private void connect(Identity identity) {
        //We don't have to stop a discovery but it's nice to do if we have found the camera that we are looking for
        cameraHandler.stopDiscovery(discoveryStatusListener);

        if (connectedIdentity != null) {
            Log.d(TAG, "connect(), in *this* code sample we only support one camera connection at the time");
            showMessage.show("connect(), in *this* code sample we only support one camera connection at the time");
            return;
        }

        if (identity == null) {
            Log.d(TAG, "connect(), can't connect, no camera  available");
            showMessage.show("connect(), can't connect, no camera available");
            return;
        }

        connectedIdentity = identity;

        updateConnectionText(identity, "CONNECTING");
        //IF your using "USB_DEVICE_ATTACHED" and "usb-device vendor-id" in the Android Manifest
        // you don't need to request permission, see documentation for more information
        if (UsbPermissionHandler.isFlirOne(identity)) {
            usbPermissionHandler.requestFlirOnePermisson(identity, this, permissionListener);
        } else {
            doConnect(identity);
        }

    }

    private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
        @Override
        public void permissionGranted(Identity identity) {
            doConnect(identity);
        }

        @Override
        public void permissionDenied(Identity identity) {
            ConnectFlirActivity.this.showMessage.show("Permission was denied for identity ");
        }

        @Override
        public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
            ConnectFlirActivity.this.showMessage.show("Error when asking for permission for FLIR ONE, error:"+errorType+ " identity:" +identity);
        }
    };

    private void doConnect(Identity identity) {
        new Thread(() -> {
            try {
                cameraHandler.connect(identity, connectionStatusListener);
                runOnUiThread(() -> {
                    updateConnectionText(identity, "CONNECTED");
                    //this is the code that will be invoked once we are connected
                    Log.d(TAG, "Lynne it is connected");


                    //call method to setup
                    setupForImageProcessingAndOverlay(cameraHandler.getCamera());

                    cameraHandler.startStream(streamDataListener);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Could not connect: " + e);
                    updateConnectionText(identity, "DISCONNECTED");
                });
            }
        }).start();
    }

    /**
     * Disconnect to a camera
     */
    private void disconnect() {
        updateConnectionText(connectedIdentity, "DISCONNECTING");
        connectedIdentity = null;
        Log.d(TAG, "disconnect() called with: connectedIdentity = [" + connectedIdentity + "]");
        new Thread(() -> {
            cameraHandler.disconnect();
            runOnUiThread(() -> {
                updateConnectionText(null, "DISCONNECTED");
            });
        }).start();
    }

    /**
     * Update the UI text for connection status
     */
    private void updateConnectionText(Identity identity, String status) {
        String deviceId = identity != null ? identity.deviceId : "";
        //connectionStatus.setText(getString(R.string.connection_status_text, deviceId + " " + status));
        connectionStatus.setText("Connected"+deviceId + " " + status);
    }

    /**
     * Start camera discovery
     */
    private void startDiscovery() {
        cameraHandler.startDiscovery(cameraDiscoveryListener, discoveryStatusListener);
    }

    /**
     * Stop camera discovery
     */
    private void stopDiscovery() {
        cameraHandler.stopDiscovery(discoveryStatusListener);
    }

    /**
     * Callback for discovery status, using it to update UI
     */
    private CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
        @Override
        public void started() {
           // discoveryStatus.setText(getString(R.string.connection_status_text, "discovering"));
            discoveryStatus.setText("Discovering");
        }

        @Override
        public void stopped() {
            //discoveryStatus.setText(getString(R.string.connection_status_text, "not discovering"));
            discoveryStatus.setText("Not discovering");
        }
    };

    /**
     * Camera connecting state thermalImageStreamListener, keeps track of if the camera is connected or not
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
    private ConnectionStatusListener connectionStatusListener = new ConnectionStatusListener() {
        @Override
        public void onDisconnected(@org.jetbrains.annotations.Nullable ErrorCode errorCode) {
            Log.d(TAG, "onDisconnected errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateConnectionText(connectedIdentity, "DISCONNECTED");
                }
            });
        }
    };

    /**
     * annonymous inner class for handling the incomming stream of IR images
     * Note CameraHandler.StreamDataListener is an Interface and here is the implementation of its methods.
     */
    private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {

        @Override
        public void images(FrameDataHolder dataHolder) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    thermalImage.setImageBitmap(dataHolder.thermalBitmap);
                    photoImage.setImageBitmap(dataHolder.dcBitmap);
                    Log.d(TAG, "inside CameraHandler.StreamDataListnerener.images(FrameDataHolder ");
                }
            });
        }

        /**
         * method that is called to add a new bitmap (image) to be processed by adding it to the
         * framesBuffer, then has a runnable thread that will process the images in the framesBuffer
         * @param thermalBitmap thermal image
         * @param dcBitmap  corresponding rgb image
         */
        @Override
        public void images(Bitmap thermalBitmap, Bitmap dcBitmap) {

            try {
                framesBuffer.put(new FrameDataHolder(thermalBitmap,dcBitmap));
            } catch (InterruptedException e) {
                //if interrupted while waiting for adding a new item in the queue
                Log.e(TAG,"images(), unable to add incoming images to frames buffer, exception:"+e);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"framebuffer size:"+framesBuffer.size());
                    FrameDataHolder poll = framesBuffer.poll();
                    thermalImage.setImageBitmap(poll.thermalBitmap);
                    photoImage.setImageBitmap(poll.dcBitmap);
                    //setup various variables for ImageProcessing --this is NOT RIGHT HERE but, we don't have an image grabbed until here
                    //setupForImageProcessingAndOverlay(poll.msxBitmap); //pass the bitmap will process


                    //TODO --get following to work
                    //Preprocess image and pass to TfLite Model here
                    processImage(poll.thermalBitmap);



                }
            });

        }
    };

    /**
     * Camera Discovery thermalImageStreamListener, is notified if a new camera was found during a active discovery phase
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
    private DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {
        @Override
        public void onCameraFound(Identity identity) {
            Log.d(TAG, "onCameraFound identity:" + identity);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cameraHandler.add(identity);
                }
            });
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
            Log.d(TAG, "onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopDiscovery();
                    ConnectFlirActivity.this.showMessage.show("onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);
                }
            });
        }
    };

    private ShowMessage showMessage = new ShowMessage() {
        @Override
        public void show(String message) {
            Toast.makeText(ConnectFlirActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

//    private void showSDKversion(String version) {
//        TextView sdkVersionTextView = findViewById(R.id.sdk_version);
//        String sdkVersionText = getString(R.string.sdk_version_text, version);
//        sdkVersionTextView.setText(sdkVersionText);
//    }

    /**
     * this method grabs handles to various GUI elements for this activity
     * connectionStatus = the text saying if connected or not
     * discoverStatus = the text giving information if performing discovery or not
     * msxImage = ImageView to display thermal greyscale image
     * photoImage = ImageView to display corresponding rgb Image
     */
    private void setupViews() {
        connectionStatus = findViewById(R.id.connection_status_text);
        discoveryStatus = findViewById(R.id.discovery_status);

        thermalImage = findViewById(R.id.msx_image);
        photoImage = findViewById(R.id.photo_image);
    }


    /**
     * this method invoked in onCreate to setup various items for performing ML Detection on stream of IR imagery
     * This method creates the tracker (to store bounding box info), and the detector (the actual model
     *  loaded from a tflite used for detection) and sets up various GUI elements and bitmaps for displaying results.
     * THis is really just a SETUP method
     */
    private void setupForDetection(){
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);


        //class to contain detection results with bounding box information
        tracker = new MultiBoxTracker(this);

        //specifying the size you want as input to your model...which will be used later in image processing of input images to resize them.
        cropSize = TF_OD_API_INPUT_SIZE;

        //load up the detector based on the specified parameters include the tflite file in the assets folder, etc.
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        Log.d(TAG, "ML detector is loaded");

    }

    /**
     * This method setups various bitmaps and variables used in preprocessing of the images
     * to prepare them for processing by our ML model (e.g. correct scaling, etc)
     * Also it setups
     * PROBLEM:  DO now know how to get resoltuion of Camera   Do not see API for this
     *   private void setupForImageProcessingAndOverlay(Camera c) {
     *        previewWidth = c.getResolutionofThermal().getWidth();
     *        previewHeight = c.getResolutionofThermal().getHeight();
     */
    private void setupForImageProcessingAndOverlay(Camera c) {
        //display size
      //  previewWidth = c.getWidth();
      //  previewHeight = c.getHeight();

        //hardcoding values for mxsImage options for Flir One Pro camera
        previewWidth =480;
        previewHeight = 640;


        sensorOrientation =  0; //90;   //sensorOreintation will be 0 for horizontal and 90 for portrait


        LOGGER.i(TAG, "Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i(TAG, "Initializing at size %dx%d", previewWidth, previewHeight);

        //seting up the bitmap input image  based on grabing it from the preview display of it.
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        // rgbFrameBitmap = Bitmap.createBitmap(480,640, Bitmap.Config.ARGB_8888);
        //setting up the bitmap to store the resized input image to the size that the model expects
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        //create a transformation that will be used to convert the input image to the right size and orientation expected by the model
        //   involves resizing (to cropsizexcropsize) from the original previewWidthxpreviewHeight
        //   involves rotation based on sensorOrientation
        //   invovles if you want aspect to be maintained
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);  //TIP: if you want no rotation than sensorOreination should be 0

        cropToFrameTransform = new Matrix();  //identity matrix initially
        frameToCropTransform.invert(cropToFrameTransform);  //calculating the cropToFrameTransform as the inversion of the frameToCropTransform


    }

    /**
     * This method setups various bitmaps and variables used in preprocessing of the images
     * to prepare them for processing by our ML model (e.g. correct scaling, etc)
     * This gets the input image size based on the incoming image
     */
    private void setupForImageProcessingAndOverlay(Bitmap image) {
        //display size
        previewWidth = image.getWidth();
        previewHeight = image.getHeight();



        sensorOrientation =  90;   //sensorOreintation will be 0 for horizontal and 90 for portrait


        LOGGER.i(TAG, "Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i(TAG, "Initializing at size %dx%d", previewWidth, previewHeight);

        //seting up the bitmap input image  based on grabing it from the preview display of it.
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
       // rgbFrameBitmap = Bitmap.createBitmap(480,640, Bitmap.Config.ARGB_8888);
        //setting up the bitmap to store the resized input image to the size that the model expects
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        //create a transformation that will be used to convert the input image to the right size and orientation expected by the model
        //   involves resizing (to cropsizexcropsize) from the original previewWidthxpreviewHeight
        //   involves rotation based on sensorOrientation
        //   invovles if you want aspect to be maintained
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);  //TIP: if you want no rotation than sensorOreination should be 0

        cropToFrameTransform = new Matrix();  //identity matrix initially
        frameToCropTransform.invert(cropToFrameTransform);  //calculating the cropToFrameTransform as the inversion of the frameToCropTransform


    }


    /**
     * This method is called every time we will to process the CURRENT frame
     * this means the current frame/image will be processed by our this.detector model
     * and results are cycled through (can be more than one deteciton in an image) and displayed
     */
    protected void processImage(Bitmap image) {
        ++timestamp;
        final long currTimestamp = timestamp;

        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        //LOAD the current image --calling getRgbBytes method into the rgbFrameBitmap object
        rgbFrameBitmap = image;

        //create a drawing canvas that is associated with the image croppedBitmap that will be the transformed input image to the right size and orientation
        final Canvas canvas = new Canvas(croppedBitmap);

        //CROP and transform
        //why working in portrait mode and not horizontal
        //canvas.drawBitmap(rgbFrameBitmap,new Matrix(), null);   //need to only rotate it.
        // canvas.drawBitmap(croppedBitmap, cropToFrameTransform, null); //try this later???
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);   ///crop and transform as necessary image

        // For examining the actual TF input. to save on local device
        //LYNNE: look at this method it is failing to make directory
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        //Need to run in separate thread ---to process the image --going to call the model to do prediction
        // because of this must run in own thread.
     //   ( new Thread() {
     //               @Override
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);  //performing detection on croppedBitmap
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;


                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);   //create canvas to draw bounding boxes inside of which will be displayed in OverlayView
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        int saveImageOnceFlag = 1;
                        String imageFileURL = "";
                        //cycling through all of the recognition detections in my image I am currently processing
                        for (final Classifier.Recognition result : results) {  //loop variable is result, represents one detection
                            final RectF location = result.getLocation();  //getting as  a rectangle the bounding box of the result detecgiton
                            if (location != null && result.getConfidence() >= minimumConfidence) { //ONLY display if the result has a confidence > threshold
                                canvas.drawRect(location, paint);  //draw in the canvas the bounding boxes-->


                                //==============================================================
                                //COVID: code to store image to CloudStore (if any results have result.getConfidence() > minimumConfidence
                                //  ONLY store one time regardless of number of recognition results.
                                if(saveImageOnceFlag == 1){

                                    //set flag so know have already stored this image
                                    saveImageOnceFlag = 0;

                                    //CEMIL: code to store image (croppedBitmap) in CloudStore
                                    //imageFileURL store the URL


                                    //**************************************************
                                    //try writing out the image being processed to a FILE
                                    // File directory = Environment.getExternalStorageDirectory();
                                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                    File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                    File dest = new File(directory, "croppedImage.png");
                                    File topLabelBox = new File(directory, "topLabelBoxImage.png");

                                    try {
                                        dest.createNewFile();
                                        FileOutputStream out = new FileOutputStream(dest);
                                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                                        out.flush();
                                        out.close();
                                        topLabelBox.createNewFile();
                                        FileOutputStream out2 = new FileOutputStream(topLabelBox);
                                        cropCopyBitmap.compress(Bitmap.CompressFormat.PNG, 90, out2);
                                        out2.flush();
                                        out2.close();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    //**************************************************

                                }

                                //==========================================================================
                                //##################################################################
                                //Store to Firebase Database  -- if we are ready since last record storage to make a new record
                                if(CovidRecord.readyStoreRecord(MapsActivity.covidRecordLastStoreTimestamp, MapsActivity.deltaCovidRecordStoreTimeMS, MapsActivity.covidRecordLastStoreLocation, MapsActivity.currentLocation, MapsActivity.deltaCovidRecordStoreLocationM)) {
                                    Date d = new Date();
                                    ArrayList<Float> angles = new ArrayList<Float>();
                                    angles.add(0, 0.0f);
                                    angles.add(1, 0.0f);
                                    angles.add(2, 0.0f);

                                    ArrayList<Float> boundingBox = new ArrayList<Float>();
                                    boundingBox.add(0, location.left);
                                    boundingBox.add(1, location.top);
                                    boundingBox.add(2, location.right);
                                    boundingBox.add( 3, location.bottom);

                                    CovidRecord myRecord = new CovidRecord(90.0f, result.getConfidence()*100,
                                            new GeoPoint(MapsActivity.currentLocation.getLatitude(), MapsActivity.currentLocation.getLongitude()),
                                            Timestamp.now(), imageFileURL, result.getTitle(),boundingBox, angles, 0.0f,
                                            MapsActivity.userEmailFirebase, MapsActivity.userIdFirebase);

                                    //SHIVALI AND PHILLIP --need to fix why this line is failing---due to failed authentication
                                    FirebaseStorageUtil.storeImageAndCovidRecord(cropCopyBitmap, myRecord, MapsActivity.currentLocation);


                  // ask helper to push record to db
               //   MapsActivity.myFirestoreHelper.addRecord(myRecord);

                  //update the last time record stored
                //  MapsActivity.covidRecordLastStoreTimestamp =  System.currentTimeMillis();


                                }
                                //###############################################

                                //the following takes the bounding box location and transforms it for coordinates in display
                                cropToFrameTransform.mapRect(location);  //transforms using Matrix the bounding box to the correct transformed coordinates

                                result.setLocation(location); // reset the newly transformed rectangle (location) representing bounding box inside the result
                                mappedRecognitions.add(result);  //add the result to a linked list




                            }
                        }

                   //PHILLIP and SHIVALI you need to add your overlay for dispalying the results in mappedRecognitions
                        //THE following lines will NOT work for you they are from my other
                   //     tracker.trackResults(mappedRecognitions, currTimestamp);  //DOES DRAWING:  OverlayView to dispaly the recognition bounding boxes that have been transformed and stored in LL mappedRecogntions
                   //     trackingOverlay.postInvalidate();

/*

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showFrameInfo(previewWidth + "x" + previewHeight);
                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                        showInference(lastProcessingTimeMs + "ms");
                                    }
                                });

 */


                    }
                });
        //.start();


    }


    protected void showFrameInfo(String frameInfo) {
        //somehow.setText(frameInfo);
    }

    protected void showCropInfo(String cropInfo) {

        //shomehow.cropValueTextView.setText(cropInfo);
    }

    protected void showInference(String inferenceTime) {
        //showmehow.inferenceTimeTextView.setText(inferenceTime);
    }



    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }


    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }


    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }


    /**
     * onResume here creates the Thread Handler and starts it.
     */
    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }


    /**
     * quiting the Thread Handler --see onResume for reinitialization
     */
    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }


    /**
     * do nothing special --only a log message
     */
    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    /**
     * do nothing special --only a log message
     */
    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }


}