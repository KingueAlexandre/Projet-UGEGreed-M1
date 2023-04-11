package fr.uge.tcpclient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Client {
  
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
      Helpers.printSelectedKey(key); // for debug
      if (!sc.finishConnect())
        return; // the selector gave a bad hint
      key.interestOps(SelectionKey.OP_READ);
    }
  }
  
  private final Selector selector;
  private final SocketChannel sc; // Client App
  private final InetSocketAddress serverAddress; // Pas ROOT : Client -> Autre Serveur
  private Context_client clientContext;
  private final Thread client;
  
  public Client(InetSocketAddress addr) throws IOException {
    this.selector = Selector.open();
    
    this.serverAddress = addr;
    sc = SocketChannel.open();
    sc.configureBlocking(false);
    sc.connect(serverAddress);
    
    this.client = Thread.ofPlatform().unstarted(() -> {
      try {
        launch_client();
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    });
  }
  
  public void launch() throws IOException, InterruptedException {
    // client side
    var clientKey = sc.register(selector, SelectionKey.OP_CONNECT);
    clientContext = new Context_client(clientKey);
    clientKey.attach(clientContext);

    this.client.start();
  }
  
  public void launch_client() throws IOException, InterruptedException {
    while (!Thread.interrupted()) {
      Helpers.printKeys(selector); // for debug
      try {
        selector.select(this::treatKey_client);
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
}
