public class ComplexSocketClient {

    Socket socket1;
    int portNumber = 57349;
    String str = "";

    socket1 = new Socket(InetAddress.getLocalHost(), portNumber);

    ObjectInputStream ois = new ObjectInputStream(socket1.getInputStream());

    ObjectOutputStream oos = new ObjectOutputStream(socket1.getOutputStream());

    ComplexCompany comp = new ComplexCompany("A");
    ComplexEmployee emp0 = new ComplexEmployee("B", 1000);
    comp.addPresident(emp0);

    oos.writeObject(comp);

    while ((str = (String) ois.readObject()) != null) {
      System.out.println(str);
      oos.writeObject("bye");

      if (str.equals("bye"))
        break;
    }

    ois.close();
    oos.close();
    socket1.close();
}
