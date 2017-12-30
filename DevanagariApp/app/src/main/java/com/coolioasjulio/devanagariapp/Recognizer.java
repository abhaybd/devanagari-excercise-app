package com.coolioasjulio.devanagariapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import java.io.IOException;
import java.io.InputStream;


public class Recognizer {
    private static final String MODEL_PATH = "model.zip";
    private static final String TAG = "Recognizer";

    private boolean ready = false;
    private ProgressBar progressBar;
    private MultiLayerNetwork network;
    public Recognizer(Context context, ProgressBar progressBar){
        this.progressBar = progressBar;
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
        return -1;
    }

    private void initAsync(Context context){
        progressBar.setVisibility(View.VISIBLE);
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
        is.close();
        Log.d(TAG, "Model loaded!");
    }

    private class LoadModelTask extends AsyncTask<String,Void,Boolean>{
        public Context context;
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
            Log.d(TAG,"AsyncTask finished!");
            ready = result;
            if(ready){
                progressBar.setVisibility(View.GONE);
            } else{
                showError(context);
            }
        }
    }
}
