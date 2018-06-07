package cz.gymjs.robot;

import org.opencv.core.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Game {
    boolean IsLoaded;
    MotorController motors;
    Point robot;
    int CollectedCans[] = new int[0];
    double rotation;
    List<Point> plechovky = new ArrayList<>();
    List<Point> roboti = new ArrayList<>();
    Point can;
    long startTime = System.nanoTime();
    long endTime = System.nanoTime();
    long totalTime = endTime - startTime;
    int N;
    int collectedCans;
    Point enemy;
    int state;
    private boolean isLoaded;

    Game(MotorController motors) throws IOException {
        //TODO: nastavení při spuštění hry
        long startTime = System.nanoTime();
    }

    void jizda(Point target) {
        float speed = 300;
        float beta = (float) Math.atan2(can.y - robot.y, can.x - robot.x);
        float alfa = (float) (beta + rotation);
        float c = (float) (1 / Math.tan(alfa));
        float[] motory = new float[2];
        if (alfa < 0) {
            motory[0] = speed;
            if (alfa > -Math.PI / 2) {
                motory[1] = c * speed;
            } else {
                motory[1] = 0;
            }

        } else {
            motory[1] = speed;
            if (alfa < Math.PI / 2) {
                motory[0] = c * speed;
            } else {
                motory[0] = 0;
            }
        }
    }

    void GameOff() {
        {
            if (20 < robot.y && robot.y < 100 && robot.x < 20) {
                jizda(new Point(0, 120));
            }
            if (100 < robot.y && robot.x < 100 && 0 < robot.x) {
                jizda(new Point(120, 120));
            }
            if (20 < robot.y && robot.y < 100 && 100 < robot.x) {
                jizda(new Point(120, 0));
            }
            if (robot.y < 20 && robot.x < 100 && 20 < robot.x) {
                jizda(new Point(0, 0));
            } else {
                jizda(new Point(0, 0));
            }
        }
    }

    public void Gameon() throws IOException, InterruptedException {
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double distance1 = Math.sqrt(Math.pow(robot.x - enemy.x, 2) + (Math.pow(robot.y - enemy.y, 2)));
        if (distance1 < 10) {
            motors.rotate(-200, -200);
            Thread.sleep(2500);
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
                    double distance = Math.sqrt(Math.pow(robot.x - can.x, 2) + (Math.pow(robot.y - can.y, 2)));
                    if (distance < 5) {
                        if ((true)) IsLoaded = true;
                        else IsLoaded = false;
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

}

