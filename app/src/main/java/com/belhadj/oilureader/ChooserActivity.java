package com.belhadj.oilureader;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class ChooserActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chooser);

        findViewById(R.id.okbtn)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int res = 0;
                        RadioGroup rgroup = findViewById(R.id.rgroup);
                        if(rgroup.getCheckedRadioButtonId() == R.id.radioButton2) res = 1;
                        if(rgroup.getCheckedRadioButtonId() == R.id.radioButton3) res = 2;
                        if(rgroup.getCheckedRadioButtonId() == R.id.radioButton4) res = 3;

                        Intent intent=new Intent();
                        intent.putExtra("CODE", res);
                        intent.setClass(ChooserActivity.this, MainActivity.class);

                        startActivity(intent);
                        finish();
                    }
                });

    }

}