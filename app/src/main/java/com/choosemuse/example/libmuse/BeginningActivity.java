package com.choosemuse.example.libmuse;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class BeginningActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beginning);
        getActionBar().setTitle("Hello");

    }
}
