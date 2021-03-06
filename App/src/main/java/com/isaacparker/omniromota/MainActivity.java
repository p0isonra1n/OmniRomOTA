package com.isaacparker.omniromota;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.isaacparker.omniromota.Helpers.RootCommands;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    public static final boolean DEBUG = false;

    public static String onDeviceVersion = null;
    public static String onServerVersion = null;
    public static String deviceName = null;
    public static String MD5Device = null;
    public static String MD5Server = null;

    public final String dlAddress = "http://dl.omnirom.org/";
    public final String TAG = "OmniRomOTA";

    public static boolean downloading = false;

    TextView tvDeviceVersion;
    TextView tvServerVersion;
    TextView tvUpdate;
    Button btUpdate;
    Button btInstall;
    ProgressBar pbUpdate;

    final DownloadMD5Task dmd5task = new DownloadMD5Task(MainActivity.this);
    final DownloadRomTask dTask = new DownloadRomTask(MainActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvDeviceVersion = (TextView)findViewById(R.id.tvDeviceVersion);
        tvServerVersion = (TextView)findViewById(R.id.tvServerVersion);
        tvUpdate = (TextView)findViewById(R.id.tvUpdate);

        btUpdate = (Button)findViewById(R.id.btUpdate);
        btInstall = (Button)findViewById(R.id.btInstall);

        pbUpdate = (ProgressBar)findViewById(R.id.pbUpdate);

        try
        {
        if (!RootTools.isAccessGiven()) {
            Toast.makeText(getBaseContext(), "Root is required", Toast.LENGTH_SHORT).show();
            finish();
        }
        }catch (Exception e){
            Log.e(TAG, "Can not get root");
            e.printStackTrace();
        }

        //Get Versions
        getDeviceVersion();
        getServerVersionTask taskServer = new getServerVersionTask();
        taskServer.execute(new String[] { dlAddress + deviceName});

        //Set Display
        tvDeviceVersion.setText(onDeviceVersion);


        //Update code in getServerVersion

        //Download Update Button

        btUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Make sure directory exits
                File wallpaperDirectory = new File("/sdcard/OmniRomOTA/");
                wallpaperDirectory.mkdirs();

                File file = new File("/sdcard/OmniRomOTA/" + onServerVersion);
                if(file.exists())
                {
                    try {
                        Command commandmd5 = new Command(0, "md5sum /sdcard/OmniRomOTA/" + onServerVersion)
                        {
                            @Override
                            public void commandOutput(int i, String s) {
                                try{
                                    Log.i(TAG, "Reading file MD5 as file exists");
                                    File filemd5 = new File("/sdcard/OmniRomOTA/" + onServerVersion + ".md5sum");
                                    if(filemd5.exists()){
                                        String file = ReadFileHelper.getStringFromFile("/sdcard/OmniRomOTA/" + onServerVersion + ".md5sum").split(" ")[0];
                                        String md5 = s.split(" ")[0];
                                        Log.i(TAG, "Device MD5: " + md5 + " Server MD5: " + file);
                                        if(file.equals(md5)){
                                            Log.i(TAG, "Downloaded file matches MD5");
                                            btUpdate.setEnabled(false);
                                            btUpdate.setText("Update Already Downloaded");
                                            downloading = false;
                                            btInstall.setEnabled(true);
                                            btInstall.setText("Install Update");
                                        }else{
                                            Log.i(TAG, "MD5 does not match redownloading file");
                                            Toast.makeText(MainActivity.this, "Incomplete file redownloading file",Toast.LENGTH_SHORT).show();
                                            dTask.execute(dlAddress + deviceName + "/" + onServerVersion);
                                        }
                                    }else{
                                        Log.i(TAG, "MD5sum file does not exist, Download did not complete");
                                        dTask.execute(dlAddress + deviceName + "/" + onServerVersion);
                                    }
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void commandTerminated(int i, String s) {
                                Log.e(TAG, "MD5 Command Terminated " + s);
                            }

                            @Override
                            public void commandCompleted(int i, int i2) {
                                Log.i(TAG, "MD5 Command Completed");
                            }
                        };
                        RootTools.getShell(true).add(commandmd5).wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                else{
                    Log.i(TAG, "File does not exist, downloading file");
                    dTask.execute(dlAddress + deviceName + "/" + onServerVersion);
                }
            }
        });

        btInstall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(downloading){
                    dTask.cancel(true);
                    dmd5task.cancel(true);
                    btUpdate.setEnabled(true);
                    btUpdate.setText("Download Update");
                    downloading = false;
                    btInstall.setEnabled(true);
                    btInstall.setText("Install Update");
                }
                else{
                    installUpdate();
                }
            }
        });

    }

    public void installUpdate(){
        try {
            Command commandmd5 = new Command(0, "md5sum /sdcard/OmniRomOTA/" + onServerVersion)
            {
                @Override
                public void commandOutput(int i, String s) {
                    Log.e("OmniRom", "Command Output " + s);
                    try{
                        String file = ReadFileHelper.getStringFromFile("/sdcard/OmniRomOTA/" + onServerVersion + ".md5sum").split(" ")[0];
                        String md5 = s.split(" ")[0];
                        if(file.equals(md5))
                        {
                            if(!DEBUG)
                            {
                                CommandCapture command = new CommandCapture(0,"rm -f cache/recovery/openrecoveryscript",
                                    "echo install /sdcard/OmniRomOTA/" + onServerVersion + " >> /cache/recovery/openrecoveryscript",
                                    "echo wipe cache >> /cache/recovery/openrecoveryscript",
                                    "echo wipe dalvik >> /cache/recovery/openrecoveryscript");
                                RootTools.getShell(true).add(command);
                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                Boolean additionalzip = sharedPref.getBoolean("checkbox_installadditionalfiles", false);
                                if(additionalzip){
                                    String[] extrazips = new String[64];
                                    int count = 0;
                                    File f = new File("/sdcard/OmniRomOTA/ExtraZips/");
                                    File extrafiles[] = f.listFiles();
                                    for(File extrafile : extrafiles){
                                        extrazips[count] = "echo install " + extrafile.toString() + " >> /cache/recovery/openrecoveryscript";
                                        count++;
                                    }
                                    CommandCapture commandextra = new CommandCapture(0, extrazips);
                                    RootTools.getShell(true).add(commandextra);
                                }
                                CommandCapture commandreboot = new CommandCapture(0,"reboot recovery");
                                RootTools.getShell(true).add(commandreboot);
                            }else
                            {
                                Toast.makeText(MainActivity.this, "DEBUG Flash File", Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            Toast.makeText(MainActivity.this, "File Incomplete", Toast.LENGTH_SHORT).show();
                            btUpdate.setEnabled(true);
                            btUpdate.setText("Download Update");
                            btInstall.setEnabled(false);
                            btInstall.setText("MD5 Mismatch");
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void commandTerminated(int i, String s) {
                    Log.e("OmniRom", "Command Terminated " + s);
                }

                @Override
                public void commandCompleted(int i, int i2) {
                     Log.e("OmniRom", "Command Completed");
                }
            };
            RootTools.getShell(true).add(commandmd5);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Install Failed", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isUpdate(String currentVersion, String newVersion){
        if(DEBUG) return true;
        if(Integer.parseInt(currentVersion.split("-")[2]) < Integer.parseInt(newVersion.split("-")[2]))
            return true;
        return false;
    }

    public void getDeviceVersion() {
        Log.i(TAG, "Getting Device Version");
        try
        {
            String result = RootCommands.CommandOutput("getprop");
            String[] PerLine = result.split("\n");
            for(int i = 0; i < PerLine.length; i++) {
                if(PerLine[i].contains("ro.modversion")){
                    String[] version = PerLine[i].split(":");
                    onDeviceVersion = version[1].replace("[", "").replace("]", "");
                    deviceName = version[1].replace("[", "").replace("]", "").split("-")[3];
                }
            }
        }catch(Exception e) {
            Toast.makeText(getBaseContext(),"Device Version not found" , Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Device Not Found");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class getServerVersionTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String url = urls[0];
            if(url.equals(null))return "No Device";
            Log.i(TAG, "Server Version URL: " + urls[0]);
            String[] serverVersions = new String[256];
            int count = 0;
            List<String> myList = new ArrayList<String>();

            String result = "";

            try{
                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
                HttpGet httpGet = new HttpGet(url);
                HttpResponse response = httpClient.execute(httpGet, localContext);


                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                response.getEntity().getContent()
                        )
                );
                String line = null;
                while ((line = reader.readLine()) != null){
                    result += line + "\n";
                }
            }catch (Exception e){
                Log.e(TAG, "Getting rom list failed!");
            }

            Pattern p = Pattern.compile("href=\"(.*?)\"");
            Matcher m = p.matcher(result);
            String link = null;
            while(m.find()) {
                myList.add(m.group(1)); // this variable should contain the link
            }
            for(int i = 0; i < myList.size() - 1;i++){
                if(myList.get(i).contains("omni") && !myList.get(i).contains("md5sum")){
                    serverVersions[count] = myList.get(i).replace("/" + deviceName + "/", "");
                    count++;
                }
            }
            if(serverVersions[0] == null) return "Not Found";
            return serverVersions[count - 1];
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "Server Version List Completed");
            onServerVersion = result;
            tvServerVersion.setText(onServerVersion.replace("omni", "OmniROM").replace(".zip", ""));
            //Check if update Available
            if(isUpdate(onDeviceVersion, onServerVersion)){
                tvUpdate.setTextColor(Color.parseColor("#00BB00"));
                tvUpdate.setText("Update Available: Yes");
                btUpdate.setVisibility(1);
                btInstall.setEnabled(false);
                btInstall.setVisibility(1);
                pbUpdate.setVisibility(1);
            }else{
                tvUpdate.setTextColor(Color.parseColor("#FF0000"));
                tvUpdate.setText("Update Available: No");
            }
        }
    }

    private class DownloadRomTask extends AsyncTask<String, Integer, String> {

        private Context context;

        public DownloadRomTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            wl.acquire();

            try {
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(sUrl[0]);
                    Log.i(TAG, "ROM URL: " + sUrl[0]);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                        return "Server returned HTTP " + connection.getResponseCode()
                                + " " + connection.getResponseMessage();

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength();

                    // download the file
                    input = connection.getInputStream();
                    output = new FileOutputStream("/sdcard/OmniRomOTA/" + onServerVersion);
                    Log.i(TAG, "ROM Save Location: " + "/sdcard/OmniRomOTA/" + onServerVersion);

                    byte data[] = new byte[65536];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // allow canceling with back button
                        if (isCancelled())
                            return null;
                        total += count;
                        // publishing the progress....
                        if (fileLength > 0) // only if total length is known
                            publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }
                } catch (Exception e) {
                    return e.toString();
                } finally {
                    try {
                        if (output != null)
                            output.close();
                        if (input != null)
                            input.close();
                    }
                    catch (IOException ignored) { }

                    if (connection != null)
                        connection.disconnect();
                }
            } finally {
                wl.release();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "Starting download of ROM");
            Toast.makeText(getBaseContext(), "Download Started", Toast.LENGTH_SHORT).show();
            btUpdate.setEnabled(false);
            downloading = true;
            btInstall.setEnabled(true);
            btInstall.setText("Cancel Download");
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            btUpdate.setText("Downloading ROM " + String.valueOf(progress[0]) + "%");
            pbUpdate.setVisibility(ProgressBar.VISIBLE);
            pbUpdate.setIndeterminate(false);
            pbUpdate.setMax(100);
            pbUpdate.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
                Log.w(TAG, "ROM Download Failed: " + result);
                btUpdate.setEnabled(true);
                btUpdate.setText("Download Update");
                downloading = false;
                pbUpdate.setProgress(0);
            }
            else{
                dmd5task.execute(dlAddress + deviceName + "/" + onServerVersion + ".md5sum");
                Log.w(TAG, "ROM Download Completed: " + result);
            }
        }
    }

    private class DownloadMD5Task extends AsyncTask<String, Integer, String> {

        private Context context;

        public DownloadMD5Task(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            wl.acquire();

            try {
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(sUrl[0]);
                    Log.i(TAG, "MD5SUM URL: " + sUrl[0]);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                        return "Server returned HTTP " + connection.getResponseCode()
                                + " " + connection.getResponseMessage();

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength();

                    // download the file
                    input = connection.getInputStream();
                    output = new FileOutputStream("/sdcard/OmniRomOTA/" + onServerVersion + ".md5sum");
                    Log.i(TAG, "MD5SUM Save Location: " + "/sdcard/OmniRomOTA/" + onServerVersion + ".md5sum");

                    byte data[] = new byte[65536];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // allow canceling with back button
                        if (isCancelled())
                            return null;
                        total += count;
                        // publishing the progress....
                        if (fileLength > 0) // only if total length is known
                            publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }
                } catch (Exception e) {
                    return e.toString();
                } finally {
                    try {
                        if (output != null)
                            output.close();
                        if (input != null)
                            input.close();
                    }
                    catch (IOException ignored) { }

                    if (connection != null)
                        connection.disconnect();
                }
            } finally {
                wl.release();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "Starting MD5 Download");
            Toast.makeText(getBaseContext(), "Download Started", Toast.LENGTH_SHORT).show();
            btUpdate.setEnabled(false);
            downloading = true;
            btInstall.setEnabled(true);
            btInstall.setText("Cancel Download");
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            btUpdate.setText("Downloading MD5" + String.valueOf(progress[0]) + "%");
            pbUpdate.setVisibility(ProgressBar.VISIBLE);
            pbUpdate.setIndeterminate(false);
            pbUpdate.setMax(100);
            pbUpdate.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
                Log.w(TAG, "ROM Download Failed: " + result);
                btUpdate.setEnabled(true);
                btUpdate.setText("Download Update");
                downloading = false;
                pbUpdate.setProgress(0);
            }
            else{
                Log.w(TAG, "ROM Download Completed: " + result);
                Toast.makeText(context,"Update downloaded", Toast.LENGTH_SHORT).show();
                btUpdate.setEnabled(false);
                btUpdate.setText("Download Complete");
                downloading = false;
                btInstall.setEnabled(true);
                btInstall.setText("Install Update");
            }
        }
    }

}
