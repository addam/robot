package cz.gymjs.robot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class TestAcvtivity extends Activity {
    String zamer;
    int obtiznost;
    long startTime = System.nanoTime();
    int collectedCans;
    int state;
    private int faze = 0;


    void setobtiznist() {
        Intent callingIntent = getIntent();
        Intent i = new Intent(this, MainActivity.class);
        obtiznost = callingIntent.getIntExtra("name", -1);
    }

    //getIntent().getExtras();

    private Command doleva() {
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_acvtivity);


    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }
}
