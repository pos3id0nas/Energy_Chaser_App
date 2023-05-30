package com.example.energychaser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.energychaser.Drawing.BorderedText;
import com.example.energychaser.Drawing.MultiBoxTracker;
import com.example.energychaser.Drawing.OverlayView;
import com.example.energychaser.livefeed.CameraConnectionFragment;
import com.example.energychaser.livefeed.ImageUtils;

import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
    public class activity_scan_list extends AppCompatActivity implements ImageReader.OnImageAvailableListener{
        Handler handler;
        private Matrix frameToCropTransform;
        private int sensorOrientation;
        private Matrix cropToFrameTransform;
        private static final int TF_OD_API_INPUT_SIZE = 384;

        // MINIMUM CONFIDENCE
        private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
        private static final boolean MAINTAIN_ASPECT = false;
        private static final float TEXT_SIZE_DIP = 10;
        private static final int PERMISSION_CODE = 321;
        OverlayView trackingOverlay;
        private BorderedText borderedText;
        private Detector detector;

        private int CurrentProgress = 0;
        private ProgressBar progressBar;
        private int passedSecs;
        private boolean isTimerRunning = false;

        @SuppressLint("SetTextI18n")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.scan_activity_list);
            //PASS THE INTEGER FROM SINGLE_PLAYER TO ACTIVITY_SCAN
            passedSecs = getIntent().getIntExtra("MY_INTEGER", 0);
            progressBar = findViewById(R.id.progressBar);
            // CALL COUNTIME METHOD
            countime();
            handler = new Handler();
            //TODO show live camera footage
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
                    String[] permission = {Manifest.permission.CAMERA};
                    requestPermissions(permission, PERMISSION_CODE);
                }
                else {
                    setFragment();
                }
            }
            //TODO intialize the tracker to draw rectangles
            tracker = new MultiBoxTracker(this);
            //TODO inialize object detector
            try {
                detector =
                        TFLiteObjectDetectionAPIModel.create(
                                (Context) this,
                                "EnergyChaserl.tflite",
                                "labelmap.txt",
                                TF_OD_API_INPUT_SIZE,
                                true);
                Log.d("tryLog","success");
                Toast.makeText((Context) this, "Model loaded Successfully", Toast.LENGTH_SHORT).show();
            } catch (final IOException e) {
                Log.d("tryException","error in town"+e.getMessage());
            }
        }
        //TODO fragment which show llive footage from camera
        int previewHeight = 0,previewWidth = 0;
        protected void setFragment() {
            final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null;
            try {
                cameraId = manager.getCameraIdList()[0];
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            Fragment fragment;
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();

                                    final float textSizePx =
                                            TypedValue.applyDimension(
                                                    TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
                                    borderedText = new BorderedText(textSizePx);
                                    borderedText.setTypeface(Typeface.MONOSPACE);

                                    tracker = new MultiBoxTracker(activity_scan_list.this);

                                    int cropSize = TF_OD_API_INPUT_SIZE;

                                    previewWidth = size.getWidth();
                                    previewHeight = size.getHeight();

                                    sensorOrientation = rotation - getScreenOrientation();

                                    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                                    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

                                    frameToCropTransform =
                                            ImageUtils.getTransformationMatrix(
                                                    previewWidth, previewHeight,
                                                    cropSize, cropSize,
                                                    sensorOrientation, MAINTAIN_ASPECT);

                                    cropToFrameTransform = new Matrix();
                                    frameToCropTransform.invert(cropToFrameTransform);

                                    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
                                    trackingOverlay.addCallback(
                                            new OverlayView.DrawCallback() {
                                                @Override
                                                public void drawCallback(final Canvas canvas) {
                                                    tracker.draw(canvas);
                                                    Log.d("tryDrawRect","inside draw");
                                                }
                                            });

                                    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
                                }
                            },
                            this,
                            R.layout.camera_fragment,
                            new Size(640, 480));
            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
            getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }
        //TODO getting frames of live camera footage and passing them to model
        private boolean isProcessingFrame = false;
        private byte[][] yuvBytes = new byte[3][];
        private int[] rgbBytes = null;
        private int yRowStride;
        private Runnable postInferenceCallback;
        private Runnable imageConverter;
        private Bitmap rgbFrameBitmap;
        @Override
        public void onImageAvailable(ImageReader reader) {
            // We need wait until we have some size from onPreviewSizeChosen
            if (previewWidth == 0 || previewHeight == 0) {
                return;
            }
            if (rgbBytes == null) {
                rgbBytes = new int[previewWidth * previewHeight];
            }
            try {
                final Image image = reader.acquireLatestImage();

                if (image == null) {
                    return;
                }
                if (isProcessingFrame) {
                    image.close();
                    return;
                }
                isProcessingFrame = true;
                final Image.Plane[] planes = image.getPlanes();
                fillBytes(planes, yuvBytes);
                yRowStride = planes[0].getRowStride();
                final int uvRowStride = planes[1].getRowStride();
                final int uvPixelStride = planes[1].getPixelStride();

                imageConverter =
                        new Runnable() {
                            @Override
                            public void run() {
                                ImageUtils.convertYUV420ToARGB8888(
                                        yuvBytes[0],
                                        yuvBytes[1],
                                        yuvBytes[2],
                                        previewWidth,
                                        previewHeight,
                                        yRowStride,
                                        uvRowStride,
                                        uvPixelStride,
                                        rgbBytes);
                            }
                        };
                postInferenceCallback =
                        new Runnable() {
                            @Override
                            public void run() {
                                image.close();
                                isProcessingFrame = false;
                            }
                        };
                processImage();
            } catch (final Exception e) {
                Log.d("tryError",e.getMessage()+"abc ");
                return;
            }
        }
        String result = "";
        Bitmap croppedBitmap;
        private MultiBoxTracker tracker;
        private int objectCount = 0;
        private int objectCoCount = 5;
        ArrayList <String> objects = new ArrayList<>();

        public void processImage(){
            imageConverter.run();;
            rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
            final Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    //TODO pass image to model and get results
                    List<Detector.Recognition> results = detector.recognizeImage(rgbFrameBitmap);
                    float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    final List<Detector.Recognition> mappedRecognitions =
                            new ArrayList<>();
                    for (final Detector.Recognition result : results) {
                        if (result.getConfidence() >= minimumConfidence) {
                            mappedRecognitions.add(result);
                            if (objectCount < 5) {
                                //Add title of recognized object to list on click
                                ImageView imgButton=findViewById(R.id.addbutton);
                                imgButton.setOnClickListener(new View.OnClickListener(){
                                    public void onClick(View v) {
                                        //  getTitle from the Detector
                                        String Device = result.getTitle();
                                        if (Device.equals("Ovens") || Device.equals("ovens")) {
                                            Device = "ΦΟΥΡΝΟΣ";
                                        }
                                        else if (Device.equals("refrigerator") || Device.equals("refrigerators")) {
                                            Device = "ΨΥΓΕΙΟ";
                                        }
                                        else if (Device.equals("Washing-Machines")) {
                                            Device = "ΠΛΥΝΤΗΡΙΟ";
                                        }
                                        else if (Device.equals("DESKTOP PC") || Device.equals("Desktop-PC")) {
                                            Device = "ΥΠΟΛΟΓΙΣΤΗΣ";
                                        }
                                        else if (Device.equals("AirCondition")) {
                                            Device = "ΚΛΙΜΑΤΙΣΤΙΚΟ";
                                        }
                                        else if (Device.equals("Monitors") || Device.equals("tvmonitor")) {
                                            Device = "ΟΘΟΝΗ";
                                        }
                                        else if (Device.equals("illumination")) {
                                            Device = "ΦΩΤΙΣΜΟΣ";
                                        } else {
                                            return;}

                                        //  Check for dublicates
                                        boolean found = false;
                                        for (String item : objects ){
                                                if (item.equals(Device)){
                                                    found = true;
                                                    break;
                                                }}
                                        if(found){
                                            Toast.makeText(activity_scan_list.this, "Έχετε προσθέσει ξανα τη συσκευή " + Device, Toast.LENGTH_SHORT).show();
                                        }
                                        else{
                                            objects.add(Device);/////add Devices from the detector to the list
                                            objectCount++;
                                            objectCoCount--;
                                            Toast.makeText(activity_scan_list.this, "Προστέθηκε : " + Device, Toast.LENGTH_SHORT).show();
                                            Toast.makeText(activity_scan_list.this, "Μένουν Άλλα : " + objectCoCount, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                            else {
                                //Finish the adding
                                Toast.makeText(activity_scan_list.this, "Ολοκληρώθηκαν Οι Προσθήκες!", Toast.LENGTH_SHORT).show();
                                sendeverything();
                            }
                        }
                    }
                    tracker.trackResults(mappedRecognitions, 10);
                    trackingOverlay.postInvalidate();
                    postInferenceCallback.run();
                }
            });
        }
        private long remain;
        private CountDownTimer Timer;
        //TIMER FOR THE CHOSEN SECONDS
        public void countime(){
            // Start the timer
            Timer = new CountDownTimer(passedSecs, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    CurrentProgress = (int) ((passedSecs - millisUntilFinished) * 100 / passedSecs);
                    progressBar.setProgress(CurrentProgress);
                    progressBar.setMax(100);
                    //This will go to next class
                    remain = millisUntilFinished;
                }
                @Override
                public void onFinish() {
                    progressBar.setProgress(100);
                    isTimerRunning = false;

                        Timer.cancel();
                        // Clear the list of objects
                        objects.clear();
                        // Display a message
                        Toast.makeText(activity_scan_list.this, "Time's up!", Toast.LENGTH_SHORT).show();
                        //Redirect to preview stage after a second
                        Runnable t = new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(activity_scan_list.this, ButtonsActivity.class));
                            }
                        };
                        Handler h = new Handler();
                        // The Runnable will be executed after the given delay time
                        h.postDelayed(t, 1000); // will be delayed for 1 seconds//
                }
            };
            Timer.start();
            isTimerRunning = true;
        }
        //Method for start Timer automatically
        @Override
        protected void onResume() {
            super.onResume();
            // resume the timer if it is running
            if (isTimerRunning) {
                Timer.start();
            }
        }
        public void sendeverything(){
            //send time and detected objects to next class
            progressBar.setProgress(100);
            isTimerRunning = false;
            Intent intent = new Intent(this, CheckList.class);
            intent.putStringArrayListExtra("objectList", objects);
            //Send Remain Time to the next class
            intent.putExtra("remainingTime", remain);
            // Start the DestinationActivity
            startActivity(intent);
        }

        protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
            // Because of the variable row stride it's not possible to know in
            // advance the actual necessary dimensions of the yuv planes.
            for (int i = 0; i < planes.length; ++i) {
                final ByteBuffer buffer = planes[i].getBuffer();
                if (yuvBytes[i] == null) {
                    yuvBytes[i] = new byte[buffer.capacity()];
                }
                buffer.get(yuvBytes[i]);
            }
        }
        protected int getScreenOrientation() {
            switch (getWindowManager().getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_270:
                    return 270;
                case Surface.ROTATION_180:
                    return 180;
                case Surface.ROTATION_90:
                    return 90;
                default:
                    return 0;
            }
        }
        @Override
        protected void onDestroy() {
            super.onDestroy();
            detector.close();
        }
        //If user gives permission then launch camera
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if(requestCode == PERMISSION_CODE && grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                setFragment();
            }
        }
    }