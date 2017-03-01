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
    SocketAddress sockaddr;

    public ChatManager(Socket socket, Handler handler, SocketAddress sockaddr) {
        this.socket = socket;
        this.handler = handler;
        this.sockaddr = sockaddr;
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
                } finally {
                    try {
                        socket.connect(sockaddr);
                    } catch (IOException e) {
                        Log.e(TAG, "can't reconnect socket", e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "can't create in/out", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "can't close socket", e);
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            //out.flush();
            Log.d(TAG, "Sent Message: " + message);
        }
    }
}
