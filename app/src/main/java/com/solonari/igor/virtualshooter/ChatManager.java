package com.solonari.igor.virtualshooter;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;



public class ChatManager implements Runnable {

    private Socket socket = null;
    private Handler handler;
    private static final String TAG = "ChatHandler";
    private String incomingMessage;
    BufferedReader in;
    PrintWriter out;

    public ChatManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }


    @Override
    public void run() {
        try {
            // Create PrintWriter object for sending messages to server.
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            //Create BufferedReader object for receiving messages from server.
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Log.d(TAG, "In/Out created");
            handler.obtainMessage(2, this).sendToTarget();

            while (true) {
                try {
                    if ((incomingMessage = in.readLine()) != null) {

                        Message msg = handler.obtainMessage(1, incomingMessage);
                        handler.sendMessage(msg);
                        Log.d(TAG, "Received Message: " + incomingMessage);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
            Log.d(TAG, "Sent Message: " + message);
        }
    }
}
