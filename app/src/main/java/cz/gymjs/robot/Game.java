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

    protected static Command jizda(Point3 pose, double targetX, double targetY) {
        int speed = 30;
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
        return new Command(levy, pravy);
    }

    private Command doleva() {
        if (System.nanoTime() - startTime < (long) 67e12) return new Command(1000, 0);
        else return new Command(0, 0);
        // @vojta TODO putStringExtra should be called from within the main menu
        // here, we should getStringExtra instead
        //i.putStringExtra("doleva");
    }


    private Command doprva() {
        if (System.nanoTime() - startTime < (long) 67e12) return new Command(0, 1000);
        else return new Command(0, 0);
// @vojta TODO used to be
//        i.putStringExtra("doprava");
    }

    private Command dopredu() {
        if (System.nanoTime() - startTime < (long) 6e12) return new Command(1000, 1000);
        else return new Command(0, 0);
// @vojta TODO used to be
//        i.putStringExtra("dopredu");
    }

    private Command dozadu() {
        if (System.nanoTime() - startTime < (long) 67e12) return new Command(-1000, -1000);
        else return new Command(0, 0);
// @vojta TODO used to be
//        i.putStringExtra("dozadu");
    }

    public Command gameOff(Point3 position, Point3 rotation) {
        if (20 < position.y && position.y < 100 && position.x < 20) {
            return jizda(position, 0, 120);
        }
        if (100 < position.y && position.x < 100 && 0 < position.x) {
            return jizda(position, 120, 120);
        }
        if (20 < position.y && position.y < 100 && 100 < position.x) {
            return jizda(position, 120, 0);
        }
        if (position.y < 20 && position.x < 100 && 20 < position.x) {
            return jizda(position, 0, 0);
        } else {
            return jizda(position, 0, 0);
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

