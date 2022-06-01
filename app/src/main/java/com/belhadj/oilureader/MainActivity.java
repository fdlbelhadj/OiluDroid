package com.belhadj.oilureader;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.barcode.BarcodeDetector;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

enum markers { OILU_CODE, QR_CODE, BAR_CODE, ARUCO_CODE;}

public class MainActivity extends Activity implements CvCameraViewListener2 { //OnTouchListener,

    private static int MIN_CONTOUR_SIZE         = 40;
    private static int CANONICAL_MARKER_SIZE    = 250;
    private static final String  TAG            = "MainActivity";
    private static final int SECOND_ACT         = 2;

    private Mat mRgba;
    markers targetCode = markers.OILU_CODE;
    Vector<com.belhadj.oilureader.OiluMarker> detectedMarkers;
    private CameraBridgeViewBase mOpenCvCameraView;
    long nbinputFrames = 0;

    private Bitmap bitmap;
    private ImageView mImgView;

    OiluMarkerDetector oiluDetector;
    QRCodeDetector qrCodeDetector;
    BarcodeDetector barcodeDetector;
    Aruco arucoDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        getCameraPermission();

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.javaCamera);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);//(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.setMaxFrameSize(1920,1920);

        Intent intent = getIntent();
        switch (intent.getIntExtra("CODE", 0))
        {
            case 0 : targetCode = markers.OILU_CODE; break;
            case 1 : targetCode = markers.QR_CODE; break;
            case 2 : targetCode = markers.BAR_CODE; break;
            case 3 : targetCode = markers.ARUCO_CODE; break;
        }
/*  detect static images
//        mImgView = (ImageView) findViewById(R.id.imgView);
//        InputStream imageStream = this.getResources().openRawResource(R.raw.oilumarkers2png);
//        bitmap = BitmapFactory.decodeStream(imageStream);
//        mImgView.setImageBitmap(bitmap);

  */
    }



    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        nbinputFrames++;
        //Core.flip(mRgba.t(),mRgba,1);
        //                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          if(nbinputFrames % 10 == 0)
        switch (targetCode)
        {
            case OILU_CODE: ProcessOiluCode(mRgba, true);
                break;
            case QR_CODE: ProcessQrCode(mRgba);
                break;
            case BAR_CODE: ProcessBarCode(mRgba);
                break;
            case ARUCO_CODE: ProcessArucoCode(mRgba);
                break;
        }
        return mRgba;
    }

    private void ProcessArucoCode(Mat mRgba) {
        Imgproc.cvtColor(mRgba, mRgba,  Imgproc.COLOR_BGRA2BGR);
        List<Mat> markers = new ArrayList<>();
        Mat ids = new Mat();

        Aruco.detectMarkers(mRgba, Aruco.getPredefinedDictionary(Aruco.DICT_6X6_250),markers, ids );
        if(! markers.isEmpty()){

            Aruco.drawDetectedMarkers(mRgba, markers, ids);
        }


    }

    private void ProcessQrCode(Mat mRgba) {
        qrCodeDetector=new QRCodeDetector();
        Mat points = new Mat();
        boolean ret =  qrCodeDetector.detect(mRgba, points);
        if(!points.empty()) {
            MatOfPoint matOfPoint = new MatOfPoint();
            points.t().convertTo(matOfPoint, CvType.CV_32S);
            List<MatOfPoint> ps = new ArrayList<>();
            ps.add(matOfPoint);
            Imgproc.drawContours(mRgba,ps, 0, new Scalar(255,0,0), 2 );
        }
    }

    private void ProcessBarCode(Mat mRgba) {
        barcodeDetector =new BarcodeDetector();
        Mat points = new Mat();
        boolean ret =  barcodeDetector.detect(mRgba, points);
        if(!points.empty()) {
            MatOfPoint matOfPoint = new MatOfPoint();
            points.t().convertTo(matOfPoint, CvType.CV_32S);
            List<MatOfPoint> ps = new ArrayList<>();
            ps.add(matOfPoint);
            Imgproc.drawContours(mRgba,ps, 0, new Scalar(255,0,0), 2 );
        }
    }

    private void ProcessOiluCode(Mat mRgba, boolean draw ) {

            List<OiluMarker> mList = oiluDetector.getCandidateMarkers(mRgba);

            ProcessMarkerList_With_MHH(mList);

            if(draw) DrawMarkersList(mRgba, mList, Color.RED, Color.GREEN, Color.BLUE);
    }



    private void DrawMarkersList(Mat mat, List<OiluMarker> mList, int c_invalid, int c_valid, int c_axis) {
        for (int i = 0; i < mList.size(); i++)
        {
            OiluMarker marker = mList.get(i);

            marker.drawMarker(mat, "" + i + " " + marker.Id, new Scalar(c_invalid), new Scalar(c_valid), new Scalar( c_axis));
            //mat = marker.getDataMat();
            //bitmap = Bitmap.createBitmap(mat.width(),mat.height(), Bitmap.Config.RGB_565);
        }

        //Utils.matToBitmap(mat, bitmap);

        //mImgView.setImageBitmap(bitmap);
    }

    private void ProcessMarkerList_With_MHH(List<com.belhadj.oilureader.OiluMarker> mList) {
        String str = "";
        String[] bins = null;
        for (int i = 0; i < mList.size(); i++)
        {
            com.belhadj.oilureader.OiluMarker marker = mList.get(i);
            String id = "";
            try {
                id = marker.getMarkerId_WithMHH( bins, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void detectStaticImage() {
        //Log.isLoggable("")

        InputStream imageStream = this.getResources().openRawResource(R.raw.oilupng);
        bitmap = BitmapFactory.decodeStream(imageStream);
        if(bitmap == null) return;
        mRgba = new Mat();
        Utils.bitmapToMat(bitmap, mRgba);
        oiluDetector = new OiluMarkerDetector(MIN_CONTOUR_SIZE, new Size(CANONICAL_MARKER_SIZE, CANONICAL_MARKER_SIZE), false);
        for (int i = 0; i < 100; i++) {
            ProcessOiluCode(mRgba, false);

        }
    }

    private void getCameraPermission() {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCvCameraView.setCameraPermissionGranted();  // <------ THIS!!!
                } else {
                    // permission denied
                }
                return;
            }
        }
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            detectStaticImage();

        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        //mRgba = new Mat( height, width, CvType.CV_8UC3);
        oiluDetector = new com.belhadj.oilureader.OiluMarkerDetector(MIN_CONTOUR_SIZE, new Size(CANONICAL_MARKER_SIZE, CANONICAL_MARKER_SIZE), false);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private final BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                    // mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
}