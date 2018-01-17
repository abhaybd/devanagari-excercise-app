package com.coolioasjulio.devanagariapp;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;


public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int BOUNDED_IMG_SIZE = 20;
    private static final int PROCESSED_IMG_SIZE = 28;

    public static int[][] bitmapToArr(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0,0, width, height);
        int[][] imageArr = new int[width][height];
        int i = 0;
        for(int y = 0; y < imageArr[0].length; y++){
            for(int x = 0; x < imageArr.length; x++){
                // This is my gross workaround instead of actually learning how android handles colors.
                // If the pixel is either white or transparent, set it to 0. Otherwise, set it to 255.
                int rgb = (pixels[i] == 0 || pixels[i] == -1)?0:255;
                imageArr[x][y] = rgb;
                i++;
            }
        }
        return imageArr;
    }

    public static Bitmap arrToBitmap(int[][] image){
        int[] pixels = new int[image.length * image[0].length];
        for(int i = 0; i < pixels.length; i++){
            int x = i % image.length;
            int y = i / image.length;
            // I couldn't get the straight up colors to work, so if it's 255, set it to black. Otherwise, set it to transparent.
            int rgb = image[x][y] == 255?-16777216:0;
            pixels[i] = rgb;
        }
        return Bitmap.createBitmap(pixels, image.length, image[0].length, Bitmap.Config.ARGB_8888);
    }

    public static double[][] processImage(Bitmap bitmap){
        int[][] image = bitmapToArr(bitmap);

        int[] bounds = getBounds(image);
        if (bounds.length != 4) throw new AssertionError();
        int minX = bounds[0];
        int minY = bounds[1];
        int maxX = bounds[2];
        int maxY = bounds[3];
        if(minX == -1 || minY == -1 || maxX == 0 || maxY == 0) return null;
        Log.d(TAG, String.format("Bounds: %s, %s, %s, %s", minX, minY, maxX, maxY));
        int[][] croppedImage = crop(image, minX, minY, maxX, maxY);
        Bitmap croppedBitmap = arrToBitmap(croppedImage);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, BOUNDED_IMG_SIZE, BOUNDED_IMG_SIZE, true);
        int[][] scaledImage = bitmapToArr(scaledBitmap);

        int[] centerMass = centerOfMass(bitmapToArr(scaledBitmap));
        if (centerMass.length != 2) throw new AssertionError();
        int centerX = centerMass[0];
        int centerY = centerMass[1];
        Log.d(TAG, String.format("Center of mass: %s, %s", centerX, centerY));

        int max = PROCESSED_IMG_SIZE/2;
        int min = BOUNDED_IMG_SIZE - max;

        centerX = MathUtils.clamp(centerX, min, max);
        centerY = MathUtils.clamp(centerY, min, max);

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

    public static int[] getBounds(int[][] image){
        int minX = -1;
        int minY = -1;
        int maxX = -1;
        int maxY = -1;
        for(int y = 0; y < image[0].length; y++){
            for(int x = 0; x < image.length; x++){
                if(image[x][y] > 0) {
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
        }
        return new int[]{minX, minY, 1+maxX, 1+maxY};
    }

    public static int[][] crop(int[][] image, int minX, int minY, int maxX, int maxY){
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

    public static int[] centerOfMass(int[][] image){
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
        int centerX = MathUtils.round(sumX/(double)numPixels);
        int centerY = MathUtils.round(sumY/(double)numPixels);
        return new int[]{centerX, centerY};
    }

    public static class PredictImageTask extends AsyncTask<Bitmap,Void,Integer>{
        private static final String TAG = "PredictImageTask";
        // This is a super ugly and gross hack. The network didn't get the proper class mappings,
        // since the classes weren't zero padded. Now, class 0 corresponds with class 10. However,
        // it's actually supposed to be class 9, since the translation isn't 0 indexed. Ew. I know.
        // So, where out is output of neural net, labels[out] - 1 == character.
        private static final int[] labels = new int[]{
                10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                1,
                20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                2,
                30, 31, 32, 33, 34, 35, 36,
                3, 4, 5, 6, 7, 8, 9
        };

        private PredictionCallback callback;
        private MultiLayerNetwork model;
        public PredictImageTask(MultiLayerNetwork model, PredictionCallback callback){
            super();
            this.model = model;
            this.callback = callback;
        }

        @Override
        protected Integer doInBackground(Bitmap... bitmaps) {
            Log.d(TAG, "Beginning inference!");
            double[][] image = processImage(bitmaps[0]);
            if(image == null) return null;
            double[] flattened = ArrayUtil.flatten(image);
            int[] shape = new int[]{1, 1, image.length, image[0].length};
            INDArray input = Nd4j.create(flattened, shape, 'f');
            int index = model.predict(input)[0];
            return labels[index]-1; // Refer to comments at top of class.
        }

        @Override
        protected void onPostExecute(Integer result){
            Log.d(TAG, "Result: " + result);
            Log.d(TAG, "Finished inference! Calling callback...");
            if(result != null){
                callback.onFinished(result);
            } else{
                callback.onFinished(-1);
            }
            Log.d(TAG, "Finished calling callback!");
        }

        public interface PredictionCallback{
            void onFinished(int output);
        }
    }
}
