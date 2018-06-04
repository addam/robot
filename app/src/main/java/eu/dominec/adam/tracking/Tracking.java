package eu.dominec.adam.tracking;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.calib3d.Calib3d.solvePnP;
import static org.opencv.core.Core.FILLED;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_64F;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.utils.Converters.Mat_to_vector_double;
import static org.opencv.utils.Converters.vector_Point2f_to_Mat;
import static org.opencv.utils.Converters.vector_Point3f_to_Mat;

public class Tracking extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    boolean isTracking;
    private Mat homography;
    Grid grid;
    Mat baseFrame;
    Size size, origSize;

    void reset() {
        homography = Mat.eye(3, 3, CV_64F);
    }

    @Override
    public void onBackPressed() {
        if (isTracking) {
            isTracking = false;
        } else if (homography.dot(homography) != 3){
            reset();
        } else {
            super.onBackPressed();
        }
    }

    MatOfPoint2f projectPoints() {
        Rect screen = new Rect(new Point(0, 0), size);
        MatOfPoint2f result = new MatOfPoint2f();
        Core.perspectiveTransform(grid.points(), result, homography);
        List<Point> points = result.toList();
        int write = 0;
        for (int i = 0; i<points.size(); ++i) {
            if (screen.contains(points.get(i))) {
                if (write != i) {
                    points.set(write, points.get(i));
                }
                ++write;
            }
        }
        result.fromList(points.subList(0, write));
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        }
        final JavaCameraView view = new JavaCameraView(this.getApplicationContext(), 0);
        view.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT));
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
        view.setOnClickListener(view1 -> isTracking = true);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        origSize = new Size(width, height);
        grid = new Grid(40);
        baseFrame = grid.image();
        reset();
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat newFrame = inputFrame.rgba();
        while(newFrame.cols() > 400) {
            Imgproc.pyrDown(newFrame, newFrame);
        }
        size = newFrame.size();
        Mat warpedFrame = new Mat();
        Imgproc.warpPerspective(baseFrame, warpedFrame, homography, size);
        if (isTracking && trackHomography(warpedFrame, newFrame)) {
            List<Double> params = trackPose();
            Imgproc.putText(newFrame, String.format("translation: %.2f %.2f %.2f", params.get(0), params.get(1), params.get(2)), new Point(10, 10), 0, 0.4, new Scalar(255, 255, 0));
            Imgproc.putText(newFrame, String.format("rotation: %.1f %.1f %.1f", params.get(3)*180/Math.PI, params.get(4)*180/Math.PI, params.get(5)*180/Math.PI), new Point(10, 20), 0, 0.4, new Scalar(255, 255, 0));
        } else {
            Core.addWeighted(warpedFrame, 0.3, newFrame, 0.7, 0, newFrame);
            isTracking = false;
        }
        for (Point p : projectPoints().toArray()) {
            Imgproc.circle(newFrame, p, 2, new Scalar(0, isTracking ? 255:0, isTracking ? 0:255), -1);
        }
        if (size.width != origSize.width) {
            Imgproc.pyrUp(newFrame, newFrame, origSize);
        }
        return newFrame;
    }

    boolean trackHomography(Mat warpedFrame, Mat newFrame) {
        MatOfPoint2f warpedPoints = projectPoints();
        MatOfPoint2f resultPoints = trackPoints(warpedPoints, warpedFrame, newFrame);
        if (resultPoints.rows() < 4) {
            return false;
        }
        Mat diff = Calib3d.findHomography(warpedPoints, resultPoints, Calib3d.RANSAC, 10);
        if (diff.empty()) {
            return false;
        }
        Core.gemm(diff, homography, 1, new Mat(), 0, homography);
        return true;
    }

    public List<Double> trackPose() {
        double focal_length = size.width;
        Mat cameraMatrix = new Mat(3, 3, CV_32F);
        cameraMatrix.put(0, 0, focal_length, 0, size.width / 2, 0, focal_length, size.height / 2, 0, 0, 1);
        Mat vrot = new Mat(), vtrans = new Mat();
        MatOfPoint2f keyPoints = new MatOfPoint2f(vector_Point2f_to_Mat(grid.keyPoints()));
        Core.perspectiveTransform(keyPoints, keyPoints, homography);
        solvePnP(grid.objectPoints(), keyPoints, cameraMatrix, new MatOfDouble(), vrot, vtrans);
        vtrans.push_back(vrot);
        List<Double> result = new ArrayList<>();
        Mat_to_vector_double(vtrans, result);
        return result;
    }

    public static MatOfPoint2f trackPoints(MatOfPoint2f leftPoints, Mat leftImg, Mat rightImg) {
        MatOfPoint2f rightPoints = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat pointError = new MatOfFloat();
        Video.calcOpticalFlowPyrLK(leftImg, rightImg, leftPoints, rightPoints, status, pointError, new Size(5, 5), 2);
        List<Point> lp = leftPoints.toList();
        List<Point> rp = rightPoints.toList();
        int write = 0;
        for (int i = 0; i < status.rows(); ++i) {
            if (status.get(i, 0)[0] == 1) {
                if (write != i) {
                    lp.set(write, lp.get(i));
                    rp.set(write, rp.get(i));
                }
                ++write;
            }
        }
        Log.d("trackPoints", write + " good points out of " + leftPoints.rows());
        if (write == 0) {
            new MatOfPoint2f().copyTo(leftPoints);
            leftPoints.copyTo(rightPoints);
        } else {
            leftPoints.fromList(lp.subList(0, write));
            rightPoints.fromList(rp.subList(0, write));
        }
        return rightPoints;
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
        Mat result = new Mat((count + 1) * size + width, (count + 1) * size + width, CV_8UC4);
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
        int n = count;
        for (int i = 1; i <= n; ++i) {
            for (int j = 1; j <= n; ++j) {
                if (i > 1 && j > 1) result.add(new Point(i * size, j * size));
                if (i < n && j > 1) result.add(new Point(i * size + width, j * size));
                if (i > 1 && j < n) result.add(new Point(i * size, j * size + width));
                if (i < n && j < n) result.add(new Point(i * size + width, j * size + width));
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