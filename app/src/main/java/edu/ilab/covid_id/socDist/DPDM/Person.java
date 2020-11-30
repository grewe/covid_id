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
     * where Person.result.getLocation() retrieves the bounding box of the person.
     * Note:  this is done by taking the upper right point of this.person
     * and the upper.left of person 2 bounding box and measuring the 2D distance between them.
     * @param person2
     * @return
     */
    float distance(Person person2){

        //current person, this object, has bounding box of this.result.getLocation()
        RectF current_person = this.result.getLocation();

        //2nd person's bounding box is person2.result.getLocation().left;
        RectF person_2 = person2.result.getLocation();
        //storing the person1(top,right) coordinates

        float x1 = current_person.right;
        float y1 = current_person.top;
        //System.out.println()
        //Storing person2(top,left) coordinates
        float x2 = person_2.left;
        float y2 = person_2.top;
        //result

        float result = (float) Math.sqrt(Math.pow(x2 - x1, 2) +  Math.pow(y2 - y1, 2)* 1.0) ;
        System.out.println("distance is " +result);
        return result;
    }

    /**
     * This calculates 2D image distance based on the centroid of this.person bounding box
     * and person 2 bounding box
     * @param person2
     * @return
     */
    float distanceCentroid(Person person2){
        //get the centroid for person 1
        float cx1 = this.result.getLocation().centerX();
        float cy1 = this.result.getLocation().centerY();
        //get the centroid for person 2
        float cx2 = person2.result.getLocation().centerX();
        float cy2 = person2.result.getLocation().centerY();

        float result = (float) Math.sqrt(Math.pow(cx2 - cx1, 2) +  Math.pow(cy2 - cy1, 2)* 1.0) ;
        System.out.println("distance is " +result);
        return result;

    }

    /**
     * utiltiy function to measure iou between 2 person's bounding boxes
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
        //first calculate the intersection
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
