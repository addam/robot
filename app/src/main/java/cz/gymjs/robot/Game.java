package cz.gymjs.robot;

import org.opencv.core.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Game {
    MotorController motors;
    Point robot = new Point(0, 0);
    List<Point> plechovky = new ArrayList<>();
    List<Point> roboti = new ArrayList<>();
    Point enemy = new Point(0, 0);
    boolean isLoaded;
    double rotation;
    long startTime = System.nanoTime();
    int collectedCans;
    int state;
    private int faze = 0;

    Game(MotorController _motors) throws IOException {
        //TODO: nastavení při spuštění hry
        motors = _motors;
        startTime = System.nanoTime();
        robot = new Point(0, 0);
    }

    void jizda(Point target) {
        int speed = 300;
        float beta = (float) Math.atan2(target.y - robot.y, target.x - robot.x);
        float alfa = (float) (beta + rotation);
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
        try {
            motors.rotate(levy, pravy);
        } catch (IOException e) {
        }
    }

    void otaceni(double stupne) throws IOException, InterruptedException {
        if (stupne > 0) {
            motors.rotate(1000, 0);
        } else {
            motors.rotate(-1000, 0);
        }
        Thread.sleep((long) (1000 * stupne / 53));
    }

    void dopredu(double centimetry) throws IOException, InterruptedException {
        if (centimetry > 0) {
            motors.rotate(1000, 1000);
        } else {
            motors.rotate(-1000, -1000);
        }
        Thread.sleep((long) (1000 * centimetry / 15.2));
    }

    void stuj() throws InterruptedException, IOException {
        motors.rotate(0, 0);
        Thread.sleep(20000);
    }
    public void evilplan() throws IOException, InterruptedException {
        if (faze == 0) {
            otaceni(16.4);
            dopredu(61.7);
            motors.rotate(0, 0);
        } else if (faze == 1) {
            otaceni(156.6);
            dopredu(58.3 - 18);
            dopredu(-10);
            motors.rotate(0, 0);
        } else if (faze == 2) {
            stuj();
            otaceni(-180 + 6.5 + 12);
            dopredu(120);
            motors.rotate(0, 0);
        } else if (faze == 3) {
            otaceni(-90 - 12);
            dopredu(130);
            motors.rotate(0, 0);
        } else {
            stuj();
        }
        faze += 1;


        /*motors.rotate(-100, 100);
        Thread.sleep(1000);
        motors.rotate(100, 100);
        Thread.sleep(3500);
        Thread.sleep(1500);
        motors.rotate(100, -100);
        Thread.sleep(500);
        motors.rotate(100, 100);
        Thread.sleep(5500);
     */
    }

    public String gameOff() {
        try {
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
            return "OK";
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }

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
}

