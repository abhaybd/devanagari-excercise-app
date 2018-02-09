package com.coolioasjulio.devanagariapp;

import android.util.Log;

import java.util.Random;

public class SessionGenerator {
    private static final String TAG = "SessionGenerator";

    private enum Mode {
        CYCLE,
        RANDOM
    }
    private Mode mode;
    private int numCategories, sessionLength;
    private int generated;
    private int lastGenerated = -1;
    private int[] incorrect;
    private int[] numbers;

    public SessionGenerator(int numCategories, int sessionLength){
        this(numCategories, sessionLength, true);
    }

    public SessionGenerator(int numCategories, int sessionLength, boolean shuffled){
        this.numCategories = numCategories;
        this.sessionLength = sessionLength;
        incorrect = new int[numCategories];
        mode = Mode.CYCLE;
        Log.d(TAG, "Mode: " + mode.name());
        if(shuffled) {
            numbers = shuffledArray(numCategories);
        } else {
            numbers = orderedArray(numCategories);
        }
    }

    /**
     * Clone incorrect array and return it.
     * @return Cloned incorrect array. Modifying this will not change the internal array of this class.
     */
    public int[] getIncorrectRecord(){
        int[] toReturn = new int[incorrect.length];
        for(int i = 0; i < toReturn.length; i++){
            toReturn[i] = incorrect[i];
        }
        return toReturn;
    }

    private int[] orderedArray(int size){
        int[] numbers = new int[size];
        for(int i = 0; i < size; i++){
            numbers[i] = i;
        }
        return numbers;
    }

    private int[] shuffledArray(int size){
        int[] numbers = orderedArray(size);
        Random random = new Random();
        for(int i = 0; i < numbers.length; i++){
            int swapIndex = random.nextInt(numbers.length);
            int temp = numbers[swapIndex];
            numbers[swapIndex] = numbers[i];
            numbers[i] = temp;
        }
        return numbers;
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
                toReturn = numbers[generated%sessionLength];
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

    /**
     * Perform softmax activation on the number of times each letter was wrong.
     * @param arr Number of times each letter was incorrectly answered.
     * @return double[] of probabilities. The sum of this array is 1.
     */
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
