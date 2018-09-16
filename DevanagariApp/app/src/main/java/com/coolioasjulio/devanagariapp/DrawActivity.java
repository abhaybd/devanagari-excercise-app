package com.coolioasjulio.devanagariapp;

import android.app.AlertDialog;
import android.content.Context;
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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.coolioasjulio.devanagariapp.ImageUtils.PredictImageTask.PredictionCallback;

public class DrawActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DrawActivity";

    private int toGuess;
    private int numCorrect;
    private int timeout;
    private int sessionLength, elapsedQuestions;
    private DrawingView drawingView;
    private SessionGenerator sessionGenerator;
    private Recognizer recognizer;
    private Button clearButton, nextButton, playSoundButton;
    private ProgressBar progressBar;
    private MediaPlayer mediaPlayer;
    private List<Timer> timers;
    private Context context;
    private TextView correctLabel, incorrectLabel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        int defaultTimeout = getResources().getInteger(R.integer.default_timeout);

        Intent intent = getIntent();
        timeout = intent.getIntExtra(Values.TIMEOUT_KEY, defaultTimeout);
        sessionLength = intent.getIntExtra(Values.SESSION_LENGTH_KEY, 50);
        sessionGenerator = new SessionGenerator(Values.NUM_CHARS, sessionLength);

        this.context = this;
        timers = new ArrayList<>();

        correctLabel = findViewById(R.id.correct_label);
        incorrectLabel = findViewById(R.id.incorrect_label);
        updateScore();

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
                nextButton.setEnabled(false);
                break;
            case R.id.play_sound_button:
                playPromptAndStartTimer();
                break;
            case R.id.next_button:
                releaseMediaPlayer();
                disableUI();
                Bitmap bitmap = drawingView.getBitmap();
                PredictionCallback callback = new PredictionCallback() {
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

                        numCorrect += correct?1:0;
                    }
                };
                recognizer.getPrediction(bitmap, callback);
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(getString(R.string.quit_to_menu_confirm));

        builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cancelAllTimers();
                dialogInterface.dismiss();
                Intent intent = new Intent(DrawActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        builder.setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateScore(){
        correctLabel.setText(getString(R.string.draw_activity_correct, numCorrect));
        incorrectLabel.setText(getString(R.string.draw_activity_incorrect, elapsedQuestions - numCorrect));
    }

    private void startReviewActivity(){
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra(Values.SESSION_LENGTH_KEY, sessionLength);
        intent.putExtra(Values.NUM_CORRECT_KEY, numCorrect);
        intent.putExtra(Values.INCORRECT_BREAKDOWN_KEY, sessionGenerator.getIncorrectRecord());
        startActivity(intent);
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
        if(mediaPlayer.isPlaying()) mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    /**
     * Play the sound prompt, and then start the countdown timer.
     */
    private void playPromptAndStartTimer(){
        mediaPlayer = getMediaPlayer(toGuess);
        if(!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    startTimeoutTimer();
                }
            });
        }
    }

    private void startTimeoutTimer() {
        setTimer(timeout, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        releaseMediaPlayer();
                        String msg = getResources().getString(R.string.result_popup_timeout);
                        msg = String.format("%s %s", msg, Values.toLetter(context,toGuess));
                        notifyUser(msg,false);
                    }
                });
            }
        });
    }

    private void cancelAllTimers() {
        for(Timer t : timers) {
            t.cancel();
        }

        timers.clear();
    }

    private void setTimer(int timeout, final Runnable onTimer){
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTimer.run();
            }
        }, timeout * 1000); // Convert seconds to milliseconds
        timers.add(timer);
    }

    private void nextQuestion(boolean lastCorrect){
        elapsedQuestions++;
        drawingView.clear();
        if(elapsedQuestions < sessionLength){
            toGuess = sessionGenerator.next(lastCorrect);
            Log.d(TAG, String.format(Values.LOCALE, "New toGuess: %d", toGuess));
            playPromptAndStartTimer();
        } else {
            double accuracy = (double)numCorrect/(double)sessionLength;
            Log.d(TAG,String.format("Session finished with accuracy: %.2f", accuracy));
            nextButton.setEnabled(false);
            clearButton.setEnabled(false);
            cancelAllTimers();
            startReviewActivity();
        }
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
        builder.setCancelable(correct); // Cancelable if correct, explicit button press if incorrect
        builder.setTitle(getResources().getString(R.string.result_popup_title));
        builder.setMessage(message);

        // gross workaround so it can be overridden by the button press (in the callback)
        final AtomicBoolean ab = new AtomicBoolean(correct);
        if(!correct){
            builder.setNegativeButton(getResources().getString(R.string.result_popup_override), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    numCorrect++;
                    ab.set(true);
                    dialogInterface.dismiss();
                }
            });
        }
        builder.setPositiveButton(getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                updateScore();
                nextQuestion(ab.get());
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
}
