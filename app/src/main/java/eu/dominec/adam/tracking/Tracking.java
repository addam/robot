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
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;

public class Tracking extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
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
                    mFrame = new Mat();
                    mPointMat = new MatOfPoint2f();
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
                Point[] tmp_array = new Point[inarr.length];
                for (int i = 0; i < inarr.length; ++i) {
                    tmp_array[i] = inarr[i].pt;
                }
                mPointMat.fromArray(tmp_array);
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
        Mat newFrame = inputFrame.rgba();
        if (!mFrame.empty() && !mPointMat.empty()) {
            mPointMat = trackPoints(mPointMat, mFrame, newFrame);
            Point[] points = mPointMat.toArray();
            for (Point p : points) {
                Imgproc.circle(newFrame, p, 3, new Scalar(0, 0, 255));
            }
        }
        mFrame = newFrame;
        return mFrame;
    }

    public static MatOfPoint2f trackPoints(MatOfPoint2f leftPoints, Mat leftImg, Mat rightImg) {
        MatOfPoint2f result = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat pointError = new MatOfFloat();
        Video.calcOpticalFlowPyrLK(leftImg, rightImg, leftPoints, result, status, pointError, new Size(20, 20), 5);
        Point[] leftArray = leftPoints.toArray();
        Point[] rightArray = result.toArray();
        ArrayList<Point> goodLeftPoints = new ArrayList<Point>();
        ArrayList<Point> goodRightPoints = new ArrayList<Point>();
        for (int i = 0; i < status.rows() && i < pointError.rows(); ++i) {
            if (status.get(i, 0)[0] == 1 && pointError.get(i, 0)[0] < 100) {
                goodLeftPoints.add(leftArray[i]);
                goodRightPoints.add(rightArray[i]);
            }
        }
        Log.d("trackPoints", goodLeftPoints.size() + " good points out of " + leftPoints.rows());
        leftPoints.fromList(goodLeftPoints);
        result.fromList(goodRightPoints);
        return result;
    }

}
