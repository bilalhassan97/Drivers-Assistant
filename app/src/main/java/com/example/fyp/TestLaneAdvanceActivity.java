package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.fyp.customutilities.ImageUtilities;
import com.google.android.material.snackbar.Snackbar;

import org.opencv.android.OpenCVLoader;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestLaneAdvanceActivity extends AppCompatActivity {

    private static final String TAG = "TestLaneAdvanceActivity";
    private ImageView imageView;
    private Snackbar initSnackbar = null;
//    private List<ParcelFileDescriptor> fds;
    private final int GALLERY_REQUEST_CODE = 100;
    private LaneDetectorAdvance ladv;
    private int keyCounter=0;
    private boolean hasProcessed = false;

    private List<Bitmap> bmps;



    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_lane_advance);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        imageView = findViewById(R.id.lane_adv_imgview);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(),R.drawable.test_img_lane);

        int srcWidth = bmp.getWidth();
        int srcHeight = bmp.getHeight();
        Bitmap resizedBmp = ImageUtilities.getResizedBitmap(
                bmp,300,300,true);

        ladv = new LaneDetectorAdvance(srcWidth,srcHeight,
                300,300);

        imageView.setImageBitmap(resizedBmp);
        bmps = new ArrayList<>();
        bmps.add(resizedBmp);
        LaneDetectorAdvance.setSharedPreference(
                getSharedPreferences("LaneDetection",0));
//        fds = new ArrayList<>();

        initSnackbar = Snackbar.make(findViewById(R.id.container_test_lane_adv),
                "loading pictures....", Snackbar.LENGTH_INDEFINITE);
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "onCreate: Opencv Loaded Successfully");
        }else{
            Log.d(TAG, "onCreate: Opencv Could not load");
        }
    }


    private void pickFromGallery(){
        bmps.clear();
        //Create an Intent with action as ACTION_PICK

        Intent intent=new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg","image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Launching the Intent
        startActivityForResult(intent,GALLERY_REQUEST_CODE);
    }
    private void detectLane(){
        if (!bmps.isEmpty()){
//            ladv.calibration(6,4,bmps,false);
//            show("calibration done");
//            ladv.unDistortImage(bmps.get(0).copy(Bitmap.Config.ARGB_8888,true));
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            pickFromGallery();
            keyCounter = 0;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {

            switch (keyCounter){
                case 0:
//                    detectLane();
//                    show("Lane detection Done");
                    if (!bmps.isEmpty()) {
                        ladv.processFrame(bmps.get(0));
                        show("processFrame Done");
                        hasProcessed = true;
                    }
                    break;
                case 1:
                    imageView.setImageBitmap(ladv.getEdgesBmp());
                    break;
                case 2:
                    imageView.setImageBitmap(ladv.getMarkedBmp());
                    break;
                case 3:
                    imageView.setImageBitmap(ladv.getWarperBmp());
                    break;
            }
            if (hasProcessed){
                keyCounter++;
                keyCounter %= 4;
            }
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST_CODE:
                    new LoadImagesTask().execute(data);

                    break;
                default:
                    break;
            }
        }else{
            show("Unsuccessful Intent");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    public void show(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }

    private class LoadImagesTask extends AsyncTask<Object,Object,Object> {

        @Override
        protected Object doInBackground(Object... intents) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initSnackbar.show();
                }
            });
            Intent data = (Intent) intents[0];
            //data.getData returns the content URI for the selected Image
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            if(data.getData()!=null){
                Log.d(TAG, "onActivityResult: got single image");
                Uri selectedImage = data.getData();
                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                // Move to first row
                if(cursor.moveToFirst()){
                    // now that you have the media URI, you can decode it to a bitmap
                    try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(selectedImage, "r")) {
                        if (pfd != null) {
//                                    fds.add(pfd);
                            Bitmap bmp = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                            bmps.add(bmp);
//                            Bitmap resizedBmp = ImageUtilities.getResizedBitmapMaintainAspectRatio(bmp,
//                                    300,300,true);
//                            bmps.add(resizedBmp);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                cursor.close();
            } else if (data.getClipData() != null) {
                ClipData mClipData = data.getClipData();
                for (int i = 0; i < mClipData.getItemCount(); i++) {
                    ClipData.Item item = mClipData.getItemAt(i);
                    Uri selectedImage = item.getUri();
                    // Get the cursor
                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn,
                            null, null, null);
                    // Move to first row
                    if(cursor.moveToFirst()){
                        // now that you have the media URI, you can decode it to a bitmap
                        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(selectedImage, "r")) {
                            if (pfd != null) {
//                                        fds.add(pfd);
                                Log.d(TAG, "onActivityResult: in ParcelFileDecriptor");
                                Bitmap bmp = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
//                                bmps.add(bmp);
                                Bitmap resizedBmp = ImageUtilities.getResizedBitmapMaintainAspectRatio(bmp,
                                        600,600,true);
                                bmps.add(resizedBmp);
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    cursor.close();
                }

                Log.d(TAG, String.format(
                        "onActivityResult: got multiple images in total = %d",
                        bmps.size()));
            } else {
                show("you did not select any image(s)");
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initSnackbar.dismiss();
                    if (!bmps.isEmpty()){
                        Bitmap copy = bmps.get(0).copy(Bitmap.Config.ARGB_8888,false);
                        imageView.setImageBitmap(copy);
                    }
                }
            });

            return null;
        }
    }
}