package com.solonari.igor.virtualshooter;

import android.os.Handler;
import android.util.Log;
import java.net.InetAddress;
import java.net.Socket;


public class TCPClient extends Thread{

    private static final String TAG = "TCPClient";
    private final String ipNumber = "178.168.41.217";
    private Handler mHandler;
    private ChatManager chat;


    public TCPClient(Handler handler) {
        this.mHandler = handler;
    }

    public void run() {
        
        //Message msg = mHandler.obtainMessage(1, "changed text");
        //mHandler.sendMessage(msg);

        try {
            // Creating InetAddress object from ipNumber passed via constructor from IpGetter class.
            InetAddress serverAddress = InetAddress.getByName(ipNumber);
            SocketAddress sockaddr = new InetSocketAddress(serverAddress, 57349);
            Log.d(TAG, "Connecting...");
            
        } catch (Exception e) {
            Log.d(TAG, "Error on socket", e);
        }
        
        boolean disconnected = true;
        do {
            try {    
                //Here the socket is created
                Socket socket = new Socket();
                socket.connect(sockaddr);
                disconnected = false;
                Log.d(TAG, "Connected");

            } catch (Exception e) {
                disconnected = true;
                Log.d(TAG, "Error on socket", e);
            }
        } while (disconnected);
        
        chat = new ChatManager(socket, mHandler);
        new Thread(chat).start();
        
    }

}
