package com.solonari.igor.virtualshooter;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class ChatManager implements Runnable {

    private Socket socket = null;
    private Handler handler;
    private static final String TAG = "ChatHandler";
    ObjectInputStream in;
    ObjectOutputStream out;

    ChatManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }


    @Override
    public void run() {
        try {
            // Create PrintWriter object for sending messages to server.
            out = new ObjectOutputStream(socket.getOutputStream());
            //Create BufferedReader object for receiving messages from server.
            in = new ObjectInputStream(socket.getInputStream());
            Log.d(TAG, "In/Out created");
            handler.obtainMessage(2, this).sendToTarget();

            while (true) {
                try {
                    Object o = in.readObject();
                    Message msg = handler.obtainMessage(1, o);
                    handler.sendMessage(msg);
                    System.out.println("Read object: "+o);
                } catch (IOException e) {
                    Log.d(TAG, "Cant read message", e);
                } catch (ClassNotFoundException e) {
                    Log.d(TAG, "Cant read this kind of object", e);
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

    void sendMessage(ArrayList message) {
        try {
            out.writeObject(message);
        } catch (Exception e){
            Log.d(TAG, "Cant send message", e);
        }
    }
}
