package cz.gymjs.robot;

import android.util.Log;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.*;

public class Detekce {
    public List<Point> plechovky = new ArrayList<Point>();
    public List<Point> prekazka = new ArrayList<Point>();
    public List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    public Mat test;

    public void detect(Mat mrizka, Mat fotka) {
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
                float ar = bounds.width / (float) bounds.height;
                if (ar >= 0.50 && ar <= 0.80) {
                    System.out.println("plechovka");
                    plechovky.add(new Point(x, y));
                } else {
                    prekazka.add(new Point(x, y));
                }
            } else {
                prekazka.add(new Point(x, y));
            }
        }
    }

    public void misto (ArrayList plechovky, ArrayList prekazka){

    }
}
