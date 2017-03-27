package com.solonari.igor.virtualshooter;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;


public class ChatManager implements Runnable {

    private Socket socket = null;
    private Handler handler;
    private Handler sHandler;
    private static final String TAG = "ChatHandler";
    ObjectInputStream in;
    ObjectOutputStream out;
    private final String id = "id";
    private final String ship = "ship";
    //ArrayList<String> line;
    private final String setHandler = "setHandler";

    ChatManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }


    @Override
    public void run() {
        sHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case setHandler:
                            handler = (Handler) msg.obj;
                            break;
                        default:
                            super.handleMessage(msg);
                    }
                }
            }
        try {
            // Create PrintWriter object for sending messages to server.
            out = new ObjectOutputStream(socket.getOutputStream());
            //Create BufferedReader object for receiving messages from server.
            in = new ObjectInputStream(socket.getInputStream());
            Log.d(TAG, "In/Out created");
            handler.obtainMessage(1, this).sendToTarget();
            handler.obtainMessage(5, new Messanger(sHandler));

            while (true) {
                    ArrayList<String> line = (ArrayList) in.readObject();
                    String head = line.get(0);
                    switch (head) {
                        case id:
                            String points = line.get(1);
                            handler.obtainMessage(2, points).sendToTarget();
                            break;
                        case ship:
                            handler.obtainMessage(3, line).sendToTarget();
                            Log.d(TAG, "Received Ships:" + line);
                            break;
                        default:
                            break;
                    }

            }

        } catch (Exception e) {
            Log.e(TAG, "can't create in/out", e);
        } finally {
            try {
                socket.close();
                Message reconect = handler.obtainMessage(4, "reconnect");
                handler.sendMessageDelayed(reconect, 5000);
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
