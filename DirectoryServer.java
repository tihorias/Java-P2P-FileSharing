import java.io.*;
import java.net.*;
import java.util.*;


public class DirectoryServer {
	public static void main(String[] args) {
		//Main Server Port
		//PeerClients connect using UDP
		//DHT Servers connect using TCP
		int port = Integer.parseInt(args[0]);
		//Server ID [1,n]
		int serverID = Integer.parseInt(args[1]);
		//Port to connect to the next DHT server
		int successorPort = Integer.parseInt(args[2]);
		//IP Address to the next server in the DHT
		String successorIP = args[3];
		Server server = new Server(port, serverID, successorPort, successorIP);
	}

	// Unique UDP for PeerClients
	public static class UniqueUDP {
		final int statusCode_200 = 200;  // OK Request
		final int statusCode_400 = 400;  // Bad Request
		final int statusCode_404 = 404;  // Not Found
		final int statusCode_505 = 505;  // HTTP Version Not Available
		int unique_Port;	 			 // Unique UDP port
		String client_IP; 				 // IP address of the PeerClient
		Thread unique_Thread; 			 // Unique thread.
		DatagramSocket unique_UDPSocket; // UDP socket.
		String SuccessorIP; 			 // IP Address of the successor server.
		int SuccessorPortNumber;         // Port number of the successor server.

		// Constructor
		public UniqueUDP(String clientIP, int port, String mySuccessorIP, int mySuccessorPortNumber) {
				this.client_IP = clientIP;
				this.SuccessorIP = mySuccessorIP;
				this.SuccessorPortNumber = mySuccessorPortNumber;
				unique_Port = port;
				try {
					// New UDP Socket
					unique_UDPSocket = new DatagramSocket(port);
				}
				catch (SocketException e) {
					System.out.println("Port is not avaliable. Try another Port");
				}
				unique_Thread = new Thread(UniqueRunnableThread);
				unique_Thread.start();
			}

			// Run main thread.
			Runnable UniqueRunnableThread = new Runnable() {
				public void run() {
					while (true) {
						String info;
						byte[] receiveData = new byte[1024];
						try {
							DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); // UDP Packet.
							unique_UDPSocket.receive(receivePacket);
							info = new String(receivePacket.getData());
							System.out.println("FROM THE CLIENT -> " + info);

							// If info = "UPLOAD", upload the file to the respective server.
							if (info.contains("UPLOAD")) {
								Scanner scan = new Scanner(info);
								scan.next();
								String fileName = scan.next(); // Filename
								System.out.println("TO THE CLIENT -> " + statusCode_200 + " Padding" + "\n");
								//Send the data to the client
								sendToClient(statusCode_200 + " Padding", client_IP, receivePacket.getPort());
								// Update server info with the client name and IP of that file
								Server.cList.put(fileName, client_IP);
							}

							// If info = "QUERY", search for the filename
							else if (info.contains("QUERY")) {
								Scanner scan = new Scanner(info);
								scan.next();
								String fileName = scan.next();

								String ipAdd = Server.cList.get(fileName);
								// If IP is not found, output HTTP 404
								if (ipAdd == null) {
									System.out.println("TO THE CLIENT -> " + statusCode_404 + " Padding" + "\n");
									sendToClient(statusCode_404 + " Padding", client_IP, receivePacket.getPort());
								}
								// If file is found, output HTTP 200
								else {
									System.out.println("TO THE CLIENT -> " + statusCode_200 + " " + ipAdd + " Padding" + "\n");
									sendToClient(statusCode_200 + " " + ipAdd + " Padding", client_IP, receivePacket.getPort());
								}
							}

							// If info = "EXIT", then exit client and send info to the next server
							else if (info.contains("EXIT")) {
								Scanner scan = new Scanner(info);
								info = scan.next() + " " + client_IP + " " + receivePacket.getPort();
								for (int i = 0; i < 4; i++)
									info += " " + scan.next();
								System.out.println("TO THE NEXT SERVER -> " + info);
								sendToNextServer(info);
							}
						}
						catch (Exception error) {
						}
					}
				}
			};

			//Close a Thread
			public void kill() {
				unique_Thread = null;
				unique_UDPSocket.close();
			}

			//Send info to the next Server
			public void sendToNextServer(String info) throws UnknownHostException, IOException {
				Socket connectToNext = new Socket(SuccessorIP, SuccessorPortNumber);
				OutputStream outToServer = connectToNext.getOutputStream();
				DataOutputStream out = new DataOutputStream(outToServer);
				out.writeUTF(info); // Write info
				connectToNext.close(); // Close connection
			}

