package edu.ilab.covid_id.crowd.CrowdCount;


import android.util.Log;

/**
 * reperst the Social Distance between 2 people
 */
public class SocDist {

    float distance;  //distance between 2 people
    String label;  //corresponding label good, bad, caution
    float risk = 90.0f;  //risk(0-100) --70to100 bad, 40-70 caution, 0-39 good
    // associated and based on confidences of the 2 detected people + distance
    float confidence;  //confidence score related to confidences of 2 people detection
    // average (person1.confidence, person2.confidence)  or minimum

    float distanceMinGood = 200.0f;
    float distanceMinCaution = 150.0f;





    /**
     * constructor to measure distance, label, etc to create instance of SocDist for 2 people
     * @param person1
     * @param person2
     */
    SocDist(Person person1, Person person2, float riskThresholdCaution_SocDist, float riskThresholdHigh_SocDist){


        //fist calculate the distance between 2 people
        this.distance = person1.distanceCentroid(person2);



        //based on distance create a label
        if(this.distance > this.distanceMinGood){
            this.label ="Good";
        }
        else if(this.distance > this.distanceMinCaution)
            this.label = "Caution";
        else
            this.label ="Bad";



        //based on the risk thresholds for our classes we will generate a risk factor based
        // on the confidence of this.results.getConfidence() and label.

        Log.d("SocDist", label);
        //get min confidence for person 1 and person 2
        confidence = Math.min(person1.result.getConfidence(), person2.result.getConfidence());
        //high risk is mapped between [riskThresholdHigh_Mask to 100]
        if(label =="Good") {
            //based on both confidence value will set risk in range
            risk = riskThresholdHigh_SocDist+ (100-riskThresholdHigh_SocDist) * confidence;
            if( risk > 100.0) risk = 100.0f; //saftey
        }
        else if(label =="Caution") {  //range [riskThresholCaution_Mask to riskTHresholdHigh_Mask]
            //based on both confidence value will set risk in range
            risk = riskThresholdCaution_SocDist + (riskThresholdHigh_SocDist-riskThresholdCaution_SocDist) * confidence;
            if( risk < riskThresholdCaution_SocDist || risk > riskThresholdHigh_SocDist) risk = riskThresholdHigh_SocDist; //saftey
        }
        else if(label == "Bad") {  //range [0 to riskThresholdCaution_Mask]
            //based on both confidence value will set risk in range
            risk = riskThresholdCaution_SocDist - (riskThresholdCaution_SocDist) * confidence;
            if( risk < 0 || risk > riskThresholdCaution_SocDist) risk = riskThresholdCaution_SocDist; //saftey
        }
        else {  //never should execute case --but, in case use default value of 90.0f
            risk = 90.0f;
        }

        Log.d("SocDistActivity: ",  "risk" + risk);


    }

    /**
     * returns the confidence associated with this SocDist object
     * @return float from 0.0 to 1.0 representing the combined (min in this case --see consructor) confidence of
     * 2 people used to create this SocDist object
     */
    public float getConfidence(){
        return this.confidence;
    }
}