package com.solonari.igor.virtualshooter;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


public class TCPClient extends Thread{

    private static final String TAG = "TCPClient";
    private final String ipNumber = "178.168.41.217";
    private String incomingMessage;
    BufferedReader in;
    PrintWriter out;
    private boolean mRun = false;
    private Handler mHandler ;


    public TCPClient(Handler handler) {
        this.mHandler = handler;
        //run();
    }

    public void run() {
        Message msg = mHandler.obtainMessage(1, "changed text");
        mHandler.sendMessage(msg);
        mRun = true;
        try {
            // Creating InetAddress object from ipNumber passed via constructor from IpGetter class.
            InetAddress serverAddress = InetAddress.getByName(ipNumber);
            Log.d(TAG, "Connecting...");
            //Here the socket is created with hardcoded port.
            Socket socket = new Socket(serverAddress, 57349);
            try {
                // Create PrintWriter object for sending messages to server.
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                //Create BufferedReader object for receiving messages from server.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.d(TAG, "In/Out created");

                //Listen for the incoming messages while mRun = true
                while (mRun) {
                    incomingMessage = in.readLine();
                    if (incomingMessage != null) {
                        Message msg = mHandler.obtainMessage(1, incomingMessage);
                        mHandler.sendMessage(msg);
                        Log.d(TAG, "Received Message: " + incomingMessage);
                    }
                    incomingMessage = null;
                }
            } catch (Exception e) {
                Log.d(TAG, "Error on streamers", e);
            } finally {
                out.flush();
                out.close();
                in.close();
                socket.close();
                Log.d(TAG, "Socket Closed");
            }
        } catch (Exception e) {
            Log.d(TAG, "Error on socket", e);
        }
    }
    
    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
            Log.d(TAG, "Sent Message: " + message);
        }
    }

    public void stopClient() {
        Log.d(TAG, "Client stopped!");
        mRun = false;
    }

}
