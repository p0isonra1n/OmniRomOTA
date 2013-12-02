package com.isaacparker.omniromota;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.isaacparker.omniromota.CommandHelper;
import com.stericson.RootTools.RootTools;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    public static String onDeviceVersion = null;
    public static String onServerVersion = null;
    public static String deviceName = null;
    public final String dlAddress = "http://dl.omnirom.org/";

    public static boolean downloading = false;

    TextView tvDeviceVersion;
    TextView tvServerVersion;
    TextView tvUpdate;
    Button btUpdate;
    Button btInstall;
    ProgressBar pbUpdate;


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

        if (!RootTools.isAccessGiven()) {
            Toast.makeText(getBaseContext(), "Root is required", Toast.LENGTH_SHORT).show();
            finish();
        }

        //Get Versions
        getDeviceVersion();
        getServerVersionTask taskServer = new getServerVersionTask();
        taskServer.execute(new String[] { dlAddress + deviceName});

        //Set Display
        tvDeviceVersion.setText(onDeviceVersion);


        //Update code in getServerVersion

        //Download Update Button
        final DownloadTask dTask = new DownloadTask(getBaseContext());
        btUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Make sure directory exits
                File wallpaperDirectory = new File("/sdcard/OmniRomOTA/");
                wallpaperDirectory.mkdirs();
                File file = new File("/sdcard/OmniRomOTA/" + onServerVersion);
                if(file.exists() && file.length() > 104857600)
                {
                    btUpdate.setEnabled(false);
                    btUpdate.setText("Update Already Downloaded");
                    downloading = false;
                    btInstall.setEnabled(true);
                    btInstall.setText("Install Update");
                }
                else{
                    dTask.execute(dlAddress + deviceName + "/" + onServerVersion);
                }
            }
        });

        btInstall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(downloading){
                    dTask.cancel(true);
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
            CommandCapture command = new CommandCapture(0,"rm -f cache/recovery/openrecoveryscript",
                    "echo install /sdcard/OmniRomOTA/" + onServerVersion + " >> /cache/recovery/openrecoveryscript",
                    "echo wipe cache >> /cache/recovery/openrecoveryscript",
                    "echo wipe dalvik >> /cache/recovery/openrecoveryscript",
                    "reboot recovery");
            RootTools.getShell(true).add(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isUpdate(String currentVersion, String newVersion){
        if(Integer.parseInt(currentVersion.split("-")[2]) < Integer.parseInt(newVersion.split("-")[2]))
            return true;
        return false;
    }

    public void getDeviceVersion() {
        try
        {
            String[] cmds = new String[1];
            cmds[0] = "getprop";
            String result = CommandHelper.runAsRoot(cmds);
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class getServerVersionTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String url = urls[0];
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
                Log.e("OmniRom OTA", "Getting rom list failed!");
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

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;

        public DownloadTask(Context context) {
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
            btUpdate.setText(String.valueOf(progress[0]) + "%");
            pbUpdate.setVisibility(ProgressBar.VISIBLE);
            pbUpdate.setIndeterminate(false);
            pbUpdate.setMax(100);
            pbUpdate.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(context,"Update downloaded", Toast.LENGTH_SHORT).show();
                btUpdate.setEnabled(true);
                btUpdate.setText("Download Update");
                downloading = false;
                btInstall.setEnabled(true);
                btInstall.setText("Install Update");
            }
        }
    }

}
