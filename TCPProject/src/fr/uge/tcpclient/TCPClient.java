package fr.uge.tcpclient;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
	
	private static int BUFFER_SIZE = 1024;

	
	static private class Context {
        private final SelectionKey key;
        private final SocketChannel sc;
        private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
        private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        private final ArrayDeque<Message> queue = new ArrayDeque<>();
        private boolean closed = false;

        private Context(SelectionKey key) {
            this.key = key;
            this.sc = (SocketChannel) key.channel();
        }

        /**
         * Process the content of bufferIn
         *
         * The convention is that bufferIn is in write-mode before the call to process
         * and after the call
         *
         */
        private void processIn() {
            // TODO
        }

        /**
         * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
         *
         * @param bb
         */
        private void queueMessage(Message msg) {
            // TODO
        }

        /**
         * Try to fill bufferOut from the message queue
         *
         */
        private void processOut() {
            // TODO
        }

        /**
         * Update the interestOps of the key looking only at values of the boolean
         * closed and of both ByteBuffers.
         *
         * The convention is that both buffers are in write-mode before the call to
         * updateInterestOps and after the call. Also it is assumed that process has
         * been be called just before updateInterestOps.
         */

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
	private ByteBuffer sendBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
	private ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

	private final ServerSocketChannel serverSocketChannel; // Serveur App
	private final SocketChannel sc; // Client App
	private final Selector selector;
	private final InetSocketAddress serverAddress; // Pas ROOT : Client -> Autre Serveur
	private final Thread client;
	private Thread server;
    private Context uniqueContext;

	public TCPClient(int port) throws IOException {
		// TODO Auto-generated constructor stub
		ROOT = true;
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


		this.serverAddress = null;
		this.selector = Selector.open();
		sc = SocketChannel.open();
		this.port = port;
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));

		logger.info(this.getClass().getName() + " starts on port " + port);
	}

	public TCPClient(int port, InetSocketAddress serverAddress) throws IOException {
		// TODO Auto-generated constructor stub
		ROOT = false;
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


		this.serverAddress = serverAddress;
		this.selector = Selector.open();
		sc = SocketChannel.open();
		this.port = port;
		sc.connect(serverAddress);
		this.port_pere = serverAddress.getPort();
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));

		logger.info(this.getClass().getName() + " starts on port " + port);
	}

	/**
	 * Iterative server main loop
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public void launch() throws IOException, InterruptedException {
		sc.configureBlocking(false);
		this.server.start();
		this.client.start();

	}


	public void launch_server() throws IOException, InterruptedException {

		logger.info("Server started");
		while (!Thread.interrupted()) {
			SocketChannel client = serverSocketChannel.accept();
			Thread.ofPlatform().start(() -> {

				try {
					logger.info("Connection accepted from " + client.getRemoteAddress());
					serve(client);
				} catch (IOException ioe) {
					logger.log(Level.SEVERE, "Connection terminated with client by IOException", ioe.getCause());
				} finally {
					silentlyClose(client);
				}
			});
		}
	}

	/**
	 * Treat the connection sc applying the protocol. All IOException are thrown
	 *
	 * @param sc
	 * @throws IOException
	 */
	private void serve(SocketChannel sc) throws IOException {

		// TODO
		for (;;) {
			try {
				ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
				ByteBuffer buffer2;
				if (!readFully(sc, buffer)) {
					return;
				}
				// sc.read(buffer);

				buffer.flip();
				int nb = buffer.getInt();
				long sum = 0;
				System.out.println(nb + " " + sum);

				buffer2 = ByteBuffer.allocate(Long.BYTES * nb);
				if (!readFully(sc, buffer2)) {
					return;
				}
				buffer2.flip();

				for (int i = 0; i < nb; i++) {
					sum += (long) buffer2.getLong();

//					System.out.println(nb + " " + i + " " + sum);
				}

				System.out.println(sum);
				sc.write(ByteBuffer.allocate(Long.BYTES).putLong(sum).flip());
			} catch (IOException ioe) {
				logger.log(Level.SEVERE, "Connection terminated with client by IOException");
			}
		}

	}
	
	
	public void launch_client() throws IOException, InterruptedException {
		while (!Thread.interrupted()) {
            try {
                selector.select(this::treatKey);
//                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
		
	}
	
	private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isConnectable()) {
                uniqueContext.doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                uniqueContext.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                uniqueContext.doRead();
            }
        } catch (IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }

	/**
	 * Close a SocketChannel while ignoring IOExecption
	 *
	 * @param sc
	 */

	private void silentlyClose(Closeable sc) {
		if (sc != null) {
			try {
				sc.close();
			} catch (IOException e) {
				// Do nothing
			}
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
		sendBuffer.flip();
		sendBuffer.put((byte) 1);
		sendBuffer.flip();
	}

	// Trame de mise à jour après connexion
	void trame02() {
		sendBuffer.flip();
		sendBuffer.put((byte) 2);
		sendBuffer.putInt(totalNumberOfClient);
		sendBuffer.flip();
	}

	// Trame de demande de calcul
	void trame03(String url, String name, int start, int end) {
		sendBuffer.flip();
		sendBuffer.put((byte) 3);
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
		sendBuffer.put((byte) 4);
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
		sendBuffer.put((byte) 5);
		sendBuffer.flip();
	}

	// Trame de remonter de la limit du sous réseau
	void trame06(int totalLimit) {
		sendBuffer.flip();
		sendBuffer.put((byte) 6);
		totalLimit += limit;
		sendBuffer.putInt(totalLimit);
		sendBuffer.flip();
	}

	// Trame de déconnexion
	void trame07() {
		sendBuffer.flip();
		sendBuffer.put((byte) 7);
		sendBuffer.flip();
	}

	// Trame de remontée des calculs en cas de déconnexion
	void trame08(int senderPort, int numberOfTheRequest, int start, int end) {
		sendBuffer.flip();
		sendBuffer.put((byte) 8);
		sendBuffer.putInt(senderPort);
		sendBuffer.putInt(numberOfTheRequest);
		sendBuffer.putInt(start);
		sendBuffer.putInt(end);
		sendBuffer.flip();
	}

	public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {
		if (args.length < 1) {
			usage();
			return;
		}
		TCPClient server;
		if (args.length == 1) {
			server = new TCPClient(Integer.parseInt(args[0]));

		} else if (args.length == 3) {
			server = new TCPClient(Integer.parseInt(args[0]),
					new InetSocketAddress(args[1], Integer.parseInt(args[2])));

		} else {
			usage();
			return;
		}
		server.launch();
	}

	private static void usage() {
		System.out.println("Usage : Client port [hostname] [port_hostname]");
	}

	

}
