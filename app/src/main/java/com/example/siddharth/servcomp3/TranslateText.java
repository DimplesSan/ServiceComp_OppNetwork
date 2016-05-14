package com.example.siddharth.servcomp3;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;

/**
 * Created by siddharth on 4/24/16.
 */
public class TranslateText extends AsyncTask<String, Void, JSONObject> {

    private static final String webAPIURL = "https://translate.yandex.net/api/v1.5/tr.json/translate?";
    private static final String authKey = "trnsl.1.1.20160404T132525Z.714b6656b3f2cd21.4f7146db818fe7d3687d5606d232c9f202b20047";
    public  static final LinkedHashMap<String, String> languageCode;
    static{
        languageCode = new LinkedHashMap<String, String>();
        languageCode.put("Albanian","sq");
        languageCode.put("English","en");
        languageCode.put("Arabic","ar");
        languageCode.put("Armenian","hy");
        languageCode.put("Hungarian","hu");
        languageCode.put("Dutch", "nl");
        languageCode.put("Greek", "el");
        languageCode.put("Danish", "da");
        languageCode.put("Indonesian", "id");
        languageCode.put("Irish", "ga");
        languageCode.put("Italian", "it");
        languageCode.put("Spanish", "es");
        languageCode.put("Chinese", "zh");
        languageCode.put("Korean", "ko");
        languageCode.put("Latin", "la");
        languageCode.put("Malay", "ms");
        languageCode.put("German", "de");
        languageCode.put("Norwegian", "no");
        languageCode.put("Persian", "fa");
        languageCode.put("Polish", "pl");
        languageCode.put("Portuguese", "pt");
        languageCode.put("Russian", "ru");
        languageCode.put("Thai", "th");
        languageCode.put("Turkish", "tr");
        languageCode.put("Ukrainian", "uk");
        languageCode.put("Finish", "fi");
        languageCode.put("French", "fr");
        languageCode.put("French", "fr");
        languageCode.put("Croatian", "hr");
        languageCode.put("Czech", "cs");
        languageCode.put("Swedish", "sv");
        languageCode.put("Japanese", "ja");
    }

    public enum InvocationCode {Loc_Ind, Rem_Ind, Loc_Comp, Rem_Comp};

    String iptext, ipLang, opLang;
    MainActivity objMainAct;
    ControlMsg remoteCtrlMsg;
    private InvocationCode invocCode;
    //Local Individual invocation Constructor
    public TranslateText(String _ip, String ipLang, String opLang,
                         MainActivity objMainAct){
        iptext = _ip;
        this.objMainAct = objMainAct;
        this.ipLang = languageCode.get(ipLang);
        this.opLang = languageCode.get(opLang);
        this.invocCode = InvocationCode.Loc_Ind;
    }

    //Remote individual invocation Constructor
    public TranslateText(String _ip, String ipLang, String opLang,  ControlMsg ctrlMsg,
                         MainActivity objMainAct){
        iptext = _ip;
        this.objMainAct = objMainAct;
        this.ipLang = languageCode.get(ipLang);
        this.opLang = languageCode.get(opLang);
        this.remoteCtrlMsg = ctrlMsg;
        this.invocCode = InvocationCode.Rem_Ind;
    }


    //Constructor for Local composed invocation
    public TranslateText(String inString, ControlMsg ctrlMsg, MainActivity objMainAct){

        this.iptext = inString;
        this.objMainAct = objMainAct;
        //Get the first service in the list
        Service tempService = ctrlMsg.serviceList.get(0);   //Get the top most service that has to be executed
        this.ipLang = tempService.inputDesc.split(" ")[0];
        this.opLang = tempService.outputDesc.split(" ")[0];
        this.remoteCtrlMsg = ctrlMsg;
        this.invocCode = InvocationCode.Loc_Comp;
    }


