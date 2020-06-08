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

package edu.ilab.covid_id.localize;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import edu.ilab.covid_id.MapsActivity;
import edu.ilab.covid_id.R;
import edu.ilab.covid_id.data.CovidRecord;
import edu.ilab.covid_id.localize.customview.OverlayView;
import edu.ilab.covid_id.localize.customview.OverlayView.DrawCallback;
import edu.ilab.covid_id.localize.env.BorderedText;
import edu.ilab.covid_id.localize.env.ImageUtils;
import edu.ilab.covid_id.localize.env.Logger;
import edu.ilab.covid_id.localize.tflite.Classifier;
import edu.ilab.covid_id.localize.tflite.TFLiteObjectDetectionAPIModel;
import edu.ilab.covid_id.localize.tracking.MultiBoxTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {

  // temporary flag for determining when to create/push records to the db
  int flag = 0;

  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);


    //class to contain detection results with bounding box information
    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

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

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();


  
    sensorOrientation =  rotation - getScreenOrientation();   //sensorOreintation will be 0 for horizontal and 90 for portrait


    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();  //identity matrix initially
    frameToCropTransform.invert(cropToFrameTransform);  //calculating the cropToFrameTransform as the inversion of the frameToCropTransform

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    //why working in portrait mode and not horizontal
    //canvas.drawBitmap(rgbFrameBitmap,new Matrix(), null);   //need to only rotate it.
   // canvas.drawBitmap(croppedBitmap, cropToFrameTransform, null); //try this later???
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);   ///IS THIS WHERE DRAWING THE IMAGE??  using frameToCropTransform --GUESS frameToCropTransform is not rotating correct???
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

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
            paint.setStyle(Style.STROKE);
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
            for (final Classifier.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);  //draw in the canvas the bounding boxes--> where is this used???? nowhere???


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

                //###################################################################
                //COVID: Dummy code to create and store 3 covid records to the firestore
                //QUESTION 1:  should I save the bounding box BELOW after it is transfromed cropToFramTransform???
                flag++;
                if(flag < 3) {
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

                  CovidRecord myRecord = new CovidRecord(80.0f, result.getConfidence()*100, new GeoPoint(MapsActivity.currentLocation.getLatitude(), MapsActivity.currentLocation.getLongitude()), Timestamp.now(), imageFileURL, result.getTitle(),boundingBox, angles, 0.0f);


                  // ask helper to push record to db
                  MapsActivity.myHelper.addRecord(myRecord);
                }
                //###############################################

                cropToFrameTransform.mapRect(location);  //transforms using Matrix the bounding box to the correct transformed coordinates

                result.setLocation(location); // reset the newly transformed rectangle (location) representing bounding box inside the result
                mappedRecognitions.add(result);  //add the result to a linked list

                //QUESTION 1:   DO I DO THE Firestore save here instead??????


              }
            }

            tracker.trackResults(mappedRecognitions, currTimestamp);  //DOES DRAWING:  OverlayView to dispaly the recognition bounding boxes that have been transformed and stored in LL mappedRecogntions
            trackingOverlay.postInvalidate();

            computingDetection = false;

            runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                  }
                });
          }
        });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(() -> detector.setUseNNAPI(isChecked));
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }
}
