package com.coolioasjulio.devanagariapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.util.Locale;

import static com.coolioasjulio.devanagariapp.ImageUtils.PredictImageTask.PredictionCallback;

public class DrawActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "DrawActivity";
    private static final int NUM_CHARS = 36;

    private int toGuess;
    private int numCorrect;
    private int numQuestions, elapsedQuestions;
    private DrawingView drawingView;
    private SessionGenerator sessionGenerator;
    private Recognizer recognizer;
    private Button clearButton, nextButton, playSoundButton;
    private ProgressBar progressBar;
    private MediaPlayer mediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        Intent intent = getIntent();
        numQuestions = intent.getIntExtra(MainActivity.SESSION_LENGTH_KEY, 60);
        sessionGenerator = new SessionGenerator(NUM_CHARS,numQuestions);

        drawingView = findViewById(R.id.scratch_pad);
        drawingView.initializePen();
        drawingView.setPenSize(55f);
        drawingView.setPenColor(Color.BLACK);

        clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(this);

        nextButton = findViewById(R.id.next_button);
        nextButton.setOnClickListener(this);

        playSoundButton = findViewById(R.id.play_sound_button);
        playSoundButton.setOnClickListener(this);

        progressBar = findViewById(R.id.model_loading_spinner);
        disableUI();
        Runnable onReadyCallback = new Runnable() {
            @Override
            public void run() {
                enableUI();
                nextQuestion(false);
            }
        };
        recognizer = new Recognizer(this, onReadyCallback);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.clear_button:
                drawingView.clear();
                break;
            case R.id.play_sound_button:
                playPrompt();
                break;
            case R.id.next_button:
                mediaPlayer.release();
                mediaPlayer = null;
                disableUI();
                Bitmap bitmap = drawingView.getBitmap();
                PredictionCallback callback = new PredictionCallback() {
                    @Override
                    public void onFinished(int output) {
                        boolean correct = (output == toGuess);
                        Log.d(TAG, String.format("To guess: %s, Network output: %s", toGuess, output));
                        String message = getResources().getString(R.string.result_popup_correct);
                        if(!correct) message = getResources().getString(R.string.result_popup_incorrect);
                        notifyUser(message, correct);

                        numCorrect += correct?1:0;
                        nextQuestion(correct);
                        enableUI();
                    }
                };
                recognizer.getPrediction(bitmap, callback);
                break;
            default:
                break;
        }
    }

    /**
     * If the current mediaPlayer is still active, return it. Otherwise, init a new one.
     * @param toGuess Character index for the user to guess.
     * @return MediaPlayer instance pointing to the mp3 file denoted by `toGuess`.
     */
    private MediaPlayer getMediaPlayer(int toGuess){
        if(mediaPlayer != null) return mediaPlayer;
        String fileName = String.format(Locale.US, "aud%02d", toGuess);
        Log.d(TAG, "Getting resource id for audio file: " + fileName);
        int resID = getResources().getIdentifier(fileName,"raw", getPackageName());
        return MediaPlayer.create(this, resID);
    }

    private void playPrompt(){
        mediaPlayer = getMediaPlayer(toGuess);
        if(!mediaPlayer.isPlaying()) mediaPlayer.start();
    }

    private void nextQuestion(boolean lastCorrect){
        elapsedQuestions++;
        drawingView.clear();
        if(elapsedQuestions < numQuestions){
            toGuess = sessionGenerator.next(lastCorrect);
            playPrompt();
        } else {
            double accuracy = (double)numCorrect/(double)numQuestions;
            Log.d(TAG,String.format("Session finished with accuracy: %.2f", accuracy));
            nextButton.setClickable(false);
            clearButton.setClickable(false);
        }
    }

    private void disableUI(){
        progressBar.setVisibility(View.VISIBLE);
        clearButton.setClickable(false);
        nextButton.setClickable(false);
        playSoundButton.setClickable(false);
        drawingView.setDrawingEnabled(false);
    }

    private void enableUI(){
        progressBar.setVisibility(View.GONE);
        clearButton.setClickable(true);
        nextButton.setClickable(true);
        playSoundButton.setClickable(true);
        drawingView.setDrawingEnabled(true);
        drawingView.clear();
    }

    private void notifyUser(String message, boolean correct){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
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
}