			//Send info to the Client
			public void sendToClient(String info, String client_IP, int client_Port) throws IOException {
				byte[] sendData = new byte[1024];
				sendData = info.getBytes();
				InetAddress ipAdd = InetAddress.getByName(client_IP);	// Get InetAddress given the Client's IP
				// Create and Send UDP Packet
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAdd, client_Port);
				unique_UDPSocket.send(sendPacket);
			}
		}



	public static class Server {
		final int statusCode_200 = 200; // OK Request
		final int statusCode_400 = 400; // Bad Request
		final int statusCode_404 = 404; // Not Found
		final int statusCode_505 = 505; // HTTP Version Not Available

		Thread mainTCPThread;		// TCP Thread
		Thread mainUDPThread;		// UDP Thread

		ServerSocket server_TCPSocket; 	// Main TCP socket
		DatagramSocket server_UDPSocket; // Main UDP socket

		int initialPortNumber;    	// Initial port for UDP
		int PortNumber; 			// Port Number of the first DHT Server
		int ServerID; 				// Unique ServerID
		String IP;					// IP Address of the server
		int SuccessorPortNumber; 	// Successor server's first active port number
		String SuccessorIP; 		// Successor server's IP
		int SuccessorServerID; 		// Successor server's server ID



		// ArrayList for UniqueUDP
		ArrayList<UniqueUDP> client_UDPList = new ArrayList<UniqueUDP>();

		// HashTable ArrayList
		// Key = File Name
		// Value = IP Address
		public static Hashtable<String, String> cList = new Hashtable<String, String>();

		// Constructor for Server
		public Server(int port, int serverID, int successor_PortNumber, String successor_IP) {
			this.PortNumber = port;
			this.ServerID = serverID;
			this.SuccessorPortNumber = successor_PortNumber;
			this.SuccessorIP = successor_IP;
			this.SuccessorServerID = ServerID == 4 ? 1 : ServerID + 1;
			this.initialPortNumber = port + 1;

			try {
				// Get IP Address
				this.IP = InetAddress.getLocalHost().getHostName();
			}
			catch (Exception error) {
				System.out.println("An IP address has not been assigned...");
			}

			System.out.println("Starting the Server. Please Wait...");
			System.out.println("	IP Address: " + IP + "\n	Port Number: " + PortNumber
					+ "\n	ServerID: " + ServerID + "\n	Successor's Port Number: "
					+ SuccessorPortNumber + "\n	Successor's IP Address: "
					+ SuccessorIP + "\n	Successor's Server ID: "
					+ SuccessorServerID);
			try {
				// New TCP Socket
				server_TCPSocket = new ServerSocket(PortNumber);
				if (serverID == 1)
					// New UDP Socket
					server_UDPSocket = new DatagramSocket(PortNumber);

				//Starting TCP and UDP Threads
				mainTCPThread = new Thread(mainTCPRunnableThread);
				mainUDPThread = new Thread(mainUDPRunnableThread);
				mainTCPThread.start();
				mainUDPThread.start();
			}
			catch (Exception error) {
				System.out.println("Ports are not avaliable. Please try different ports");
			}
		}

		// Run the main TCP thread.
		Runnable mainTCPRunnableThread = new Runnable() {
			public void run() {
				System.out.println("The main TCP Thread is starting...");
				while (true) {
					String info;
					try {
						Socket getPredecessor = server_TCPSocket.accept();
						DataInputStream in = new DataInputStream(getPredecessor.getInputStream());
						info = in.readUTF();
						System.out.println("FROM THE PREVIOUS SERVER -> " + info);

						// HTTP 200 to client if serverID = 1 and info = "GET ALL IP"
						if (ServerID == 1 && info.contains("GET ALL IP")) {
							String[] information = init(info);
							String new_Message = statusCode_200 + " " + info + " Padding";
							System.out.println("TO THE CLIENT -> " + new_Message + "\n");
							DataToClient(new_Message, information[0], Integer.parseInt(information[1]));
						}

						// If info = "GET ALL IP", send data to the next server
						else if (info.contains("GET ALL IP")) {
							String[] information = init(info);
							int unique_Port;
							unique_Port = findUDPPort();
							client_UDPList.add(new UniqueUDP(information[0], unique_Port, SuccessorIP, SuccessorPortNumber));

							info += " " + IP + " " + unique_Port;
							System.out.println("TO THE SUCCESSOR -> " + info + "\n");
							sendToNextServer(info);
						}

						// If serverID is = 1 and the info = "EXIT", then send message to client
						else if (ServerID == 1 && info.contains("EXIT")) {
							String[] information = exit(info);
							info = statusCode_200 + " Padding";
							System.out.println("TO THE CLIENT -> " + info + "\n");
							DataToClient(info, information[0], Integer.parseInt(information[1]));
						}

						// // If the message is "EXIT", send the data to the successor server.
						else if (info.contains("EXIT")) {
							exit(info);
							System.out.println("TO THE SUCCESSOR -> " + info + "\n");
							sendToNextServer(info);
						}
						getPredecessor.close();
					}
					catch (Exception error) {
					}
				}
			}
		};

		// Run the main UDP thread.
		Runnable mainUDPRunnableThread = new Runnable() {
			public void run() {
				System.out.println("The main UDP Thread is starting...");
				while (true) {
					String info;
					byte[] receiveData = new byte[1024];
					try {
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
						// Get the packet using the UDP socket
						server_UDPSocket.receive(receivePacket);
						info = new String(receivePacket.getData());
						System.out.println("FROM CLIENT -> " + info);

						// If info = "GET ALL IP", create new messages with client info and push it to the next DHT server.
						if (info.contains("GET ALL IP")) {
							int unique_Port;
							unique_Port = findUDPPort();
							client_UDPList.add(new UniqueUDP(receivePacket.getAddress().getHostAddress(), unique_Port, SuccessorIP, SuccessorPortNumber));
							info = "GET ALL IP " + receivePacket.getPort() + " " + receivePacket.getAddress().getHostAddress() + " " + IP + " " + unique_Port;
							System.out.println("TO THE SUCCESSOR SERVER -> " + info);
							sendToNextServer(info);
						}
					}
					catch (Exception error) {
					}
				}
			}
		};

		// Find the next UDP Port that is available
		public int findUDPPort() {
			int port = 0;
			int portFind = initialPortNumber;
			boolean ok = false;
			while (ok == false) {

				// Try for new ports
				try {
					DatagramSocket tryNewPort = new DatagramSocket(portFind);
					ok = true;
					tryNewPort.close();
					break;
				}
				// If port unavailable, then increment portFind
				catch (SocketException e) {
					portFind++;
				}
			}
			port = portFind;
			return port;
		}

		// Message to be sent to the next Server
		public void sendToNextServer(String info) throws UnknownHostException, IOException {
			// Connect to the next socket and stream the data out
			Socket connectToNext = new Socket(SuccessorIP, SuccessorPortNumber);
			OutputStream outToServer = connectToNext.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(info);		// Write the message.
			connectToNext.close(); // Close the connection to the successor server.
		}

		// Message to be sent to the Client
		public void DataToClient(String info, String clientIP, int clientPort) throws IOException {
			byte[] sendData = new byte[1024];
			sendData = info.getBytes();
			InetAddress ip = InetAddress.getByName(clientIP); // Client IP address
			// UDP Packet to be sent
			DatagramPacket sendThePacket = new DatagramPacket(sendData, sendData.length, ip, clientPort);
			server_UDPSocket.send(sendThePacket);
		}


		// Exit request from other servers
		public String[] exit(String info) {
			Scanner scan = new Scanner(info);
			scan.next();

			String client_IP = scan.next();
			int clientPort = scan.nextInt();
			int port = 0;

			for (int i = 0; i < ServerID; i++) {
				port = scan.nextInt();
			}

			for (int i = 0; i < client_UDPList.size(); i++) {
				if (client_UDPList.get(i).client_IP.equals(client_IP)
						&& client_UDPList.get(i).unique_Port == port) {
					client_UDPList.get(i).kill();
					client_UDPList.remove(i);
					break;
				}
			}

			Enumeration e = cList.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				if (cList.get(key).equals(client_IP))
					cList.remove(key);
			}
			return new String[] { client_IP, clientPort + "" };
		}

		// Server receives a request to initialize
		public String[] init(String info) {
			Scanner scan = new Scanner(info);
			scan.next();
			scan.next();
			scan.next();
			int clientPort = scan.nextInt();
			String clientIP = scan.next();
			return new String[] { clientIP, clientPort + "" };
		}
	}


}
