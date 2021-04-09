package edu.ilab.covid_id.crowd.CrowdLearnDetect;

import android.util.Log;

public class PersonCount {

    String label;
    float risk;
    float confidence;
    int total_persons;

    int min_person = 3;
    int med_person = 7;


    PersonCount(Person person, int total_persons, float riskThresholdCaution_crowd, float riskThresoldHigh_crowd)
    {
        this.total_persons = total_persons;

        if (this.total_persons < this.min_person)
        {
            this.label = "Low";
        }
        else if(this.total_persons < this.med_person)
        {
            this.label = "Med";
        }
        else
        {
            this.label = "High";
        }

        Log.d("PersonCount", label);

        confidence = person.result.getConfidence();
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

        Log.d("PersonCountRisk", "risk" + risk);

    }

    public float getConfidence()
    {
        return this.confidence;
    }

}
