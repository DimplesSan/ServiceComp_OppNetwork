package com.example.siddharth.servcomp3;


import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

//Maintains reference to
// 3 message hashmaps
// 1 ServiceGraph
// Helper objects(Network I/O )
public class ServCompHandler{

    public enum NetworkProtocols {BT,BTLE,WIFI}  //Networking modes Bluetooth, bluetooth lowenergy, wi-fi p2p

    public MainActivity objMainAct;    //Reference to tbe main activity
    public NetworkProtocols nwProtocol;    //Enum var holding the appropriate n/w protocol

    public static boolean isAppAlive;

    public ConcurrentHashMap<ControlMsg,String> reqQueue;  //Queue to keep track of service reqs
    public ConcurrentHashMap<ControlMsg, String> respQueue; //Queue to keep track of service responses
    public ConcurrentHashMap<ControlMsg, String> proxyQueue;//Queue to forward msgs
    public ConcurrentHashMap<ControlMsg, Long>  ackQueue;

    public ServiceGraph objServiceGraph;    //Reference of the service graph
    public NetworkIOHelper objNIOHelper;    //Reference to the generic network i/o helper


    //Constructor to initialize the Service Composition
    //handler
    public ServCompHandler(MainActivity _objMainAct) {

        //Initialize all queues
        this.reqQueue = new ConcurrentHashMap<ControlMsg, String>();
        this.respQueue = new ConcurrentHashMap<ControlMsg, String>();
        this.proxyQueue = new ConcurrentHashMap<ControlMsg, String>();
        this.ackQueue = new ConcurrentHashMap<ControlMsg, Long>();

        this.objMainAct = _objMainAct;  //Initialize the ref to main activity
        this.objServiceGraph = new ServiceGraph(this.getInitServiceList());  //Initialize the service graph object
    }



    //Method to return the initial list of services
    public ArrayList<Service> getInitServiceList(){

        //TODO - Implement code for lookup in a registry of services
        ArrayList<Service> retList = new ArrayList<Service>();

        //------------------- HardCoded Services for Testing----------------------------
////
//        String [] serviceParams5= {"Spanish text", "German text","","",
//                "txt","txt","Language translation from Spanish to German", "D1" };
//        retList.add(new Service(serviceParams5));

//        String [] serviceParams2 = {"German text", "Italian text","","",
//                "txt","txt","Language translation from German to Italian", "D2" };
//        retList.add(new Service(serviceParams2));
//
        String [] serviceParams6 = {"Italian text", "Finish text","","",
                "txt","txt","Language translation from Italian to Finish", "Nexus 5" };
        retList.add(new Service(serviceParams6));

//
//        String [] serviceParams1 = {"Finish text", "French text","","",
//                "txt","txt","Language translation from Finish to French", "D2" };
//        retList.add(new Service(serviceParams1));


        String [] serviceParams1 = {"French text", "English text","","",
                "txt","txt","Language translation from French to English", "Nexus 5" };
        retList.add(new Service(serviceParams1));



        //------------------- HardCoded Services for Testing----------------------------

        Log.i("getInitServiceList", "List of services updated.");
        return  retList;
    }



    //Method to initialize the network protocol and network io helper
    public void setNWProtocol(NetworkProtocols _nwProtocol){

        this.nwProtocol = _nwProtocol;  //Set the network protocol

        //Use protocol based on user choice and initialize the appropriate helper
        switch (this.nwProtocol){

            case BT: Log.i("setNWProtocol","Selected NW protocol " + this.nwProtocol);
                     this.objNIOHelper = BluetoothHelper.getBTHelper();  //Use bluetooth as default network protocol
                     break;

            //TODO - Add additional cases for additional n/w protocols
            default:
                    Log.e("setNWProtocol", "Unsupported protocol. Using Blutooth as default protocol");
                    this.nwProtocol = NetworkProtocols.BT;  //Set default network protocol
                    this.objNIOHelper = BluetoothHelper.getBTHelper();  //Use bluetooth as default network protocol
                    break;
        }
    }

