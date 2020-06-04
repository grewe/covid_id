package edu.ilab.covid_id.data;

import com.google.firebase.firestore.FirebaseFirestore;

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
    }

    /**
     * adds a record to the firestore CovidRecord document collection
     * @param record
     * @return an error code (0: success, -1: failure)
     */
    public int addRecord(CovidRecord record) {
        mFirestore.collection("CovidRecord").add(record);
        return 0;
    }

}
