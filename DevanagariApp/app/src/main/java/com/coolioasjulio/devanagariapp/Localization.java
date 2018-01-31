package com.coolioasjulio.devanagariapp;

public class Localization {
    //private static final int OFFSET = 0x966 - 0x30; // Devanagari 0 - English 0
    private static final int OFFSET = 0;

    public static String localizeInteger(int num){
        char[] arr = String.valueOf(num).toCharArray();
        for(int i = 0; i < arr.length; i++){
            if(Character.isDigit(arr[i])){
                arr[i] += OFFSET;
            }
        }
        return new String(arr);
    }

    public static String localize(double d){
        return localize(d, "%f");
    }

    public static String localize(double d, String format){
        String s = String.format(Values.LOCALE, format, d);
        char[] arr = s.toCharArray();
        for(int i = 0; i < arr.length; i++){
            if(Character.isDigit(arr[i])){
                arr[i] += OFFSET;
            }
        }
        return new String(arr);
    }
}
