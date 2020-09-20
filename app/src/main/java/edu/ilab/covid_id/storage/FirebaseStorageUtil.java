package edu.ilab.covid_id.storage;

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import edu.ilab.covid_id.MapsActivity;
import edu.ilab.covid_id.data.CovidRecord;
import edu.ilab.covid_id.data.CrowdRecord;
import edu.ilab.covid_id.data.IRRecord;
import edu.ilab.covid_id.data.MaskRecord;
import edu.ilab.covid_id.data.SocDistRecord;

/**
 * Utility class with static methods to store Images and Data Records to appropriate Firebase Storage and Firestore respectively
 * Note the class edu.ilab.covid_id.data.FireStoreHelper is the helper class for doing the Firestore record database insertion and is called appropriately in this class
 * using the static instance MapsActivity.myFirestoreHelper
 */
public class FirebaseStorageUtil {


    public static String imageFileURL;


    /**
     * Store image (if flag set MapsActivity.flagStoreImageFiles) to Firebase Storage AND store the myRecord to the appopriate FireStore database
     * @param rgbFrameBitmap
     * @param myRecord
     * @param location
     */
    public static void storeImageAndCovidRecord(Bitmap rgbFrameBitmap, CovidRecord myRecord , Location location){
        //**************************************************
        //Store to Firebase Database  -- if we are ready since last record storage to make a new record
        if(CovidRecord.readyStoreRecord(MapsActivity.covidRecordLastStoreTimestamp,
                MapsActivity.deltaCovidRecordStoreTimeMS,
                MapsActivity.covidRecordLastStoreLocation,
                MapsActivity.currentLocation,
                MapsActivity.deltaCovidRecordStoreLocationM)) {

            Date d = new Date();

            //CEMIL ONLY FOR Classifier Activity --rotate the image if running oNLY in portrait mode 90 degree (or -90) ---test it out.
            //see if app is running in mode to store Images to Firebase Storage or not
            if(MapsActivity.flagStoreImageFiles) {
                //------------------------------------------------------
                //first store the Image to Firebase Storage
                //build name so it is unique associated with the User's id and date
                String filename = MapsActivity.userIdFirebase + "_" + d.getTime();
                StorageReference newImageFileRef = MapsActivity.imagesFirebaseRef.child(filename);
                //convert to a jpeg format and then to a byte[] array which is what is used by Firebase storage task to upload
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rgbFrameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                //upload so that get back URL of location it was uploaded ---will be an asynchronous task
                UploadTask uploadTask = newImageFileRef.putBytes(data);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return newImageFileRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            imageFileURL = downloadUri.toString();
                        } else {
                            // Handle failures
                            imageFileURL = null;
                        }

                        //alter the CovidRecord to add the ImageURL to myRecord
                        myRecord.setFilenameURL(imageFileURL);


                        // ask helper to push record to db
                        MapsActivity.myFirestoreHelper.addRecord(myRecord);

                        //update the last time record stored
                        MapsActivity.covidRecordLastStoreTimestamp =  System.currentTimeMillis();

                        //update the last location record stored
                        MapsActivity.covidRecordLastStoreLocation = location;
                    }
                });
            }//end case of storing Images

            //-------------------------------------------------
            else {  //NO image file storage  --set fileURL to null in CovidRecord

               myRecord.setFilenameURL(null);

                // ask helper to push record to db
                MapsActivity.myFirestoreHelper.addRecord(myRecord);

                //update the last time record stored
                MapsActivity.covidRecordLastStoreTimestamp = System.currentTimeMillis();

                //update the last location record stored
                MapsActivity.covidRecordLastStoreLocation = location;
            }
        }

    }




    /**
     * Store image (if flag set MapsActivity.flagStoreImageFiles) to Firebase Storage AND store the MASK myRecord to the appopriate FireStore database
     * @param rgbFrameBitmap
     * @param myRecord
     * @param location  is the loaction where the record was created
     */
    public static void storeImageAndMaskRecord(Bitmap rgbFrameBitmap, MaskRecord myRecord, Location location  ){
        //**************************************************
        //Store to Firebase Database  -- if we are ready since last record storage to make a new record
        if(CovidRecord.readyStoreRecord(MapsActivity.covidRecordLastStoreTimestamp, MapsActivity.deltaCovidRecordStoreTimeMS, MapsActivity.covidRecordLastStoreLocation, MapsActivity.currentLocation, MapsActivity.deltaCovidRecordStoreLocationM)) {


            Date d = new Date();

            //CEMIL ONLY FOR Classifier Activity --rotate the image if running oNLY in portrait mode 90 degree (or -90) ---test it out.
            //see if app is running in mode to store Images to Firebase Storage or not
            if(MapsActivity.flagStoreImageFiles) {
                //------------------------------------------------------
                //first store the Image to Firebase Storage
                //build name so it is unique associated with the User's id and date
                String filename = MapsActivity.userIdFirebase + "_" + d.getTime();
                StorageReference newImageFileRef = MapsActivity.imagesFirebaseRef.child(filename);
                //convert to a jpeg format and then to a byte[] array which is what is used by Firebase storage task to upload
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rgbFrameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                //upload so that get back URL of location it was uploaded ---will be an asynchronous task
                UploadTask uploadTask = newImageFileRef.putBytes(data);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return newImageFileRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            imageFileURL = downloadUri.toString();
                        } else {
                            // Handle failures
                            imageFileURL = null;
                        }

                        //alter the CovidRecord to add the ImageURL to myRecord
                        myRecord.setFilenameURL(imageFileURL);


                        // ask helper to push record to db
                        MapsActivity.myFirestoreHelper.addRecord(myRecord);

                        //update the last time record stored
                        MapsActivity.covidRecordLastStoreTimestamp =  System.currentTimeMillis();


                        //update the last location record stored
                        MapsActivity.covidRecordLastStoreLocation = location;
                    }
                });
            }//end case of storing Images

            //-------------------------------------------------
            else {  //NO image file storage  --set fileURL to null in CovidRecord

                myRecord.setFilenameURL(null);

                // ask helper to push record to db
                MapsActivity.myFirestoreHelper.addRecord(myRecord);

                //update the last time record stored
                MapsActivity.covidRecordLastStoreTimestamp = System.currentTimeMillis();

                //update the last location record stored
                MapsActivity.covidRecordLastStoreLocation = location;
            }
        }

    }




    /**
     * Store image (if flag set MapsActivity.flagStoreImageFiles) to Firebase Storage AND store the FEVER myRecord to the appopriate FireStore database
     * @param rgbFrameBitmap
     * @param myRecord
     * @param location
     */
    public static void storeImageAndFeverRecord(Bitmap rgbFrameBitmap, CovidRecord myRecord , Location location ){
            Date d = new Date();

            //CEMIL ONLY FOR Classifier Activity --rotate the image if running oNLY in portrait mode 90 degree (or -90) ---test it out.
            //see if app is running in mode to store Images to Firebase Storage or not
            if(MapsActivity.flagStoreImageFiles) {
                //------------------------------------------------------
                //first store the Image to Firebase Storage
                //build name so it is unique associated with the User's id and date
                String filename = MapsActivity.userIdFirebase + "_" + d.getTime();
                StorageReference newImageFileRef = MapsActivity.imagesFirebaseRef.child(filename);
                //convert to a jpeg format and then to a byte[] array which is what is used by Firebase storage task to upload
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rgbFrameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                //upload so that get back URL of location it was uploaded ---will be an asynchronous task
                UploadTask uploadTask = newImageFileRef.putBytes(data);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return newImageFileRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            imageFileURL = downloadUri.toString();
                        } else {
                            // Handle failures
                            imageFileURL = null;
                        }

                        //alter the CovidRecord to add the ImageURL to myRecord
                        myRecord.setFilenameURL(imageFileURL);

                        // ask helper to push record to db
                        MapsActivity.myFirestoreHelper.addRecord(myRecord);

                        //update the last time record stored
                        MapsActivity.covidRecordLastStoreTimestamp =  System.currentTimeMillis();

                        //update the last location record stored
                        MapsActivity.covidRecordLastStoreLocation = location;
                    }
                });
            }//end case of storing Images

            //-------------------------------------------------
            else {  //NO image file storage  --set fileURL to null in CovidRecord

                myRecord.setFilenameURL(null);

                // ask helper to push record to db
                MapsActivity.myFirestoreHelper.addRecord(myRecord);

                //update the last time record stored
                MapsActivity.covidRecordLastStoreTimestamp = System.currentTimeMillis();

                //update the last location record stored
                MapsActivity.covidRecordLastStoreLocation = location;
            }


    }




    /**
     * Store image (if flag set MapsActivity.flagStoreImageFiles) to Firebase Storage AND store the SocDist myRecord to the appopriate FireStore database
     * @param rgbFrameBitmap
     * @param myRecord
     * @param location
     */
    public static void storeImageAndSocDistRecord(Bitmap rgbFrameBitmap, SocDistRecord myRecord, Location location  ){
        //**************************************************
        //Store to Firebase Database  -- if we are ready since last record storage to make a new record
        if(CovidRecord.readyStoreRecord(MapsActivity.covidRecordLastStoreTimestamp, MapsActivity.deltaCovidRecordStoreTimeMS, MapsActivity.covidRecordLastStoreLocation, MapsActivity.currentLocation, MapsActivity.deltaCovidRecordStoreLocationM)) {


            Date d = new Date();

            //CEMIL ONLY FOR Classifier Activity --rotate the image if running oNLY in portrait mode 90 degree (or -90) ---test it out.
            //see if app is running in mode to store Images to Firebase Storage or not
            if(MapsActivity.flagStoreImageFiles) {
                //------------------------------------------------------
                //first store the Image to Firebase Storage
                //build name so it is unique associated with the User's id and date
                String filename = MapsActivity.userIdFirebase + "_" + d.getTime();
                StorageReference newImageFileRef = MapsActivity.imagesFirebaseRef.child(filename);
                //convert to a jpeg format and then to a byte[] array which is what is used by Firebase storage task to upload
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rgbFrameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                //upload so that get back URL of location it was uploaded ---will be an asynchronous task
                UploadTask uploadTask = newImageFileRef.putBytes(data);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return newImageFileRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            imageFileURL = downloadUri.toString();
                        } else {
                            // Handle failures
                            imageFileURL = null;
                        }

                        //alter the CovidRecord to add the ImageURL to myRecord
                        myRecord.setFilenameURL(imageFileURL);


                        // ask helper to push record to db
                        MapsActivity.myFirestoreHelper.addRecord(myRecord);

                        //update the last time record stored
                        MapsActivity.covidRecordLastStoreTimestamp =  System.currentTimeMillis();

                        //update the last location record stored
                        MapsActivity.covidRecordLastStoreLocation = location;
                    }
                });
            }//end case of storing Images

            //-------------------------------------------------
            else {  //NO image file storage  --set fileURL to null in CovidRecord

                myRecord.setFilenameURL(null);

                // ask helper to push record to db
                MapsActivity.myFirestoreHelper.addRecord(myRecord);

                //update the last time record stored
                MapsActivity.covidRecordLastStoreTimestamp = System.currentTimeMillis();

                //update the last location record stored
                MapsActivity.covidRecordLastStoreLocation = location;
            }
        }

    }



    /**
     * Store image (if flag set MapsActivity.flagStoreImageFiles) to Firebase Storage AND store the Crowd myRecord to the appopriate FireStore database
     * @param rgbFrameBitmap
     * @param myRecord
     * @param location
     */
    public static void storeImageAndCrowdRecord(Bitmap rgbFrameBitmap, CrowdRecord myRecord, Location location  ){
        //**************************************************
        //Store to Firebase Database  -- if we are ready since last record storage to make a new record
        if(CovidRecord.readyStoreRecord(MapsActivity.covidRecordLastStoreTimestamp, MapsActivity.deltaCovidRecordStoreTimeMS, MapsActivity.covidRecordLastStoreLocation, MapsActivity.currentLocation, MapsActivity.deltaCovidRecordStoreLocationM)) {


            Date d = new Date();

            //CEMIL ONLY FOR Classifier Activity --rotate the image if running oNLY in portrait mode 90 degree (or -90) ---test it out.
            //see if app is running in mode to store Images to Firebase Storage or not
            if(MapsActivity.flagStoreImageFiles) {
                //------------------------------------------------------
                //first store the Image to Firebase Storage
                //build name so it is unique associated with the User's id and date
                String filename = MapsActivity.userIdFirebase + "_" + d.getTime();
                StorageReference newImageFileRef = MapsActivity.imagesFirebaseRef.child(filename);
                //convert to a jpeg format and then to a byte[] array which is what is used by Firebase storage task to upload
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rgbFrameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                //upload so that get back URL of location it was uploaded ---will be an asynchronous task
                UploadTask uploadTask = newImageFileRef.putBytes(data);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return newImageFileRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            imageFileURL = downloadUri.toString();
                        } else {
                            // Handle failures
                            imageFileURL = null;
                        }

                        //alter the CovidRecord to add the ImageURL to myRecord
                        myRecord.setFilenameURL(imageFileURL);


                        // ask helper to push record to db
                        MapsActivity.myFirestoreHelper.addRecord(myRecord);

                        //update the last time record stored
                        MapsActivity.covidRecordLastStoreTimestamp =  System.currentTimeMillis();

                        //update the last location record stored
                        MapsActivity.covidRecordLastStoreLocation = location;
                    }
                });
            }//end case of storing Images

            //-------------------------------------------------
            else {  //NO image file storage  --set fileURL to null in CovidRecord

                myRecord.setFilenameURL(null);

                // ask helper to push record to db
                MapsActivity.myFirestoreHelper.addRecord(myRecord);

                //update the last time record stored
                MapsActivity.covidRecordLastStoreTimestamp = System.currentTimeMillis();

                //update the last location record stored
                MapsActivity.covidRecordLastStoreLocation = location;
            }
        }

    }


}
