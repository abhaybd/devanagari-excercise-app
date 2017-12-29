package com.coolioasjulio.devanagariapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

public class DrawActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "DrawActivity";
    private static final int NUM_CHARS = 36;

    private int toGuess;
    private int numCorrect;
    private int numQuestions, elapsedQuestions;
    private DrawingView drawingView;
    private SessionGenerator sessionGenerator;
    private Recognizer recognizer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        Intent intent = getIntent();
        numQuestions = intent.getIntExtra(MainActivity.SESSION_LENGTH_KEY, 60);
        sessionGenerator = new SessionGenerator(NUM_CHARS,numQuestions);

        ProgressBar spinner = findViewById(R.id.model_loading_spinner);
        recognizer = new Recognizer(this, spinner);

        drawingView = findViewById(R.id.scratch_pad);
        drawingView.initializePen();
        drawingView.setPenSize(55f);
        drawingView.setPenColor(Color.BLACK);

        Button clear = findViewById(R.id.clear_button);
        clear.setOnClickListener(this);

        Button next = findViewById(R.id.next_button);
        next.setOnClickListener(this);
    }

    private void nextQuestion(boolean lastCorrect){
        elapsedQuestions++;
        toGuess = sessionGenerator.next(lastCorrect);
        drawingView.clear();
        if(elapsedQuestions == numQuestions){
            Log.d(TAG,"Session finished!");
        }
    }

    private void notifyUser(String message, boolean correct){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(getResources().getString(R.string.result_popup_title));
        builder.setMessage(message);
        if(!correct){
            builder.setNegativeButton(getResources().getString(R.string.result_popup_override), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    numCorrect++;
                    dialogInterface.dismiss();
                }
            });
        }
        builder.setPositiveButton(getResources().getString(R.string.result_popup_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.clear_button:
                drawingView.clear();
                break;
            case R.id.next_button:
                if(!recognizer.isReady()) break;
                Bitmap bitmap = drawingView.getBitmap();
                int bitmapWidth = bitmap.getWidth();
                int bitmapHeight = bitmap.getHeight();
                int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(pixels,0,bitmapWidth,0,0, bitmapWidth,bitmapHeight);
                int pred = recognizer.getPrediction(bitmap);

                boolean correct = (pred == toGuess);
                String message = getResources().getString(R.string.result_popup_correct);
                if(!correct) message = getResources().getString(R.string.result_popup_incorrect);
                notifyUser(message, correct);

                numCorrect += correct?1:0;
                nextQuestion(correct);
                break;
            default:
                break;
        }
    }
}
