package com.solonari.igor.virtualshooter;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

public class TCPService extends Service {
    
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private static final String TAG = "TCPClient";
    final String ipNumber = "178.168.41.217";
    final int port = 57349;
    Messenger outMessenger;
    Message msg;
    SocketAddress sockaddr;
    Socket socket;
    ObjectInputStream in;
    ObjectOutputStream out;
    final String id = "id";
    final String ship = "ship";
    
    public TCPService() {
    }
    
    @Override
    public void onCreate() {
        new Thread(new ChatManager()).start();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
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
                            case id:
                                msg = Message.obtain(null, 2, line.get(1));
                                outMessenger.send(msg);
                                break;
                            case ship:
                                msg = Message.obtain(null, 3, line);
                                outMessenger.send(msg);
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
                    msg = Message.obtain(null, 4, null);
                    //TODO: make a delay for reconnection
                    outMessenger.send(msg);
                } catch (Exception e) {
                    Log.e(TAG, "can't close socket", e);
                }
            }
        }
    }
    
    //Handler of incoming messages from clients.
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    outMessenger = (Messenger) message.obj;
                    Log.d(TAG, "set messenger");
                    break;
                case 2:
                    try {
                        out.writeObject((message.getData()).getStringArrayList("ship"));
                    } catch (Exception e){
                        Log.d(TAG, "Cant send message", e);
                    }
                    break;
                default:
                    super.handleMessage(message);
            }
        }
    }

}
