package edu.ilab.covid_id.crowd.CrowdLearnDetect;

import android.util.Log;

import edu.ilab.covid_id.localize.tflite.Classifier;

public class ResultCount {

    String label;
    float risk;
    float confidence;
    int total_persons;

    int min_person = 3;
    int med_person = 7;


    ResultCount(Classifier.Recognition result, int lowCnt, float riskThresholdCaution_crowd, float riskThresoldHigh_crowd)
    {
        this.total_persons = total_persons;
        this.label = result.getTitle();

        if(lowCnt > 1)
        {
            this.label = "Med";
        }
        else
        {
            this.label = result.getTitle();
        }

//        if (this.total_persons < this.min_person)
//        {
//            this.label = "Low";
//        }
//        else if(this.total_persons < this.med_person)
//        {
//            this.label = "Med";
//        }
//        else
//        {
//            this.label = "High";
//        }

        Log.d("PersonCount", label);

        confidence = result.getConfidence();
        if(label == "Low")
        {
            risk = riskThresoldHigh_crowd + (100-riskThresoldHigh_crowd) * confidence;
            if (risk > 100) risk = 100f;
        }
        else if (label == "Med") //range [riskThresoldCaution_crowd to riskThresoldHigh_crowd]
        {
            risk = riskThresholdCaution_crowd + (riskThresoldHigh_crowd-riskThresholdCaution_crowd) * confidence;
            if (risk < riskThresholdCaution_crowd || risk > riskThresoldHigh_crowd) risk = riskThresoldHigh_crowd;
        }
        else if ( label == "High")
        {
            risk = riskThresholdCaution_crowd - (riskThresholdCaution_crowd) * confidence;
            if (risk < 0 || risk > riskThresholdCaution_crowd) risk = riskThresholdCaution_crowd;
        }
        else
        {
            risk = 90f;
        }

        Log.d("ResultCountRisk", "risk" + risk);

    }

    public float getConfidence()
    {
        return this.confidence;
    }

}
