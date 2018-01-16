package com.coolioasjulio.devanagariapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.modelimport.keras.preprocessors.TensorFlowCnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import org.nd4j.shade.jackson.databind.jsontype.NamedType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.coolioasjulio.devanagariapp.ImageUtils.PredictImageTask;
import static com.coolioasjulio.devanagariapp.ImageUtils.PredictImageTask.PredictionCallback;


public class Recognizer {

    private static final String MODEL_PATH = "model.zip";
    private static final String TAG = "Recognizer";

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
     * Perform inference with loaded neural network
     * @param bitmap Bitmap to process and feed into the model.
     * @param callback PredictionCallback to call once prediction is finished.
     */
    public void getPrediction(Bitmap bitmap, PredictionCallback callback){
        PredictImageTask imageTask = new PredictImageTask(network, callback);
        imageTask.execute(bitmap);
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

        // What follows is a gross workaround to importing the keras layers. Refer to this github issue:
        // https://github.com/deeplearning4j/deeplearning4j/issues/4504
        List<NamedType> types = new ArrayList<>();
        types.add(new NamedType(TensorFlowCnnToFeedForwardPreProcessor.class, "TensorFlowCnnToFeedForwardPreProcessor"));
        //Do this "new NamedType(...) for the other layers and preprocessors you are having trouble with
        NeuralNetConfiguration.reinitMapperWithSubtypes(types);
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