    //Method to create ServerSocket and accept incoming connections
    public void acceptIncomingConnections() {

        //TODO - Add additional cases for additional n/w protocols
        switch (this.nwProtocol){
            case BT: Log.i("acceptIncomingConn","Selected NW protocol " + this.nwProtocol +
                            ". Starting BTServer Socket." );

                    BluetoothHelper btHelper = (BluetoothHelper)this.objNIOHelper;
                    new Thread( btHelper. new BTServerSocket(this) ).start(); //Create the BT Server Socket runnable task
                    break;
        }

    }



    //Method to create a req control msg and adds it to the request queue for the request invoker
    public void queueServiceReq(Service selectedService, String nameOfFile, int lengthOfPayLd,
                                byte numOfHops) {

        //TODO - Remove type cast for objNIOHelper and test
        //TODO - Change srcAddr to BlueTooth address from BTName
        String [] addrs = {((BluetoothHelper)objNIOHelper).currDevName,
                             selectedService.location
                          };
        ArrayList<Service> tempServiceList = new ArrayList<Service>();
        tempServiceList.add(selectedService);
        long ctrlMsgId = ControlMsgCounter.getCount();
        long currUnixTimeStamp = System.currentTimeMillis();//(new Date()).getTime();
        ControlMsg ctrlMsg = new ControlMsg(ControlMsg.CtrlMsgCodes.REQ,ctrlMsgId,
                                            addrs,lengthOfPayLd,
                                            tempServiceList,
                                            currUnixTimeStamp,
                                            numOfHops
                                           );
        Log.i("queueServiceReq", "Constructed ControlMsg: " + ctrlMsg);

        //Add the control message to the queue
        this.reqQueue.put(ctrlMsg, nameOfFile);

        Log.i("queueServiceReq", "File: " + nameOfFile + " added to the request queue with the controlMsg: ");
    }



    //Method to create composed service request
    public void  queueComposedServiceReq(ArrayList<Service> serviceList, String fileName, int
                                         payLdLen, byte numOfHops){

        //TODO - Remove type cast for objNIOHelper and test
        //TODO - Change srcAddr to BlueTooth address from BTName
        String [] addrs = {((BluetoothHelper)objNIOHelper).currDevName, //Curr device is the src device
                            serviceList.get(0).location //Location of the first device is the dest addr
                          };
        long ctrlMsgId = ControlMsgCounter.getCount();
        long currUnixTimeStamp = System.currentTimeMillis();    //Get the timestamp of the curr request
        ControlMsg ctrlMsg = new ControlMsg(ControlMsg.CtrlMsgCodes.REQ,ctrlMsgId,
                                            addrs,payLdLen,
                                            serviceList,
                                            currUnixTimeStamp,
                                            numOfHops
                                           );

        Log.i("queueComposedServiceReq", "Constructed ControlMsg: " + ctrlMsg + " composed service request");

        //Add the control message to the queue
        this.reqQueue.put(ctrlMsg, fileName);
        Log.i("queueComposedServiceReq", "File: " + fileName + " added to the request queue with the controlMsg");
    }


    //A method to return a CtrlMessage for handshake
    public ControlMsg getHandshakeCtrlMsg(String destAddr){

        String [] addrs = {((BluetoothHelper)objNIOHelper).getCurrDevAddr(), destAddr };
        long currentTimeStamp = System.currentTimeMillis();
        ControlMsg ctrlMsg = new ControlMsg(addrs, currentTimeStamp);   //Return the control message for handshaking purpose
        Log.i("queueServiceReq","ControlMsg: "+ ctrlMsg);
        return ctrlMsg;
    }




    //Method to start the handler threads
    public void startHandlerThreads(){

        //Start the handler threads
        Log.i("startHandlerThreads", "Starting handler threads.");

        new Thread(new CollectServiceGraph(this)).start();
        Log.i("startHandlerThreads", "Service graph collector started.");

        new Thread(new DispatchRequests(this)).start();
        Log.i("startHandlerThreads", "Request dispatcher started.");

        new Thread(new ForwardMessages(this)).start();
        Log.i("startHandlerThreads", "Message forwarder started.");

        new Thread(new DispatchResponses(this)).start();
        Log.i("startHandlerThreads", "Response dispatcher started.");

        new  Thread(new DispatchAcknowledgements(this)).start();
        Log.i("startHandlerThreads","Ack dispatcher started");
    }



}
