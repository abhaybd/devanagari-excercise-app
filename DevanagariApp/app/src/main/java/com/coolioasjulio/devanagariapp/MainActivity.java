package com.coolioasjulio.devanagariapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener{
    public static final String SESSION_LENGTH_KEY = "SESSION_LENGTH";
    private static final String TAG = "MAIN_ACTIVITY";

    private String currentSelection;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = findViewById(R.id.session_spinner);
        List<String> options = Arrays.asList(getResources().getStringArray(R.array.session_length));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        currentSelection = options.get(0);

        spinner.setOnItemSelectedListener(this);
        findViewById(R.id.start_button).setOnClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        currentSelection = (String)adapterView.getItemAtPosition(pos);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, DrawActivity.class);
        intent.putExtra(SESSION_LENGTH_KEY, Integer.valueOf(currentSelection));
        startActivity(intent);
    }
}
