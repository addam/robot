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
import static org.opencv.core.Core.FILLED;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.core.CvType.CV_64F;
import static org.opencv.core.CvType.CV_8UC;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.utils.Converters.Mat_to_vector_double;
import static org.opencv.utils.Converters.vector_Point2f_to_Mat;
import static org.opencv.utils.Converters.vector_Point3f_to_Mat;

public class Tracking extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private MatOfPoint2f basePoints;
    private MatOfPoint2f warpedPoints;
    private Mat homography;
    Grid grid = new Grid(40);
    Mat baseFrame;
    Size size;

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
                    baseFrame = grid.image();
                    homography = Mat.eye(3, 3, CV_64F);
                    basePoints = grid.points();
                    warpedPoints = new MatOfPoint2f();
                } else {
                    super.onManagerConnected(status);
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Core.perspectiveTransform(basePoints, warpedPoints, homography);
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
        size = newFrame.size();
        Mat warpedFrame = new Mat();
        Imgproc.warpPerspective(baseFrame, warpedFrame, homography, size);
        if (!newFrame.empty() && warpedPoints.rows() >= 4) {
            MatOfPoint2f resultPoints = trackPoints(warpedPoints, warpedFrame, newFrame);
            Log.d("resultPoints", resultPoints.rows() + "x" + resultPoints.cols());
            Log.d("warpedPoints", warpedPoints.rows() + "x" + warpedPoints.cols());
            if (resultPoints.rows() < 4) {
                warpedPoints = new MatOfPoint2f();
                return newFrame;
            }
            Mat newHomography = Calib3d.findHomography(warpedPoints, resultPoints, Calib3d.RANSAC, 10);
            if (newHomography.rows() != 3 || newHomography.cols() != 3) {
                return newFrame;
            }
            Core.gemm(newHomography, homography, 1, homography, 0, homography);
            Log.d("homography", homography.dump());
            Imgproc.warpPerspective(baseFrame, newFrame, homography, size);
            Core.perspectiveTransform(basePoints, warpedPoints, homography);
            Point[] points = warpedPoints.toArray();
            for (Point p : points) {
                Imgproc.circle(newFrame, p, 3, new Scalar(0, 0, 255), -1);
            }
            List<Double> params = new ArrayList<>();
            Mat_to_vector_double(trackPose(), params);
            Imgproc.putText(newFrame, String.format("translation: %.2f %.2f %.2f", params.get(0), params.get(1), params.get(2)), new Point(10, 10), 0, 0.4, new Scalar(255, 255, 0));
            Imgproc.putText(newFrame, String.format("rotation: %.2f %.2f %.2f", params.get(3), params.get(4), params.get(5)), new Point(10, 20), 0, 0.4, new Scalar(255, 255, 0));
            return newFrame;
        } else {
            Core.addWeighted(warpedFrame, 0.3, newFrame, 0.7, 0, newFrame);
            return newFrame;
        }
    }

    public Mat trackPose() {
        double focal_length = size.width;
        Mat cameraMatrix = new Mat(3, 3, CV_32F);
        cameraMatrix.put(0, 0, focal_length, 0, size.width / 2, 0, focal_length, size.height / 2, 0, 0, 1);
        Mat rotation = new Mat(), translation = new Mat();
        MatOfPoint2f keyPoints = new MatOfPoint2f(vector_Point2f_to_Mat(grid.keyPoints()));
        Core.perspectiveTransform(keyPoints, keyPoints, homography);
        solvePnP(grid.objectPoints(), keyPoints, cameraMatrix, new MatOfDouble(), rotation, translation);
        translation.push_back(rotation);
        return translation;
    }

    public static MatOfPoint2f trackPoints(MatOfPoint2f leftPoints, Mat leftImg, Mat rightImg) {
        MatOfPoint2f result = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat pointError = new MatOfFloat();
        Video.calcOpticalFlowPyrLK(leftImg, rightImg, leftPoints, result, status, pointError, new Size(31, 31), 5);
        Point[] leftArray = leftPoints.toArray();
        Point[] rightArray = result.toArray();
        ArrayList<Point> goodLeftPoints = new ArrayList<Point>();
        ArrayList<Point> goodRightPoints = new ArrayList<Point>();
        for (int i = 0; i < status.rows() && i < pointError.rows(); ++i) {
            if (status.get(i, 0)[0] == 1) {// && pointError.get(i, 0)[0] < 100) {
                goodLeftPoints.add(leftArray[i]);
                goodRightPoints.add(rightArray[i]);
            }
        }
        Log.d("trackPoints", goodLeftPoints.size() + " good points out of " + leftPoints.rows());
        if (goodLeftPoints.isEmpty()) {
            new MatOfPoint2f().copyTo(leftPoints);
            leftPoints.copyTo(result);
        } else {
            leftPoints.fromList(goodLeftPoints);
            result.fromList(goodRightPoints);
        }
        return result;
    }
}

class Grid {
    int count = 7;
    int size;
    int width;

    Grid(int _size) {
        size = _size;
        width = size * 15 / 200;
    }

    Mat image() {
        Mat result = new Mat((count + 2) * size, (count + 2) * size, CV_8UC4);
        result.setTo(new Scalar(255, 255, 255));
        for (int i = 1; i <= count; ++i) {
            Imgproc.rectangle(result, new Point(i * size, size), new Point(i * size + width, count * size), new Scalar(0, 0, 0), FILLED);
            Imgproc.rectangle(result, new Point(size, i * size), new Point(count * size, i * size + width), new Scalar(0, 0, 0), FILLED);
        }
        return result;
    }

    List<Point> keyPoints() {
        List<Point> result = new ArrayList<>();
        result.add(new Point(size, size));
        result.add(new Point(count * size + width, size));
        result.add(new Point(size, count * size + width));
        result.add(new Point(count * size + width, count * size + width));
        return result;
    }

    MatOfPoint2f points() {
        List<Point> result = keyPoints();
        for (int i = 1; i <= count; ++i) {
            for (int j = 1; j <= count; ++j) {
                if (i > 0) result.add(new Point(i * size, j * size));
                if (i < count) result.add(new Point(i * size + width, j * size));
                if (j > 0) result.add(new Point(i * size + width, j * size + width));
                if (j < count) result.add(new Point(i * size, j * size + width));
            }
        }
        return new MatOfPoint2f(vector_Point2f_to_Mat(result));
    }

    MatOfPoint3f objectPoints() {
        List<Point3> result = new ArrayList<>();
        result.add(new Point3(size, size, 0));
        result.add(new Point3(count * size + width, size, 0));
        result.add(new Point3(size, count * size + width, 0));
        result.add(new Point3(count * size + width, count * size + width, 0));
        return new MatOfPoint3f(vector_Point3f_to_Mat(result));
    }
}