package com.coolioasjulio.devanagariapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;


public class Recognizer {
    private static final Logger log = LoggerFactory.getLogger(Recognizer.class);

    private static final String MODEL_PATH = "model2.zip";
    private static final String TAG = "Recognizer";
    private static final int BOUNDED_IMG_SIZE = 20;
    private static final int PROCESSED_IMG_SIZE = 28;

    private boolean ready = false;
    private Runnable onReadyCallback;
    private MultiLayerNetwork network;
    public Recognizer(Context context, Runnable onReadyCallback){
        this.onReadyCallback = onReadyCallback;
        initAsync(context);
    }

    /**
     * Is the neural network fully loaded?
     * @return Whether the network is ready for inference.
     */
    public boolean isReady(){
        return ready;
    }

    /**
     * Perform inference with data from the supplied bitmap.
     * @param bitmap Bitmap to feed into the neural net.
     * @return Predicted devanagari character index.
     */
    public int getPrediction(Bitmap bitmap){
        //double[][] image = processImage(bitmap);
        return -1;
    }

    private double[][] processImage(Bitmap bitmap){
        int[][] image = bitmapToArr(bitmap);

        int[] bounds = getBounds(image);
        if (bounds.length != 4) throw new AssertionError();
        int minX = bounds[0];
        int minY = bounds[1];
        int maxX = bounds[2];
        int maxY = bounds[3];

        int[][] croppedImage = crop(image, minX, minY, maxX, maxY);
        Bitmap croppedBitmap = arrToBitmap(croppedImage);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, BOUNDED_IMG_SIZE, BOUNDED_IMG_SIZE, true);
        int[][] scaledImage = bitmapToArr(scaledBitmap);

        int[] centerMass = centerOfMass(bitmapToArr(scaledBitmap));
        if (centerMass.length != 2) throw new AssertionError();
        int centerX = centerMass[0];
        int centerY = centerMass[1];
        int max = PROCESSED_IMG_SIZE/2;
        int min = BOUNDED_IMG_SIZE - max;

        centerX = clamp(centerX, min, max);
        centerY = clamp(centerY, min, max);

        int half = max;
        double[][] processedImage = new double[PROCESSED_IMG_SIZE][PROCESSED_IMG_SIZE];
        for(int y = half - centerY; y < half - centerY + BOUNDED_IMG_SIZE; y++){
            for(int x = half - centerX; x < half - centerX + BOUNDED_IMG_SIZE; x++){
                int shiftedX = x - (half - centerX);
                int shiftedY = y - (half - centerY);
                processedImage[x][y] = (double)scaledImage[shiftedX][shiftedY] / 255.0;
            }
        }
        return processedImage;
    }

    private int[] centerOfMass(int[][] image){
        double sumX = 0;
        double sumY = 0;
        int numPixels = 0;
        for(int y = 0; y < image[0].length; y++){
            for(int x = 0; x < image.length; x++){
                if(image[x][y] > 0){
                    sumX += x;
                    sumY += y;
                    numPixels++;
                }
            }
        }
        int centerX = round(sumX/(double)numPixels);
        int centerY = round(sumY/(double)numPixels);
        return new int[]{centerX, centerY};
    }

    private int round(double d){
        return (int) Math.floor(d+0.5);
    }

    private int clamp(int i, int min, int max){
        return Math.max(Math.min(i,max), min);
    }

    private int[][] bitmapToArr(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[bitmap.getHeight() * bitmap.getRowBytes()];
        bitmap.getPixels(pixels, 0, width, 0,0, width, height);
        int[][] imageArr = new int[width][height];
        for(int i = 0; i < pixels.length; i++){
            int x = i % width;
            int y = i / width;
            int rgb = pixels[i] & 0xFF;
            imageArr[x][y] = rgb;
        }
        return imageArr;
    }

    private Bitmap arrToBitmap(int[][] image){
        int[] pixels = new int[image.length * image[0].length];
        for(int i = 0; i < pixels.length; i++){
            int x = i % image.length;
            int y = i / image[0].length;
            int rgb = image[x][y];
            rgb = rgb | (rgb << 8) | (rgb << 16);
            pixels[i] = rgb;
        }
        return Bitmap.createBitmap(pixels, image.length, image[0].length, Bitmap.Config.ARGB_8888);
    }

    private int[] getBounds(int[][] image){
        int minX = -1;
        int minY = -1;
        int maxX = -1;
        int maxY = -1;
        for(int y = 0; y < image[0].length; y++){
            for(int x = 0; x < image.length; x++){
                if(image[x][y] == 0) continue;
                if(x < minX || minX == -1){
                    minX = x;
                }
                if(y < minY || minY == -1){
                    minY = y;
                }
                if(x > maxX || maxX == -1){
                    maxX = x;
                }
                if(y > maxY || maxY == -1){
                    maxY = y;
                }
            }
        }
        return new int[]{minX, minY, 1+maxX, 1+maxY};
    }

    private int[][] crop(int[][] image, int minX, int minY, int maxX, int maxY){
        // Make sure the constraints are within the image and make sense.
        if(minX >= maxX || minY >= maxY
                || minX < 0 || maxX > image.length
                || minY < 0 || maxY > image[0].length) throw new AssertionError();
        int[][] cropped = new int[maxX - minX][maxY - minY];
        for(int y = minY; y < maxY; y++){
            for(int x = minX; x < maxX; x++){
                cropped[x-minX][y-minY] = image[x][y];
            }
        }
        return cropped;
    }

    private void initAsync(Context context){
        LoadModelTask loadModelTask = new LoadModelTask();
        loadModelTask.context = context;
        loadModelTask.execute(MODEL_PATH);
    }

    private void showError(final Context context){
        String title = context.getResources().getString(R.string.model_import_error_title);
        String error = context.getResources().getString(R.string.model_import_error);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(error);
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                context.startActivity(intent);
            }
        });
        dialog.show();
    }

    private void loadModel(Context context, String path) throws IOException {
        Log.d(TAG, "Loading model...");
        InputStream is = context.getAssets().open(path);
        network = ModelSerializer.restoreMultiLayerNetwork(is);
        Log.d(TAG, "Summary: " + network.summary());
        is.close();
        Log.d(TAG, "Model loaded!");
    }

    private class LoadModelTask extends AsyncTask<String,Void,Boolean>{
        Context context;
        @Override
        protected Boolean doInBackground(String... paths) {
            if(context == null){
                return false;
            }
            try{
                loadModel(context, paths[0]);
                return true;
            } catch(IOException e){
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            Log.d(TAG,"Finished loading model!");
            ready = result;
            if(ready){
                onReadyCallback.run();
            } else{
                showError(context);
            }
        }
    }
}
