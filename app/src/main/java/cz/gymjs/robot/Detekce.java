package cz.gymjs.robot;

import android.util.Log;

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
import java.util.Collections;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.MORPH_DILATE;
import static org.opencv.imgproc.Imgproc.MORPH_OPEN;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.utils.Converters.vector_Point2f_to_Mat;

public class Detekce {
    public List<Point> plechovky = new ArrayList<Point>();
    public List<Point> prekazka = new ArrayList<Point>();
    public List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    public Mat test;

    public void detect(Mat mrizka, Mat fotka) {
        plechovky.clear();
        prekazka.clear();
        Mat vysledek = new Mat(fotka.size(), CV_8UC4);
        int l = 90;
        int r = 110;
        float a = 255.0f / (r - l);
        float b = l;
        //Imgproc.cvtColor(fotka, fotka, Imgproc.COLOR_BGR2BGRA);
        Core.subtract(fotka, new Scalar(b, b, b), vysledek);
        Core.multiply(vysledek, new Scalar(a, a, a), vysledek);
        test = vysledek.clone();
        Log.d("onCameraFrame", "test cloned");
        Mat kelner = getStructuringElement(MORPH_RECT, new Size(10, 10));
        Core.absdiff(mrizka, vysledek, vysledek);
        Imgproc.morphologyEx(vysledek, vysledek, MORPH_OPEN, kelner);
        Imgproc.morphologyEx(vysledek, vysledek, MORPH_DILATE, kelner);
        Mat mat = new Mat();
        Imgproc.cvtColor(vysledek, vysledek, COLOR_RGB2GRAY);
        contours.clear();
        Imgproc.findContours(vysledek, contours, mat, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
            Rect bounds = Imgproc.boundingRect(points);
            int pointCount = points.toList().size();
            double x = bounds.x + bounds.width / 2;
            double y = bounds.y + bounds.height * 0.8;
            if (pointCount >= 4) {
                float ar = bounds.height / (float) bounds.width;
                if (ar >= 0.50 && ar <= 0.80 && bounds.width <= 300) {
                    Imgproc.rectangle(test, bounds.tl(), bounds.br(), new Scalar(255, 0, 255));
                    plechovky.add(new Point(x, y));
                } else {
                    prekazka.add(new Point(x, y));
                }
            } else {
                prekazka.add(new Point(x, y));
            }
        }
    }

    public List<Point> misto(Mat homography) {
        if (plechovky.isEmpty()) {
            return Collections.emptyList();
        } else {
            MatOfPoint2f p = new MatOfPoint2f(vector_Point2f_to_Mat(plechovky));
            MatOfPoint2f vysledek = new MatOfPoint2f();
            Core.perspectiveTransform(p, vysledek, homography.inv());
            List<Point> vysledplech = vysledek.toList();
            for (int i = 0; i < plechovky.size(); i++) {
                System.out.println("plechovka je tady x: " + plechovky.get(i).x + " y: " + plechovky.get(i).y);
                System.out.println("Souradnice plechovky x: " + vysledplech.get(i).x / 40 + " y: " + vysledplech.get(i).y / 40);
            }
            return vysledplech;
        }


    }
}
