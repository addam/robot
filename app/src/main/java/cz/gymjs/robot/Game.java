package cz.gymjs.robot;

import org.opencv.core.Point;
import org.opencv.core.Point3;

import java.util.ArrayList;
import java.util.List;

public class Game {
    List<Point> plechovky = new ArrayList<>();
    List<Point> roboti = new ArrayList<>();
    Point enemy = new Point(0, 0);
    boolean isLoaded;
    double rotation;
    long startTime = System.nanoTime();
    int collectedCans;
    int state;
    private int faze = 0;

    Game() {
        startTime = System.nanoTime();
    }

    protected static Point jizda(Point3 pose, double targetX, double targetY) {
        int speed = 300;
        float beta = (float) Math.atan2(targetY - pose.y, targetX - pose.x);
        float alfa = (float) (beta + pose.z);
        float c = (float) (1 / Math.tan(alfa));
        int levy, pravy;
        if (alfa < 0) {
            levy = speed;
            if (alfa > -Math.PI / 2) {
                pravy = (int) c * speed;
            } else {
                pravy = 0;
            }

        } else {
            pravy = speed;
            if (alfa < Math.PI / 2) {
                levy = (int) c * speed;
            } else {
                levy = 0;
            }
        }
        return new Point(levy, pravy);
    }

    public Point gameOff(Point3 pose) {
        if (20 < pose.y && pose.y < 100 && pose.x < 20) {
            return jizda(pose, 0, 120);
        }
        if (100 < pose.y && pose.x < 100 && 0 < pose.x) {
            return jizda(pose, 120, 120);
        }
        if (20 < pose.y && pose.y < 100 && 100 < pose.x) {
            return jizda(pose, 120, 0);
        }
        if (pose.y < 20 && pose.x < 100 && 20 < pose.x) {
            return jizda(pose, 0, 0);
        } else {
            return jizda(pose, 0, 0);
        }
    }

    /*
    public void gameOn() throws IOException, InterruptedException {
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double distance1 = Math.sqrt(Math.pow(robot.x - enemy.x, 2) + (Math.pow(robot.y - enemy.y, 2)));
        if (plechovky.isEmpty()) ;
        {
            motors.rotate(-150, -150);
            Thread.sleep(2500);
            if (distance1 < 10) {
                motors.rotate(-200, -200);
                Thread.sleep(2500);
            }
        }
        if (totalTime > 1500000000) {
            if (100 < robot.y && robot.y < 140) {
                jizda(new Point(120, 120));
            } else {
                jizda(new Point(0, 120));
            }
        }
        if (isLoaded) {
            int i2 = 0;
            if (robot.y < 50) {
                jizda(new Point((collectedCans + i2) * 6, 40));
            } else {
                motors.rotate(100, 100);
                Thread.sleep(2500);
                while (robot.y > 0) {
                    motors.rotate(-300, -300);
                    Thread.sleep(500);
                }
                if (vyberPlechovku(plechovky) >= 0) {
                    Point pl = plechovky.get(vyberPlechovku(plechovky));
                    double distance = Math.sqrt(Math.pow(robot.x - pl.x, 2) + (Math.pow(robot.y - pl.y, 2)));
                    if (distance < 5) {
                        // TODO wtf?
                        if ((true)) isLoaded = true;
                        else isLoaded = false;
                        state = 2;
                    } else if (state == 2) {
                        motors.rotate(100, 100);
                        Thread.sleep(2500);
                        if (distance < 5) {
                            motors.rotate(100, 100);
                            i2 += 1;
                        } else {
                            motors.rotate(-200, 200);
                            Thread.sleep(500);
                        }
                    }
                }
            }
        }
    }

    private int vyberPlechovku(List<Point> plechovky) {
        return 0;
    }
    */
}

