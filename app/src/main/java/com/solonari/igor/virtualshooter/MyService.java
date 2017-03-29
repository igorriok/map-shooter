package com.solonari.igor.virtualshooter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MyService extends Service {
    
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private static final String TAG = "TCPClient";
    private final String ipNumber = "178.168.41.217";
    private Handler mHandler;
    private ChatManager chat;
    SocketAddress sockaddr;
    Socket socket;
    
    public MyService() {
    }
    
    @Override
    public void onCreate() {
        new Thread(new ChatManager(mHandler)).start();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mMessenger.getBinder();
    }
    
    class ChatManager extends Thread {

        private static final String TAG = "TCPClient";
        private final String ipNumber = "178.168.41.217";
        private Socket socket = null;
        private Handler handler;
        private Handler sHandler;
        private static final String TAG = "ChatHandler";
        ObjectInputStream in;
        ObjectOutputStream out;
        private final String id = "id";
        private final String ship = "ship";
        //ArrayList<String> line;
        public final int setHandler = 6;

        ChatManager(Socket socket, Handler handler) {
            this.socket = socket;
            this.handler = handler;
        }


        @Override
        public void run() {
            
            try {
                // Creating InetAddress object from ipNumber passed via constructor from IpGetter class.
                InetAddress serverAddress = InetAddress.getByName(ipNumber);
                sockaddr = new InetSocketAddress(serverAddress, 57349);
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
                handler.obtainMessage(1, this).sendToTarget();
                handler.obtainMessage(5, new Messenger(sHandler)).sendToTarget();

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
    }
    
    //Handler of incoming messages from clients.
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
