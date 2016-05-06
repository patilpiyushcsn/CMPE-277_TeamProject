package com.example.piyush.smartparking;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Toast;

public class RangeActivity extends AppCompatActivity {

    NumberPicker numberPicker;
    int numPickerVal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_range);

        numberPicker = (NumberPicker)findViewById(R.id.numberPicker);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(3);
        numberPicker.setValue(0);
        numberPicker.setWrapSelectorWheel(false);

        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                numPickerVal = newVal;
            }
        });
    }

    public void findParking(View view)
    {
        Toast.makeText(this, "Selected Range: " + Integer.toString(numPickerVal),
                Toast.LENGTH_LONG).show();
    }
}
