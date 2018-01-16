package com.coolioasjulio.devanagariapp;

public class MathUtils {
    public static int round(double d){
        return (int) Math.floor(d+0.5);
    }

    public static int clamp(int i, int min, int max){
        return Math.max(Math.min(i,max), min);
    }
}
