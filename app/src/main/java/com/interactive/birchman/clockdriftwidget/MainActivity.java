package com.interactive.birchman.clockdriftwidget;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //I don't trust Async's .close(), so this is some extra insurance
    public Boolean isRunning = false;

    //Work-horse Async task for this endeavor
    private AsyncTask mTask;

    //Work-horse variables
    int iterations = 0;
    double avg = 0.0;
    double avgPool = 0.0;
    double offset = 0.0;
    double sysTimeDiff = 0.0;
    double sysTime = 0.0;
    double netTime = 0.0;
    double timeDev = 0.0;

    //Clock Drift Tiers for selecting color
    double base = 0.0; //TO-DO: Allow user to set these values at run-time?
    double tier1 = 1.0; //1 minute
    double tier2 = 5.0; //5 minutes

    //Data logging strings
    String timeString = "";
    String timeColor = "#9E9E9E"; //Default color is "Brad" Gray
    private String fileName = "Clock_Log";
    private String email = "tehmatty86@gmail.com"; //TO-DO: Add a mechanism for user to manually type in email address
    private String emailSubject = "Clock Drift Data"; //TO-DO: Same as above, but add to subject-line some sort of identifier for the device that is transmitting?

    FileOutputStream fos = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.findViewById(android.R.id.content).setBackgroundColor(Color.parseColor("#9E9E9E"));
    }//onCreate

    /**
     * Poll Current system time, and look for network time.
     */
    public void pollTime(View view) {
        //Check and Set 'running' flag for this button
        Button butt = (Button) findViewById(R.id.poll_button);

        //isRunning governs while loop operation for this ASync Task, cancel() will not necessarily halt the loop at the correct time
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
    }//pollTime

    public void setTime(String time, String color) {

        //Call runOnUIThread to allow AsyncTask to change View indirectly
        //Convert system time into Hours and minutes
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        timeString = df.format(sysTime);
        //save delta as ms for later use
        timeDev = Math.floor(sysTimeDiff);
        //Convert delta from millis -> seconds -> minutes
        sysTimeDiff = (sysTimeDiff/1000)/60;

        //Calculate average
        avg = getAvg(timeDev);

        //Switch statement for text Color selection according to "Drift"
        if(sysTimeDiff > tier2){

            // Red -> More than 5 minutes
            timeColor = "#F44336";

        } else if(sysTimeDiff < tier2 && sysTimeDiff > tier1){

            // Green -> Less than 5 minutes
            timeColor = "#4CAF50";

        } else if(sysTimeDiff < tier1 && sysTimeDiff > base){

            // Yellow -> Less than a minute
            timeColor = "#FFEB3B";

        } else {

            // Gray -> Unknown
            timeColor = "#9E9E9E";
        }

        setTimeUI(timeString, timeColor, Double.toString(timeDev), avg);
    }//setTime

    /*
    * Calculate average of drift
     */
    public double getAvg(double plus){

        iterations += 1;
        avgPool += plus;

        return (avgPool/iterations);

    }//getAvg

    public void setTimeUI(String time, String color, String deviation, Double avgDev) {

        //Need to 'final' these to call runOnUIThread()
        final String t = time;
        final String c = color;
        final String d = deviation;
        final Double a = Math.floor(avgDev);
        runOnUiThread(new Runnable() {
             @Override
            public void run () {

                 //Fill current time to textView
                 TextView currentTime = (TextView) findViewById(R.id.current_time);
                 currentTime.setText(String.valueOf(t));
                 //Set background to the 'drift' color
                 currentTime.setBackgroundColor(Color.parseColor(c));
                 //Parse data to screen and local storage
                 TextView logView = (TextView) findViewById(R.id.log_View);
                 String log = "Current Drift: " + d + "ms at " + t + "\n" + "Average Drift: " + a + "ms\n";
                 logView.setText(log);
                 storeLog(log);
        }//run
    }
    );
}//setTimeUI


    public void storeLog(String str){
        //We need to store this locally, which is a breeze on Android

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

    }//storeLog

    public void fudgeTime(View view){
        //Add a substantial time event
        Random r = new Random();
        //Randomly generate double in range
        offset = r.nextDouble() * (750000.00 - 100.00) + 100.00; //TO-DO: Could make this random to simulate time event data
    }//fudgeTime

    /**
     * For debugging purposes
     */
    public void reset(View view){
        //Reset time tracking for current session
        iterations = 0;
        avg = 0.0;
        avgPool = 0.0;
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
        //Put butt in chair, this could take a while

        //Reset 'running' flag, cancel Asynctask, reset button label
        isRunning = false;
        butt.setText("Begin Polling");
        mTask.cancel(true);

        //Delete local file
        String dir = getFilesDir().getAbsolutePath();
        File f0 = new File(dir, fileName);
        boolean d0 = f0.delete();
        Log.w("File Deleted?: ", "Local File Deleted: " + dir + fileName + d0);
        Toast t = new Toast(this);
        Toast.makeText(getBaseContext(), "Local File Deleted...", Toast.LENGTH_SHORT).show();

        //Reset Timer status
        setTimeUI(timeString, timeColor, Double.toString(timeDev), avg);
    }//reset

    public void transmit(View view){
        //Pull data from internal device storage and email

        String logData = "";
        StringBuffer sb = new StringBuffer("");

        //Buffer the read from local storage line by line so as to not overwhelm allocated device memory with a single "batch" of bytes
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

        //Could transmit this serially to a server, but emailing this for the moment is easier (>^..^<)/
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL,new String[] { email });
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject );
        intent.putExtra(Intent.EXTRA_TEXT, logData);
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        }
    }//transmit


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
                netTime = System.currentTimeMillis() + offset; //At present, NTP time is proving difficult to poll outside of TextClock, will have to manually generate events for now
                offset = 0.0; //Reset Fudge
                //Find delta between system and actual UTC
                sysTimeDiff = Math.abs(sysTime - netTime);

                //Set time data for this iteration
                setTime(timeString, timeColor);
                //Delay loop arbitrarily to prevent constant processing
                try {
                    Thread.sleep(1000); //1 sec poll iterations seem to be enough of a delay
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }

            }
            return null;
        }//doInBackground

        @Override
        protected void onPostExecute(Void result) {

        }//onPostExecute
    }//Class TimeCop


}//Class MainActivity



