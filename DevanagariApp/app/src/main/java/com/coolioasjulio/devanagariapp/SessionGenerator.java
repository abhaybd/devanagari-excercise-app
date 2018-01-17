package com.coolioasjulio.devanagariapp;

import android.util.Log;

public class SessionGenerator {
    private static final String TAG = "SessionGenerator";
    private enum Mode {CYCLE, RANDOM};
    private int numCategories, sessionLength;
    private int generated;
    private Mode mode;
    private int[] incorrect;
    private int lastGenerated = -1;
    public SessionGenerator(int numCategories, int sessionLength){
        this.numCategories = numCategories;
        this.sessionLength = sessionLength;
        incorrect = new int[numCategories];
        mode = Mode.CYCLE;
        Log.d(TAG, "Mode: " + mode.name());
    }

    /**
     * Return another character to guess.
     * @param correct Was the last given character guessed correctly?
     * @return A new character to guess. Pass whether the answer was correct or not as the parameter for the next call.
     */
    public int next(boolean correct){
        if(lastGenerated >= 0){
            incorrect[lastGenerated] += correct?0:1;
        }
        int toReturn = 0;
        switch(mode){
            case CYCLE:
                toReturn = generated % sessionLength;
                if(generated >= numCategories - 1){
                    mode = Mode.RANDOM;
                    Log.d(TAG, "Switching mode to: " + mode.name());
                }
                break;

            case RANDOM:
                double[] probabilities = getProbabilities(incorrect);
                double rand = Math.random();
                for(int i = 0; i < probabilities.length; i++){
                    rand -= probabilities[i];
                    if(rand <= 0){
                        toReturn = i;
                        break;
                    }
                }
                break;
        }
        generated++;
        lastGenerated = toReturn;
        return toReturn;
    }

    private double[] getProbabilities(int[] arr){
        double[] probabilities = new double[arr.length];
        double denominator = 0;
        for(int a:arr){
            denominator += Math.exp(a);
        }

        for(int i = 0; i < arr.length; i++){
            probabilities[i] = Math.exp(arr[i]) / denominator;
        }
        return probabilities;
    }
}
