package com.solonari.igor.virtualshooter;

/**
 * Created by isolo on 1/26/2017.
 */
public class Singleton {
    private static Singleton mInstance = null;

    private String mString;

    private Singleton(){
        mString = "";
    }

    public static Singleton getInstance(){
        if(mInstance == null) {
            mInstance = new Singleton();
        }
        return mInstance;
    }

    public String getString(){
        return this.mString;
    }

    public void setString(String value){
        mString = value;
    }
}
