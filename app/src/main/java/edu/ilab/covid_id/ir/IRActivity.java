package edu.ilab.covid_id.ir;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import edu.ilab.covid_id.R;

public class IRActivity extends edu.ilab.covid_id.localize.DetectorActivity {





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ir_actvity_layout);
    }

}
