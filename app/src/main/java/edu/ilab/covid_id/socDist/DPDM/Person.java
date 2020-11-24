package edu.ilab.covid_id.socDist.DPDM;
import android.graphics.RectF;

import edu.ilab.covid_id.localize.tflite.Classifier;
public class Person {
    Classifier.Recognition result;
    int unique_id;

    float io_threshold_different_People = 0.9f;


    private float location;

    Person(int id, Classifier.Recognition result){
        this.unique_id=id;
        this.result =  new Classifier.Recognition(result);

    }
    //tell if different people based on ids and based on IOU(overlap)
    Boolean different(Person person2) {

        if (this.unique_id == person2.unique_id)
            return false;

        //measure IOU
        float iou = measureIOU(this.result.getLocation(), person2.result.getLocation()); //look for iou code online

        //if the IOU is really high then likely the same person -
        if (iou > 0.9)
            return false;

        return true;

    }


    /**
     * implmeent the clacluation of distance between 2 people, this person and person 2.
     * where Person.result.getLocation() retrieves the bounding box of the person
     * @param person2
     * @return
     */
    float distance(Person person2){

        //current person, this object, has bounding box of this.result.getLocation()
        //RectF current_person = this.result.getLocation();

        //2nd person's bounding box is person2.result.getLocation().left;
        //RectF person_2 = person2.result.getLocation();


        return 1.0f;
    }

    /**
     * untiltiy function to measure iou between 2 person's bounding boxes
     * @param person1Location
     * @param person2Location
     * @return
     */
    float measureIOU(RectF person1Location, RectF person2Location) {

        //create new local rectangels and union and intersection update the rectf working on
        RectF intersection  = new RectF(person1Location);
        intersection.intersect(person2Location);
        float intersection_area = (float) intersection.width()*intersection.height();

        //intersection over union
        //first caculuate the intersection
        RectF union = new RectF(person1Location);
        union.union(person2Location);
        float union_area = (float) union.width()*union.height();

        float iou = intersection_area /union_area;


        //saftey check
        if(iou< 0.0f || iou > 1.0f)
            return 0.0f;  //this will be our default --eroneous but, should never hit this


        return iou;
    }




}
