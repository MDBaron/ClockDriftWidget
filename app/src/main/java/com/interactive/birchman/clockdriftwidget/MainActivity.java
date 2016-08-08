package com.interactive.birchman.clockdriftwidget;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    public Boolean isRunning = false;
    private AsyncTask mTask;
    double offset = 0.0;
    double sysTimeDiff = 0.0;
    double sysTime = 0.0;
    double netTime = 0.0;

    String timeString = "";
    String timeColor = "#9E9E9E";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.findViewById(android.R.id.content).setBackgroundColor(Color.parseColor("#9E9E9E"));
    }

    /**
     * Poll Current system time, and look for network time.
     */
    public void pollTime(View view) {
        //Check and Set 'running' flag for this button
        Button butt = (Button) findViewById(R.id.poll_button);

        if (isRunning) {
            isRunning = false;
            butt.setText("Begin Polling");
            mTask.cancel(true);
            return;
        } else {
            isRunning = true;
            butt.setText("Stop Polling...");
            mTask = new TimeCop().execute();
        }
    }

    public void setTime(String time, String color) {
        //Call runOnUIThread to allow AsyncTask to change View indirectly
        //Convert system time into Hours and minutes
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        timeString = df.format(sysTime);
        //Switch statement for text Color selection according to "Drift"
        sysTimeDiff = sysTimeDiff/1000/60;
        if(sysTimeDiff > 5){
            timeColor = "#F44336";// Red -> More than 5 minutes
        } else if(sysTimeDiff < 5 && sysTimeDiff > 1){
            timeColor = "#4CAF50";// Green -> Less than 5 minutes
        } else if(sysTimeDiff < 1 && sysTimeDiff > 0){
            timeColor = "#FFEB3B";// Yellow -> Less than a minute
        } else {
            timeColor = "#9E9E9E";// Gray -> Unknown
        }
        setTimeUI(timeString, timeColor);
    }

    public void setTimeUI(String time, String color) {
    final String t = time;
    final String c = color;
    runOnUiThread(new Runnable() {
        @Override
        public void run () {
            TextView currentTime = (TextView) findViewById(R.id.current_time);
            currentTime.setText(String.valueOf(t));
            currentTime.setBackgroundColor(Color.parseColor(c));
        }
    }
    );
}
    public void fudgeTime(View view){
        //Add a substantial time event
        offset = 1000000;
    }

    /**
     * For debugging purposes
     */
    public void reset(View view){
        //Reset time tracking for current session
        offset = 0.0;
        sysTimeDiff = 0.0;
        netTime = 0.0;
        sysTime = System.currentTimeMillis();
        //Reset Timer status attributes
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        timeString = df.format(sysTime);
        timeColor = "#9E9E9E";

        //Find butt
        Button butt = (Button) findViewById(R.id.poll_button);
        //Reset 'running' flag, cancel Asynctask, reset button label
        isRunning = false;
        butt.setText("Begin Polling");
        mTask.cancel(true);
        //Reset Timer status
        setTimeUI(timeString, timeColor);
    }


    protected class TimeCop extends AsyncTask<Void, Void, Void> {
        /**
         * Allow process to run apart from UI thread
         */


        @Override
        protected Void doInBackground(Void... params) {

            while(isRunning) {
                //Grab Time display
                TextView currentTime = (TextView) findViewById(R.id.current_time);
                //Poll System time
                sysTime = System.currentTimeMillis();
                //TO-DO: Poll NTP time
                netTime = System.currentTimeMillis() + offset;
                offset = 0.0; //Reset Fudge
                //Find delta between system and actual UTC
                sysTimeDiff = Math.abs(sysTime - netTime);
                //Delay loop arbitrarily to prevent constant processing
                setTime(timeString, timeColor);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

        }
    }


}



