package com.coolioasjulio.devanagariapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener{
    private static final String TAG = "MAIN_ACTIVITY";

    private String currentSelection;
    private List<String> options;
    private int[] intOptions;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = findViewById(R.id.session_spinner);

        intOptions = getResources().getIntArray(R.array.session_length);
        options = new ArrayList<>();
        for(int length: intOptions){
            options.add(String.format(Values.LOCALE, "%d", length));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
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
    public void onNothingSelected(AdapterView<?> adapterView) {}

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, DrawActivity.class);
        int index = options.indexOf(currentSelection);
        if (index < 0) throw new AssertionError();
        intent.putExtra(Values.SESSION_LENGTH_KEY, intOptions[index]);
        startActivity(intent);
    }
}
