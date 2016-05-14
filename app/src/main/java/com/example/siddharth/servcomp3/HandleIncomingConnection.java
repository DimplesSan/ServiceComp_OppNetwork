package com.example.siddharth.servcomp3;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by siddharth on 4/25/16.
 */
public class HandleIncomingConnection implements Runnable {

    private BluetoothHelper btHelperRef;
    private BluetoothSocket btConn;
    private ServCompHandler servCompHandler;
    private ServCompHandler.NetworkProtocols nwProtocol;

    //Constuctor for inoming bluetooth connection
    public HandleIncomingConnection(BluetoothHelper _helper,
                                    BluetoothSocket btSock,
                                    ServCompHandler _servComHandler){
        this.btHelperRef = _helper;
        this.btConn = btSock;
        this.servCompHandler = _servComHandler;
        this.nwProtocol = ServCompHandler.NetworkProtocols.BT;
    }

    //TODO-Add constructors for other network protocols

    @Override
    public void run() {
        try{

            //TODO- Add cases for additional network protocols
            switch (this.nwProtocol){

                //Case for handling incoming connections on bluetooth
                case BT:
                    Log.i("HandleIncomingConn_BT", "Beginning to process incoming message");
                    //Check what the client wants
                    checkControlMsg();
                    break;
            }

            btConn.close();

        }catch (IOException e){
            Log.i("HandleIncomingConn", "Error on trying to close connection " + e.getMessage());
        }


    }


    //Method to check the control message and complete appropriate processing
    private void checkControlMsg() {

        String remoteDevName = btConn.getRemoteDevice().getName();  //Name of remote device

        Log.i("checkControlMsg", "Start of check for Ctrl Msg from: " + remoteDevName);
        try{

                //Receive control msg from client
                Object obj = btHelperRef.readObjectFromBTStream(btConn);
                if(obj!= null){

                    Log.i("checkControlMsg", "Ctrl Msg received from: " + remoteDevName);
                    ControlMsg ctrlMsgObj = (ControlMsg)obj;
                    switch (ctrlMsgObj.ctrlMsgCode){

                        case HANDSHAKE:
                            Log.i("CtrlMsgType","Handshake ctrl msg received");
                            performHandshake(); //Send the service graph to the connected device
                            break;

                        case REQ: // Invoke service
                            Log.i("CtrlMsgType", "Request to invoke service");
                            processRequests(ctrlMsgObj);
                            break;

                        case RESP: //Received a Service Response
                            Log.i("CtrlMsgType", "Response CtrlMsg received for invoked service.");
                            processResponse(ctrlMsgObj);
                            break;

                        case ACK:
                            Log.i("CtrlMsgType", "Ack CtrlMsg received for invoked service.");
                            processAck(ctrlMsgObj);
                            break;

                        default:performHandshake();
                            break;

                    }
                } else {
                    Log.e("checkControlMsg", "Read of control message failed. Null reference returned.");
                }



         }
        catch(ClassNotFoundException e) {
            Log.e("BTHandleConnThreadErr", "Exception on trying to cast the control msg object from " +
                    remoteDevName + ": " + e.getMessage());
         }
    }




    //Method to perform BT handshake
    private void performBTHandshake() {

        String remDevname = btConn.getRemoteDevice().getName();
        Log.i("performBTHandshake", "Handshaking with: " +
                remDevname);

        //Send the service graph
        btHelperRef.writeObjectToBTStream(btConn, servCompHandler.objServiceGraph);
        Log.i("performBTHandshake", "Service graph sent to " + remDevname);

        try {

            Object o = btHelperRef.readObjectFromBTStream(btConn);
            if (o != null) {

                //Receive the service graph
                ServiceGraph recServiceGraph = (ServiceGraph)o;

                //Update the current service graph
                servCompHandler.objServiceGraph.updateServiceGraph(recServiceGraph);

                //Required to display the content on the UI thread from the current background thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
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

                Log.i("performBTHandshake", "Service graphs exchanged successfully with " + remDevname);



            }
            else {
                Log.e("performBTHandshake","Null reference returned by: "+remDevname );
            }
        }
        catch(ClassNotFoundException e) {
            Log.e("performBTHandshake","Incompatible reference returned by: "+remDevname+" :"+
                    e.getMessage());
        }

    }

