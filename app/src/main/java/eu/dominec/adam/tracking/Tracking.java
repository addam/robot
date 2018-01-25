package eu.dominec.adam.tracking;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

public class Tracking extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private Point[] mPoints = new Point[0];
    private Mat mFrame;
    private MatOfPoint2f mPointMat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final JavaCameraView view = new JavaCameraView(this.getApplicationContext(), 0);
        view.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.WRAP_CONTENT));
        setContentView(view);
        view.setVisibility(SurfaceView.VISIBLE);
        view.setCvCameraViewListener(this);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                if (status == LoaderCallbackInterface.SUCCESS) {
                    view.enableView();
                } else {
                    super.onManagerConnected(status);
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MatOfKeyPoint points = new MatOfKeyPoint();
                FeatureDetector detector = FeatureDetector.create(FeatureDetector.HARRIS);
                detector.detect(mFrame, points);
                KeyPoint[] inarr = points.toArray();
                mPoints = new Point[inarr.length];
                for (int i = 0; i < inarr.length; ++i) {
                    mPoints[i] = inarr[i].pt;
                }
                mPointMat = new MatOfPoint2f();
                mPointMat.fromArray(mPoints);
            }
        });

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mFrame = inputFrame.rgba();

        for (Point p : mPoints) {
            Imgproc.circle(mFrame, p, 3, new Scalar(0, 0, 255));
        }
        return mFrame;
    }
}
