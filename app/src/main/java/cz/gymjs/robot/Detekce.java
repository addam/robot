package cz.gymjs.robot;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.MORPH_DILATE;
import static org.opencv.imgproc.Imgproc.MORPH_OPEN;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

public class Detekce {
    public List<Point> plechovky = new ArrayList<Point>();
    public List<Point> prekazka = new ArrayList<Point>();

    public void detect(Mat mrizka, Mat fotka) {
        Mat vysledek = new Mat();
        int l = 90;
        int r = 110;
        float a = 255 / (r - l);
        float b = l;
        Core.subtract(fotka, new Scalar(b, b, b), vysledek);
        Core.multiply(vysledek, new Scalar(a, a, a), vysledek);
        Mat kelner = getStructuringElement(MORPH_RECT, new Size(10, 10));
        Core.absdiff(mrizka, vysledek, vysledek);
        Imgproc.morphologyEx(vysledek, vysledek, MORPH_OPEN, kelner);
        Imgproc.morphologyEx(vysledek, vysledek, MORPH_DILATE, kelner);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat mat = new Mat();
        Imgproc.cvtColor(vysledek, vysledek, COLOR_RGB2GRAY);
        Imgproc.findContours(vysledek, contours, mat, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
            Rect bounds = Imgproc.boundingRect(points);
            int pointCount = points.toList().size();
            if (pointCount >= 4) {
                float ar = bounds.width / (float) bounds.height;
                if (ar >= 0.50 & ar <= 0.80) {
                    System.out.println("plechovka");
                    int x = bounds.x + bounds.width / 2;
                    double y = bounds.y + bounds.height * 0.2;
                    plechovky.add(new Point(x, y));
                } else {
                    int x = bounds.x + bounds.width / 2;
                    double y = bounds.y + bounds.height * 0.2;
                    prekazka.add(new Point(x, y));
                }
            } else {
                int x = bounds.x + bounds.width / 2;
                double y = bounds.y + bounds.height * 0.2;
                prekazka.add(new Point(x, y));
            }
        }
    }
}
