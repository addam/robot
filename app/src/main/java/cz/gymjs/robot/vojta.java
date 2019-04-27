package cz.gymjs.robot;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

import static cz.gymjs.robot.Game.GAMETYPE_BACKWARD;
import static cz.gymjs.robot.Game.GAMETYPE_FORWARD;
import static cz.gymjs.robot.Game.GAMETYPE_GAME;
import static cz.gymjs.robot.Game.GAMETYPE_ROTATION;

public class vojta extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vojta);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public void jizdadopredu(View view) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("name", GAMETYPE_FORWARD);
        startActivity(i);

    }

    public void jizdadozadu(View view) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("name", GAMETYPE_BACKWARD);
        startActivity(i);
    }

    public void doprava(View view) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("name", GAMETYPE_ROTATION);
        startActivity(i);
    }

    public void hra(View view) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("name", GAMETYPE_GAME);
        startActivity(i);
    }
}