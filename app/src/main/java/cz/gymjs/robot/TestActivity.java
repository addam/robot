package cz.gymjs.robot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TestActivity extends Activity {
    String zamer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_activity);
        Intent i = getIntent();
        // commented out to make things compile
        // @vojta TODO hint: add some double quotes
        /*zamer = i.getStringExtra(funkce);
        if (i = dopredu return Command(dopredu));
        if (i = *return Command( *));
        */
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }
}
