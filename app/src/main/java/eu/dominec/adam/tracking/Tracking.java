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
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.calib3d.Calib3d.SOLVEPNP_AP3P;
import static org.opencv.calib3d.Calib3d.SOLVEPNP_P3P;
import static org.opencv.calib3d.Calib3d.solvePnP;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.core.CvType.CV_64F;
import static org.opencv.utils.Converters.Mat_to_vector_double;
import static org.opencv.utils.Converters.vector_Point2f_to_Mat;
import static org.opencv.utils.Converters.vector_Point3f_to_Mat;

public class Tracking extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private Mat mFrame;
    private MatOfPoint2f warpedPoints;
    private MatOfPoint3f objectPoints;
    private MatOfPoint2f keyPoints;
    private Mat baseFrame;
    private Mat homography;
    private MatOfPoint2f basePoints;

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
        Log.i("start", "start");
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                if (status == LoaderCallbackInterface.SUCCESS) {
                    view.enableView();
                    mFrame = new Mat();
                    baseFrame = new Mat();
                    homography = Mat.eye(3, 3, CV_64F);
                    basePoints = new MatOfPoint2f();
                    warpedPoints = new MatOfPoint2f();
                } else {
                    super.onManagerConnected(status);
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                baseFrame = mFrame.clone();
                MatOfKeyPoint points = new MatOfKeyPoint();
                FeatureDetector detector = FeatureDetector.create(FeatureDetector.HARRIS);
                Imgproc.medianBlur(mFrame, mFrame, 9);
                detector.detect(mFrame, points);
                KeyPoint[] inarr = points.toArray();
                Point[] tmp_array = new Point[inarr.length];
                for (int i = 0; i < inarr.length; ++i) {
                    tmp_array[i] = inarr[i].pt;
                }
                basePoints.fromArray(tmp_array);
                warpedPoints.fromArray(tmp_array);
                Log.i("onClick", "got " + basePoints.rows() + " points");
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
        mFrame = newFrame;
        if (!mFrame.empty() && !basePoints.empty()) {
            Mat warpedFrame = new Mat();
            Imgproc.warpPerspective(baseFrame, warpedFrame, homography, newFrame.size());
            MatOfPoint2f resultPoints = trackPoints(warpedPoints, warpedFrame, newFrame);
            if (resultPoints.empty()) {
                basePoints.fromArray(new Point[]{});
                return mFrame;
            }
            keyPoints = new MatOfPoint2f(vector_Point2f_to_Mat(resultPoints.toList().subList(0, 4)));
            List<Point3> tmpPoints = new ArrayList<>();
            for (Point p : keyPoints.toArray()) {
                tmpPoints.add(new Point3(p.x, p.y, 0));
            }
            objectPoints = new MatOfPoint3f(vector_Point3f_to_Mat(tmpPoints));
            List<Double> params = new ArrayList<>();
            Mat_to_vector_double(trackPose(), params);
            Mat newHomography = Calib3d.findHomography(warpedPoints, resultPoints, Calib3d.RANSAC, 10);
            Log.i("onCameraFrame", "newHomography is " + newHomography.rows() + "x" + newHomography.cols() + " of type " + newHomography.type() + ", homography has type " + homography.type());
            Core.gemm(newHomography, homography, 1, homography, 0, homography);
            Imgproc.warpPerspective(baseFrame, newFrame, homography, baseFrame.size());
            Core.perspectiveTransform(basePoints, warpedPoints, homography);
            Point[] points = warpedPoints.toArray();
            for (Point p : points) {
                Imgproc.circle(newFrame, p, 3, new Scalar(0, 0, 255), -1);
            }
            Imgproc.putText(newFrame, String.format("translation: %.2f %.2f %.2f", params.get(0), params.get(1), params.get(2)), new Point(10, 10), 0, 0.4, new Scalar(255, 255, 0));
            Imgproc.putText(newFrame, String.format("rotation: %.2f %.2f %.2f", params.get(3), params.get(4), params.get(5)), new Point(10, 20), 0, 0.4, new Scalar(255, 255, 0));
            return newFrame;
        } else {
            return mFrame;
        }

    }

    public Mat trackPose() {
        double focal_length = mFrame.cols(); // Approximate focal length.
        Point center = new Point(mFrame.cols()/2,mFrame.rows()/2);
        Mat camera_matrix = new Mat(3, 3, CV_32F);
        camera_matrix.put(0, 0, focal_length, 0, center.x, 0 , focal_length, center.y, 0, 0, 1);
        MatOfDouble dist_coeffs = new MatOfDouble(new Mat(4,1, CV_64F)); // Assuming no lens distortion
        dist_coeffs.setTo(new Scalar(0));
        Mat rotation_vector = new Mat(); // Rotation in axis-angle form
        Mat translation_vector = new Mat();
        solvePnP(objectPoints, keyPoints, camera_matrix, dist_coeffs, rotation_vector, translation_vector, false, SOLVEPNP_P3P);
        translation_vector.push_back(rotation_vector);
        return translation_vector;
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
