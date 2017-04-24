package com.solonari.igor.virtualshooter;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

public class TCPService extends Service {
    
    private static final String TAG = "TCPClient";
    final String ipNumber = "178.168.41.217";
    final int port = 57349;
    SocketAddress sockaddr;
    Socket socket;
    ObjectInputStream in;
    ObjectOutputStream out;
    final static String id = "id";
    final static String ship = "ship";
    final static String missileArray = "missileArray";
    final static String points = "points";
    final static String exp = "exp";
    private Handler handler;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private Handler sHandler;
    ChatManager cm;
    
    public TCPService() {
    }
    
    @Override
    public void onCreate() {
        cm = new ChatManager();
        cm.start();
        serviceHandler();
    }
    
    public class LocalBinder extends Binder {
        TCPService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TCPService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    private class ChatManager extends Thread {

        @Override
        public void run() {
            
            try {
                // Creating InetAddress object from ipNumber passed via constructor from IpGetter class.
                InetAddress serverAddress = InetAddress.getByName(ipNumber);
                sockaddr = new InetSocketAddress(serverAddress, port);
                Log.d(TAG, "Connecting...");

            } catch (Exception e) {
                Log.d(TAG, "Error on socket", e);
            }

            boolean disconnected;
            do {
                try {    
                    //Here the socket is created
                    socket = new Socket();
                    socket.connect(sockaddr);
                    disconnected = false;
                    Log.d(TAG, "Connected");

                } catch (Exception e) {
                    disconnected = true;
                    Log.d(TAG, "Error on socket", e);
                    try {
                        cm.sleep(2000);
                    } catch (InterruptedException er) {
                        er.printStackTrace();
                    }
                }
            } while (disconnected);
            
            try {
                
                // Create PrintWriter object for sending messages to server.
                out = new ObjectOutputStream(socket.getOutputStream());
                //Create BufferedReader object for receiving messages from server.
                in = new ObjectInputStream(socket.getInputStream());
                Log.d(TAG, "In/Out created");

                while (true) {
                        ArrayList<String> line = (ArrayList) in.readObject();
                        String head = line.get(0);
                        switch (head) {
                            case points:
                                //update points
                                handler.obtainMessage(2, line.get(1)).sendToTarget();
                                break;
                            case id:
                                //update ID
                                SharedPreferences settings = getSharedPreferences("Pref_file", 0);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("ID", line.get(1));
                                editor.apply();
                                Log.d(TAG, "Set ID: " + line.get(1));
                                break;
                            case ship:
                                handler.obtainMessage(3, line).sendToTarget();
                                Log.d(TAG, "Received Ships:" + line);
                                break;
                            case missileArray:
                                handler.obtainMessage(5, line).sendToTarget();
                                //Log.d(TAG, "Received Missles:" + line);
                                break;
                            case exp:
                                handler.obtainMessage(6, line).sendToTarget();
                                //Log.d(TAG, "Received exp:" + line);
                                break;
                            default:
                                break;
                        }

                }

            } catch (Exception e) {
                Log.e(TAG, "can't create in/out", e);
            } finally {
                try {
                    in.close();
                    out.close();
                    socket.close();
                    //TODO: make a delay for reconnection
                    sHandler.obtainMessage(2, null).sendToTarget();
                } catch (Exception e) {
                    Log.e(TAG, "can't close socket", e);
                }
            }
        }
    }
    
    public void sendMessage(ArrayList message) {
        sHandler.obtainMessage(1, message).sendToTarget();
    }
    
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void serviceHandler() {
        sHandler = new Handler(){
            @Override
            public void handleMessage(Message inMessage) {
                switch(inMessage.what) {
                    case 1:
                        ArrayList<String> mess = (ArrayList) inMessage.obj;
                        try {
                            out.writeObject(mess);
                        } catch (Exception e) {
                            Log.d(TAG, "Cant send message", e);
                        }
                        break;
                    case 2:
                        cm = new ChatManager();
                        cm.start();
                        break;
                    default:
                        break;
                }
            }
        };
    }

}