    //Method to perform handshaking
    private void performHandshake() {

        //TODO - Add cases for other network protocols
        switch (this.nwProtocol){

            //Case for handling incoming connections on bluetooth
            case BT:
                Log.i("performHandshake_BT", "Beginning BT handshake process.");
                performBTHandshake();   //Perform bluetooth handshake.
                break;
        }
    }


    //Method to process acknowledgement
    private void processAck(ControlMsg ctrlMsgObj) {

        //Check if this ackCtrlMsg exists in the Ack queue or not
        for(ControlMsg ackCtrlMsg : servCompHandler.ackQueue.keySet()){
            if(ctrlMsgObj.ctrlMsgId == ackCtrlMsg.ctrlMsgId){
                Log.i("processAck","Ack ctrl msg already exists in the queue. Dropping received ack ctrl msg.");
                String fileName = servCompHandler.proxyQueue.get(ctrlMsgObj);
                servCompHandler.proxyQueue.remove(ctrlMsgObj);
                servCompHandler.proxyQueue.put(ackCtrlMsg, fileName);
                return;
            }
        }

        Log.i("processAck","Ack ctrl msg doesn't exist in the queue. Adding the received ack ctrl msg.");
        long timeOrReceivedAck = System.currentTimeMillis();
        servCompHandler.ackQueue.put(ctrlMsgObj, timeOrReceivedAck);

        ArrayList<ControlMsg> ctrlMsgsToBeRemoved = new ArrayList<ControlMsg>();    //Arraylist to hold the control messages that have to be deleted from the queue

        //Remove the corresponding reqCtrlMsg
        for(ControlMsg cReq : servCompHandler.reqQueue.keySet()){

            if(cReq.ctrlMsgId == ctrlMsgObj.ctrlMsgId)
            {   ctrlMsgsToBeRemoved.add(cReq);     break;}

        }for( ControlMsg cMsgToBeRemoved  : ctrlMsgsToBeRemoved){
            servCompHandler.reqQueue.remove(cMsgToBeRemoved);
            Log.i("processAck", "Ctrl msg with id: "+cMsgToBeRemoved.ctrlMsgId + " removed from " +
                  "req queue.");
        }
        ctrlMsgsToBeRemoved.clear();


        //Remove the corresponding resCtrlMsg
        for(ControlMsg cRes : servCompHandler.respQueue.keySet()){

            if(cRes.ctrlMsgId == ctrlMsgObj.ctrlMsgId)
            {   ctrlMsgsToBeRemoved.add(cRes);     break;}

        }for( ControlMsg cMsgToBeRemoved  : ctrlMsgsToBeRemoved){
            servCompHandler.respQueue.remove(cMsgToBeRemoved);
            Log.i("processAck", "Ctrl msg with id: "+cMsgToBeRemoved.ctrlMsgId + " removed from " +
                  "resp queue.");
        }
        ctrlMsgsToBeRemoved.clear();


        //Remove multiple control messages from the proxy queue
        for(ControlMsg cProxy : servCompHandler.proxyQueue.keySet()){
            if(cProxy.ctrlMsgId == ctrlMsgObj.ctrlMsgId)
               ctrlMsgsToBeRemoved.add(cProxy);
        }
        for(ControlMsg remCtrlMsg : ctrlMsgsToBeRemoved){
            servCompHandler.proxyQueue.remove(remCtrlMsg);
            Log.i("processAck", "Ctrl msg with id: " + remCtrlMsg.ctrlMsgId + " removed from " +
                  "proxy queue.");
        }
        ctrlMsgsToBeRemoved.clear();

    }


