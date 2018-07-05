package com.tcs.experiment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class Main2Activity extends AppCompatActivity {
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private SurfaceView cameraView;

    private static final int CAMERA_PERMISSION_CAMERA = 0x000000;
    public static final String EXTRA_MESSAGE = "com.tcs.barcodescanner.STR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        if (ContextCompat.checkSelfPermission(Main2Activity.this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(Main2Activity.this,
                    Manifest.permission.CAMERA)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(Main2Activity.this,new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CAMERA);

                // CAMERA_PERMISSION_CAMERA is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        cameraView = (SurfaceView) findViewById(R.id.surface_view);
//     /*error*/   transferOrder = (TextView) findViewById(R.id.transferOrder);//*************************************************************

        Main2Activity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        barcodeDetector = new BarcodeDetector.Builder(getApplicationContext()).setBarcodeFormats(Barcode.ALL_FORMATS).build();

        cameraSource = new CameraSource
                .Builder(getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(35.0f)
                .setRequestedPreviewSize(640, 400)
                .setAutoFocusEnabled(true)
                .build();

        //setupButtons();


        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(Main2Activity.this,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        //      TODO: CONSIDER CALLING
                        //ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.


                        return;
                    }
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder,
                                       int format,
                                       int width,
                                       int height){}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
//                database = FirebaseDatabase.getInstance();
//                myRef = database
//                       .getReference(getTime());
                if (barcodes.size() != 0) {
                    Intent intent = new Intent();
                    String str = barcodes.valueAt(0).displayValue;
                    intent.putExtra(EXTRA_MESSAGE, str);
                    setResult(RESULT_OK, intent);
                    finish();
                }

            }
        });
    }

}
