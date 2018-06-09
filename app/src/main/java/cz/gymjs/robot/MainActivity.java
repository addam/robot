package cz.gymjs.robot;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    String error = "";
    Tracker tracker;
    Game game;
    Size origSize;
    boolean isTracking;
    boolean isPlaying;
    private MotorController motors;
    private Simulator simulator = new Simulator();

    @Override
    public void onBackPressed() {
        error = "";
        if (isPlaying) {
            isPlaying = false;
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

        view.setOnClickListener(view1 -> {
            error = "";
            try {
                if (isTracking) {
                    if (isPlaying) {
                        isPlaying = false;
                        motors.rotate(0, 0);
                    } else {
                        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                        motors = new MotorController(manager);
                        game = new Game();
                        isPlaying = true;
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
        try {
            if (frame.cols() != tracker.size.width) {
                Imgproc.resize(frame, frame, tracker.size);
            }
            Mat predictedFrame = tracker.warpedGrid();
            if (isTracking) {
                if (isPlaying) {
                    tracker.setPose(simulator.simulatedPose());
                }
                Point3 pose = tracker.pose(predictedFrame, frame);
                if (isPlaying) {
                    pose = simulator.predictPose(pose);
                    Point velocity = game.gameOff(pose);
                    motors.rotate((int) velocity.x, (int) velocity.y);
                    simulator.setVelocity(velocity);
                }
                Imgproc.putText(frame, String.format("x: %.2f y: %.2f rot: %.1f", pose.x, pose.y, pose.z * 180 / Math.PI), new Point(10, 10), 0, 0.4, new Scalar(255, 255, 0));
            } else {
                Core.addWeighted(predictedFrame, 0.3, frame, 0.7, 0, frame);
            }
            for (Point p : tracker.warpedPoints().toArray()) {
                Scalar color = isTracking ? isPlaying ? new Scalar(255, 0, 0) : new Scalar(0, 255, 0) : new Scalar(0, 0, 255);
                Imgproc.circle(frame, p, 2, color, -1);
            }
        } catch (Exception e) {
            error = e.getLocalizedMessage();
        }
        if (error.length() > 0) {
            Imgproc.putText(frame, error, new Point(0, 20), 0, 0.4, new Scalar(255, 64, 0));
        }
        if (frame.cols() != origSize.width) {
            Imgproc.resize(frame, frame, origSize);
        }
        return frame;
    }

}
