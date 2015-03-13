package com.doepiccoding.nsd.connection;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.doepiccoding.nsd.util.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by martincazares on 3/10/15.
 */
public class NSDListen {
    private static final String TAG = "TrackingFlow";
    private NsdManager mNsdManager;
    public String mDiscoveryServiceName = "NSDDoEpicCodingListener";
    private Context mContext;
    private int mSelectedPort = -1;
    private SocketServerConnection mSocketServerConnection;
    private ServerSocket mDiscoverableServerSocket;
    private REGISTRATION_STATUS mCurrentRegistrationStatus = REGISTRATION_STATUS.NON_REGISTERED;

    private enum REGISTRATION_STATUS{
        REGISTERED,
        NON_REGISTERED
    }

    public NSDListen(Context context){
        this.mContext = context;
        this.mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        //Start a thread with the server socket ready to receive connections...
        mSocketServerConnection = new SocketServerConnection();
        mSocketServerConnection.openConnection();

    }

    /**
     * This method should be triggered after createServerThread has been executed...
     */
    public void registerDevice(){
        if(mCurrentRegistrationStatus == REGISTRATION_STATUS.REGISTERED)return;

        if(mSelectedPort > -1) {
            setupDeviceRegistration();
        } else {
            Log.d(TAG, "No Socket available..., make sure this method is called after createServerThread has been executed...");
        }
    }

    private void setupDeviceRegistration(){
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(mSelectedPort);
        serviceInfo.setServiceName(mDiscoveryServiceName);
        serviceInfo.setServiceType(Constants.SERVICE_TYPE);

        mCurrentRegistrationStatus = REGISTRATION_STATUS.REGISTERED;
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    /**
     * Registration Listener for our NDS Listen logic
     */
    private NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            mDiscoveryServiceName = NsdServiceInfo.getServiceName();
            Toast.makeText(mContext, "Registered DEVICE!", Toast.LENGTH_LONG).show();
            android.util.Log.e("TrackingFlow", "This device has been registered to be discovered through NSD...:" + mDiscoveryServiceName);
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo arg0) {
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        }

    };

    public void shutdown() {
        try {
            mNsdManager.unregisterService(mRegistrationListener);
            if(mSocketServerConnection != null){
                mSocketServerConnection.release();
            }
        }catch (Exception e){e.printStackTrace();}
    }

    /**
     * This class has the functionality required to start
     * and use the ServerSocket...
     */
    private class SocketServerConnection {
        private boolean mIsReady;
        private DataOutputStream mSocketOutput;
        private DataInputStream mSocketInput;

        public SocketServerConnection(){
            try{
                mDiscoverableServerSocket = new ServerSocket(0);
                mSelectedPort = mDiscoverableServerSocket.getLocalPort();
            } catch (IOException e) {e.printStackTrace();}
        }

        /**
         * Start a Server Socket and get it ready
         * to wait for a connection...
         */
        public void openConnection(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //Assign the socket that will be used for communication and let the thread die...
                        Log.e("TrackingFlow", "Waiting for connection...");
                        Socket socket = mDiscoverableServerSocket.accept();
                        Log.e("TrackingFlow", "Connection found...");
                        mIsReady = true;
                        mSocketOutput = new DataOutputStream(socket.getOutputStream());
                        mSocketInput = new DataInputStream(socket.getInputStream());

                        listenForMessages();

                        //At this point you can start using the socket
                        //get outputStream and inputStream
                    } catch (IOException e) {
                        Log.e(TAG, "Error creating ServerSocket: ", e);
                        e.printStackTrace();
                    }finally {
                        if(mSocketInput != null) {
                            try{mSocketInput.close();}catch (Exception e){e.printStackTrace();}
                        }
                        if(mSocketOutput != null){
                            try {mSocketOutput.close();}catch (Exception e){e.printStackTrace();}
                        }
                    }
                    //Reopen the connection to wait for another message...
                    openConnection();
                }
            }).start();
        }

        public void listenForMessages() {
            if (!mIsReady || mSocketInput == null) return;
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            StringBuilder sb = new StringBuilder();
            int length = Integer.MAX_VALUE;

            try {
                while (length >= bufferSize) {
                    length = mSocketInput.read(buffer);
                    sb.append(new String(buffer, 0, length));
                }
                final String receivedMessage = sb.toString();
                mSocketOutput.write(("Echo: " + receivedMessage).getBytes());
                mSocketOutput.flush();
                //TODO:Send message on the main thread, Note: We don't need to create a handler every time, this is just for prototyping...
                new Handler(mContext.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "Message received: " + receivedMessage, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {e.printStackTrace();}
        }

        public void release(){
            if(mSocketOutput != null){
                try {
                    mSocketOutput.close();
                } catch (IOException e) {e.printStackTrace();}
            }
            if(mSocketInput != null){
                try {
                    mSocketInput.close();
                } catch (IOException e) {e.printStackTrace();}
            }
        }
    }

}
