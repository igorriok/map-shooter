package com.solonari.igor.virtualshooter;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by isolo on 1/28/2017.
 * AsyncTask class which manages connection with server app and is sending command.
 */
public class ServerTask extends AsyncTask<String, Void, String> {

    private TCPClient tcpClient;
    private Handler mHandler;
    private static final String TAG = "ServerTask";
    private String idToken;

    /**
     * ShutdownAsyncTask constructor with handler passed as argument. The UI is updated via handler.
     * In doInBackground(...) method, the handler is passed to TCPClient object.
     * @param mHandler Handler object that is retrieved from MainActivity class and passed to TCPClient
     *                 class for sending messages and updating UI.
     */
    public ServerTask(Handler mHandler){
        this.mHandler = mHandler;
    }
    
    protected void onPreExecute(){
        this.idToken = Singleton.getInstance().getString();
    }
    /**
     * Overriden method from AsyncTask class. There the TCPClient object is created.
     * @param params From MainActivity class empty string is passed.
     * @return TCPClient object for closing it in onPostExecute method.
     */
    @Override
    protected String doInBackground(String... params) {
        Log.d(TAG, "In doInBackground");
        if (tcpClient == null) {
            try{
                tcpClient = new TCPClient(mHandler, idToken, "192.168.1.154", new TCPClient.MessageCallback() {
                            @Override
                            public void callbackMessageReceiver(String message) {
                                protected msg = message;
                            }
                        });

            }catch (NullPointerException e){
                Log.d(TAG, "Caught null pointer exception");
                e.printStackTrace();
            }
            tcpClient.run();
        }
        return msg;
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
        Log.d(TAG, "In on post execute");
        //mHandler.sendEmptyMessageDelayed(map.SENT, 4000);
        String message = result;
        Rating.setText(message.substring(0,5));
    }
}
