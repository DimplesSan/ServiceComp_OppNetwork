package com.example.siddharth.servcomp3;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

//Represents the header part of the communication
//Contains reference to
// ctrMsgCode
// srcBTAddr
// destBTAddr
// sizeOfPayload
// Service ref
// servReqTimeStamp
public class ControlMsg implements Parcelable, Serializable {

    public enum CtrlMsgCodes {HANDSHAKE, REQ, RESP, ACK};   //All possible types of message codes

    public CtrlMsgCodes ctrlMsgCode;   //code from enum representing the type of msg
    public long ctrlMsgId;  //Message identifier to match the request and response
    public String srcAddr, destAddr;    //BT addresses of the src and the dest node
    public long sizeOfPayload;  //size of the incoming payload
    public ArrayList<Service> serviceList; //List of service transformations
    public long reqTimeStamp; //Timestamp of the originating request
    public byte numOfHops;  //Indicates the number of hops the message should take -> -1 indicates flooding
                            //Else the value will be decremented with every hop

    //Constructor to create a control message
    public ControlMsg(CtrlMsgCodes _code, long ctrlMsgId,  String [] addrs, long _sizeOfFollwPayload,
                      ArrayList<Service> _follServices, long _reqTimeStamp, byte _nOfHops){

        this.ctrlMsgCode = _code;
        this.ctrlMsgId = ctrlMsgId;
        this.srcAddr = addrs[0];
        this.destAddr = addrs[1];
        this.sizeOfPayload = _sizeOfFollwPayload;
        this.serviceList = _follServices;
        this.reqTimeStamp = _reqTimeStamp;
        this.numOfHops = _nOfHops;
    }

    //Constructor for control message specific for handshaking purpose
    public ControlMsg(String [] addrs, long reqTimeStamp){

        this.ctrlMsgCode = CtrlMsgCodes.HANDSHAKE;  //Set the type of control message as handshake
        this.ctrlMsgId = ControlMsgCounter.getCount();
        this.srcAddr = addrs[0];
        this.destAddr = addrs[1];
        this.sizeOfPayload = 0;
        this.serviceList = new ArrayList<Service>();
        this.reqTimeStamp = reqTimeStamp;
        this.numOfHops = -1;
    }


    //--------------------------------Parcelable Implementation-------------------------------------
    protected ControlMsg(Parcel in) {
        ctrlMsgId = in.readLong();
        srcAddr = in.readString();
        destAddr = in.readString();
        sizeOfPayload = in.readLong();
        serviceList = in.createTypedArrayList(Service.CREATOR);
        reqTimeStamp = in.readLong();
        numOfHops = in.readByte();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(ctrlMsgId);
        dest.writeString(srcAddr);
        dest.writeString(destAddr);
        dest.writeLong(sizeOfPayload);
        dest.writeTypedList(serviceList);
        dest.writeLong(reqTimeStamp);
        dest.writeByte(numOfHops);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ControlMsg> CREATOR = new Creator<ControlMsg>() {
        @Override
        public ControlMsg createFromParcel(Parcel in) {
            return new ControlMsg(in);
        }

        @Override
        public ControlMsg[] newArray(int size) {
            return new ControlMsg[size];
        }
    };

    @Override
    public String toString(){
        return "Code- "+ctrlMsgCode + " SrcAddr- " + srcAddr +" DestAddr- " +destAddr + " PayLoadSize- "
                +sizeOfPayload + " TimeStamp- " + reqTimeStamp +
                " ServiceCount- " + serviceList.size() + " Num of hops- "+numOfHops;
    }

}


class ControlMsgCounter{

    public static long count = 0;

    public static long getCount(){++count; return count;}
}