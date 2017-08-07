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
    final static String id = "id";
    final static String ship = "ship";
    final static String missileArray = "missileArray";
    final static String points = "points";
    final static String exp = "exp";
    final static String hit = "hit";
    private Handler handler;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    ChatManager cm;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    
    public TCPService() {
    }
    
    @Override
    public void onCreate() {
        cm = new ChatManager();
        cm.start();
        HandlerThread thread = new HandlerThread("ServiceThread");
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
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
                    Log.d(TAG, "Socket connected");
                    // Create PrintWriter object for sending messages to server.
                    out = new ObjectOutputStream(socket.getOutputStream());
                    //Create BufferedReader object for receiving messages from server.
                    in = new ObjectInputStream(socket.getInputStream());
                    Log.d(TAG, "In/Out created");
                } catch (Exception e) {
                    disconnected = true;
                    Log.d(TAG, "Error on socket", e);
                    try {
                        sleep(2000);
                    } catch (InterruptedException er) {
                        er.printStackTrace();
                    }
                }
            } while (disconnected);

            try {

                handler.obtainMessage(1, null).sendToTarget();

                while (true) {
                        ArrayList<String> line = (ArrayList) in.readObject();
                        String head = line.get(0);
                        switch (head) {
                            case points:
                                //update points
                                handler.obtainMessage(2, line).sendToTarget();
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
                            case hit:
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
                Log.e(TAG, "can't create in/out", e);
            } finally {
                try {
                    in.close();
                    out.close();
                    socket.close();
                    //TODO: make a delay for reconnection
                    Message restartMsg = mServiceHandler.obtainMessage(2, null);
                    mServiceHandler.sendMessageDelayed(restartMsg, 2000);
                } catch (Exception e) {
                    Log.e(TAG, "can't close socket", e);
                }
            }
        }
    }
    
    public void sendMessage(ArrayList message) {
        mServiceHandler.obtainMessage(1, message).sendToTarget();
    }
    
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    ArrayList<String> mess = (ArrayList) msg.obj;
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
    }
}
