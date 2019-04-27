package cz.gymjs.robot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class volba extends Activity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.volba);
    }
}
     /*
if(i==1)return dopredu()
        Else if(i==2)return dozadu()


        void setobtiznist() {
        Intent callingIntent = getIntent();
        obtiznost = callingIntent.getIntExtra("name", -1);
        */
