package com.coolioasjulio.devanagariapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class InfiniteActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private static String TAG = "InfiniteActivity";

    private int toGuess;
    private DrawingView drawingView;
    private Recognizer recognizer;
    private Button clearButton, nextButton, playSoundButton;
    private ProgressBar progressBar;
    private MediaPlayer mediaPlayer;
    private Context context;
    private Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infinite);

        this.context = this;

        Spinner letterSpinner = findViewById(R.id.letter_spinner);
        String[] options = getResources().getStringArray(R.array.letters);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        letterSpinner.setAdapter(adapter);

        letterSpinner.setOnItemSelectedListener(this);

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

        drawingView.setOnTouchDownEvent(new Runnable() {
            @Override
            public void run() {
                if(!nextButton.isEnabled()) nextButton.setEnabled(true);
            }
        });

        progressBar = findViewById(R.id.model_loading_spinner);
        disableUI();
        Runnable onReadyCallback = new Runnable() {
            @Override
            public void run() {
                enableUI();
                nextQuestion();
            }
        };
        recognizer = new Recognizer(this, onReadyCallback);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.clear_button:
                drawingView.clear();
                nextButton.setEnabled(false);
                break;
            case R.id.play_sound_button:
                playPrompt();
                break;
            case R.id.next_button:
                disableUI();
                Bitmap bitmap = drawingView.getBitmap();
                ImageUtils.PredictImageTask.PredictionCallback callback = new ImageUtils.PredictImageTask.PredictionCallback() {
                    @Override
                    public void onFinished(int output) {
                        boolean correct = (output == toGuess);
                        Log.d(TAG, String.format("To guess: %s, Network output: %s", toGuess, output));
                        String message = getResources().getString(R.string.result_popup_correct);
                        if(!correct) {
                            String correctLetter = Values.toLetter(context, toGuess);
                            message = String.format("%s %s",
                                    getResources().getString(R.string.result_popup_incorrect),
                                    correctLetter);
                        }
                        notifyUser(message, correct);
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

    private void releaseMediaPlayer(){
        if(mediaPlayer == null) return;
        if(mediaPlayer.isPlaying()) mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void playPrompt(){
        mediaPlayer = getMediaPlayer(toGuess);
        if(!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void nextQuestion(){
        drawingView.clear();
        Log.d(TAG, String.format(Values.LOCALE, "New toGuess: %d", toGuess));
        playPrompt();
    }

    private void setTimer(int timeout, final Runnable onTimer){
        if(timer != null) timer.cancel();
        timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTimer.run();
            }
        }, timeout * 1000); // Convert seconds to milliseconds
    }

    private void disableUI(){
        progressBar.setVisibility(View.VISIBLE);
        clearButton.setEnabled(false);
        nextButton.setEnabled(false);
        playSoundButton.setEnabled(false);
        drawingView.setDrawingEnabled(false);
    }

    private void enableUI(){
        progressBar.setVisibility(View.GONE);
        clearButton.setEnabled(true);
        playSoundButton.setEnabled(true);
        drawingView.setDrawingEnabled(true);
        drawingView.clear();
    }

    private void notifyUser(String message, boolean correct){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true); // Cancelable if correct, explicit button press if incorrect
        builder.setTitle(getResources().getString(R.string.result_popup_title));
        builder.setMessage(message);

        builder.setPositiveButton(getResources().getString(R.string.result_popup_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                nextQuestion();
                enableUI();
            }
        });
        dialog.show();

        if(correct) {
            setTimer(Values.CORRECT_PROMPT_TIMEOUT, new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        toGuess = i;
        releaseMediaPlayer();
        playPrompt();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}
}