    //Method to process responses
    private void processResponse(ControlMsg ctrlMsgObj) {


        //Get the data from the stream
        try {
            BufferedInputStream bis = new BufferedInputStream(btConn.getInputStream());

            //Get the associated data
            int buffLen = (int)ctrlMsgObj.sizeOfPayload;
            final byte buff[] = new byte[buffLen];
            bis.read(buff);

            if(btHelperRef.currDevName.equalsIgnoreCase(ctrlMsgObj.destAddr)){

                Log.i("processResponse","Dest addr reached for "+ctrlMsgObj );

                ControlMsg ctrlMsgToBeRemoved = null;
                //Extract the Control Message object from reqQueu based on recceived control message
                for(ControlMsg cReq : servCompHandler.reqQueue.keySet() ){

                    //Current reqObjects Id is same as the received response's Id
                    if(cReq.ctrlMsgId == ctrlMsgObj.ctrlMsgId){

                        Log.i("processResponse","Request located for the control message with " +
                              "the id: "+ctrlMsgObj.ctrlMsgId );

                        //Create a response message
                        String [] arrAddr = {cReq.srcAddr, cReq.destAddr};
                        long currTime = System.currentTimeMillis();
                        ControlMsg cAckMsg = new ControlMsg(ControlMsg.CtrlMsgCodes.ACK,
                                                            cReq.ctrlMsgId,
                                                            arrAddr,
                                                            (long)0,
                                                            cReq.serviceList,
                                                            currTime,
                                                            cReq.numOfHops
                                                           );

                        //Add it to the ack queue
                        servCompHandler.ackQueue.put(cAckMsg, currTime);
                        Log.i("processResponse", "Ack message added to Ack queue for the control" +
                                " message with the id: " + ctrlMsgObj.ctrlMsgId);
                        ctrlMsgToBeRemoved = cReq;
                        break;
                    }
                }if (ctrlMsgToBeRemoved!= null)  //Remove the control message from the req queue
                    servCompHandler.reqQueue.remove(ctrlMsgToBeRemoved);


                //Remove the ctrl msg from the proxy queue
                for(ControlMsg c : servCompHandler.proxyQueue.keySet()){
                    if(c.ctrlMsgId == ctrlMsgObj.ctrlMsgId) {
                        ctrlMsgToBeRemoved = c;     break;
                    }
                }if (ctrlMsgToBeRemoved!= null)  //Remove the control message from the proxy queue
                    servCompHandler.proxyQueue.remove(ctrlMsgToBeRemoved);


                //Remove the ctrl msg from the proxy queue
                for(ControlMsg c : servCompHandler.respQueue.keySet()){
                    if(c.ctrlMsgId == ctrlMsgObj.ctrlMsgId) {
                        ctrlMsgToBeRemoved = c;     break;
                    }
                }if (ctrlMsgToBeRemoved!= null) //Remove the control message from the proxy queue
                    servCompHandler.respQueue.remove(ctrlMsgToBeRemoved);





               //Required to display the content on the UI thread from the current background thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        //Display dialog box
                        AlertDialog alertDialog = new AlertDialog.Builder(servCompHandler.objMainAct).create();
                        alertDialog.setTitle("Translated Text");
                        alertDialog.setMessage(new String(buff));
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                });

            }
            else{
                Log.i("processResponse","Dest addr not reached for "+ctrlMsgObj );

                //Check if the ctrlMSg exists in the queue or not
                for(ControlMsg c : servCompHandler.proxyQueue.keySet()){
                    if(c.ctrlMsgId == ctrlMsgObj.ctrlMsgId &&
                            c.ctrlMsgCode == ctrlMsgObj.ctrlMsgCode){
                        Log.i("processResponse", "Received ctrl msg exists in the proxy queue. Replacing it");
                        String fileName = servCompHandler.proxyQueue.get(c);
                        servCompHandler.proxyQueue.remove(c);
                        servCompHandler.proxyQueue.put(ctrlMsgObj, fileName);
                        return;
                    }
                }
                Log.i("processRequests", "Received ctrl msg does not exist in the proxy queue. Adding it");


                String strFileName = ctrlMsgObj.ctrlMsgId + "_" + ctrlMsgObj.destAddr +"."+
                        ctrlMsgObj.serviceList.get(0).inputFileType;
                FileIOHelper fioHelper = new FileIOHelper(strFileName);

                //Write the data to a file
                fioHelper.writeDataToFile(buff);

                //Write to the proxy queue
                servCompHandler.proxyQueue.put(ctrlMsgObj, strFileName);
            }
        }catch (IOException e){
            Log.e("processResponse","Error on trying to process data from input stream "+ e.getMessage());
        }
    }




    //Method to process requests
    private void processRequests(ControlMsg ctrlMsgObj) {

        //Get the data from the stream
        try{

            BufferedInputStream bis = new BufferedInputStream(btConn.getInputStream());
            
           //Get the associated data
            int buffLen = (int)ctrlMsgObj.sizeOfPayload;
            byte buff[] = new byte[buffLen];
            bis.read(buff);
            
            if(btHelperRef.currDevName.equalsIgnoreCase(ctrlMsgObj.destAddr)){

                Log.i("processRequests","Dest addr reached for "+ctrlMsgObj );

                for(ControlMsg resCtrlMsg : servCompHandler.respQueue.keySet()){
                    if(resCtrlMsg.ctrlMsgId == ctrlMsgObj.ctrlMsgId){
                        Log.i("processRequests", "Received ctrl msg exists in the res queue. Dropping it");
                        return;
                    }
                }

                Log.i("processRequests", "Received ctrl msg doesn not exist in the res queue. Invoking service");
                Service s = ctrlMsgObj.serviceList.get(0);  //Get the service
                String srcLang  = s.inputDesc.split(" ")[0];    //Get the source language
                String destLang = s.outputDesc.split(" ")[0];   //Get the destination language
                TranslateText t = new TranslateText(new String(buff), srcLang,
                                                    destLang, ctrlMsgObj, servCompHandler.objMainAct);  //CtrlMsg passed to indicate remote invocation
                t.execute();//Service invocation
                Log.i("processRequests", "Invoked service: "+ s);

            }
            else{

                Log.i("processRequests","Dest addr not reached for "+ctrlMsgObj );

                //Check if the ctrlMSg exists in the queue or not
                for(ControlMsg c : servCompHandler.proxyQueue.keySet()){
                    if(c.ctrlMsgId == ctrlMsgObj.ctrlMsgId &&
                       c.ctrlMsgCode == ctrlMsgObj.ctrlMsgCode){
                        Log.i("processRequests", "Received ctrl msg exists in the proxy queue. Replacing it");
                        String fileName = servCompHandler.proxyQueue.get(c);
                        servCompHandler.proxyQueue.remove(c);
                        servCompHandler.proxyQueue.put(ctrlMsgObj, fileName);
                        return;
                    }
                }

                Log.i("processRequests", "Received ctrl msg does not exist in the proxy queue. Adding it");

                String strFileName = ctrlMsgObj.ctrlMsgId + "_" + ctrlMsgObj.destAddr +"."+
                        ctrlMsgObj.serviceList.get(0).inputFileType;
                FileIOHelper fioHelper = new FileIOHelper(strFileName);

                //Write the data to a file
                fioHelper.writeDataToFile(buff);

                //Write to the proxy queue
                servCompHandler.proxyQueue.put(ctrlMsgObj, strFileName);


            }
//            checkForOtherRequests(bis);
        }
        catch (IOException e){
            Log.e("processRequests","Error on trying to process data from input stream "+ e.getMessage());
        }

    }

    //Method to process additional requests
    private void checkForOtherRequests(BufferedInputStream bis) throws IOException{

        try{

            while(bis.available() >0){

                Log.i("checkForOtherRequests","Additional msgs in stream from device" +
                        btConn.getRemoteDevice().getName());

                //Receive control msg from client
                Object obj = btHelperRef.readObjectFromBTStream(btConn);
                ControlMsg ctrlMsgObj = (ControlMsg) obj;
                Log.i("checkForOtherRequests","Extracted Next ctrl msg from device" +
                        btConn.getRemoteDevice().getName());

                //Get the associated data
                int buffLen = (int)ctrlMsgObj.sizeOfPayload;
                byte buff[] = new byte[buffLen];
                bis.read(buff);
                Log.i("checkForOtherRequests", "File data from device" +
                        btConn.getRemoteDevice().getName());

                //Intended for the current device
                if(btHelperRef.currDevName.equalsIgnoreCase(ctrlMsgObj.destAddr)){

                    Log.i("checkForOtherRequests","Dest addr reached for "+ctrlMsgObj );

                    Service s = ctrlMsgObj.serviceList.get(0);  //Get the service
                    String srcLang  = s.inputDesc.split(" ")[0];
                    String destLang = s.outputDesc.split(" ")[0];
                    TranslateText t = new TranslateText(new String(buff), srcLang,
                            destLang, ctrlMsgObj, servCompHandler.objMainAct);  //CtrlMsg passed to indicate remote invocation
                    t.execute();//Service invocation
                    Log.i("checkForOtherRequests", "Invoked service: "+ s);

                }
                //Not intented for the current device
                else{
                    //Write to the proxy queue
                }
            }

            Log.i("checkForOtherRequests", "End of messages in the current stream.");

        }
        catch(ClassNotFoundException c){

            Log.e("checkForOtherRequests","Unrecognized object: "+ c.getMessage());
        }

    }

}
