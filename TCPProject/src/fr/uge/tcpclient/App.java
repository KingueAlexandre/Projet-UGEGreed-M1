package fr.uge.tcpclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Logger;

//import fr.uge.tcpclient.TCPClient.Context;

public class App {
	
	int rootPort;
	int numberOfRequest = 0;
	private final boolean ROOT;
	private int port;
//	private int port_pere;
	private int totalNumberOfClient = 0;
	private ArrayList<Integer> clientOnEachUnderNetwork = new ArrayList<>();
	private static final Logger logger = Logger.getLogger(TCPClient.class.getName());

	private static Charset UTF8 = StandardCharsets.UTF_8; // Charset.forName("US-ASCII");
//	private final ServerSocketChannel serverSocketChannel; // Serveur App
//	private final SocketChannel sc; // Client App
//	private final Selector selector;
	private final InetSocketAddress serverAddress; // Pas ROOT : Client -> Autre Serveur
//	private final Thread client;
//	private Context uniqueContext;
	
	private Server server;
	private Client client;
	
	public App(int port) throws IOException {
		// TODO Auto-generated constructor stub
		ROOT = true;
		server = new Server(port);
		client = null;

		this.serverAddress = null;
//		this.selector = Selector.open();
//		sc = SocketChannel.open();
		this.port = port;
//		serverSocketChannel = ServerSocketChannel.open();
//		serverSocketChannel.bind(new InetSocketAddress(port));

		logger.info(this.getClass().getName() + " starts on port " + port);
	}

	public App(int port, InetSocketAddress serverAddress) throws IOException {
		// TODO Auto-generated constructor stub
		ROOT = false;
		server = new Server(port);
		client = new Client(serverAddress);

		this.serverAddress = serverAddress;
//		this.selector = Selector.open();
//		sc = SocketChannel.open();
		this.port = port;
//		sc.connect(serverAddress);
//		this.port_pere = serverAddress.getPort();
//		serverSocketChannel = ServerSocketChannel.open();
//		serverSocketChannel.bind(new InetSocketAddress(port));

		logger.info(this.getClass().getName() + " starts on port " + port);
	}
	
	public void launch() throws IOException, InterruptedException {
		this.server.launch();
		if(!ROOT) {
			this.client.launch();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 1) {
			usage();
			return;
		}
		App app;
		if (args.length == 1) {
			app = new App(Integer.parseInt(args[0]));

		} else if (args.length == 3) {
			app = new App(Integer.parseInt(args[0]),
					new InetSocketAddress(args[1], Integer.parseInt(args[2])));

		} else {
			usage();
			return;
		}
		app.launch();

	}
	
	private static void usage() {
		System.out.println("Usage : App port [hostname] [port_hostname]");
	}

}
