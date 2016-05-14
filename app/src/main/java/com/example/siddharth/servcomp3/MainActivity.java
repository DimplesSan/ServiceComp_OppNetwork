package com.example.siddharth.servcomp3;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Switch startSwitch; //Ref to swith on screen

    ArrayAdapter servicesArrAdapter, composedServiceAdapter;
    ListView servicesDevicesLstView, composedServicesLstView;

    ServCompHandler objTaskHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        objTaskHandler = new ServCompHandler(this); //Initialize the task handler


        //Set the Adapter for list view
        servicesArrAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,
                                              objTaskHandler.objServiceGraph.listOfServices );
        Log.i("MainAct_onCreate","Num of services: "+ objTaskHandler.objServiceGraph.listOfServices.size() );
        servicesDevicesLstView = (ListView)findViewById(R.id.ServicesListView);
        servicesDevicesLstView.setAdapter(servicesArrAdapter);
        servicesDevicesLstView.setOnItemClickListener(new ListItemClickListener(this));  //Set the listener for a click on an item in the list


        //Get the list of composed services
        composedServiceAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,
                                          objTaskHandler.objServiceGraph.composedServiceRepresentations);
        Log.i("MainAct_onCreate", "Num of composed services: " +
                objTaskHandler.objServiceGraph.composedServiceRepresentations.size());
        composedServicesLstView = (ListView)findViewById(R.id.ComposedServicesListView);;
        composedServicesLstView.setAdapter(composedServiceAdapter);
        composedServicesLstView.setOnItemClickListener(new ComposedListItemClickListener(this));//Set the listener for composed services


        Button btnRefreshComp = (Button)findViewById(R.id.refreshBtn);
        btnRefreshComp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ServiceGraphRefresh(objTaskHandler.objMainAct).execute();
            }
        });


        startSwitch = (Switch)findViewById(R.id.startBT_P2PSwitch);
        startSwitch.setOnCheckedChangeListener(new StartSwitchStateListener(this));
        //TODO - Insert listeners for other switches
    }



    public void displayToast(String msg, int timeOut) {

        Toast toast = Toast.makeText(this, msg, timeOut);
        toast.show();
    }


}





//Listener for Individual services
class ComposedListItemClickListener implements AdapterView.OnItemClickListener{

    //Reference to the main Activity
    static MainActivity objMainActivity;
    public ComposedListItemClickListener(MainActivity mainActRef){
        objMainActivity = mainActRef;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {

        //Based on the position extract service information and pass on to the dialog click listener

        //Show a dialog to accept user input
        final Dialog inputDialog = new Dialog(objMainActivity);
        inputDialog.setContentView(R.layout.text_input);
        inputDialog.show();

        final EditText inputParam = (EditText)inputDialog.findViewById(R.id.inputTextForService);
        Button sendInputParams = (Button)inputDialog.findViewById(R.id.sendInputParams);


        sendInputParams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String inputParamString = inputParam.getText().toString();  //Get the input
                Service selectedService = (Service) parent.getAdapter().getItem(position);//Get the service name

                //Persist input data only if service is not located on the current device
                String currDeviceName = ((BluetoothHelper) objMainActivity.
                        objTaskHandler.objNIOHelper).currDevName;

                //Get the list of services
                ArrayList <Service> seviceList = new ArrayList<Service>(
                                                                    objMainActivity.objTaskHandler.
                                          objServiceGraph.mapOfHighLevelServiceToComposition.get(
                                                                                    selectedService
                                                                                                )
                                                                       );

                //Check if the first service is availbale on the current device
                Service firstService = seviceList.get(0);
                if (firstService.location.equalsIgnoreCase(currDeviceName)) {

                    //Directly invoke service
                    //TODO Lookup in registry to get the method and execute it

                    //Create Control message using the serviceList
                    String [] addrs = {currDeviceName,firstService.location}; //Generate addresses
                    long ctrlMsgId = ControlMsgCounter.getCount();  //Generate a new ReqID
                    long currUnixTimeStamp = System.currentTimeMillis();//(new Date()).getTime();
                    ControlMsg ctrlMsg = new ControlMsg(ControlMsg.CtrlMsgCodes.REQ,ctrlMsgId,
                                                                addrs,0,
                                                                seviceList,
                                                                currUnixTimeStamp,
                                                                (byte)-1
                                                        );
                    TranslateText t = new TranslateText(inputParamString, ctrlMsg, objMainActivity);
                    t.execute();//Service invocation, intermediate output will be queued by TranslateText
                }
                else{

                    //Write it to a file
                    StringBuilder strFileName = new StringBuilder();
                    strFileName.append(selectedService.inputDesc + "_"+selectedService.outputDesc );
//                strFileName.append( (new Date()).getTime() );   //get the unix time stamp of current date
                    strFileName.append("."+selectedService.inputFileType);  //Get the file extension
                    FileIOHelper objFileIOHelper = new FileIOHelper(strFileName.toString());
                    byte[] temp = new byte[inputParamString.getBytes().length];
                    objFileIOHelper.writeDataToFile(inputParamString.getBytes());

                    objMainActivity.displayToast("Ip data written out to file.", Toast.LENGTH_SHORT);

                    //Create control message and push it into the queue
                    objMainActivity.objTaskHandler.queueComposedServiceReq(seviceList,
                                                                strFileName.toString(),
                                                                inputParamString.getBytes().length,
                                                                (byte) -1
                                                                          );
                    objMainActivity.displayToast("Composed Service request queued.", Toast.LENGTH_LONG);
                }
                //Clear the dialog from the screen
                inputDialog.cancel();
            }
        });



    }
}



