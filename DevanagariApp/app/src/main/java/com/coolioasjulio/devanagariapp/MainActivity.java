package com.coolioasjulio.devanagariapp;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener, TextWatcher{
    private static final String TAG = "MainActivity";

    private String currentSelection;
    private List<String> options;
    private int[] intOptions;
    private EditText timeoutInput;
    private Button startButton;
    private Button infiniteButton;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = findViewById(R.id.session_spinner);
        timeoutInput = findViewById(R.id.timeout);

        // Set the timeout to the default timeout
        String defaultTimeout = String.valueOf(getResources().getInteger(R.integer.default_timeout));
        timeoutInput.setText(defaultTimeout);
        timeoutInput.addTextChangedListener(this);

        intOptions = getResources().getIntArray(R.array.session_length);
        options = new ArrayList<>();

        Log.d(TAG, NumberFormat.getInstance().format(1));
        for(int length: intOptions){
            options.add(Localization.localizeInteger(length));
            Log.d(TAG, Localization.localizeInteger(length));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        currentSelection = options.get(0);

        spinner.setOnItemSelectedListener(this);

        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(this);

        infiniteButton = findViewById(R.id.infinite_button);
        infiniteButton.setOnClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        currentSelection = (String)adapterView.getItemAtPosition(pos);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    @Override
    public void onClick(View view) {
        Intent intent;
        switch(view.getId()){
            case R.id.infinite_button:
                intent = new Intent(this, InfiniteActivity.class);
                startActivity(intent);
                break;

            case R.id.start_button:
                int index = options.indexOf(currentSelection);
                if (index < 0) throw new AssertionError();

                int timeout = Integer.parseInt(timeoutInput.getText().toString());

                intent = new Intent(this, DrawActivity.class);
                intent.putExtra(Values.SESSION_LENGTH_KEY, intOptions[index]);
                intent.putExtra(Values.TIMEOUT_KEY, timeout);
                startActivity(intent);
                break;

            default:
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        boolean enabled = editable.length() != 0;
        startButton.setEnabled(enabled);
        infiniteButton.setEnabled(enabled);
    }
}
