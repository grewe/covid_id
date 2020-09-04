package edu.ilab.covid_id.ir;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import edu.ilab.covid_id.R;

/**
 * Shivali ---- this is YOUR class
 * going to load some sample IR iamge/ temp files located statically in the assets directory
 * or drawable directories.....
 *
 * going to add support for Mobile SDK from flir
 * read in images and display ...play with extracting temperature data
 * understand palettes and REFER TO THE WHITEBOARD PICTURE --develop a method to
 * take temp data -> mask to [20F...100F range] set other values to 0F -> map temp values
 * to a greyscale image [0 to 255] linearly or whatever function you want as long as one to one and invertible
 * display the image in the IMageView that is part of the interface
 *
 * YOU could only load the same static image each time OR have a button in the
 * interface that lets you load up an image from the camera's photo album (store images on camera)
 *
 */
public class IRStaticDataExploreActivity extends FragmentActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ir_static_data_explore_activity_layout);
    }


}
