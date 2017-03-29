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
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mMessenger.getBinder();
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
