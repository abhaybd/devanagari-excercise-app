package com.coolioasjulio.devanagariapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener{
    private static final String TAG = "MainActivity";

    private String currentSelection;
    private List<String> options;
    private int[] intOptions;
    private EditText timeoutInput;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = findViewById(R.id.session_spinner);
        timeoutInput = findViewById(R.id.timeout);

        // Set the timeout to the default timeout
        String defaultTimeout = String.format(Values.LOCALE, "%d",
                getResources().getInteger(R.integer.default_timeout));
        timeoutInput.setText(defaultTimeout);

        intOptions = getResources().getIntArray(R.array.session_length);
        options = new ArrayList<>();
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("hi", "IN"));
        for(int length: intOptions){
            options.add(nf.format(length));
            Log.d(TAG, nf.format(length));
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
        int index = options.indexOf(currentSelection);
        if (index < 0) throw new AssertionError();

        int timeout = Integer.parseInt(timeoutInput.getText().toString());

        Intent intent = new Intent(this, DrawActivity.class);
        intent.putExtra(Values.SESSION_LENGTH_KEY, intOptions[index]);
        intent.putExtra(Values.TIMEOUT_KEY, timeout);
        startActivity(intent);
    }
}
