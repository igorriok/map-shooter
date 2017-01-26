package com.solonari.igor.virtualshooter;

import java.io.Serializable;

public class Pilot implements Serializable {

  private String idToken;
  private int points;
  
  public Pilot (String idToken, int points) {
    this.idToken = idToken;
    this.points = points;
  }

}
