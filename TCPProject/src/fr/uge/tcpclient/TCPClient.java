package fr.uge.tcpclient;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPClient {

  private static int BUFFER_SIZE = 4096;

  static private class Context_client {
    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private boolean closed = false;

    private Context_client(SelectionKey key) {
      this.key = key;
      this.sc = (SocketChannel) key.channel();
    }

    private void updateInterestOps() {
      // TODO
    }

    private void silentlyClose() {
      try {
        sc.close();
      } catch (IOException e) {
        // ignore exception
      }
    }

    /**
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doRead and after the call
     *
     * @throws IOException
     */
    private void doRead() throws IOException {
      // TODO
    }

    /**
     * Performs the write action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doWrite and after the call
     *
     * @throws IOException
     */

    private void doWrite() throws IOException {
      // TODO
    }

    public void doConnect() throws IOException {
      // TODO
    }
  }
  
  static private class Context_server {
    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private boolean closed = false;

    private Context_server(SelectionKey key) {
      this.key = key;
      this.sc = (SocketChannel) key.channel();
    }

    private void updateInterestOps() {
      // TODO
      var interestOps = 0;
      if (bufferIn.hasRemaining() && !closed) {
        interestOps |= SelectionKey.OP_READ;
      }
      if (bufferIn.position() == 0) {
        interestOps |= SelectionKey.OP_WRITE;
      }
      if (interestOps == 0) {
        silentlyClose();
        return;
      }
      key.interestOps(interestOps);
    }

    private void silentlyClose() {
      try {
        sc.close();
      } catch (IOException e) {
        // ignore exception
      }
    }

    /**
     * Performs the read action on sc
     *
     * The convention is that buffer is in write-mode before calling doRead and is in
     * write-mode after calling doRead
     *
     * @throws IOException
     */
    private void doRead() throws IOException {
      // TODO
      if(sc.read(bufferIn)==-1) {
        closed =true;
        logger.info("not readfull");
        return;
      }
      if(bufferIn.hasRemaining()) {
        logger.info("remain place in the buffer");
        return;
      }
      bufferIn.flip();
      updateInterestOps();
    }

    /**
     * Performs the write action on sc
     *
     * The convention is that buffer is in write-mode before calling doWrite and is in
     * write-mode after calling doWrite
     *
     * @throws IOException
     */
    private void doWrite() throws IOException {
      // TODO
      sc.write(bufferIn);
      bufferIn.compact();
      updateInterestOps();
    }
  }

  int rootPort;
  int numberOfRequest = 0;
  private final boolean ROOT;
  private int port;
  private int port_pere;
  private int limit;
  private int totalNumberOfClient = 0;
  private ArrayList<Integer> clientOnEachUnderNetwork = new ArrayList<>();
  private static final Logger logger = Logger.getLogger(TCPClient.class.getName());

  private static Charset UTF8 = StandardCharsets.UTF_8; // Charset.forName("US-ASCII");
  private ByteBuffer BufferIn = ByteBuffer.allocateDirect(BUFFER_SIZE);
  private ByteBuffer BufferOut = ByteBuffer.allocateDirect(BUFFER_SIZE);

  private final ServerSocketChannel ssc; // Serveur App
  private final SocketChannel sc; // Client App
  private final Selector clientSelector;
  private final Selector serverSelector;
  private final InetSocketAddress serverAddress; // Pas ROOT : Client -> Autre Serveur
  private final Thread client;
  private Thread server;
  private Context_client clientContext;
//  private Context_server serverContext;

  public TCPClient(int host_port) throws IOException {
    // TODO Auto-generated constructor stub
    ROOT = true;

    this.clientSelector = Selector.open();
    this.serverSelector = Selector.open();

    this.serverAddress = null;
    sc = SocketChannel.open();

    this.port = port;
    ssc = ServerSocketChannel.open();
    ssc.bind(new InetSocketAddress(port));

    this.server = Thread.ofPlatform().unstarted(() -> {
      try {
        launch_server();
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    });
    this.client = Thread.ofPlatform().unstarted(() -> {
      try {
        launch_client();
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    });

    logger.info(this.getClass().getName() + " starts on port " + port);
  }

  public TCPClient(int host_port, InetSocketAddress serverAddress) throws IOException {
    // TODO Auto-generated constructor stub
    ROOT = false;
    
    this.clientSelector = Selector.open();
    this.serverSelector = Selector.open();
    
    this.serverAddress = serverAddress;
    sc = SocketChannel.open();
    sc.connect(serverAddress);
    
    this.port = port;
    this.port_pere = serverAddress.getPort();
    ssc = ServerSocketChannel.open();
    ssc.bind(new InetSocketAddress(port));

    this.server = Thread.ofPlatform().unstarted(() -> {
      try {
        launch_server();
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    });
    this.client = Thread.ofPlatform().unstarted(() -> {
      try {
        launch_client();
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    });

    logger.info(this.getClass().getName() + " starts on port " + port);
  }

  /**
   * Iterative server main loop
   *
   * @throws IOException
   * @throws InterruptedException
   */

  public void launch() throws IOException, InterruptedException {
    // client side
    sc.configureBlocking(false);
    var clientKey = sc.register(clientSelector, SelectionKey.OP_CONNECT);
    clientContext = new Context_client(clientKey);
    clientKey.attach(clientContext);
    
    // server side
    ssc.configureBlocking(false);
    var serverKey = ssc.register(serverSelector, SelectionKey.OP_ACCEPT);
//    serverContext = new Context_server(serverKey);
//    serverKey.attach(serverContext);
        
    this.server.start();
    this.client.start();
  }

  public void launch_server() throws IOException, InterruptedException {
    logger.info("Server started");
    while (!Thread.interrupted()) {
      try {
        serverSelector.select(this::treatKey_server);
      } catch (UncheckedIOException tunneled) {
        throw tunneled.getCause();
      }
    }
  }

  public void launch_client() throws IOException, InterruptedException {
    while (!Thread.interrupted()) {
      try {
        clientSelector.select(this::treatKey_client);
      } catch (UncheckedIOException tunneled) {
        throw tunneled.getCause();
      }
    }
  }

  private void treatKey_client(SelectionKey key) {
    try {
      if (key.isValid() && key.isConnectable()) {
        clientContext.doConnect();
      }
      if (key.isValid() && key.isWritable()) {
        clientContext.doWrite();
      }
      if (key.isValid() && key.isReadable()) {
        clientContext.doRead();
      }
    } catch (IOException ioe) {
      // lambda call in select requires to tunnel IOException
      throw new UncheckedIOException(ioe);
    }
  }

  private void treatKey_server(SelectionKey key) {
    try {
      if (key.isValid() && key.isAcceptable()) {
        doAccept(key);
      }
    } catch (IOException ioe) {
      // lambda call in select requires to tunnel IOException
      throw new UncheckedIOException(ioe);
    }
    try {
      if (key.isValid() && key.isWritable()) {
        ((Context_server) key.attachment()).doWrite();
      }
      if (key.isValid() && key.isReadable()) {
        ((Context_server) key.attachment()).doRead();
      }
    } catch (IOException e) {
      logger.log(Level.INFO, "Connection closed with client due to IOException", e);
      silentlyClose(key);
    }
  }
  
  private void doAccept(SelectionKey key) throws IOException {
    // TODO
    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
    SocketChannel sc = ssc.accept();
    if(sc == null) {
      logger.info("Selector gave bad hint");
      return;
    }
    sc.configureBlocking(false);
    var newKey = sc.register(serverSelector,SelectionKey.OP_READ);
    newKey.attach(new Context_server(newKey));
  }

  /**
   * Close a SocketChannel while ignoring IOExecption
   *
   * @param sc
   */

  private void silentlyClose(SelectionKey key) {
    var sc = (Channel) key.channel();
    try {
      sc.close();
    } catch (IOException e) {
      // ignore exception
    }
  }

  static boolean readFully(SocketChannel sc, ByteBuffer buffer) throws IOException {
    while (buffer.hasRemaining()) {

      if (sc.read(buffer) == -1) {
        logger.info("Input stream closed");
        return false;
      }
    }
    return true;
  }

  // Le ByteBuffer sera par convention toujours en mode écriture avant et après
  // chaque méthode.
  // Trame de connexion vers une autre application
  void trame01() {
    BufferOut.flip();
    BufferOut.put((byte) 1);
    BufferOut.flip();
  }

  // Trame de mise à jour après connexion
  void trame02() {
    BufferOut.flip();
    BufferOut.put((byte) 2);
    BufferOut.putInt(totalNumberOfClient);
    BufferOut.flip();
  }

  // Trame de demande de calcul
  void trame03(String url, String name, int start, int end) {
    BufferOut.flip();
    BufferOut.put((byte) 3);
    BufferOut.putInt(numberOfRequest++);
    BufferOut.putInt(url.length());
    BufferOut.put(UTF8.encode(url));
    BufferOut.putInt(name.length());
    BufferOut.put(UTF8.encode(name));
    BufferOut.putInt(start);
    BufferOut.putInt(end);
    BufferOut.flip();
  }

  // Trame de retour de calcul
  void trame04(int senderPort, int numberOfTheRequest, int start, int end, String result) {
    BufferOut.flip();
    BufferOut.put((byte) 4);
    BufferOut.putInt(senderPort);
    BufferOut.putInt(numberOfTheRequest);
    BufferOut.putInt(start);
    BufferOut.putInt(end);
    BufferOut.putInt(result.length());
    BufferOut.put(UTF8.encode(result));
    BufferOut.flip();
  }

  // Trame de demande d'espace disponible
  void trame05() {
    BufferOut.flip();
    BufferOut.put((byte) 5);
    BufferOut.flip();
  }

  // Trame de remonter de la limit du sous réseau
  void trame06(int totalLimit) {
    BufferOut.flip();
    BufferOut.put((byte) 6);
    totalLimit += limit;
    BufferOut.putInt(totalLimit);
    BufferOut.flip();
  }

  // Trame de déconnexion
  void trame07() {
    BufferOut.flip();
    BufferOut.put((byte) 7);
    BufferOut.flip();
  }

  // Trame de remontée des calculs en cas de déconnexion
  void trame08(int senderPort, int numberOfTheRequest, int start, int end) {
    BufferOut.flip();
    BufferOut.put((byte) 8);
    BufferOut.putInt(senderPort);
    BufferOut.putInt(numberOfTheRequest);
    BufferOut.putInt(start);
    BufferOut.putInt(end);
    BufferOut.flip();
  }

  public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {
    if (args.length < 1) {
      usage();
      return;
    } else if (args.length == 1) {
      new TCPClient(Integer.parseInt(args[0])).launch();
    } else {
      new TCPClient(Integer.parseInt(args[0]), new InetSocketAddress(args[1], Integer.parseInt(args[2]))).launch();
    }
  }

  private static void usage() {
    System.out.println("Usage : Client server_port [connection_name] [connection_port]");
  }

}