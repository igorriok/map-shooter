package com.solonari.igor.virtualshooter;

import android.os.Handler;
import android.os.Looper;
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
    private final String ipNumber = "192.168.0.100";
    private Handler mHandler;
    private ChatManager chat;


    public TCPClient(Handler handler) {
        this.mHandler = handler;
        //run();
    }

    public void run() {
        
        //Message msg = mHandler.obtainMessage(1, "changed text");
        //mHandler.sendMessage(msg);

        try {
            // Creating InetAddress object from ipNumber passed via constructor from IpGetter class.
            InetAddress serverAddress = InetAddress.getByName(ipNumber);
            Log.d(TAG, "Connecting...");
            //Here the socket is created with hardcoded port.
            Socket socket = new Socket(serverAddress, 57349);
            Log.d(TAG, "Connected");

            chat = new ChatManager(socket, mHandler);
            new Thread(chat).start();

        } catch (Exception e) {
            Log.d(TAG, "Error on socket", e);
        }
        
    }

}
