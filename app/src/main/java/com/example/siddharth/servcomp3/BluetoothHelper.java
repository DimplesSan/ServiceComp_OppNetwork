package com.example.siddharth.servcomp3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

/**
 * Created by siddharth on 4/10/16.
 */
public class BluetoothHelper implements NetworkIOHelper {


    public static final int REQUEST_ENABLE_BT = 1;  //Required if Bluetooth is being swithched on using Intent
    public static final int REQUEST_PAIRED_DEVICE = 2;  //Required if Bluetooth is being swithched on using Intent

    public static final String appLookupName= "ServComp";
    public static final UUID appUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static BluetoothHelper btHelperRef;  //Singleton reference

    private BluetoothAdapter btAdapter; //Reference to the bluetooth adapter

    public String currDevName;  //Bluetooth Name of the current device

    private String currDevAddr; //Bluetooth address of the current device

    private BluetoothServerSocket btServerSocket;



    //Constructor
    private BluetoothHelper(){

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        currDevName = btAdapter.getName();
        currDevAddr = btAdapter.getAddress();
    }

    public String getCurrDevAddr() {
        return currDevAddr;
    }

    public BluetoothServerSocket getBtServerSocket() {
        return btServerSocket;
    }

    public BluetoothAdapter getBtAdapter() {
        return btAdapter;
    }



    //Method to get the singleton reference
    public static synchronized BluetoothHelper getBTHelper(){

        if(BluetoothHelper.btHelperRef != null)
            return BluetoothHelper.btHelperRef;
        else {
            Log.i("getBTHelper","Creating new BT helper reference");
            BluetoothHelper.btHelperRef = new BluetoothHelper();
            Log.i("getBTHelper","New BT helper reference: " + BluetoothHelper.btHelperRef);
            return BluetoothHelper.btHelperRef;
        }
    }



    //Method to switch ON bluetooth
    public short enableBluetoothAndListen(){

        byte resultCode = 0;    // resultCode = 0 --> Device doesn't support the bluetooth

        if(btAdapter != null){  // Device supports bluetooth

            if(btAdapter.isEnabled()) {  //Bluetooth is already enabled
                resultCode = 1;
                Log.i("enableBluetooth", "BT is already enabled - " + resultCode);
            }
            else{   //Bluetooth isn't enabled

                btAdapter.enable(); //Directly enable bluetooth
                try {

                    Thread.sleep(3000);
                    if (btAdapter.isEnabled()) {  //Check the status of the adapter after 3 secs

                        Log.i("enableBTAndListen","Bluetooth is enabled");
                        resultCode = 1;
                    } else {
                        resultCode = -1; //Error on trying to switch on bluetooth
                        Log.e("enableBTAndListen","Bluetooth couldn't be enabled");
                    }
                }
                catch (InterruptedException e){
                    Log.e("enableBTAndListen", e.getMessage());
                }
            }
        }
        else
            Log.i("enableBluetooth", "Device does not support bluetooth");


        if( resultCode == 1){
            try {

                btServerSocket = btAdapter.listenUsingRfcommWithServiceRecord(appLookupName,
                        appUUID);
                Log.i("enableBTAndListen","BT Server socket started");

            } catch (IOException | NullPointerException e) {
                Log.e("enableBTAndListen", "Exception on trying to initialize" +
                        " bluetooth server socket " + e.getMessage());
            }
        }
        else
            Log.e("enableBluetooth","Couldn't start BT server socket due to result code = "
                  + resultCode);

        Log.i("enableBluetooth", "Returning resultCode - " + resultCode);
        return resultCode;
    }



    //Method to switch off Bluetooth
    public short disableBluetooth(){
        byte resultCode = 0;    // resultCode = 0 --> Device doesn't support the bluetooth

        if(btAdapter != null){  // Device supports bluetooth

            if(btAdapter.isEnabled()){  //Bluetooth is already enabled

                Log.i("disableBluetooth", "Closing BT server socket");
                try {
                    btServerSocket.close(); //Close the server socket
                } catch (IOException | NullPointerException e) {
                    Log.e("disableBluetooth","Exception on trying to close" +
                          " bluetooth server socket -" + e.getMessage());
                }

                try{

                    Log.i("disableBluetooth", "Switching off BT");
                    btAdapter.disable();//Disable the bluetooth adapter

                    Thread.sleep(3000);
                    if(!btAdapter.isEnabled())  //Check status of the bluetooth adapter
                        resultCode = 1; //+ve return code for successful change of state
                    else
                        resultCode = -1;    //-ve return code for unsuccessful change of state
                }
                catch (InterruptedException | NullPointerException e){
                    Log.e("disableBluetooth", e.getMessage());
                }
            }

        }
        else
            Log.i("disableBluetooth", "Device does not support bluetooth");

        Log.i("disableBluetooth", "Returning result code: " + resultCode);
        return resultCode;
    }


    //Method to write object to stream
    public ObjectOutputStream writeObjectToBTStream(BluetoothSocket btSock, Object obj){

        ObjectOutputStream objOOS = null;
        try{
            Log.i("writeObjectToBTStream","Beginning writeObj To BT stream");
            objOOS =  new ObjectOutputStream((btSock.getOutputStream()));
            objOOS.writeObject(obj);
            objOOS.flush();
            Log.i("writeObjectToBTStream", "Object sent.");
//            objOOS.close();
        }
        catch(IOException e){
            Log.e("writeObjectToBTStream",e.getMessage());
        }
        return objOOS;
    }

    //Method to read object from stream
    public Object readObjectFromBTStream(BluetoothSocket btSock) throws ClassNotFoundException {

        ObjectInputStream objOIS = null;
        Object temp = null;
        try{
            Log.i("readObjectFromBTStream","Beginning readObj from BT stream");
            objOIS =   new ObjectInputStream(btSock.getInputStream());
            temp =  objOIS.readObject();
            Log.i("readObjectFromBTStream", "Object recceived.");
//            objOIS.close();
        }
        catch(IOException e){
            Log.e("readObjectFromBTStream", e.getMessage());
        }
        return temp;
    }


    //Class that contains the run method for Server functionality
    public class BTServerSocket implements Runnable{

        ServCompHandler objServCompHandler;

        public BTServerSocket(ServCompHandler objHandler){
            this.objServCompHandler = objHandler;
        }

        @Override
        public void run() {

            Log.i("BTServerSocket_run","Starting to listen for incoming connections.");
            //while the app is still alive
            while(ServCompHandler.isAppAlive){

                try {

                    BluetoothSocket tmpSock = btServerSocket.accept();    //Accept incoming connection
                    new Thread(new HandleIncomingConnection(BluetoothHelper.getBTHelper(),
                                                            tmpSock,
                                                            objServCompHandler)).start(); //Start a parallel thread to process the incoming message


                } catch (IOException e) {

                    Log.i("BTServerSocket_run()","Error on trying to accept incoming connection: "+
                            e.getMessage());
                    break;  //Stop the server socket
                }
            }
        }

    }

}
