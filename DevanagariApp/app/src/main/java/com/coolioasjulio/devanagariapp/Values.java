package com.coolioasjulio.devanagariapp;


import java.util.Locale;

public class Values {
    public static final int NUM_CHARS = 36;

    public static final String SESSION_LENGTH_KEY      = "SESSION_LENGTH";
    public static final String TIMEOUT_KEY             = "TIMEOUT";
    public static final String NUM_CORRECT_KEY         = "NUM_CORRECT";
    public static final String INCORRECT_BREAKDOWN_KEY = "SCORE_BREAKDOWN";

    //public static final Locale LOCALE = Locale.US;
    public static final Locale LOCALE = new Locale("hi","IN");

    private static final String[] intToLetter = new String[]{
            "क","ख","ग","घ","ङ","च","छ","ज","झ","ञ",
            "ट","ठ","ड","ढ","ण","त","थ","द","ध","न",
            "प","फ","ब","भ","म","य","र","ल","व","श",
            "ष","स","ह","क्ष","त्र","ज्ञ"
    };

    public static String toLetter(int character){
        return intToLetter[character];
    }
}
