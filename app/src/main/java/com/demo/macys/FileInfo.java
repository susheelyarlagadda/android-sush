package com.demo.macys;

import android.os.Parcel;
import android.os.Parcelable;

public class FileInfo implements Parcelable{
    private String name;
    private double size;

    public FileInfo(){

    }

    public FileInfo(Parcel source) {
        this.name = source.readString();
        this.size = source.readDouble();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeDouble(size);
    }

    public static final Creator<FileInfo> CREATOR = new Creator<FileInfo>(){
        @Override
        public FileInfo createFromParcel(Parcel source) {
            return new FileInfo(source);
        }

        @Override
        public FileInfo[] newArray(int size) {
            return new FileInfo[0];
        }
    };

    @Override
    public String toString() {
        return "FileInfo{" +
                "name='" + name + '\'' +
                ", size=" + size +
                '}';
    }
}
