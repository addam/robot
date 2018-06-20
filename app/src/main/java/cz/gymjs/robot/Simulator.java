package cz.gymjs.robot;

import android.util.Log;
import org.opencv.core.Point;
import org.opencv.core.Point3;

class Simulator {
    private Point velocity = new Point(0, 0);
    private Point3 position;
    private Point3 rotation;
    private long previousTime = System.nanoTime();

    // interpolate between a and b using factor c from 0 to 1
    // save result to a
    private static void lerp(Point3 a, Point3 b, double c) {
        double d = 1 - c;
        a.x = a.x * c + b.x * d;
        a.y = a.y * c + b.y * d;
        a.z = a.z * c + b.z * d;
    }

    private static Point3 subtract(Point3 a, Point3 b) {
        return new Point3(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    private static double poseNorm(Point3 a) {
        double radius = 5;
        double rot = 2 * Math.PI * radius * a.z;
        return Math.sqrt(a.x * a.x + a.y * a.y + rot * rot);
    }

    void setVelocity(Point _velocity) {
        double wheelVelocity = 15.2 / 300;
        velocity = new Point(_velocity.x * wheelVelocity, _velocity.y * wheelVelocity);
        previousTime = System.nanoTime();
    }

    private Point3 simulatedPose() {
        double time = (previousTime - System.nanoTime()) / 1e9;
        double rotz = rotation.z;
        double radius = 8;
        double levyX = position.x - radius * Math.cos(rotz) - velocity.x * time * Math.sin(rotz);
        double levyY = position.y - radius * Math.sin(rotz) + velocity.x * time * Math.cos(rotz);
        double pravyX = position.x + radius * Math.cos(rotz) - velocity.y * time * Math.sin(rotz);
        double pravyY = position.y + radius * Math.sin(rotz) + velocity.y * time * Math.cos(rotz);
        Log.d("simulatePose", "traveled distance " + Math.sqrt(Math.pow(position.x - (levyX + pravyX) / 2, 2) + Math.pow(position.y - (levyY + pravyY) / 2, 2)));
        return new Point3((levyX + pravyX) / 2, (levyY + pravyY) / 2, Math.atan2(pravyY - levyY, pravyX - levyX));
    }

    void tunePose(Point3 _position, Point3 _rotation) {
        if (position != null && rotation != null) {
            Point3 prediction = simulatedPose();
            Point3 diff = subtract(new Point3(position.x, position.y, rotation.z), prediction);
            double maxDistance = 5;
            double certanity = Math.exp(-poseNorm(diff) / maxDistance);
            Log.d("tunePose", "certanity " + certanity);
            lerp(_position, new Point3(prediction.x, prediction.y, position.z), certanity);
            lerp(_rotation, new Point3(rotation.x, rotation.y, prediction.z), certanity);
        }
        position = _position;
        rotation = _rotation;
    }

    public void reset() {
        position = null;
        rotation = null;
    }
}