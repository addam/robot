package cz.gymjs.robot;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    Tracker tracker;
    Game game;
    Size origSize;
    boolean isTracking;
    boolean isPlaying;

    @Override
    public void onBackPressed() {
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

        view.setOnClickListener(view1 -> {
            if (isTracking) {
                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                try {
                    MotorController motors = new MotorController(manager);
                    game = new Game(motors);
                    isPlaying = true;
                } catch (IOException e) {
                    Log.d("onCreate", "Initialization failed: " + e.getLocalizedMessage());
                }
            } else {
                isTracking = true;
            }
        });
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        origSize = new Size(width, height);
        tracker = new Tracker(new Size(320, 240));
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputData) {
        Mat frame = inputData.rgba();
        if (frame.cols() != tracker.size.width) {
            Imgproc.resize(frame, frame, tracker.size);
        }
        Mat predictedFrame = tracker.warpedGrid();
        if (isTracking && tracker.updateHomography(predictedFrame, frame)) {
            List<Double> params = tracker.pose();
            if (isPlaying) {
                game.robot.x = params.get(0);
                game.robot.y = params.get(1);
                game.rotation = params.get(3);
                game.gameOff();
            }
            Imgproc.putText(frame, String.format("translation: %.2f %.2f %.2f", params.get(0), params.get(1), params.get(2)), new Point(10, 10), 0, 0.4, new Scalar(255, 255, 0));
            Imgproc.putText(frame, String.format("rotation: %.1f %.1f %.1f", params.get(3)*180/Math.PI, params.get(4)*180/Math.PI, params.get(5)*180/Math.PI), new Point(10, 20), 0, 0.4, new Scalar(255, 255, 0));
        } else {
            Core.addWeighted(predictedFrame, 0.3, frame, 0.7, 0, frame);
            isTracking = false;
        }
        for (Point p : tracker.warpedPoints().toArray()) {
            Imgproc.circle(frame, p, 2, new Scalar(0, isTracking ? 255:0, isTracking ? 0:255), -1);
        }
        if (frame.cols() != origSize.width) {
            Imgproc.resize(frame, frame, origSize);
        }
        return frame;
    }

}
