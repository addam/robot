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
    public static final int GAMETYPE_FORWARD = 1;
    public static final int GAMETYPE_BACKWARD = 2;
    public static final int GAMETYPE_ROTATION = 3;
    public static final int GAMETYPE_GAME = 4;
    int gametype = 0;


    Game(int intent) {
        startTime = System.nanoTime();
        gametype = intent;

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

    public Command ruler(Point3 position, Point3 rotation) throws InterruptedException {
        Point3 pose = new Point3(position.x, position.y, rotation.z);
        if (gametype == GAMETYPE_FORWARD) return dopredu();
        else if (gametype == GAMETYPE_BACKWARD) return dozadu();
        else if (gametype == GAMETYPE_ROTATION) return doprava();
        else return gameOff(pose);

    }


    private Command doleva() {
        if (System.nanoTime() - startTime < (long) 67e12) return new Command(1000, 0);
        else return new Command(0, 0);
        // @vojta TODO putStringExtra should be called from within the main menu
        // here, we should getStringExtra instead
        //i.putStringExtra("doleva");
    }


    private Command doprava() {
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

    public Command gameOff(Point3 pose) {
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

    public Command gameOn(Point3 pose) {
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double distance1 = Math.sqrt(Math.pow(pose.x - enemy.x, 2) + (Math.pow(pose.y - enemy.y, 2)));
        if (plechovky.isEmpty())
        {
            return new Command(0, 1000);
        }

        if (totalTime > 1500000000)

        {
            if (pose.y > 100 && pose.y < 140) {
                return new Command(500, 500);
            } else {
                return new Command(0, 120);
            }
        }
        if (isLoaded)

        {
            int i2 = 0;
            if (pose.y < 50) {
                return new Command((collectedCans + i2) * 6, 40);
            } else {
                return new Command(100, 100);
                while (pose.y > 0) {
                    return new Command(-300, -300);
                }
                if (vyberPlechovku(plechovky) >= 0) {
                    Point pl = plechovky.get(vyberPlechovku(plechovky));
                    double distance = Math.sqrt(Math.pow(pose.x - pl.x, 2) + (Math.pow(pose.y - pl.y, 2)));
                    if (distance < 5) {
                        // TODO wtf?
                        if ((true)) isLoaded = true;
                        else isLoaded = false;
                        state = 2;
                    } else if (state == 2) {
                        return new Command(100, 100);
                        if (distance < 5) {
                            return new Command(100, 100);
                            i2 += 1;
                        } else {
                            return new Command(-200, 200);
                        }
                    }
                }
            }
        }
        return null;
    }

    private int vyberPlechovku(List<Point> plechovky) {
        return 0;
    }
}
