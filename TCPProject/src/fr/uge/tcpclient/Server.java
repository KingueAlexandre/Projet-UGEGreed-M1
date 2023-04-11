package fr.uge.tcpclient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

	private static int BUFFER_SIZE = 4096;

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
		 * The convention is that buffer is in write-mode before calling doRead and is
		 * in write-mode after calling doRead
		 *
		 * @throws IOException
		 */
		private void doRead() throws IOException {
			// TODO
			if (sc.read(bufferIn) == -1) {
				closed = true;
				logger.info("not readfull");
				return;
			}
			if (bufferIn.hasRemaining()) {
				logger.info("remain place in the buffer");
				return;
			}
			bufferIn.flip();
			updateInterestOps();
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that buffer is in write-mode before calling doWrite and is
		 * in write-mode after calling doWrite
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
	private static final Logger logger = Logger.getLogger(TCPClient.class.getName());

	private static Charset UTF8 = StandardCharsets.UTF_8; // Charset.forName("US-ASCII");
	private ByteBuffer BufferIn = ByteBuffer.allocateDirect(BUFFER_SIZE);
	private ByteBuffer BufferOut = ByteBuffer.allocateDirect(BUFFER_SIZE);

	private final ServerSocketChannel ssc; // Serveur App
	private final SocketChannel sc; // Client App
	private final Selector serverSelector;
	private final InetSocketAddress serverAddress; // Pas ROOT : Client -> Autre Serveur
	private Thread server;
//  private Context_server serverContext;

	public Server(int host_port) throws IOException {
		// TODO Auto-generated constructor stub
		ROOT = true;

		this.serverSelector = Selector.open();

		this.serverAddress = null;
		sc = SocketChannel.open();

		this.port = host_port;
		ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(port));

		this.server = Thread.ofPlatform().unstarted(() -> {
			try {
				launch_server();
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
		// server side
		ssc.configureBlocking(false);
		var serverKey = ssc.register(serverSelector, SelectionKey.OP_ACCEPT);
//    serverContext = new Context_server(serverKey);
//    serverKey.attach(serverContext);

		this.server.start();
	}

	public void launch_server() throws IOException, InterruptedException {
		logger.info("Server started");
		while (!Thread.interrupted()) {
			Helpers.printKeys(serverSelector); // for debug
			try {
				serverSelector.select(this::treatKey_server);
			} catch(ClosedByInterruptException e) {
				logger.info("ClosedByInterruptedException " + e);
			} catch (AsynchronousCloseException e) {
				logger.info("Closed connection due to timeout");
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			System.out.println("Select finished");
		}
	}

	private void treatKey_server(SelectionKey key) {
		Helpers.printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				logger.info("doAccept");
				doAccept(key);
			}
			
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			Helpers.printSelectedKey(key); // for debug

			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isWritable()) {
				logger.info("doWrite");
				((Context_server) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				logger.info("doRead");
				((Context_server) key.attachment()).doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "Connection closed with client due to IOException", e);
			silentlyClose(key);
		}
		Helpers.printSelectedKey(key); // for debug

	}

	private void doAccept(SelectionKey key) throws IOException {
		// TODO
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssc.accept();
		if (sc == null) {
			logger.info("Selector gave bad hint");
			return;
		}
		sc.configureBlocking(false);
		var newKey = sc.register(serverSelector, SelectionKey.OP_READ);
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
}
