package com.plutoo.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout super semplice creato via codice
        TextView tv = new TextView(this);
        tv.setText("Plutoo TEST");
        tv.setTextSize(24f);
        tv.setPadding(40, 40, 40, 40);

        setContentView(tv);
    }
}
