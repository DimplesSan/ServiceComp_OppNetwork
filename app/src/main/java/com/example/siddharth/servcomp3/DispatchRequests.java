package com.example.siddharth.servcomp3;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * Created by siddharth on 5/3/16.
 */
public class DispatchRequests implements Runnable {

    private ServCompHandler servCompHandler;    //Reference to the parent handler

    //Constructor
    public DispatchRequests(ServCompHandler _handler){
        this.servCompHandler = _handler;
    }


    @Override
    public void run() {


        //Keep running till app is alive
        while (ServCompHandler.isAppAlive) {

            //Check the request queue
            if(this.servCompHandler.reqQueue.size() > 0){

                //Try to dispatch request to every neighbor
                for(ControlMsg reqCtrlMsg : servCompHandler.reqQueue.keySet()){

                    //TODO- Remove casting for BluetoothHelper
                    for(BluetoothDevice btDev : ((BluetoothHelper)servCompHandler.objNIOHelper).
                            getBtAdapter().
                            getBondedDevices() )
                        dispatchRequestToBtDevice(btDev, reqCtrlMsg);
                }


                //Sleep and check for devices in the neighborhood
                try {
                    Thread.sleep(3000);//Sleep for 5 mins
                } catch (InterruptedException e) {
                    Log.e("DispatchRequests", e.getMessage());
                }
            }
            else{

                //Sleep till request are added to the queue
                try {
                    Thread.sleep(10000);//Sleep for 1 min
                } catch (InterruptedException e) {
                    Log.e("DispatchRequests", e.getMessage());
                }
            }

        }


    }


    private void dispatchRequestToBtDevice(BluetoothDevice btDev, ControlMsg reqCtrlMsg){

        BluetoothSocket btSocket = null;
        String nameOfRemoteDev = btDev.getName();
        try{

            Log.i("dispatchReqToBtDevice", "Trying to connect to " + nameOfRemoteDev);

            //Create a socket connection
            btSocket = btDev.createRfcommSocketToServiceRecord(BluetoothHelper.appUUID);

            //Try to connect to the BT device using the socket
            btSocket.connect();
            Log.i("dispatchReqToBtDevice", "Conected to device: " + btDev.getName());

            BufferedOutputStream bos = new BufferedOutputStream(btSocket.getOutputStream());    //Get output stream to write payload

            String fileName;
            if( (fileName = this.servCompHandler.reqQueue.get(reqCtrlMsg)) != null
                 && reqCtrlMsg != null){

                FileIOHelper objFileIOHelper = new FileIOHelper(fileName);
                byte tempArr[] =  new byte[(int)reqCtrlMsg.sizeOfPayload];
                objFileIOHelper.readDataFromFile(tempArr);
                Log.i("dispatchReqToBtDevice", "File " + fileName + " read from ext storage");

                ((BluetoothHelper)(servCompHandler.objNIOHelper)).writeObjectToBTStream(btSocket, reqCtrlMsg);//Send control message
                Log.i("dispatchReqToBtDevice",  "Control msg with id " + reqCtrlMsg.ctrlMsgId + " forwarded to " + nameOfRemoteDev);

                bos.write(tempArr); //Send the payload
                bos.flush();
                Log.i("dispatchReqToBtDevice", "File " + fileName + " sent to " + nameOfRemoteDev);
            }



            btSocket.close();
        }
        catch (IOException e){
            Log.e("dispatchReqToBtDevice","Couldn't communicate wih " + nameOfRemoteDev);
        }

    }



}
