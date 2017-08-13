package com.solonari.igor.mapshooter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
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
    final static String ID = "ID";
    final static String SHIP = "SHIP";
    final static String MISSILE_ARRAY = "MISSILE_ARRAY";
    final static String POINTS = "POINTS";
    final static String EXP = "EXP";
    final static String HIT = "HIT";
    private Handler handler;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    ChatManager cm;
    Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    
    public TCPService() {}
    
    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceThread");
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }
    
    class LocalBinder extends Binder {
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
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            
            try {
                // Creating InetAddress object from ipNumber passed via constructor from IpGetter class.
                InetAddress serverAddress = InetAddress.getByName(ipNumber);
                sockaddr = new InetSocketAddress(serverAddress, port);
                Log.d(TAG, "Connecting...");

            } catch (Exception e) {
                Log.d(TAG, "Cant creat socket", e);
            }

            boolean disconnected;
            do {
                try {    
                    //Here the socket is created
                    socket = new Socket();
                    socket.connect(sockaddr);
                    disconnected = false;
                    Log.d(TAG, "Socket connected");
                    // Create PrintWriter object for sending messages to server.
                    out = new ObjectOutputStream(socket.getOutputStream());
                    //Create BufferedReader object for receiving messages from server.
                    in = new ObjectInputStream(socket.getInputStream());
                    Log.d(TAG, "In/Out created");
                } catch (Exception e) {
                    disconnected = true;
                    Log.d(TAG, "Cant create in/out", e);
                }
            } while (disconnected);

            try {
                handler.obtainMessage(1, 0).sendToTarget();
            } catch (Exception e) {
                Log.e(TAG, "can't set Handler", e);
            }

            try {
                while (true) {
                    ArrayList<String> line = (ArrayList) in.readObject();
                    String head = line.get(0);
                    switch (head) {
                        case POINTS:
                            //update POINTS
                            handler.obtainMessage(2, line).sendToTarget();
                            break;
                        case ID:
                            //update ID
                            SharedPreferences settings = getSharedPreferences("PREF_FILE", 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString("ID", line.get(1));
                            editor.apply();
                            Log.d(TAG, "Set ID: " + line.get(1));
                            break;
                        case SHIP:
                            handler.obtainMessage(3, line).sendToTarget();
                            Log.d(TAG, "Received Ships:" + line);
                            break;
                        case MISSILE_ARRAY:
                            handler.obtainMessage(5, line).sendToTarget();
                            //Log.d(TAG, "Received Missles:" + line);
                            break;
                        case EXP:
                            handler.obtainMessage(6, line).sendToTarget();
                            //Log.d(TAG, "Received EXP:" + line);
                            break;
                        case HIT:
                            Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate for 500 milliseconds
                            v.vibrate(200);
                            handler.obtainMessage(7, line.get(2)).sendToTarget();
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "can't read from socket", e);
            } finally {
                try {
                    in.close();
                    out.close();
                    socket.close();
                    //Message restartMsg = mServiceHandler.obtainMessage(2, null);
                    //mServiceHandler.sendMessageDelayed(restartMsg, 2000);
                } catch (Exception e) {
                    Log.e(TAG, "can't close socket", e);
                }
            }
        }
    }
    
    synchronized public void sendMessage(ArrayList message) {
        mServiceHandler.obtainMessage(1, message).sendToTarget();
    }
    
    public void setHandler(Handler handler) {
        this.handler = handler;
        if (cm == null) {
            cm = new ChatManager();
            cm.start();
        }
    }

    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    ArrayList<String> mess = (ArrayList) msg.obj;
                    try {
                        out.writeObject(mess);
                    } catch (Exception e) {
                        Log.d(TAG, "Cant send message", e);
                        Message restartMsg = mServiceHandler.obtainMessage(2, null);
                        mServiceHandler.sendMessageDelayed(restartMsg, 2000);
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
    }
}
