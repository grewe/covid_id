package edu.ilab.covid_id.ir;




import android.graphics.Bitmap;

class FrameDataHolder {

    public final Bitmap thermalBitmap;
    public final Bitmap dcBitmap;

    FrameDataHolder(Bitmap thermalBitmap, Bitmap dcBitmap){
        this.thermalBitmap = thermalBitmap;
        this.dcBitmap = dcBitmap;
    }
}
