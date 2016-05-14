package com.example.siddharth.servcomp3;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Service implements Parcelable,Serializable
{
    public String inputDesc, outputDesc,    //To be used in semantic graph
                  inputFilePath, outputFilePath,    //To be used in syntax graph
                  inputFileType, outputFileType,    //To be used in syntax graph
                  servDesc, location;   //Repository specific for retrieval

    public long computationTime;   //To be computed based by recording start and end time

    public int serviceCost; //Repository specific

    //Default Constructor
    public Service(String []params)
    {

        inputDesc = params[0]; outputDesc = params[1];
        inputFilePath = params[2]; outputFilePath = params[3];
        inputFileType = params[4]; outputFileType = params[5];
        servDesc = params[6]; location = params[7];
    }


    public void setServiceCost(int cost){serviceCost = cost;}


    @Override
    public String toString() {
        return "Service description: "+servDesc +"\n"+"Input description: " +inputDesc +
                "\nOutput description: "+outputDesc + "\nLocation: " +location  ;
    }

    @Override
    public boolean equals(Object obj){

        //Return false if the supplied object is null
        if(obj == null)
            return false;

        // If the object is compared with itself then return true
        if (obj == this)
            return true;

        //Check if this is an instance of Service class
        if (!(Service.class.isAssignableFrom(obj.getClass())))
            return false;
        else{
            final Service s = (Service) obj;
            if(this.location.equalsIgnoreCase(s.location) &&
               this.servDesc.equalsIgnoreCase(s.servDesc) &&
               this.inputDesc.equalsIgnoreCase(s.inputDesc) &&
               this.outputDesc.equalsIgnoreCase(s.outputDesc) &&
               this.inputFileType.equalsIgnoreCase(s.inputFileType) &&
               this.outputFileType.equalsIgnoreCase(s.outputFileType)
               )
                return true;
            else
                return false;
        }

        //Service objService = (Service) obj;// typecast o to Service

//        return false;// Compare the data members and return accordingly
    }


    //--------------- Parcelable specific code ---------------
    protected Service(Parcel in) {
        inputDesc = in.readString();
        outputDesc = in.readString();
        inputFilePath = in.readString();
        outputFilePath = in.readString();
        inputFileType = in.readString();
        outputFileType = in.readString();
        servDesc = in.readString();
        location = in.readString();
        computationTime = in.readLong();
        serviceCost = in.readInt();
    }

    public static final Creator<Service> CREATOR = new Creator<Service>() {
        @Override
        public Service createFromParcel(Parcel in) {
            return new Service(in);
        }

        @Override
        public Service[] newArray(int size) {
            return new Service[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(inputDesc);
        dest.writeString(outputDesc);
        dest.writeString(inputFilePath);
        dest.writeString(outputFilePath);
        dest.writeString(inputFileType);
        dest.writeString(outputFileType);
        dest.writeString(servDesc);
        dest.writeString(location);
        dest.writeLong(computationTime);
        dest.writeInt(serviceCost);
    }
}
