package com.example.siddharth.servcomp3;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by siddharth on 4/12/16.
 */
public class CollectServiceGraph implements Runnable {

    private ServCompHandler servCompHandler;    //Reference to the parent handler
    private static ArrayList<ServiceGraph> serviceGraphList = new ArrayList<ServiceGraph>();    //List of collected service graphs



    //Constructor
    CollectServiceGraph(ServCompHandler _handler){
        this.servCompHandler = _handler;
    }

    @Override
    public void run() {


        //Keep running till app is alive
        while (ServCompHandler.isAppAlive) {

            //TODO- Remove casting for BluetoothHelper
            for(BluetoothDevice btDev : ((BluetoothHelper)servCompHandler.objNIOHelper).
                                                                            getBtAdapter().
                                                                            getBondedDevices() ){
                Log.i("ServGraphCollector", "Polling device: "+ btDev.getName());
                handShakeWithRemoteDev(btDev);
            }

            //Check the serviceGraphList count - if >  0 then merge with current Service graph
            if(CollectServiceGraph.serviceGraphList.size() > 0){

                Log.i("CollectServiceGaph_run", "Number of service graphs collected= " +
                        CollectServiceGraph.serviceGraphList.size());

                for(ServiceGraph sg : CollectServiceGraph.serviceGraphList)
                    this.servCompHandler.objServiceGraph.updateServiceGraph(sg);

                //Update the List view.
                servCompHandler.objMainAct.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("NotifyDataSetChanged",""+servCompHandler.objServiceGraph.listOfServices.size());
                        servCompHandler.objMainAct.servicesArrAdapter.notifyDataSetChanged();
                        servCompHandler.objMainAct.composedServiceAdapter.notifyDataSetChanged();

                        //Show the list only if after collection the current list count is > 0
                        if(servCompHandler.objServiceGraph.listOfServices.size() > 0)
                            servCompHandler.objMainAct.servicesDevicesLstView.setVisibility(View.VISIBLE);

                        if(servCompHandler.objServiceGraph.composedServiceRepresentations.size() >0)
                            servCompHandler.objMainAct.composedServicesLstView.setVisibility(View.VISIBLE);

                        //Display message
                        servCompHandler.objMainAct.displayToast("Service graph updated", Toast.LENGTH_SHORT);
                    }
                });
                Log.i("CollectServiceGaph_run", "List view updated.");
            }

            try {
                Thread.sleep(600000);//Sleep for 2 mins
            } catch (InterruptedException e) {
                Log.e("ServGraphCollector",e.getMessage());
            }
        }
    }


    //Method to handshake with remote device
    private void handShakeWithRemoteDev(BluetoothDevice btDev) {

        ServiceGraph returnedServiceGraph = null;
        BluetoothSocket btSocket = null;

        try{

            Log.i("handShakeWithRemoteDev", "Trying to connect to " + btDev.getName());

            //Create a socket connection
            btSocket = btDev.createRfcommSocketToServiceRecord(BluetoothHelper.appUUID);

            //Try to connect to the BT device using the socket
            btSocket.connect();
            Log.i("handShakeWithRemoteDev", "Conected to device: " + btDev.getName());

            //Get I/O streams
            ControlMsg ctrlMsg = servCompHandler.getHandshakeCtrlMsg(btDev.getName());

            //Get the ref to the helper
            BluetoothHelper btHelper = (BluetoothHelper)servCompHandler.objNIOHelper;

            //Use the helper to Send the control message
            btHelper.writeObjectToBTStream(btSocket, ctrlMsg);
            Log.i("handShakeWithRemoteDev", "Control msg sent to device: " + btDev.getName());

            //Get the service graph that was sent back as a response
            ServiceGraph receivedServGraph = (ServiceGraph)btHelper.readObjectFromBTStream(btSocket);
            Log.i("handShakeWithRemoteDev", "Service graph received from device: " + btDev.getName());

            //Send own service graph as a response
            btHelper.writeObjectToBTStream(btSocket, servCompHandler.objServiceGraph);
            Log.i("handShakeWithRemoteDev", "Service graph sent to device: " + btDev.getName());

            //Add the service graph to the list of service graphs
            CollectServiceGraph.serviceGraphList.add(receivedServGraph);
            Log.i("handShakeWithRemoteDev", "Service graph added to list of collected service graph." +
                    " Current count= "+ CollectServiceGraph.serviceGraphList.size());

            btSocket.close();
        }

        catch (IOException e){

            Log.e("BTConnWithDev",
                    "Cannot communicate with device: " + btDev.getName() + " " + e.getMessage());
        }
        catch (ClassNotFoundException e2) {
            Log.e("BTConnWithDev",
                    "Unrecognized object received from: " + btDev.getName() + " " +e2.getMessage());
        }


    }
}
