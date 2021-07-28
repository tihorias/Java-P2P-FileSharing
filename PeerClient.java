import java.io.*;
import java.net.*;
import java.util.*;

// Client Connections
public class PeerClient {
	static String server1_IP;   // DHT Server1
	static int server1_Port;    // DHT Port for Server 1
	static int peer_ServerPort; // Client Server's Port

	public static void main(String[] args) {
		Thread main_Thread;
		peer_ServerPort = Integer.parseInt(args[0]); // Client Server Port
		server1_IP = args[1];						// DHT Server1 IP Address
		server1_Port = Integer.parseInt(args[2]);    // DHT Server 1 Port
		main_Thread = new Thread(Runnable);
		main_Thread.start();
	}

	static Runnable Runnable = new Runnable() {
		public void run() {
			System.out.println("Client is starting...");

			Client peer_Client = new Client(server1_IP, server1_Port,peer_ServerPort);
			PeerServer peer_Server = new PeerServer(peer_ServerPort);

			Scanner scannerIn = new Scanner(System.in); // Read User Input
			String userInput; // User input in string

			while (true) {
				System.out.println("Please select a function: Enter U to Upload, Q to Query and E to Exit");
				System.out.print("Please Enter: ");
				userInput = scannerIn.next();

				// User enters U
				if (userInput.equalsIgnoreCase("U")) {
					System.out.print("Please enter File Name: ");
					userInput = scannerIn.next();

					// Make a ServerID using mod4 for one of the four DHT Servers
					int calculate_ServerID = 0;
					for (int i = 0; i < userInput.length(); i++) {
						calculate_ServerID += (int) userInput.charAt(i);
					}
					calculate_ServerID = calculate_ServerID % 4;

					try {
						peer_Client.uploadData(calculate_ServerID, userInput);
					}
					catch (Exception e) {
						System.out.println("Cannot connect to server.");
					}
				}

				// User enters Q
				else if (userInput.equalsIgnoreCase("Q")) {
					System.out.print("Please enter File Name: ");
					userInput = scannerIn.next();

					// Make a ServerID using mod4 for one of the four DHT Servers
					int calculate_ServerID = 0;
					for (int i = 0; i < userInput.length(); i++) {
						calculate_ServerID += (int) userInput.charAt(i);
					}
					calculate_ServerID = calculate_ServerID % 4;

					try {
						peer_Client.query(calculate_ServerID, userInput);
					}
					catch (Exception e) {
						System.out.println("Cannot connect to server.");
					}
				}

				// User enters E
				else if (userInput.equalsIgnoreCase("E")) {
					try {
						peer_Client.exit();
					}
					catch (Exception e) {
						System.out.println("Cannot connect to server.");
					}
				}

				else {
					System.out.println("Please enter a valid input.");
				}
			}
		}
	};

	public static class Client {
		int peer_ServerPort;					// Client server port.
		String[] fileName;						// Filename.
		int[] server_PortNumbers = new int[4];	// Opening ports on DHT servers
		String[] serverIPs = new String[4];		// IPs of DHT servers
		DatagramSocket clientUDPSocket;			// UDP


		// Constructor
		public Client(String server1_IP, int server1_Port, int peer_ServerPort) {
			this.peer_ServerPort = peer_ServerPort;
			this.serverIPs[0] = server1_IP;
			this.server_PortNumbers[0] = server1_Port;

			try {
				// UDP
				clientUDPSocket = new DatagramSocket();
				init();
			}
			catch (Exception e) {
			}
		}

		//HTTP Response
		public String getHTTPResponse(Scanner scan, String rep) {
					String temp;
					while (scan.hasNext()) {
						temp = scan.nextLine() + "\r\n";
						rep += temp;
						if (temp.equals("\r\n")) {
							break;
						}
					}
					return rep;
				}

	    //HTTP Request
		public String createHTTPRequest(String request, String object, String connection, String host, String acceptType, String acceptLan) {
					String req = "";
					req += request + " /" + object + ".jpeg" + " HTTP/1.1\r\n";
					req += "Host: " + host + "\r\n";
					req += "Connection: " + connection + "\r\n";
					req += "Accept: " + acceptType + "\r\n";
					req += "Accept-Language: " + acceptLan + "\r\n\r\n";
					return req;
				}



		// init()
		public void init() throws Exception {
			String message;
			String status;
			sendDataToServer("GET ALL IP", serverIPs[0], server_PortNumbers[0]); // Message to Server
			message = receiveDataFromServer();
			Scanner scan = new Scanner(message);
			status = scan.next();
			scan.next();
			scan.next();
			scan.next();
			scan.next();
			scan.next(); // Gathering Port and IP

			if (status.equals("200")) {
				System.out.println("MESSAGE FROM SERVER -> Client Connected To Server");
			}
			// Array to store serverIPs and serverPortNumbers
			for (int i = 0; i < 4; i++) {
				serverIPs[i] = scan.next();
				server_PortNumbers[i] = Integer.parseInt(scan.next());
			}
		}


		// Send data to a server
		public void sendDataToServer(String message, String server_IP, int serverPort)
						throws IOException {
					byte[] send_Data = new byte[1024];
					send_Data = message.getBytes();
					InetAddress internetAddress = InetAddress.getByName(server_IP); // Inet address of the server
					DatagramPacket sendPacket = new DatagramPacket(send_Data, send_Data.length, internetAddress, serverPort);
					clientUDPSocket.send(sendPacket); // Send the packet using UDP.
				}

