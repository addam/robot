package cz.gymjs.robot;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;

public class Jizda extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jizda);
    }
    public Command DoPredu(View view) {
        long startTime = 0;
        if (System.nanoTime() - startTime < (long) 6e9) return new Command(1000, 1000);
        else return new Command(0, 0);

    }
    public void DoZadu(View view) {

    }
    public void Vpravo(View view) {

    }
    public void Vlevo(View view) {

    }
}
