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
import android.os.Messenger;
import android.os.Vibrator;
import android.util.Log;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPService extends Service {
    
    private static final String TAG = "TCPClient";
    final String ipNumber = "ec2-54-187-92-130.us-west-2.compute.amazonaws.com";
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
    private boolean connecting = false;
    private boolean connected = false;
    ExecutorService clientPool = Executors.newSingleThreadExecutor();
    Messenger mMessenger;
    
    public TCPService() {}
    
    @Override
    public void onCreate() {
        //TODO: set THREAD_PRIORITY_BACKGROUND
        HandlerThread thread = new HandlerThread("ServiceThread");
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mMessenger = new Messenger(mServiceHandler);
    }
    
    class LocalBinder extends Binder {
        TCPService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TCPService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
    private class ChatManager implements Runnable {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {

            connected = false;
            
            try {
                // Creating InetAddress object from ipNumber passed via constructor from IpGetter class.
                InetAddress serverAddress = InetAddress.getByName(ipNumber);
                sockaddr = new InetSocketAddress(serverAddress, port);
                Log.d(TAG, "Connecting...");
                //Here the socket is created
                socket = new Socket();
                socket.connect(sockaddr);
                Log.d(TAG, "Socket connected");
                // Create PrintWriter object for sending messages to server.
                out = new ObjectOutputStream(socket.getOutputStream());
                //Create BufferedReader object for receiving messages from server.
                in = new ObjectInputStream(socket.getInputStream());
                Log.d(TAG, "In/Out created");
                connected = true;
            } catch (Exception e) {
                Log.d(TAG, "Cant create in/out", e);
                connecting = false;
            }

            if (connected) {
                handler.obtainMessage(1, 0).sendToTarget();
                Log.d(TAG, "set Handler");
                connecting = false;
                Log.d(TAG, "connecting: " + connecting);
            }

            try {
                while (connected) {
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
                closeSocket();
                Message restartMsg = mServiceHandler.obtainMessage(2, null);
                mServiceHandler.sendMessageDelayed(restartMsg, 2000);
            }
        }
    }
  
    synchronized void closeSocket() {
        try {
            in.close();
        } catch (Exception e) {
            Log.e(TAG, "can't close in", e);
        }
        try {
            socket.close();
        } catch (Exception e) {
            Log.e(TAG, "can't close out", e);
        }
        try {
            socket.close();
        } catch (Exception e) {
            Log.e(TAG, "can't close socket", e);
        }
    }
    
    synchronized public void sendMessage(ArrayList message) {
        mServiceHandler.obtainMessage(1, message).sendToTarget();
        Log.d(TAG, "mServiceHandler sent");
    }
    
    void setHandler(Handler handler) {
        this.handler = handler;
        if (cm == null) {
            cm = new ChatManager();
            clientPool.execute(cm);
        }
    }

    private class ServiceHandler extends Handler {
        private ServiceHandler(Looper looper) {
            super(looper);
        }
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    ArrayList<String> mess = (ArrayList) msg.obj;
                    try {
                        Log.d(TAG, "message sent");
                        out.writeObject(mess);
                    } catch (Exception e) {
                        Log.d(TAG, "Cant send message", e);
                        connected = false;
                        Message restartMsg = mServiceHandler.obtainMessage(2, null);
                        mServiceHandler.sendMessageDelayed(restartMsg, 2000);
                        closeSocket();
                    }
                    break;
                case 2:
                    if (!connecting && socket.isClosed()) {
                        connecting = true;
                        Log.d(TAG, "connecting: " + connecting);
                        //cm = new ChatManager();
                        //cm.run();
                        clientPool.execute(cm);
                        Log.d(TAG, "Restart CM");
                    }
                    break;
                case 3:
                    handler = (Handler) msg.obj;
                    if (cm == null) {
                        cm = new ChatManager();
                        clientPool.execute(cm);
                        Log.d(TAG, "set Handler");
                    }
                    break;
                default:
                    break;
            }
        }
    }
  
    @Override
    public void onDestroy() {
        clientPool.shutdownNow();
        connected = false;
    }

}
