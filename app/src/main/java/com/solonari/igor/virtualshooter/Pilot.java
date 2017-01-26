public class Pilot implements serializable {

  private String idToken;
  private int points;
  
  public Pilot (String idToken, int points) {
    this.idToken = idToken;
    this.points = points;
  }

}
