package com.example.siddharth.servcomp3;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

//Class that exposes methods to read and write data to external storage
//Also provides methods to delete specified files from the external storage
public class FileIOHelper {

    private BufferedOutputStream bOS;
    private BufferedInputStream bIS;
    private File objFile;



    //Constructor
    public FileIOHelper(String _fileName){
        this.objFile = new File(Environment.getExternalStorageDirectory(), _fileName);
    }


    //Method to write data to file from the specified byte arr
    public boolean writeDataToFile(byte[] _dataByteArr){

        boolean retVal = false;

        //data Can be wrapped in a ByteBuffer if required
        try {

            bOS = new BufferedOutputStream(new FileOutputStream(objFile));  //Create Buffered op stream
            bOS.write(_dataByteArr);//Write the data out to file
            bOS.flush();//Flush the stream
            bOS.close();    //Close the stream
            retVal = true;  //set the flag to true, if data is successfully written out
        } catch (IOException e) {
            Log.e("writeDataToFile", "Exception of trying to write file "+ objFile.getName()
                  +" to external storage - " + e.getMessage());
        }
        return retVal;
    }



    //Method to read data into the specified byte array
    public boolean readDataFromFile(byte[] _dataByteArr){

        boolean retVal = false; //Flag to indicate successful read from file
        //data Can be wrapped in a ByteBuffer if required

        try{

            bIS = new BufferedInputStream( new FileInputStream(objFile));   //Create a buffered ip stream

            int numOfBytes = bIS.read(_dataByteArr); //Read the data into the specified byte arr
            if(numOfBytes > 0)  //Check the number of bytes read
                retVal = true;  //set the flag to indicate successful read
            else
                Log.e("readDataFromFile", "0 bytes read from file " + objFile.getName()
                      + " from external storage. Size of received byte arr: "+_dataByteArr.length);

            bIS.close();
        }
        catch (IOException e){
            Log.e("readDataFromFile","Exception on trying to read from file "+objFile.getName()
                  +" from external stiorage - " +e.getMessage());
        }
        return retVal;

    }



    //Method to delete the specified file from list
    public void deleteFile(){

        objFile.delete();
        Log.i("HandleFile_deleteFile", "File "+ objFile.getName() +" deleted");

    }

}
