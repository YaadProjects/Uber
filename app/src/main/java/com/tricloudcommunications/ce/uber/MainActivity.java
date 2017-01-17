package com.tricloudcommunications.ce.uber;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.onesignal.OneSignal;
import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    Switch userTypeSwitch;
    String userType = "";

    public void getStarted(View view){

        if (userTypeSwitch.isChecked()){

            userType = String.valueOf(userTypeSwitch.getTextOn());
            Log.i("Switch Status", userTypeSwitch.getTextOn().toString());

        }else {

            userType = String.valueOf(userTypeSwitch.getTextOff());
            Log.i("Switch Status", userTypeSwitch.getTextOff().toString());
        }

        ParseUser.getCurrentUser().put("userType", userType);
        Log.i("info", "Redirecting as: " + userType);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove Action/Title Bar and makes full screen
        //Source http://stackoverflow.com/questions/2862528/how-to-hide-app-title-in-android
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userTypeSwitch = (Switch) findViewById(R.id.userTypeSwitch);

        if (ParseUser.getCurrentUser() == null){

            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {

                    if (e == null){

                        Log.i("info", "Anonymous login successful");
                    }else {

                        Log.i("info", "Anonymous login failed: " + e.toString());
                    }
                }
            });
        }else {

            if (ParseUser.getCurrentUser().get("userType") != null){

                Log.i("Info", "Already Logged in Redirecting as: " + ParseUser.getCurrentUser().get("userType"));

            }

        }

        userTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Log.i("Switch Status", Boolean.toString(isChecked));
            }
        });


        //Initialize Parse
        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        //Initialize Onsignal
        OneSignal.startInit(this).init();
        // Call syncHashedEmail anywhere in your app if you have the user's email.
        // This improves the effectiveness of OneSignal's "best-time" notification scheduling feature.
        //OneSignal.syncHashedEmail(ParseUser.getCurrentUser().getEmail());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
