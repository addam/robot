package cz.gymjs.robot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class vojta extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vojta);
    }

    public void jes(View view) {
        System.out.println("jes");
    }

    public void jizdadopredu(View view) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("name", 1);
        startActivity(i);

    }

    public void jizdadozadu(View view) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("name", 2);
        startActivity(i);
    }
}
