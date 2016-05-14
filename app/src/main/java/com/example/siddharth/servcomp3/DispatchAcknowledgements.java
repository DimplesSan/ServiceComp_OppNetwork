package com.example.siddharth.servcomp3;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

/**
 * Created by siddharth on 5/6/16.
 */
public class DispatchAcknowledgements implements Runnable {

    private ServCompHandler servCompHandler;    //Reference to the parent handler

    //Constructor
    public DispatchAcknowledgements(ServCompHandler _handler) {
        this.servCompHandler = _handler;
    }

    @Override
    public void run() {
        //Keep running till app is alive
        while (ServCompHandler.isAppAlive) {
            //Check the request queue
            if (this.servCompHandler.ackQueue.size() > 0) {


                //Try to dispatch ack to every neighbor
                for (ControlMsg c : servCompHandler.ackQueue.keySet()) {

                    //TODO- Remove casting for BluetoothHelper
                    for (BluetoothDevice btDev : ((BluetoothHelper) servCompHandler.objNIOHelper).
                            getBtAdapter().
                            getBondedDevices())
                        if(dispatchAck(btDev, c)){
                            long currTime  = System.currentTimeMillis();
                            long timeInQ = currTime - servCompHandler.ackQueue.get(c);
                        }
                }


                //Sleep after a try to forward files in ack queue
                try {
                    Thread.sleep(5000);//Sleep for 5 mins
                } catch (InterruptedException e) {
                    Log.e("ForwardMessages", e.getMessage());
                }

            } else {

                //Sleep till request are added to the queue
                try {
                    Thread.sleep(10000);//Sleep for 1 min
                } catch (InterruptedException e) {
                    Log.e("ForwardMessages", e.getMessage());
                }
            }
        }
    }

    private boolean dispatchAck(BluetoothDevice btDev, ControlMsg ackCtrlMsg) {


        BluetoothSocket btSocket = null;
        String nameOfRemoteDev = btDev.getName();
        boolean retVal = false;

        try {

            Log.i("dispatchAck", "Trying to connect to " + nameOfRemoteDev);

            //Create a socket connection
            btSocket = btDev.createRfcommSocketToServiceRecord(BluetoothHelper.appUUID);

            //Try to connect to the BT device using the socket
            btSocket.connect();
            Log.i("dispatchAck", "Conected to device: " + btDev.getName());

            ((BluetoothHelper)(servCompHandler.objNIOHelper)).writeObjectToBTStream(btSocket, ackCtrlMsg);//Send control message
            Log.i("dispatchAck", "Ack sent to " + nameOfRemoteDev);

            btSocket.close();
            retVal = true;
        } catch (IOException e) {
            Log.e("dispatchAck", "Couldn't communicate wih " + nameOfRemoteDev);}

        return retVal;
    }

}