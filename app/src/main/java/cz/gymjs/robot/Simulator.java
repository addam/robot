package cz.gymjs.robot;


import org.opencv.core.Point;
import org.opencv.core.Point3;

class Simulator {
    protected Point velocity;
    double radius = 8;
    double wheelVelocity = 15.2;
    double smoothing = 1;
    Point3 pose;
    long previousTime = System.nanoTime();

    protected static Point3 slerp(Point3 a, Point3 b, double c) {
        double d = 1 - c;
        return new Point3(a.x * c + b.x * d, a.y * c + b.y * d, a.z * c + b.z * d);
    }

    protected static Point3 subtract(Point3 a, Point3 b) {
        return new Point3(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    protected static double norm(Point3 a) {
        return Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
    }

    public void setVelocity(Point _velocity) {
        velocity = new Point(_velocity.x * wheelVelocity, _velocity.y * wheelVelocity);
        previousTime = System.nanoTime();
    }

    Point3 simulatedPose() {
        double time = (previousTime - System.nanoTime()) / 1e9;
        double levyX = pose.x - radius * Math.cos(pose.z) - velocity.x * time * Math.sin(pose.z);
        double levyY = pose.y - radius * Math.sin(pose.z) + velocity.x * time * Math.cos(pose.z);
        double pravyX = pose.x + radius * Math.cos(pose.z) - velocity.x * time * Math.sin(pose.z);
        double pravyY = pose.y + radius * Math.sin(pose.z) + velocity.y * time * Math.cos(pose.z);
        return new Point3((levyX + pravyX) / 2, (levyY + pravyY) / 2, Math.atan2(levyY - pravyY, pravyX - levyX));
    }

    public Point3 predictPose(Point3 guess) {
        if (previousTime == 0) {
            return pose;
        } else {
            Point3 prediction = simulatedPose();
            Point3 diff = subtract(guess, prediction);
            double certanity = Math.exp(-smoothing * norm(diff));
            return slerp(guess, prediction, certanity);
        }
    }
}