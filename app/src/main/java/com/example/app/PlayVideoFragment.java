package com.example.app;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.ImageSource;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;


public class PlayVideoFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "Info";

    private File directoryToStore;
    private File directoryToDocs;

    private VideoView videoView;
    private Button reportBTN, checkBTN;
    private Uri videoUri;
    private TextView showProgress, resTxt, colorExplain;
    private ImageView frameImg;

    private int totalNumFiles = 0;
    private int frameRate = 0;
    private int numAnger = 0, numJoy = 0, numSurprise = 0, numSorrow = 0, numNone = 0, empty = 0;
    private String name = "";

    Vision.Builder visionBuilder;
    private StorageReference mStorageRef;

    ExtractClass extractClass;

    public PlayVideoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_play_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        directoryToStore = getActivity().getBaseContext().getExternalFilesDir("Frames");
        directoryToDocs = getActivity().getBaseContext().getExternalFilesDir("Docs");

        mStorageRef = FirebaseStorage.getInstance().getReference();

        numAnger = 0;
        numJoy = 0;
        numSurprise = 0;
        numSorrow = 0;
        numNone = 0;
        empty = 0;

        visionBuilder = new Vision.Builder(
                new NetHttpTransport(),
                new AndroidJsonFactory(),
                null);

        // DON'T FORGET TO CHANGE API
        visionBuilder.setVisionRequestInitializer(
                new VisionRequestInitializer("AIzaSyCSAIt0DifJznzoDVv_K-CFX1-93KxWcU8"));

        init(view);

    }

    // Initialize UI
    public void init(View v){

        videoView = v.findViewById(R.id.videoView);
        reportBTN = v.findViewById(R.id.reportBtn);
        checkBTN = v.findViewById(R.id.tempBTN);
        showProgress = v.findViewById(R.id.showProgress);
        resTxt = v.findViewById(R.id.resAfter);
        frameImg = v.findViewById(R.id.frameImg);
        colorExplain = v.findViewById(R.id.colorExplain);

        colorExplain.setText(Html.fromHtml("<font color = #fc0303>RED</font> is ANGER"));
        colorExplain.append("\n");
        colorExplain.append(Html.fromHtml("<font color = #55bd15>GREEN</font> is JOY"));
        colorExplain.append("\n");
        colorExplain.append(Html.fromHtml("<font color = #fce300>YELLOW</font> is SURPRISE"));
        colorExplain.append("\n");
        colorExplain.append(Html.fromHtml("<font color = #0576ff>BLUE</font> is SORROW"));
        colorExplain.append("\n");
        colorExplain.append(Html.fromHtml("<font color = #000000>BLACK</font> is NONE"));

        frameImg.setImageResource(0);

        checkBTN.setOnClickListener(this);
        reportBTN.setOnClickListener(this);

        // get the URI of video
        if(getArguments() != null){
            PlayVideoFragmentArgs args = PlayVideoFragmentArgs.fromBundle(getArguments());
            videoUri = Uri.parse(args.getUri());
            videoView.setVideoURI(videoUri);
            extractFrames();
            MediaController mediaController = new MediaController(getContext());
            mediaController.setPadding(0, 0, 0,1500);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
            videoView.start();
        }

    }


    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tempBTN:
                try {
                    checkEmotionFunction();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.reportBtn:
                sendReportFunction();
                break;
        }
    }

    public void sendReportFunction(){

        File filepath = new File(directoryToDocs, name + "_Extra.txt");
        try{
            final FileWriter writer = new FileWriter(filepath);
            int total = totalNumFiles - empty;
            float tmp = (float) (numAnger * 100) / total;
            String tmp2 = String.format("%.2f", tmp);
            writer.append("Total Anger is " + tmp2 + "% (" + numAnger + ")\n");
            tmp = (float) (numJoy * 100) / total;
            tmp2 = String.format("%.2f", tmp);
            writer.append("Total Joy is " + tmp2 + "% (" + numJoy + ")\n");
            tmp = (float) (numSurprise * 100) / total;
            tmp2 = String.format("%.2f", tmp);
            writer.append("Total Surprise is " + tmp2 + "% (" + numSurprise + ")\n");
            tmp = (float) (numSorrow * 100) / total;
            tmp2 = String.format("%.2f", tmp);
            writer.append("Total Sorrow is " + tmp2 + "% (" + numSorrow + ")\n");
            tmp = (float) (numNone * 100) / total;
            tmp2 = String.format("%.2f", tmp);
            writer.append("Total None is " + tmp2 + "% (" + numNone + ")\n");
            writer.flush();
            writer.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        File[] list = directoryToDocs.listFiles();

        if(list.length != 0){
            final ProgressDialog pd = new ProgressDialog(getActivity());
            pd.setTitle("Uploading...");
            pd.show();
            for(File f : list){
                String name = f.getName();
                Uri uri = Uri.fromFile(new File("/storage/emulated/0/Android/data/com.example.app/files/Docs/" + name));
                final StorageReference riversRef = mStorageRef.child("reports/" + name);

                riversRef.putFile(uri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                pd.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                pd.dismiss();
                                Toast.makeText(getActivity(), "Error!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                pd.setMessage(((int)progress) + "% Uploaded...");
                            }
                        })
                ;
            }

            // delete shit
            /*
            if (directoryToDocs.isDirectory()){
                String[] children = directoryToDocs.list();
                for (int i = 0; i < children.length; i++)
                    new File(directoryToDocs, children[i]).delete();
            }
             */
        }

        reportBTN.setEnabled(false);

    }


    @NonNull
    private Image getImageEncodeImage(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }

    public void checkEmotionFunction() throws IOException {
        GoogleApi task = new GoogleApi();
        task.execute();
    }

    private class GoogleApi extends AsyncTask<Void, String, Void>{

        String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            checkBTN.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try{

                totalNumFiles = directoryToStore.listFiles().length;
                Log.i(TAG, String.valueOf(totalNumFiles));

                Vision vision = visionBuilder.build();

                Feature feature = new Feature();
                feature.setType("FACE_DETECTION");
                feature.setMaxResults(1);

                // this will create a new name everytime and unique
                name = DateFormat.format("MM-dd-yyyyy-h-mmss", System.currentTimeMillis()).toString();
                File filepath = new File(directoryToDocs, name + ".txt");

                final FileWriter writer = new FileWriter(filepath);
                writer.append("FPS = " + frameRate);
                writer.append("\nEmotions:\tanger    joy     surprise    sorrow      none\n");

                for(int j = 1; j <= totalNumFiles; j++) {

                    //File imageFile = new File(getContext().getFilesDir(), "Frames" + j + ".jpg");
                    File imageFile = new File("/storage/emulated/0/Android/data/com.example.app/files/Frames/image" + j + ".jpg");
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);

                    //Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.p);

                    List<Feature> featureList = new ArrayList<>();
                    featureList.add(feature);

                    AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
                    annotateImageReq.setFeatures(featureList);
                    annotateImageReq.setImage(getImageEncodeImage(bitmap));

                    BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();

                    batchRequest.setRequests(Arrays.asList(annotateImageReq));

                    BatchAnnotateImagesResponse batchResponse = vision.images().annotate(batchRequest).execute();

                    List<FaceAnnotation> faces = batchResponse.getResponses().get(0).getFaceAnnotations();

                    //int numberOfFaces = faces.size();
                    if (faces != null){
                        for (FaceAnnotation annotation : faces) {
                            String res1 = annotation.getAngerLikelihood();
                            String res2 = annotation.getJoyLikelihood();
                            String res3 = annotation.getSurpriseLikelihood();
                            String res4 = annotation.getSorrowLikelihood();
                            String res6 = "VERY_UNLIKELY";

                            String w = "";

                            if (res1.equals("VERY_UNLIKELY") & res2.equals("VERY_UNLIKELY") & res3.equals("VERY_UNLIKELY") & res4.equals("VERY_UNLIKELY")) {
                                res6 = "VERY_LIKELY";
                                numNone++;
                                w = "<font color = #000000>-</font>";
                            }

                            if (res1.equals("VERY_LIKELY")) {
                                numAnger++;
                                w = "<font color = #fc0303>-</font>";
                                //meh.setSpan(new ForegroundColorSpan(Color.RED), 0, meh.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else if (res2.equals("VERY_LIKELY")) {
                                numJoy++;
                                w = "<font color = #55bd15>-</font>";
                                //meh.setSpan(new ForegroundColorSpan(Color.GREEN), 0, meh.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else if (res3.equals("VERY_LIKELY")) {
                                numSurprise++;
                                w = "<font color = #fce300>-</font>";
                                //meh.setSpan(new ForegroundColorSpan(Color.YELLOW), 0, meh.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else if (res4.equals("VERY_LIKELY")) {
                                numSorrow++;
                                w = "<font color = #0576ff>-</font>";
                                //meh.setSpan(new ForegroundColorSpan(Color.BLUE), 0, meh.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else if (res1.equals("LIKELY")) {
                                numAnger++;
                                w = "<font color = #fc0303>-</font>";
                            } else if (res2.equals("LIKELY")) {
                                numJoy++;
                                w = "<font color = #55bd15>-</font>";
                            } else if (res3.equals("LIKELY")) {
                                numSurprise++;
                                w = "<font color = #fce300>-</font>";
                            } else if (res4.equals("LIKELY")) {
                                numSorrow++;
                                w = "<font color = #0576ff>-</font>";
                            } else if (res1.equals("POSSIBLE")) {
                                numAnger++;
                                w = "<font color = #fc0303>-</font>";
                            } else if (res2.equals("POSSIBLE")) {
                                numJoy++;
                                w = "<font color = #55bd15>-</font>";
                            } else if (res3.equals("POSSIBLE")) {
                                numSurprise++;
                                w = "<font color = #fce300>-</font>";
                            } else if (res4.equals("POSSIBLE")) {
                                numSorrow++;
                                w = "<font color = #0576ff>-</font>";
                            } else if (res1.equals("UNLIKELY")) {
                                numAnger++;
                                w = "<font color = #fc0303>-</font>";
                            } else if (res2.equals("UNLIKELY")) {
                                numJoy++;
                                w = "<font color = #55bd15>-</font>";
                            } else if (res3.equals("UNLIKELY")) {
                                numSurprise++;
                                w = "<font color = #fce300>-</font>";
                            } else if (res4.equals("UNLIKELY")) {
                                numSorrow++;
                                w = "<font color = #0576ff>-</font>";
                            }

                            //Log.i(TAG, res1);
                            //Log.i(TAG, res2);
                            //Log.i(TAG, res3);
                            //Log.i(TAG, res4);
                            //Log.i(TAG, res5);
                            //Log.i(TAG, res6);

                            String res = "Frame #" + j;

                            res = res + " " + res1 + " " + res2 + " " + res3 + " " + res4 + " " + res6 + "\n";

                            writer.append(res);

                            publishProgress(String.valueOf(j), w);
                        }
                    }
                    if(faces == null) empty++;
                }

                writer.flush();
                writer.close();

                // delete shit
                if (directoryToStore.isDirectory()){
                    String[] children = directoryToStore.list();
                    for (int i = 0; i < children.length; i++)
                        new File(directoryToStore, children[i]).delete();
                }
            }
            catch (IOException e){
                e.printStackTrace();
                result = e.toString();
            }
            return null;
        }

        protected void onProgressUpdate(String... j) {
            showProgress.setText("Frame #" + j[0] + " processed!");
            resTxt.append(Html.fromHtml(j[1]));
            File imgFile = new  File("/storage/emulated/0/Android/data/com.example.app/files/Frames/image" + j[0] + ".jpg");
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            frameImg.setImageBitmap(myBitmap);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //resTxt.setText(xyz.toString());
            showProgress.setText("Complete!");
            reportBTN.setEnabled(true);
            super.onPostExecute(aVoid);
        }

    }


    public void extractFrames(){

        // get string path of video using uri
        String fileName = "";
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getActivity().getContentResolver().query(videoUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            fileName = cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.i(TAG, fileName);

        // extract frames
        extractClass = new ExtractClass(this.getContext(), fileName);
        extractClass.execute();

        // get fps number
        MediaExtractor extractor = new MediaExtractor();
        frameRate = 24; //may be default
        try {
            //Adjust data source as per the requirement if file, URI, etc.
            extractor.setDataSource(fileName);
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; ++i) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //Release stuff
            extractor.release();
        }

    }

}