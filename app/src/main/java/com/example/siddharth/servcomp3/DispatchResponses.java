package com.example.siddharth.servcomp3;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * Created by siddharth on 5/5/16.
 */
public class DispatchResponses implements Runnable {

    private ServCompHandler servCompHandler;    //Reference to the parent handler

    //Constructor
    public DispatchResponses(ServCompHandler _handler){
        this.servCompHandler = _handler;
    }

    @Override
    public void run() {
        //Keep running till app is alive
        while (ServCompHandler.isAppAlive) {
            //Check the request queue
            if(this.servCompHandler.respQueue.size() > 0){


                //Try to dispatch request to every neighbor
                for (ControlMsg resCtrlMsg : servCompHandler.respQueue.keySet()){

                    //TODO- Remove casting for BluetoothHelper
                    for(BluetoothDevice btDev : ((BluetoothHelper)servCompHandler.objNIOHelper).
                            getBtAdapter().
                            getBondedDevices() )
                        dispatchResponses(btDev, resCtrlMsg);
                }



                //Sleep and check for devices in the neighborhood
                try {
                    Thread.sleep(2000);//Sleep for 5 mins
                } catch (InterruptedException e) {
                    Log.e("DispatchResponses", e.getMessage());
                }

            }
            else{

                //Sleep till request are added to the queue
                try {
                    Thread.sleep(10000);//Sleep for 1 min
                } catch (InterruptedException e) {
                    Log.e("DispatchResponses", e.getMessage());
                }
            }


        }
    }


    private void dispatchResponses(BluetoothDevice btDev, ControlMsg resCtrlMsg){

        BluetoothSocket btSocket = null;
        String nameOfRemoteDev = btDev.getName();
        try{

            Log.i("dispatchResponses", "Trying to connect to " + nameOfRemoteDev);

            //Create a socket connection
            btSocket = btDev.createRfcommSocketToServiceRecord(BluetoothHelper.appUUID);

            //Try to connect to the BT device using the socket
            btSocket.connect();
            Log.i("dispatchResponses", "Conected to device: " + btDev.getName());

            BufferedOutputStream bos = new BufferedOutputStream(btSocket.getOutputStream());    //Get output stream to write payload


            String fileName;
            if( (fileName = this.servCompHandler.respQueue.get(resCtrlMsg)) != null
                    && resCtrlMsg != null){

                FileIOHelper objFileIOHelper = new FileIOHelper(fileName);
                byte tempArr[] =  new byte[(int)resCtrlMsg.sizeOfPayload];
                objFileIOHelper.readDataFromFile(tempArr);
                Log.i("dispatchResponses", "File " + fileName + " read from ext storage");

                ((BluetoothHelper)(servCompHandler.objNIOHelper)).writeObjectToBTStream(btSocket, resCtrlMsg);//Send control message
                Log.i("dispatchResponses", "Control msg with id " + resCtrlMsg.ctrlMsgId + " forwarded to " + nameOfRemoteDev);

                bos.write(tempArr); //Send the payload
                bos.flush();
                Log.i("dispatchResponses", "File " + fileName + " sent to " + nameOfRemoteDev);
            }


            btSocket.close();
        }
        catch (IOException e){
            Log.e("dispatchResponses","Couldn't communicate wih " + nameOfRemoteDev);
        }

    }

}
