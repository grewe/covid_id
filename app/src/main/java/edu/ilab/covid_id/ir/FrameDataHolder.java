package edu.ilab.covid_id.ir;




import android.graphics.Bitmap;

class FrameDataHolder {

    public final Bitmap msxBitmap;
    public final Bitmap dcBitmap;

    FrameDataHolder(Bitmap msxBitmap, Bitmap dcBitmap){
        this.msxBitmap = msxBitmap;
        this.dcBitmap = dcBitmap;
    }
}
