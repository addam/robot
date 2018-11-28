package cz.gymjs.robot;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;

public class Hlavniscreen extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hlavniscreen);
    }

    public void Jizda(View view) {
        Intent intent = new Intent(this, Jizda.class);
        startActivity(intent);
    }
}

