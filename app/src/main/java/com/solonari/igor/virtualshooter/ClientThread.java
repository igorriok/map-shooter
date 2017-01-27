package com.solonari.igor.virtualshooter;

class ClientThread implements Runnable {

  int portNumber = 57349;
  InetAddress ip = InetAddress.getByName("192.168.1.154");

		@Override
		public void run() {

			try {
				socket1 = new Socket(ip, portNumber);
        
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}
}
