package fr.uge.tcpclient;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TCPClient {
  int rootPort;
  int numberOfRequest = 0;
  private int limit;
  private int totalNumberOfClient = 0;
  private ArrayList<Integer> clientOnEachUnderNetwork = new ArrayList<>();
  private int BUFFER_SIZE = 1024;
  private static Charset UTF8 = StandardCharsets.UTF_8; //Charset.forName("US-ASCII");
  private ByteBuffer sendBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
  private ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
  
  public TCPClient() {
    // TODO Auto-generated constructor stub
  }
  
  
  // Le ByteBuffer sera par convention toujours en mode écriture avant et après chaque méthode.
  // Trame de connexion vers une autre application
  void trame01() {
    sendBuffer.flip();
    sendBuffer.put((byte)1);
    sendBuffer.flip();
  }
  
  // Trame de mise à jour après connexion
  void trame02() {
    sendBuffer.flip();
    sendBuffer.put((byte)2);
    sendBuffer.putInt(totalNumberOfClient);
    sendBuffer.flip();
  }
  
  // Trame de demande de calcul
  void trame03(String url, String name, int start, int end) {
    sendBuffer.flip();
    sendBuffer.put((byte)3);
    sendBuffer.putInt(numberOfRequest++);
    sendBuffer.putInt(url.length());
    sendBuffer.put(UTF8.encode(url));
    sendBuffer.putInt(name.length());
    sendBuffer.put(UTF8.encode(name));
    sendBuffer.putInt(start);
    sendBuffer.putInt(end);
    sendBuffer.flip();
  }
  
  // Trame de retour de calcul
  void trame04(int senderPort, int numberOfTheRequest, int start, int end, String result) {
    sendBuffer.flip();
    sendBuffer.put((byte)4);
    sendBuffer.putInt(senderPort);
    sendBuffer.putInt(numberOfTheRequest);
    sendBuffer.putInt(start);
    sendBuffer.putInt(end);
    sendBuffer.putInt(result.length());
    sendBuffer.put(UTF8.encode(result));
    sendBuffer.flip();
  }
  
  // Trame de demande d'espace disponible
  void trame05() {
    sendBuffer.flip();
    sendBuffer.put((byte)5);
    sendBuffer.flip();
  }
  
  // Trame de remonter de la limit du sous réseau
  void trame06(int totalLimit) {
    sendBuffer.flip();
    sendBuffer.put((byte)6);
    totalLimit += limit;
    sendBuffer.putInt(totalLimit);
    sendBuffer.flip();
  }
  
  // Trame de déconnexion
  void trame07() {
    sendBuffer.flip();
    sendBuffer.put((byte)7);
    sendBuffer.flip();
  }
  
  // Trame de remontée des calculs en cas de déconnexion
  void trame08(int senderPort, int numberOfTheRequest, int start, int end) {
    sendBuffer.flip();
    sendBuffer.put((byte)8);
    sendBuffer.putInt(senderPort);
    sendBuffer.putInt(numberOfTheRequest);
    sendBuffer.putInt(start);
    sendBuffer.putInt(end);
    sendBuffer.flip();
  }
}
