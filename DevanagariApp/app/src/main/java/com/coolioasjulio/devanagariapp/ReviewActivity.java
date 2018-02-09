package com.coolioasjulio.devanagariapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class ReviewActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        Intent intent = getIntent();
        int sessionLength = intent.getIntExtra(Values.SESSION_LENGTH_KEY, -1);
        int numCorrect = intent.getIntExtra(Values.NUM_CORRECT_KEY, -1);
        int[] incorrectRecord = intent.getIntArrayExtra(Values.INCORRECT_BREAKDOWN_KEY);
        if(numCorrect == -1 || sessionLength == -1 || incorrectRecord == null) throw new AssertionError();

        double accuracy = (double)numCorrect/(double)sessionLength;

        String correctText = getResources().getString(R.string.review_activity_correct);
        String totalQuestionsText = getResources().getString(R.string.review_activity_questions);
        String accuracyText = getResources().getString(R.string.review_activity_accuracy);

        TextView textView = findViewById(R.id.review_activity_statistics);
        textView.setText(String.format(Values.LOCALE,
                "%s: %s\n" +
                "%s: %s\n" +
                "%s: %s%%",
                correctText, Localization.localizeInteger(numCorrect), totalQuestionsText,
                Localization.localizeInteger(sessionLength), accuracyText,
                Localization.localize(accuracy*100d, "%.2f")));

        TextView breakdownTextView = findViewById(R.id.incorrect_breakdown);
        StringBuilder sb = new StringBuilder();
        String incorrectText = getResources().getString(R.string.review_activity_incorrect);
        for(int letter = 0; letter < incorrectRecord.length; letter++){
            int numIncorrect = incorrectRecord[letter];
            String letterString = Values.toLetter(letter);
            sb.append(String.format(Values.LOCALE, "%s - %d %s\n",
                    letterString, numIncorrect, incorrectText));
        }
        breakdownTextView.setText(sb.toString());

        Button mainMenu = findViewById(R.id.review_activity_main_menu);
        mainMenu.setOnClickListener(this);
    }

    public void onClick(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
