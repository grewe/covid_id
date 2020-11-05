package edu.ilab.covid_id.crowd;

/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ilab.covid_id.MapsActivity;
import edu.ilab.covid_id.R;
import edu.ilab.covid_id.classification.env.BorderedText;
import edu.ilab.covid_id.classification.env.Logger;
import edu.ilab.covid_id.classification.tflite.Classifier;
import edu.ilab.covid_id.classification.tflite.Classifier.Device;
import edu.ilab.covid_id.data.CovidRecord;
import edu.ilab.covid_id.storage.FirebaseStorageUtil;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {

    public static String imageFileURL;

    // flag for pushing records
    int flag = 0;

    private static final Logger LOGGER = new Logger();
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final float TEXT_SIZE_DIP = 10;
    private Bitmap rgbFrameBitmap = null;
    private long lastProcessingTimeMs;
    private Integer sensorOrientation;
    private Classifier classifier;
    private BorderedText borderedText;
    /** Input image size of the model along x axis. */
    private int imageSizeX;
    /** Input image size of the model along y axis. */
    private int imageSizeY;

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_ic_camera_connection_fragment;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        recreateClassifier(getDevice(), getNumThreads());
        if (classifier == null) {
            LOGGER.e("No classifier on preview!");
            return;
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    }

    /**
     * processImage = method that will process each Image
     *
     * NOTE: This ClassifierActivity extends CameraActivity that  gets the camera input using the functions defined in the file CameraActivity.java.
     * This file depends on AndroidManifest.xml to set the camera orientation.
     *
     * CameraActivity also contains code to capture user preferences from the UI and make them available to other classes via convenience methods.
     *
     * model = Model.valueOf(modelSpinner.getSelectedItem().toString().toUpperCase());
     * device = Device.valueOf(deviceSpinner.getSelectedItem().toString());
     * numThreads = Integer.parseInt(threadsTextView.getText().toString().trim());
     *
     * NOTE 2: The file Classifier.java contains most of the complex logic for processing the camera input and running inference.
     *      *
     *      * A subclasses of the file exist, in ClassifierFloatMobileNet.java (in other Tensorflowlite examples there is ClassifierQuantizedMobileNet.java), to demonstrate the use of
     *      * floating point (and quantized) models.
     *      *
     *      * The Classifier class implements a static method, create, which is used to instantiate the appropriate subclass based on the supplied model type (quantized vs floating point).
     *      *
     */
    @Override
    protected void processImage() {
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final int cropSize = Math.min(previewWidth, previewHeight);
        flag++;

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        if (classifier != null) {
                            final long startTime = SystemClock.uptimeMillis();

                            //COVID_ID:  ADD any OTHER preprocessing here

                            //Calling the Classifier
                            final List<Classifier.Recognition> results =
                                    classifier.recognizeImage(rgbFrameBitmap, sensorOrientation);
                            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                            LOGGER.v("Detect: %s", results);

                            //COVID_ID:  ADD  Any Post Processing (after recognition)  code here
                            //Following code creates  display that shows results
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            //DISPLAY information including recognition results
                                            showResultsInBottomSheet(results);
                                            showFrameInfo(previewWidth + "x" + previewHeight);
                                            showCropInfo(imageSizeX + "x" + imageSizeY);
                                            showCameraResolution(cropSize + "x" + cropSize);
                                            showRotationInfo(String.valueOf(sensorOrientation));
                                            showInference(lastProcessingTimeMs + "ms");

                                            //==============================================================
                                            //COVID: code to store image to CloudStore  AND store record in database

                                            //**************************************************
                                            //try writing out the image being processed to a FILE locally on Android
                                            //File sd = Environment.getExternalStorageDirectory();
                                            //File dest = new File(sd, "classifiedImage.png");
                                            ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                            File dest = new File(directory, "croppedImage.png");

                                            try {
                                                    dest.createNewFile();
                                                    FileOutputStream out = new FileOutputStream(dest);
                                                    rgbFrameBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                                                    out.flush();
                                                    out.close();
                                            } catch (Exception e) {
                                                    e.printStackTrace();
                                            }

                                            //**************************************************
                                            //Store to Firebase Database  -- if we are ready since last record storage to make a new record
                                            if(CovidRecord.readyStoreRecord(MapsActivity.covidRecordLastStoreTimestamp, MapsActivity.deltaCovidRecordStoreTimeMS, MapsActivity.covidRecordLastStoreLocation, MapsActivity.currentLocation, MapsActivity.deltaCovidRecordStoreLocationM)) {

                                                //setup variables for CovidRecord to store to FireStore
                                                ArrayList<Float> angles = new ArrayList<Float>();
                                                angles.add(0, 0.0f);
                                                angles.add(1, 0.0f);
                                                angles.add(2, 0.0f);

                                                CovidRecord myRecord = new CovidRecord(80.0f, results.get(0).getConfidence() * 100,
                                                        new GeoPoint(MapsActivity.currentLocation.getLatitude(), MapsActivity.currentLocation.getLongitude()),
                                                        Timestamp.now(), ClassifierActivity.imageFileURL, results.get(0).getTitle(), angles, 0.0f, MapsActivity.userEmailFirebase, MapsActivity.userIdFirebase, "covidRecord");

                                                //CEMIL ONLY FOR Classifier Activity --rotate the rgbFrameBitmap image ONLY if running in portrait mode 90 degree (or -90) ---test it out.
                                                FirebaseStorageUtil.storeImageAndCovidRecord(rgbFrameBitmap, myRecord,MapsActivity.currentLocation, "covidRecord" );
                                            }
                                            //=========================================================================
                                        }
                                    });
                        }
                       readyForNextImage();
                    }
                });
    }

    @Override
    protected void onInferenceConfigurationChanged() {
        if (rgbFrameBitmap == null) {
            // Defer creation until we're getting camera frames.
            return;
        }
        final Device device = getDevice();
        final int numThreads = getNumThreads();
        runInBackground(() -> recreateClassifier(device, numThreads));
    }

    private void recreateClassifier(Device device, int numThreads) {
        if (classifier != null) {
            LOGGER.d("Closing classifier.");
            classifier.close();
            classifier = null;
        }
        try {
            LOGGER.d(
                    "Creating classifier (device=%s, numThreads=%d)", device, numThreads);
            classifier = Classifier.create(this, device, numThreads);
        } catch (IOException e) {
            LOGGER.e(e, "Failed to create classifier.");
        }

        // Updates the input image size.
        imageSizeX = classifier.getImageSizeX();
        imageSizeY = classifier.getImageSizeY();
    }
}
