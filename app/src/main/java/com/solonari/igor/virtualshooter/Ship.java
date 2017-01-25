package com.solonari.igor.virtualshooter;

import java.io.Serializable;
import android.location.Location;

/**
 * Created by isolo on 1/25/2017.
 */

public class Ship implements Serializable {

    private String shipName;
    private Location location;

    public Ship (String shipName, Location location){
        this.shipName = shipName;
        this.location = location;
    }

}
