package edu.ilab.covid_id.data;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;

import static android.content.ContentValues.TAG;

/**
 * Class to facilitate connections to the Firestore DB
 */
public class FirestoreHelper {
    /**
     * handle for connection for Firestore
     */
    private FirebaseFirestore mFirestore;

    /**
     * Constructor that initializes a connection to the firestore
     */
    public FirestoreHelper() {
        mFirestore = FirebaseFirestore.getInstance();
       // mFirestore = FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    /**
     * adds a record to the firestore CovidRecord document collection
     * @param record
     * @return 0
     */
    public int addRecord(CovidRecord record) {
       // mFirestore.collection("CovidRecord").add(record);

        mFirestore.collection("CovidRecord").add(record).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("DBSTORE", "DocumentSnapshot written with ID: " + documentReference.getId());
            }


        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DBSTORE", "Error adding document", e);
                    }
                });
        return 0;
    }


    /**
     * adds a record to the firestore MaskRecord document collection
     * @param record
     * @return 0
     */
    public int addRecord(MaskRecord record) {
        // mFirestore.collection("CovidRecord").add(record);

        mFirestore.collection("MaskRecord").add(record).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("DBSTORE", "DocumentSnapshot written with ID: " + documentReference.getId());
            }


        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DBSTORE", "Error adding document", e);
                    }
                });
        return 0;
    }



    /**
     * adds a record to the firestore CrowdRecord document collection
     * @param record
     * @return an error code (0: success, -1: failure)
     */
    public int addRecord(CrowdRecord record) {
        // mFirestore.collection("CovidRecord").add(record);

        mFirestore.collection("MaskRecord").add(record).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("DBSTORE", "DocumentSnapshot written with ID: " + documentReference.getId());
            }


        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DBSTORE", "Error adding document", e);
                    }
                });
        return 0;
    }

    /**
     * adds a record to the firestore FeverRecord document collection
     * @param record
     * @return 0
     */
    public int addRecord(FeverRecord record) {
        // mFirestore.collection("CovidRecord").add(record);

        mFirestore.collection("MaskRecord").add(record).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("DBSTORE", "DocumentSnapshot written with ID: " + documentReference.getId());
            }


        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DBSTORE", "Error adding document", e);
                    }
                });
        return 0;
    }

    /**
     * adds a record to the firestore FeverRecord document collection
     * @param record
     * @return 0
     */
    public int addRecord(SocDistRecord record) {
        // mFirestore.collection("CovidRecord").add(record);

        mFirestore.collection("SocDistRecord").add(record).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("DBSTORE", "DocumentSnapshot written with ID: " + documentReference.getId());
            }


        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DBSTORE", "Error adding document", e);
                    }
                });
        return 0;
    }



}
