package com.isaacparker.omniromota.Helpers;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeoutException;

/**
 * Created by Isaac on 12/3/13.
 */
public class RootCommands {

    public static void CommandNoOutput(String Command) throws RootDeniedException , IOException, TimeoutException, InterruptedException{
        CommandCapture command = new CommandCapture(0, Command);
        RootTools.getShell(true).add(command).wait();
    }
    public static void CommandsNoOutput(String[] Commands) throws RootDeniedException , IOException, TimeoutException, InterruptedException{
        CommandCapture command = new CommandCapture(0, Commands);
        RootTools.getShell(true).add(command).wait();
    }

    public static String CommandOutput(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        InputStream is = p.getInputStream();
        String result = null;
            os.writeBytes(cmd+"\n");
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
        os.writeBytes("exit\n");
        os.flush();
        return result;
    }
    public static String CommandsOutput(String[] cmds) throws Exception {
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
    /*
    static String result = null;
    public static String CommandOutput(String RootCommand) throws RootDeniedException , IOException, TimeoutException, InterruptedException{
        Command command = new Command(0, RootCommand)
        {
            @Override
            public void commandOutput(int i, String s) {
                result = s;
            }

            @Override
            public void commandTerminated(int i, String s) {

            }

            @Override
            public void commandCompleted(int i, int i2) {

            }

            @Override
            public void output(int id, String line)
            {
                result = line;
            }
        };
        RootTools.getShell(true).add(command).wait();
        return result;
    }
    static String result2 = null;
    public static String CommandsOutput(String[] RootCommands) throws RootDeniedException , IOException, TimeoutException, InterruptedException{
        Command command = new Command(0, RootCommands)
        {
            @Override
            public void commandOutput(int i, String s) {
                result2 = s;
            }

            @Override
            public void commandTerminated(int i, String s) {

            }

            @Override
            public void commandCompleted(int i, int i2) {

            }

            @Override
            public void output(int id, String line)
            {
                result2 = line;
            }
        };
        RootTools.getShell(true).add(command).wait();
        return result2;
    }
    */
}
