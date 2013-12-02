package com.isaacparker.omniromota;

import java.io.DataOutputStream;
import java.io.InputStream;

/**
 * Created by Isaac on 11/30/13.
 */
public class CommandHelper {

    public static String runAsRoot(String[] cmds) throws Exception {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        InputStream is = p.getInputStream();
        String result = null;
        for (String tmpCmd : cmds) {
            os.writeBytes(tmpCmd+"\n");
            int readed = 0;
            byte[] buff = new byte[4096];
                while( is.available() <= 0) {
                    try { Thread.sleep(5000); } catch(Exception ex) {}
                }

                while( is.available() > 0) {
                    readed = is.read(buff);
                    if ( readed <= 0 ) break;
                    String seg = new String(buff,0,readed);
                    result=seg; //result is a string to show in textview
                }
        }
        os.writeBytes("exit\n");
        os.flush();
        return result;
    }
    public static void RunAsRootNoOutput(String[] cmds){
        try{
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd+"\n");
            }
            os.writeBytes("exit\n");
            os.flush();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
