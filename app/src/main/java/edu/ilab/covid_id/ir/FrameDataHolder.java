package edu.ilab.covid_id.ir;




import android.graphics.Bitmap;

import com.flir.thermalsdk.image.ThermalImage;

class FrameDataHolder {

    public final Bitmap thermalBitmap;
    public final Bitmap dcBitmap;
    //public final ThermalImage ti;
    public final double[][] tempArray;

    FrameDataHolder(Bitmap thermalBitmap, Bitmap dcBitmap, double[][] tempArray){
        this.thermalBitmap = thermalBitmap;
        this.dcBitmap = dcBitmap;
        this.tempArray=tempArray;

    }
}
