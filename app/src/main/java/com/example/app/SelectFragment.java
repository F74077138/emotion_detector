package com.example.app;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

import static android.app.Activity.RESULT_OK;

public class SelectFragment extends Fragment implements View.OnClickListener{

    private static final int VIDEO_REQUEST = 101;
    private static final int SELECT_VIDEO_REQUEST=102;

    private static final String TAG = "Info";

    Uri videoUri;
    String newLink = "";

    private NavController navController;
    private EditText linkTxt;

    public SelectFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init(view);

        // create folders
        File directoryToStore = getActivity().getBaseContext().getExternalFilesDir("Frames");
        if(!directoryToStore.exists()){
            if(directoryToStore.mkdir()) ; //directory is created;
        }

        File directoryToDocs = getActivity().getBaseContext().getExternalFilesDir("Docs");
        if(!directoryToDocs.exists()){
            if(directoryToDocs.mkdir()) ; //directory is created;
        }

        // delete previous files
        if (directoryToStore.isDirectory()){
            String[] children = directoryToStore.list();
            for (int i = 0; i < children.length; i++)
                new File(directoryToStore, children[i]).delete();
        }

        if (directoryToDocs.isDirectory()){
            String[] children = directoryToDocs.list();
            for (int i = 0; i < children.length; i++)
                new File(directoryToDocs, children[i]).delete();
        }

    }

    /*************************/
    /**    initialize UI     */
    /*************************/
    private void init(View view){

        verifyPermissions();

        navController = Navigation.findNavController(view);

        Button selectGallery = view.findViewById(R.id.galleryBtn);
        Button uploadBtn = view.findViewById(R.id.uploadBtn);
        Button recordBtn = view.findViewById(R.id.recordBtn);

        selectGallery.setOnClickListener(this);
        uploadBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);

        linkTxt = view.findViewById(R.id.linkTxt);
    }
    /*******************************/
    /** end of UI initialization **/
    /******************************/

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.galleryBtn:
                fromGalleryFunction();
                break;
            case R.id.uploadBtn:
                checkLink();
                break;
            case R.id.recordBtn:
                recordFunction();
                break;
        }
    }


    public void checkLink(){
        if(linkTxt.getText().toString().trim().length() > 0){
            downloadVideoFunction(linkTxt.getText().toString().trim());
            linkTxt.setText("");
        }
        else Toast.makeText(getActivity(),"Link is empty!",Toast.LENGTH_SHORT).show();
    }

    public void downloadVideoFunction(String link){

        YouTubeExtractor youTubeExtractor = new YouTubeExtractor(this.getActivity()) {
            @Override
            protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                if(ytFiles != null){
                    List<Integer> iTags = Arrays.asList(22, 137, 18);
                    for (Integer tag : iTags) {
                        //int tag = 22;
                        YtFile ytFile = ytFiles.get(tag);
                        if (ytFile != null) {
                            newLink = ytFile.getUrl();
                            //if (newLink != null && !newLink.isEmpty()) {
                                String title = DateFormat.format("MM-dd-yyyyy-h-mmss", System.currentTimeMillis()).toString();
                                DownloadManager.Request request=new DownloadManager.Request(Uri.parse(newLink));
                                request.setTitle(title);
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,title+".mp4");
                                DownloadManager downloadManager=(DownloadManager)getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                request.allowScanningByMediaScanner();
                                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                                downloadManager.enqueue(request);
                                return;
                            //}
                        //newLink = ytFiles.get(tag).getUrl();
                        //String title = DateFormat.format("MM-dd-yyyyy-h-mmss", System.currentTimeMillis()).toString();
                        //DownloadManager.Request request=new DownloadManager.Request(Uri.parse(newLink));
                        //request.setTitle(title);
                        //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,title+".mp4");
                        //DownloadManager downloadManager=(DownloadManager)getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                        //request.allowScanningByMediaScanner();
                        //request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                        //downloadManager.enqueue(request);
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(SparseArray<YtFile> ytFiles) {
                super.onPostExecute(ytFiles);
                Toast.makeText(getActivity(),"Finished Downloading!",Toast.LENGTH_SHORT).show();
            }

        };

        Log.i(TAG, link);
        youTubeExtractor.execute(link);
    }


    public void recordFunction(){
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if(videoIntent.resolveActivity(getActivity().getPackageManager()) != null){
            startActivityForResult(videoIntent, VIDEO_REQUEST);
        }
    }

    public void fromGalleryFunction(){
        Intent selectIntent = new Intent(Intent.ACTION_PICK);
        selectIntent.setType("video/mp4");
        if(selectIntent.resolveActivity(getActivity().getPackageManager()) != null){
            startActivityForResult(selectIntent, SELECT_VIDEO_REQUEST);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == VIDEO_REQUEST && resultCode == RESULT_OK){
            videoUri = data.getData();
            showOutput();
        }
        else if(requestCode == SELECT_VIDEO_REQUEST && resultCode == RESULT_OK){
            videoUri = data.getData();
            showOutput();
        }
    }

    public void showOutput(){
        SelectFragmentDirections.ActionSelectFragmentToPlayVideoFragment action = SelectFragmentDirections.actionSelectFragmentToPlayVideoFragment();
        action.setUri(videoUri.toString());
        navController.navigate(action);
    }


    /*************************/
    /** Ask for permissions **/
    /*************************/

    private void verifyPermissions(){

        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA, Manifest.permission.INTERNET};

        if(ContextCompat.checkSelfPermission(getContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(), permissions[3]) == PackageManager.PERMISSION_GRANTED){
        }
        else{
            ActivityCompat.requestPermissions(getActivity(), permissions, 1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        verifyPermissions();
    }

    /*******************************/
    /** End of asking permissions **/
    /*******************************/
}