//Listener for Individual services
class ListItemClickListener implements AdapterView.OnItemClickListener{

    //Reference to the main Activity
    static MainActivity objMainActivity;
    public ListItemClickListener(MainActivity mainActRef){
        objMainActivity = mainActRef;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {

        //Based on the position extract service information and pass on to the dialog click listener

        //Show a dialog to accept user input
        final Dialog inputDialog = new Dialog(objMainActivity);
        inputDialog.setContentView(R.layout.text_input);
        inputDialog.show();

        final EditText inputParam = (EditText)inputDialog.findViewById(R.id.inputTextForService);
        Button sendInputParams = (Button)inputDialog.findViewById(R.id.sendInputParams);


        sendInputParams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String inputParamString = inputParam.getText().toString();  //Get the input
                Service selectedService = (Service) parent.getAdapter().getItem(position);//Get the service name

                //Persist input data only if service is not located on the current device
                String currDeviceName = ((BluetoothHelper) objMainActivity.
                        objTaskHandler.objNIOHelper).currDevName;

                if (selectedService.location.equalsIgnoreCase(currDeviceName)) {

                    //Directly invoke service
                    //TODO Lookup in registry to get the method and execute it
//                    ArrayList tempServiceList = new ArrayList<Service>();
//                    tempServiceList.add(selectedService);
                    TranslateText t = new TranslateText(inputParamString,
                                                        selectedService.inputDesc.split(" ")[0],
                                                        selectedService.outputDesc.split(" ")[0],
                                                        objMainActivity
                                                       );
                    t.execute();//Service invocation

                } else {
                    //Write it to a file
                    StringBuilder strFileName = new StringBuilder();
                    strFileName.append(selectedService.inputDesc + "_" + selectedService.outputDesc);
//                strFileName.append( (new Date()).getTime() );   //get the unix time stamp of current date
                    strFileName.append("." + selectedService.inputFileType);  //Get the file extension
                    FileIOHelper objFileIOHelper = new FileIOHelper(strFileName.toString());
                    byte[] temp = new byte[inputParamString.getBytes().length];
                    objFileIOHelper.writeDataToFile(inputParamString.getBytes());

                    objMainActivity.displayToast("Ip data written out to file.", Toast.LENGTH_SHORT);

                    //Create control message and push it into the queue
                    objMainActivity.objTaskHandler.queueServiceReq(selectedService,
                                                                    strFileName.toString(),
                                                                    inputParamString.getBytes().length,
                                                                    (byte) -1
                                                                  );

                    objMainActivity.displayToast("Service request queued.", Toast.LENGTH_LONG);
                }

                //Clear the dialog from the screen
                inputDialog.cancel();
            }
        });



    }
}




