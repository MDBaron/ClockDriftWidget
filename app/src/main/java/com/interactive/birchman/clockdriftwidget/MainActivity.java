package com.interactive.birchman.clockdriftwidget;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    public Boolean isRunning = false;
    private AsyncTask mTask;
    double offset = 0.0;
    double sysTimeDiff = 0.0;
    double sysTime = 0.0;
    double netTime = 0.0;
    double timeDev = 0.0;

    String timeString = "";
    String timeColor = "#9E9E9E";
    private String fileName = "Clock_Log";

    FileOutputStream fos = null;

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
        timeDev = sysTimeDiff;
        sysTimeDiff = sysTimeDiff/1000/60;
        if(sysTimeDiff > 5){
            // Red -> More than 5 minutes
            timeColor = "#F44336";
        } else if(sysTimeDiff < 5 && sysTimeDiff > 1){
            // Green -> Less than 5 minutes
            timeColor = "#4CAF50";
        } else if(sysTimeDiff < 1 && sysTimeDiff > 0){
            // Yellow -> Less than a minute
            timeColor = "#FFEB3B";
        } else {
            // Gray -> Unknown
            timeColor = "#9E9E9E";
        }
        setTimeUI(timeString, timeColor, Double.toString(timeDev));
    }

    public void setTimeUI(String time, String color, String deviation) {
    final String t = time;
    final String c = color;
        final String d = deviation;
    runOnUiThread(new Runnable() {
        @Override
        public void run () {
            TextView currentTime = (TextView) findViewById(R.id.current_time);
            currentTime.setText(String.valueOf(t));
            currentTime.setBackgroundColor(Color.parseColor(c));
            TextView logView = (TextView) findViewById(R.id.log_View);
            String log = "Current Clock drift of " + d + "milliseconds at " + t + "\n";
            logView.setText(log);
            storeLog(log);
        }
    }
    );
}


    public void storeLog(String str){
        //TO-DO: Split stored data with /n

        try {
            fos = openFileOutput(fileName, MODE_APPEND);
        } catch(IOException e) {
            e.printStackTrace();
        }

        //Output Stream appends each script to byte-wise file
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        try {
            osw.write(str + "\n");
            osw.flush();
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        timeDev = 0.0;
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
        setTimeUI(timeString, timeColor, Double.toString(timeDev));
    }

    public void transmit(View view){
        //Pull data from internal device storage and email

        String logData = "";
        StringBuffer sb = new StringBuffer("");

        try {
            FileInputStream fis = openFileInput("Clock_Log");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            String inputStr = br.readLine();
            while(inputStr != null){
                sb.append(inputStr);
                inputStr = br.readLine();
            }

            isr.close();
            fis.close();
        } catch (IOException e){
            e.printStackTrace();
        }

        logData = sb.toString();

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL,new String[] { "tehmatty86@gmail.com" });
        intent.putExtra(Intent.EXTRA_SUBJECT, "Clock Sync Data" );
        intent.putExtra(Intent.EXTRA_TEXT, logData);
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        }
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
                    Thread.sleep(1000);
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