		// Receive data from a server
		public String receiveDataFromServer() throws IOException {
					byte[] receiveData = new byte[1024];
					DatagramPacket recievePacket = new DatagramPacket(receiveData, receiveData.length);
					clientUDPSocket.receive(recievePacket);
					return new String(recievePacket.getData());
				}

		//Connect to a peer server
		public String connectToPeerServer(String message, String ip, int port) throws UnknownHostException, IOException {
					Socket connectToPeerServer = new Socket(ip, port);

					OutputStream outToServer = connectToPeerServer.getOutputStream();
					DataOutputStream out = new DataOutputStream(outToServer);
					out.writeUTF(message);
					DataInputStream in = new DataInputStream(connectToPeerServer.getInputStream());
					message = in.readUTF();
					connectToPeerServer.close();
					return message;
				}


		// Upload file
		public void uploadData(int id, String file_Name) throws Exception {
			String statusCode;
			String message = "UPLOAD " + file_Name + " " + InetAddress.getLocalHost().getHostAddress() + " Padding";
			sendDataToServer(message, serverIPs[id], server_PortNumbers[id]); // Send upload to the server.
			message = receiveDataFromServer();
			Scanner scan = new Scanner(message);
			statusCode = scan.next();

			if (statusCode.equals("200")) {
				System.out.println("MESSAGE FROM SERVER -> File Succesfully Added To DHT");
			}
		}

		// Removing a Client from P2P
		public void exit() throws Exception {
			byte[] receiveData = new byte[1024];
			String status_Code;
			String message = "EXIT " + server_PortNumbers[0] + " " + server_PortNumbers[1] + " " + server_PortNumbers[2] + " " + server_PortNumbers[3] + " Padding";
			sendDataToServer(message, serverIPs[0], server_PortNumbers[0]); // Message to Server
			message = receiveDataFromServer();
			clientUDPSocket.close(); // Close UDP socket.
			Scanner scan = new Scanner(message);
			status_Code = scan.next();

			// If the status code returned by the server is 200 OK, then the client has been successfully removed.
			if (status_Code.equals("200")) {
				System.out.println("FROM SERVER -> All contents removed sucessfully");
			}
			System.exit(0); // Quit the application with exit code 0 (success).
		}

		// Query for a file
		public void query(int id, String file_Name) throws Exception {
			String clientToContactIP;
			String status_Code;
			String message = "QUERY " + file_Name + " Padding";
			sendDataToServer(message, serverIPs[id], server_PortNumbers[id]); // Message to Server
			message = receiveDataFromServer();
			Scanner scan = new Scanner(message);
			status_Code = scan.next();

			if (status_Code.equals("404")) {
				System.out.println("MESSAGE FROM SERVER -> Content not available");
			}

			else if (status_Code.equals("200")) {
				System.out.println("MESSAGE FROM SERVER -> Content is available, IP released ");
				scan = new Scanner(message);
				scan.next();
				clientToContactIP = scan.next(); // IP of the client who possesses the file

				// HTTP GET REQUEST
				String HTTPRequest = createHTTPRequest("GET", file_Name, "Close", InetAddress.getByName(clientToContactIP).getHostName(), "image", "en-ca");
				message = connectToPeerServer("OPEN " + file_Name, clientToContactIP, peer_ServerPort); // Connect to the server of the client who has the file.
				scan = new Scanner(message);
				status_Code = scan.next();
				int newPort = scan.nextInt();

				if (status_Code.equals("200")) {
					System.out.println("MESSAGE FROM PEER SERVER -> New Connection Opened On The Port " + newPort);
					System.out.println("**HTTP Request Sent to Server** BEGIN\n" + HTTPRequest + "**HTTP Request Sent to Server**END\n");
					connectToUniqueServer(file_Name, HTTPRequest, clientToContactIP, newPort);
				}
			}
		}


		// Connect to a Unique Server
		public void connectToUniqueServer(String fileName, String httpRequest, String ip, int port) throws UnknownHostException, IOException {
			Socket connectToUniqueServer = new Socket(ip, port);

			OutputStream outToServer = connectToUniqueServer.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(httpRequest);

			InputStream in = connectToUniqueServer.getInputStream();
			DataInputStream distream = new DataInputStream(in);
			int length = distream.readInt();
			byte[] data = new byte[length];
			if (length > 0) {
				distream.readFully(data);
			}
			connectToUniqueServer.close();
			String s = new String(data);
			Scanner scan = new Scanner(s);
			String responseStatus = scan.nextLine() + "\r\n";
			String temp;

			if (responseStatus.contains("HTTP/1.1 200 OK")) {
				responseStatus = getHTTPResponse(scan, responseStatus);
				File outputfile = new File(fileName + ".jpeg");
				int fileSize = data.length - responseStatus.getBytes().length;
				byte[] backToBytes = new byte[fileSize];

				for (int i = responseStatus.getBytes().length; i < data.length; i++) {
					backToBytes[i - responseStatus.getBytes().length] = data[i];
				}

				FileOutputStream fos = new FileOutputStream(outputfile);
				fos.write(backToBytes);
				fos.close(); // End the output stream
			}
			else if (responseStatus.contains("HTTP/1.1 400, Bad Request")) {
				responseStatus = getHTTPResponse(scan, responseStatus);
			}
			else if (responseStatus.contains("HTTP/1.1 404, Request Not Found")) {
				responseStatus = getHTTPResponse(scan, responseStatus);
			}
			else if (responseStatus.contains("HTTP/1.1 505, HTTP Version is not supported")) {
				responseStatus = getHTTPResponse(scan, responseStatus);
			}
			System.out.println("**HTTP Response Got From Server** START\n" + responseStatus + "**HTTP Response Got From Server**END\n");
		}


	}
}
