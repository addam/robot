package eu.dominec.adam.tracking;

import android.util.Log;

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

public class Tracker {
    float smoothing = 1; // smooth out noise from the pose calculation
    private Mat homography;
    private Mat prevDiff;
    private Grid grid = new Grid(40);
    Size size;

    Tracker(Size _size) {
        size = _size;
        reset();
    }

    boolean reset() {
        if (homography == null || homography.dot(homography) != 3) {
            homography = Mat.eye(3, 3, CV_64F);
            return true;
        } else {
            return false;
        }
    }

    MatOfPoint2f warpedPoints() {
        Rect screen = new Rect(new Point(0, 0), size);
        MatOfPoint2f result = new MatOfPoint2f();
        Core.perspectiveTransform(grid.points(), result, homography);
        List<Point> points = result.toList();
        // keep only points within the screen area
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

    boolean updateHomography(Mat leftFrame, Mat rightFrame) {
        MatOfPoint2f leftPoints = warpedPoints();
        MatOfPoint2f rightPoints = trackPoints(leftPoints, leftFrame, rightFrame);
        if (rightPoints.rows() < 4) {
            return false;
        }
        Mat diff = Calib3d.findHomography(leftPoints, rightPoints, Calib3d.RANSAC, 10);
        if (diff.empty()) {
            return false;
        }
        if (prevDiff != null) {
            double prod = diff.dot(prevDiff) / Core.norm(prevDiff, Core.NORM_L2SQR);
            double coef = Math.exp(-smoothing * Math.abs(prod - 1));
            prevDiff = diff.clone();
            Core.addWeighted(diff, coef, Mat.eye(3, 3, CV_64F), 1-coef, 0, diff);
        } else {
            prevDiff = diff;
        }
        Core.gemm(diff, homography, 1, new Mat(), 0, homography);
        return true;
    }

    public List<Double> pose() {
        double focal_length = size.width;
        Mat cameraMatrix = new Mat(3, 3, CV_32F);
        cameraMatrix.put(0, 0, focal_length, 0, size.width / 2, 0, focal_length, size.height / 2, 0, 0, 1);
        Mat vrot = new Mat(), vtrans = new Mat();
        MatOfPoint2f keyPoints = new MatOfPoint2f(vector_Point2f_to_Mat(grid.keyPoints()));
        Core.perspectiveTransform(keyPoints, keyPoints, homography);
        solvePnP(grid.objectPoints(), keyPoints, cameraMatrix, new MatOfDouble(), vrot, vtrans);
        Mat rotation = new Mat();
        Calib3d.Rodrigues(vrot, rotation);
        Core.gemm(rotation.t(), vtrans, 1, new Mat(), 0, vtrans);
        vtrans.push_back(vrot);
        List<Double> result = new ArrayList<>();
        Mat_to_vector_double(vtrans, result);
        return result;
    }

    private static MatOfPoint2f trackPoints(MatOfPoint2f leftPoints, Mat leftImg, Mat rightImg) {
        MatOfPoint2f rightPoints = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat pointError = new MatOfFloat();
        Video.calcOpticalFlowPyrLK(leftImg, rightImg, leftPoints, rightPoints, status, pointError, new Size(5, 5), 2);
        List<Point> lp = leftPoints.toList();
        List<Point> rp = rightPoints.toList();
        // keep only points with status == 1
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
            return new MatOfPoint2f();
        } else {
            leftPoints.fromList(lp.subList(0, write));
            rightPoints.fromList(rp.subList(0, write));
            return rightPoints;
        }
    }

    public Mat warpedGrid() {
        Mat result = new Mat();
        Imgproc.warpPerspective(grid.image, result, homography, size);
        return result;
    }
}

class Grid {
    private int count = 7;
    private int size;
    private int width;
    Mat image;

    Grid(int _size) {
        size = _size;
        width = size * 15 / 200;
        image = new Mat((count + 1) * size + width, (count + 1) * size + width, CV_8UC4);
        image.setTo(new Scalar(255, 255, 255));
        for (int i = 1; i <= count; ++i) {
            Imgproc.rectangle(image, new Point(i * size, size), new Point(i * size + width, count * size), new Scalar(0, 0, 0), FILLED);
            Imgproc.rectangle(image, new Point(size, i * size), new Point(count * size, i * size + width), new Scalar(0, 0, 0), FILLED);
        };
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
