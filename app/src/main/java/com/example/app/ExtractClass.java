package com.example.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.FFmpeg;

public class ExtractClass extends AsyncTask<Void, Integer, String> {

    private final static String TAG = "Info";

    private Context ctx;
    private ProgressDialog pd;

    private String filename;

    public ExtractClass(Context ctx, String filename){
        this.ctx = ctx;
        this.filename = filename;
    }

    @Override
    protected void onPreExecute() {
        pd = new ProgressDialog(ctx);
        pd.setTitle("Extracting Frames From Video...");
        pd.setMessage("Please Wait");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();
        pd.setCancelable(false);
    }

    protected String doInBackground(Void... voids) {
        try {
            int rc = FFmpeg.execute("-i " + filename + " /storage/emulated/0/Android/data/com.example.app/files/Frames/image%d.jpg");
            Log.i(TAG, "Extracting is done");
            return "Done!";
        }
        catch (Exception e){
            Log.i(TAG, "Extracting is not done");
            return "Error!";
        }
    }

    protected void onPostExecute(String result) {
        Toast.makeText(ctx, result, Toast.LENGTH_SHORT).show();
        pd.dismiss();
    }

}
