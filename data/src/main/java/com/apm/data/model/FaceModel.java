package com.apm.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FaceModel implements Parcelable {
    public int id;
    public String personType;
    public  String personPic;
    public String noEnterFlag;
    public String personId;
    public String passcode;
    public  String name;
    public String delFlag;

    public FaceModel() {
    }



    protected FaceModel(Parcel in) {
        id = in.readInt();
        personType = in.readString();
        personPic = in.readString();
        noEnterFlag = in.readString();
        personId = in.readString();
        passcode = in.readString();
        name = in.readString();
        delFlag = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(personType);
        dest.writeString(personPic);
        dest.writeString(noEnterFlag);
        dest.writeString(personId);
        dest.writeString(passcode);
        dest.writeString(name);
        dest.writeString(delFlag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FaceModel> CREATOR = new Creator<FaceModel>() {
        @Override
        public FaceModel createFromParcel(Parcel in) {
            return new FaceModel(in);
        }

        @Override
        public FaceModel[] newArray(int size) {
            return new FaceModel[size];
        }
    };

    @Override
    public String toString() {
        return "FaceModel{" +
                "id=" + id +
                ", personType='" + personType + '\'' +
                ", personPic='" + personPic + '\'' +
                ", noEnterFlag='" + noEnterFlag + '\'' +
                ", personId='" + personId + '\'' +
                ", passcode='" + passcode + '\'' +
                ", name='" + name + '\'' +
                ", delFlag='" + delFlag + '\'' +
                '}';
    }

    public boolean delete(){
        return delFlag!=null&&delFlag.equals("1");
    }

    public String absolutePicUrl(String prefix){
        if(personPic.startsWith("http")){
            return  personPic;
        }else {
            return  prefix+personPic;
        }
    }
}
