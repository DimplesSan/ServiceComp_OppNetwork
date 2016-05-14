package com.example.siddharth.servcomp3;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * Created by siddharth on 5/4/16.
 */
public class ForwardMessages implements Runnable {


    private ServCompHandler servCompHandler;    //Reference to the parent handler

    //Constructor
    public ForwardMessages(ServCompHandler _handler){
        this.servCompHandler = _handler;
    }


    @Override
    public void run() {

        //Keep running till app is alive
        while (ServCompHandler.isAppAlive) {
            //Check the request queue
            if(this.servCompHandler.proxyQueue.size() > 0){


                //Try to dispatch request to every neighbor
                for(ControlMsg fwdCtrlMsg : servCompHandler.proxyQueue.keySet()){

                    //TODO- Remove casting for BluetoothHelper
                    for(BluetoothDevice btDev : ((BluetoothHelper)servCompHandler.objNIOHelper).
                            getBtAdapter().
                            getBondedDevices() )
                        forwardMsgs(btDev, fwdCtrlMsg);

                }


                //Sleep after a try to forward files in proxy queue
                try {
                    Thread.sleep(5000);//Sleep for 5 mins
                } catch (InterruptedException e) {
                    Log.e("ForwardMessages", e.getMessage());
                }

            }
            else{

                //Sleep till request are added to the queue
                try {
                    Thread.sleep(10000);//Sleep for 1 min
                } catch (InterruptedException e) {
                    Log.e("ForwardMessages", e.getMessage());
                }
            }


        }
    }


    private void forwardMsgs(BluetoothDevice btDev, ControlMsg fwdCtrlMsg){

        BluetoothSocket btSocket = null;
        String nameOfRemoteDev = btDev.getName();
        try{

            Log.i("forwardMsgs", "Trying to connect to " + nameOfRemoteDev);

            //Create a socket connection
            btSocket = btDev.createRfcommSocketToServiceRecord(BluetoothHelper.appUUID);

            //Try to connect to the BT device using the socket
            btSocket.connect();
            Log.i("forwardMsgs", "Conected to device: " + btDev.getName());

            BufferedOutputStream bos = new BufferedOutputStream(btSocket.getOutputStream());    //Get output stream to write payload

            String fileName;
            if( (fileName = this.servCompHandler.proxyQueue.get(fwdCtrlMsg)) != null
                    && fwdCtrlMsg != null){

                FileIOHelper objFileIOHelper = new FileIOHelper(fileName);
                byte tempArr[] =  new byte[(int)fwdCtrlMsg.sizeOfPayload];
                objFileIOHelper.readDataFromFile(tempArr);
                Log.i("forwardMsgs", "File " + fileName + " read from ext storage");

                ((BluetoothHelper)(servCompHandler.objNIOHelper)).writeObjectToBTStream(btSocket, fwdCtrlMsg);//Send control message
                Log.i("forwardMsgs", "Control msg with id " + fwdCtrlMsg.ctrlMsgId + " forwarded to " + nameOfRemoteDev);

                bos.write(tempArr); //Send the payload
                bos.flush();
                Log.i("forwardMsgs", "File " + fileName + " sent to " + nameOfRemoteDev);
            }


            btSocket.close();
        }
        catch (IOException e){
            Log.e("forwardMsgs","Couldn't communicate wih " + nameOfRemoteDev);
        }

    }

}