//Listener for Bluetooth switch
class StartSwitchStateListener implements CompoundButton.OnCheckedChangeListener {

    MainActivity objMainsActivity;

    public StartSwitchStateListener (MainActivity _objMain){
        objMainsActivity = _objMain;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        Log.i("SwitchStateChange", "BT Switch state changed to - " + isChecked);

        //Set the network protocol in the handler and initialize the NW helper
        objMainsActivity.objTaskHandler.setNWProtocol(
                ServCompHandler.NetworkProtocols.BT);//Networking through BT


        //If the switch is in ON position
        if (isChecked) {

            //TODO - Insert code to disable other switches
            ServCompHandler.isAppAlive = true;//Set the alive flag in the Handler

            Log.i("SwitchStateChange", "Switching on Bluetooth");
            short retBtStatus = ((BluetoothHelper) objMainsActivity.
                    objTaskHandler.objNIOHelper).enableBluetoothAndListen();//Enable BT

            Log.i("SwitchStateChange", "BTStatus = " + retBtStatus);
            //Check the status of the Bluetooth
            if (retBtStatus == 0) {

                objMainsActivity.displayToast("Device does not support bluetooth", Toast.LENGTH_SHORT);
                objMainsActivity.startSwitch.setChecked(false);
            } else if (retBtStatus < 0) {
                objMainsActivity.displayToast("Error while starting bluetooth", Toast.LENGTH_SHORT);
                objMainsActivity.startSwitch.setChecked(false);
            } else {
                objMainsActivity.displayToast("Bluetooth adapter is ON", Toast.LENGTH_SHORT);

                //Display only more than 1 service is present in the list
                if(objMainsActivity.servicesArrAdapter.getCount() > 0)
                    objMainsActivity.servicesDevicesLstView.setVisibility(View.VISIBLE);

                if(objMainsActivity.composedServiceAdapter.getCount() >0)
                    objMainsActivity.composedServicesLstView.setVisibility(View.VISIBLE);

                objMainsActivity.objTaskHandler.acceptIncomingConnections();    //Start the server behavior of the phone


                objMainsActivity.objTaskHandler.startHandlerThreads();//Start the handler threads
            }
        } else {

            Log.i("SwitchStateChange", "Switching off Bluetooth");
            short retBtStatus = ((BluetoothHelper) objMainsActivity.
                    objTaskHandler.objNIOHelper).disableBluetooth();    //Disable BT

            Log.i("SwitchStateChange", "BT status code: "+ retBtStatus);
            //Check the status of the disable action
            if (retBtStatus == 0) {

                objMainsActivity.displayToast("Device does not support bluetooth", Toast.LENGTH_SHORT);
                objMainsActivity.startSwitch.setChecked(false);
            } else if (retBtStatus < 0) {
                objMainsActivity.displayToast("Error while switching off bluetooth", Toast.LENGTH_SHORT);
                objMainsActivity.startSwitch.setChecked(true);
            } else {

                Log.i("SwitchStateChange", "Setting the flag to false to kill threads.");
                ServCompHandler.isAppAlive = false; //Set the flag to false to kill handler threads
                //Empty all queues
                objMainsActivity.objTaskHandler.proxyQueue.clear();
                objMainsActivity.objTaskHandler.reqQueue.clear();
                objMainsActivity.objTaskHandler.respQueue.clear();
                objMainsActivity.objTaskHandler.ackQueue.clear();

                objMainsActivity.servicesDevicesLstView.setVisibility(View.INVISIBLE);  //Hide the individual service list
                objMainsActivity.composedServicesLstView.setVisibility(View.INVISIBLE); //Hide the composed service lists

                objMainsActivity.displayToast("Bluetooth adapter is OFF", Toast.LENGTH_SHORT);
            }

        }
    }
}
