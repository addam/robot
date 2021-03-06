package cz.gymjs.robot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private String error;
    private Tracker tracker;
    private Game game;
    private Size origSize;
    private boolean isTracking;
    private boolean isPlaying;
    private MotorController motors;
    private Simulator simulator = new Simulator();
    private Detekce detekce = new Detekce();

    /* private Command doleva() {
           if (System.nanoTime() - startTime < (long) 67e12) return new Command(1000, 0);
           else return new Command(0, 0);

           i.putStringExtra("doleva", 1);
       }


       private Command doprva() {
           if (System.nanoTime() - startTime < (long) 67e12) return new Command(0, 1000);
           else return new Command(0, 0);
           i.putStringExtra("doprava", 2);
       }

       private Command dopredu() {
           if (System.nanoTime() - startTime < (long) 6e12) return new Command(1000, 1000);
           else return new Command(0, 0);
           i.putStringExtra("dopredu", 3);
       }

       private Command dozadu() {
           if (System.nanoTime() - startTime < (long) 67e12) return new Command(-1000, -1000);
           else return new Command(0, 0);
           i.putStringExtra("dozadu", 4);
       }
       */
    @Override
    public void onBackPressed() {
        error = null;
        if (isPlaying) {
            isPlaying = false;
            simulator.reset();
            if (motors != null) {
                try {
                    motors.rotate(0, 0);
                } catch (IOException ignored) {
                }
            }
        } else if (isTracking) {
            isTracking = false;
        } else if (!tracker.reset()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
        Intent intent = getIntent();
        view.setOnClickListener(view1 -> {
            error = null;
            try {
                if (isTracking) {
                    if (isPlaying) {
                        isPlaying = false;
                        simulator.reset();
                        if (motors != null) {
                            motors.rotate(0, 0);
                        }
                    } else {
                        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                        game = new Game(intent.getIntExtra("name", 0));
                        isPlaying = true;
                        motors = new MotorController(manager);
                    }
                } else {
                    isTracking = true;
                }
            } catch (IOException e) {
                error = e.getLocalizedMessage();
            }
        });
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        origSize = new Size(width, height);
        tracker = new Tracker(new Size(320, height * 320 / width));
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputData) {
        Mat frame = inputData.rgba();
        Mat predictedFrame = tracker.warpedGrid();
        try {
            if (frame.cols() != tracker.size.width) {
                Imgproc.resize(frame, frame, tracker.size);
            }
            if (isTracking) {
                Pair<Point3, Point3> gridPose = tracker.pose(predictedFrame, frame);
                Pair<Point3, Point3> robotPose = Tracker.reflectPose(gridPose.first, gridPose.second);
                if (isPlaying) {
                    simulator.tunePose(gridPose.first, gridPose.second);
                    predictedFrame = tracker.warpedGrid();
                    detekce.detect(predictedFrame, frame);
                    List<Point> wow = detekce.misto(tracker.homography);
                    Mat test = detekce.test;
                    Imgproc.resize(test, test, origSize);
                    Log.d("onCameraFrame", "simulate...");
                    Command velocity = game.ruler(robotPose.first, robotPose.second);
                    if (motors != null) {
                        Log.d("onCameraFrame", "simulate..." + velocity.left + velocity.right);
                        motors.rotate(velocity.left, velocity.right);
                    } else {
                        Imgproc.putText(frame, String.format(Locale.ENGLISH, "left: %d right: %d", velocity.left, velocity.right), new Point(10, frame.rows()), 0, 0.4, new Scalar(0, 255, 255));

                        Log.d("Plechovky", "plechovky jsou na techto mistech" + wow);


                        Imgproc.drawContours(frame, detekce.contours, -1, new Scalar(255, 0, 0));
                        Imgproc.putText(frame, String.format(Locale.ENGLISH, "left: %.1d right: %.1d", velocity.left, velocity.right), new Point(10, frame.rows()), 0, 0.4, new Scalar(0, 255, 255));
                    }
                    //simulator.setVelocity(velocity);
                    tracker.setPose(gridPose);
                    Imgproc.warpPerspective(tracker.grid.image, predictedFrame, tracker.homography, predictedFrame.size());
                    Core.addWeighted(predictedFrame, 0.3, frame, 0.7, 0, frame);
                    return test;
                }
                Imgproc.putText(frame, String.format(Locale.ENGLISH, "x: %.2f y: %.2f rot: %.1f", gridPose.first.x, gridPose.first.y, gridPose.second.z * 180 / Math.PI), new Point(10, 10), 0, 0.4, new Scalar(0, 255, 255));
            } else {
                Core.addWeighted(predictedFrame, 0.3, frame, 0.7, 0, frame);
            }
            for (Point p : tracker.warpedPoints().toArray()) {
                Scalar color = isTracking ? isPlaying ? new Scalar(255, 0, 0) : new Scalar(0, 255, 0) : new Scalar(0, 0, 255);
                Imgproc.circle(frame, p, 2, color, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = e.getLocalizedMessage();
        }
        if (error != null) {
            for (int y = 20; error.length() > 50; y += 10) {
                Imgproc.putText(frame, error, new Point(0, y), 0, 0.4, new Scalar(255, 64, 0));
                error = error.substring(50);
            }
        }
        if (frame.cols() != origSize.width) {
            Imgproc.resize(frame, frame, origSize);
        }
        return frame;
    }

}