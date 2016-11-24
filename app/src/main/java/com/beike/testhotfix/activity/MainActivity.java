package com.beike.testhotfix.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.beike.testhotfix.R;
import com.beike.testhotfix.model.People;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "itper";

    private Button btTest;

    private People people;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btTest = (Button) findViewById(R.id.bt_test);

        people = new People();

        btTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, people.say(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