    @Override
    protected JSONObject doInBackground(String... params)
    {
        StringBuilder urlString = new StringBuilder();
        urlString.append(webAPIURL);
        urlString.append("key=").append(authKey);
        urlString.append("&text=").append(iptext);
        urlString.append("&lang=").append(ipLang +"-" + opLang);
        urlString.append("&format=").append("plain");
        urlString.append("&options=").append("1");

        Log.i("URLString", urlString.toString());

        HttpURLConnection urlConnection = null;
        URL url = null;
        JSONObject object = null;

        try
        {
            url = new URL(urlString.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.connect();
            InputStream inStream = null;
            inStream = urlConnection.getInputStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));
            String temp, response = "";
            while ((temp = bReader.readLine()) != null)
                response += temp;
            bReader.close();
            inStream.close();
            urlConnection.disconnect();
            object = (JSONObject) new JSONTokener(response).nextValue();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return (object);
    }

    @Override
    protected void onPostExecute(JSONObject result)
    {
        try{
            if(result!=null){

                String opText = result.getString("text");
                Log.i("Result", opText);

                //Check the origin of request
                if(invocCode == InvocationCode.Rem_Ind || invocCode == InvocationCode.Loc_Comp ||
                   invocCode == InvocationCode.Rem_Comp){


                    //Write to file
                    byte buff[] = opText.getBytes();
                    String fileName = this.ipLang+"_"+this.opLang+"_op"+".txt";
                    FileIOHelper fioHelper = new FileIOHelper(fileName);
                    fioHelper.writeDataToFile(buff);
                    Log.i("XlateTxt_postExe","Translation output written out to file: " + fileName);

                    //Remove the top level service from the list
                    Service lastService = this.remoteCtrlMsg.serviceList.remove(0);

                    //if Last service was invoked -- Return reponse
                    if(remoteCtrlMsg.serviceList.size() == 0 ){

                        Log.i("XlateTxt_postExe", "Final request from a remote device serviced.");

                        //Final service is located at the request originating device
                        if(this.remoteCtrlMsg.srcAddr.equalsIgnoreCase(this.remoteCtrlMsg.destAddr)){

                            //Directly display the output
                            Log.i("XlateTxt_postExe","Final service is same as the source device.");

                            //Display dialog box
                            AlertDialog alertDialog = new AlertDialog.Builder(objMainAct).create();
                            alertDialog.setTitle("Translated Text");
                            alertDialog.setMessage(opText);
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();

                            //Write the acknowledgement

                            ControlMsg ctrlMsgToBeRemoved = null;
                            //Extract the Control Message object from reqQueu based on recceived control message
                            for(ControlMsg cReq : objMainAct.objTaskHandler.reqQueue.keySet() ){

                                //Current reqObjects Id is same as the received response's Id
                                if(cReq.ctrlMsgId == remoteCtrlMsg.ctrlMsgId){

                                    Log.i("processResponse","Request located for the control message with " +
                                            "the id: "+cReq.ctrlMsgId );

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
                                    objMainAct.objTaskHandler.ackQueue.put(cAckMsg, currTime);
                                    Log.i("processResponse", "Ack message added to Ack queue for the control" +
                                            " message with the id: " + cReq.ctrlMsgId);
                                    ctrlMsgToBeRemoved = cReq;
                                    break;
                                }
                            }if (ctrlMsgToBeRemoved!= null)  //Remove the control message from the req queue
                                objMainAct.objTaskHandler.reqQueue.remove(ctrlMsgToBeRemoved);


                            //Remove the ctrl msg from the proxy queue
                            for(ControlMsg c : objMainAct.objTaskHandler.proxyQueue.keySet()){
                                if(c.ctrlMsgId == remoteCtrlMsg.ctrlMsgId) {
                                    ctrlMsgToBeRemoved = c;     break;
                                }
                            }if (ctrlMsgToBeRemoved!= null)  //Remove the control message from the proxy queue
                                objMainAct.objTaskHandler.proxyQueue.remove(ctrlMsgToBeRemoved);


                            //Remove the ctrl msg from the proxy queue
                            for(ControlMsg c : objMainAct.objTaskHandler.respQueue.keySet()){
                                if(c.ctrlMsgId == remoteCtrlMsg.ctrlMsgId) {
                                    ctrlMsgToBeRemoved = c;     break;
                                }
                            }if (ctrlMsgToBeRemoved!= null) //Remove the control message from the proxy queue
                                objMainAct.objTaskHandler.respQueue.remove(ctrlMsgToBeRemoved);


                        }else{

                            //Create response control message
                            String addrs[] ={this.remoteCtrlMsg.destAddr, this.remoteCtrlMsg.srcAddr};
                            this.remoteCtrlMsg.serviceList.add(lastService);    //Add the service back to the list
                            //as file type is required for writing the files to ext storage
                            ControlMsg respCtrlMsg = new ControlMsg(ControlMsg.CtrlMsgCodes.RESP,
                                    this.remoteCtrlMsg.ctrlMsgId,   //Extract the control message id
                                    addrs, //Reverse the addresses
                                    (long)buff.length,
                                    this.remoteCtrlMsg.serviceList, //Return the current state of service list
                                    this.remoteCtrlMsg.reqTimeStamp,    //Timestamp of the originating request
                                    (byte)-1    //Lifetime of message
                            );
                            Log.i("XlateTxt_postExe", "Ctrl msg created for file: " + fileName +
                                    " ctrlMsg: "+respCtrlMsg );

                            //Queue up the response
                            this.objMainAct.objTaskHandler.respQueue.put(respCtrlMsg, fileName);

                            Log.i("XlateTxt_postExe", "Response queued");
                            objMainAct.displayToast("Result queued for response", Toast.LENGTH_SHORT);
                        }

                    }

                    //Since last service was not invoked -- Return Request
                    else {

                        if(this.remoteCtrlMsg.serviceList.get(0).location.equalsIgnoreCase(
                                ((BluetoothHelper)objMainAct.objTaskHandler.objNIOHelper ).currDevName) )
                        {
                            Log.i("XlateTxt_postExe", "Non final request from a remote device serviced.");
                            Log.i("XlateTxt_postExe", "Next service is also from  the same device. Invoking the next service.");

                            Service s = this.remoteCtrlMsg.serviceList.get(0);  //Get the service
                            String srcLang  = s.inputDesc.split(" ")[0];    //Get the source language
                            String destLang = s.outputDesc.split(" ")[0];   //Get the destination language
                            TranslateText t = new TranslateText(new String(buff), srcLang,
                                    destLang, this.remoteCtrlMsg, objMainAct);  //CtrlMsg passed to indicate remote invocation
                            t.execute();//Service invocation

                        }
                        else{
                            Log.i("XlateTxt_postExe", "Non final request from a remote device serviced.");

                            //Create response control message
                            Service nextService = this.remoteCtrlMsg.serviceList.get(0);
                            String nextDest = nextService.location;
                            String addrs[] ={this.remoteCtrlMsg.srcAddr, nextDest}; //Src addr will remain the same as the current ctrlMsg
                            //Dest is the next device in the list of services

                            ControlMsg reqCtrlMsg = new ControlMsg(ControlMsg.CtrlMsgCodes.REQ,    //Create a Req msg
                                    this.remoteCtrlMsg.ctrlMsgId,   //Extract the control message id
                                    addrs, //Reverse the addresses
                                    (long)buff.length,
                                    this.remoteCtrlMsg.serviceList, //Return the current state of service list
                                    this.remoteCtrlMsg.reqTimeStamp,    //Timestamp of the originating request
                                    this.remoteCtrlMsg.numOfHops    //Lifetime of message
                            );

                            Log.i("XlateTxt_postExe", "Ctrl msg created for file: " + fileName +
                                    " ctrlMsg: "+reqCtrlMsg );

                            //Queue up the response
                            this.objMainAct.objTaskHandler.proxyQueue.put(reqCtrlMsg, fileName);

                            Log.i("XlateTxt_postExe", "Req queued after intermediate service");
                            objMainAct.displayToast("Req queued after intermediate service",
                                    Toast.LENGTH_SHORT);
                        }
;
                    }

                }

                //Local individual invocation -- Display response
                else{

                    Log.i("XlateTxt_postExe","Request is from the current device.");

                    //Display dialog box
                    AlertDialog alertDialog = new AlertDialog.Builder(objMainAct).create();
                    alertDialog.setTitle("Translated Text");
                    alertDialog.setMessage(opText);
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }
            else{
                Log.e("translate_PostExe", "Null reference returned.");
            }

//            super.onPostExecute(result);
        }
        catch(JSONException e){
            e.printStackTrace();
        }



    }
}
