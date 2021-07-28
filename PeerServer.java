import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.Charset;
import java.text.*;


// Client Server
public class PeerServer {
	final int statusCode200 = 200; // OK Response
	final int statusCode400 = 400; // Bad Request
	final int statusCode404 = 404; // Not Found
	final int statusCode505 = 505; // HTTP Version is not Supported

	int initPort; // Peer Server initial port
	ServerSocket pServerTCPSocket; // TCP socket for Peer Server
	public static ArrayList<UniqueTCP> peerCUniqueList = new ArrayList<UniqueTCP>();
	Thread peerServerThread; // Main Thread

	//Constructor
	public PeerServer(int initial_port) {
		initPort = initial_port;
		try {
			// Create TCP Socket
			pServerTCPSocket = new ServerSocket(initPort);
			peerServerThread = new Thread(mainTCPRunnable);
			peerServerThread.start(); // Start Thread for Peer Server
		}
		catch (IOException e) {
			System.out.println("Port is not available");
		}
	}
	
	// Find Available UDP Port
	public int findAvailableUDPPort() {
		int port = 0; // Initial Port = 0
		int portFind = initPort;
		boolean ok = false;
		while (ok == false) {

			// Try different ports or increment if unavailable
			try {
				ServerSocket tryPort = new ServerSocket(portFind);
				ok = true;
				tryPort.close();
				break;
			}
			catch (Exception e) {
				portFind++;
			}
		}
		port = portFind;
		return port;
	}
	
	


	// Main Thread
	Runnable mainTCPRunnable = new Runnable() {
		public void run() {
			while (true) {
				String message;

				try {
					Socket getClient = pServerTCPSocket.accept(); // Socket to accept information from the client.
					DataInputStream in = new DataInputStream(getClient.getInputStream());
					message = in.readUTF();
					Scanner scan = new Scanner(message);
					scan.next();

					// Set port to UDP
					int port1 = findAvailableUDPPort();
					peerCUniqueList.add(new UniqueTCP(port1));
					message = statusCode200 + " " + port1;

					DataOutputStream out = new DataOutputStream(getClient.getOutputStream());
					out.writeUTF(message);
					getClient.close(); // Close connection.
				}
				catch (IOException e) {
					System.out.println("Error connecting to main socket");
				}
			}
		}
	};

	

	// Unique TCP connection
	public class UniqueTCP {
		final int statusCode200 = 200; // OK Request
		final int statusCode400 = 400; // Bad Request
		final int statusCode404 = 404; // Not Found Message
		final int statusCode505 = 505; // HTTP Version is not supported

		ServerSocket uniqueTCPSocket; // Peer server's TCP socket.
		Thread TCPRunnableThread;	  // Peer server's main thread.

		// Constructor
		public UniqueTCP(int port) {
			try {
				uniqueTCPSocket = new ServerSocket(port); // New TCP socket.
				TCPRunnableThread = new Thread(TCPRunnable1);
				TCPRunnableThread.start(); // Main Thread begins
			}
			catch (IOException e) {
				System.out.println("Port is not Available");
			}
		}

		// Running Main Thread
		Runnable TCPRunnable1 = new Runnable() {
			public void run() {
				try {
					byte[] finalBytesArray = null;
					String message = ""; 		// Message.
					String fileName = ""; 		// Name of the file.
					String request = ""; 		// Request message.
					String httpVersion = ""; 	// HTTP version
					String HTTPResponse = ""; 	// HTTP response message.
					String connection = "";   	// Connection Type
					String contentType = "";	// File Type
					String timeString = getCurrentTime();	// Time.
					Socket socket = uniqueTCPSocket.accept();

					DataInputStream input = new DataInputStream(socket.getInputStream());
					message = input.readUTF();

					Scanner scan = new Scanner(message);
					request = scan.next();

					// Request is "GET".
					if (request.equals("GET")) {
						fileName = scan.next();
						httpVersion = scan.next();
						connection = "Close";
						contentType = "image";

						// HTTP version is 1.1
						if (httpVersion.equals("HTTP/1.1")) {
							fileName = fileName.substring(1);
							File f = new File(fileName);
							try {
								String FileName = "";
								String FileName2 = "";
								FileName = fileName.substring(0, fileName.indexOf(".jpeg"));
								FileName2 = fileName.substring(0, fileName.indexOf(".jpeg"));
								FileName += "-----------.jpeg";
								File isFileNameBad = new File(FileName);
								isFileNameBad.createNewFile();
								isFileNameBad.delete();
								File f2 = new File(FileName2 + ".jpg");

								if (f2.exists())
									f = new File(FileName2 + ".jpg");

								// HTTP 200
								if (f.exists()) {
									double fileSizeBytes = f.length();
									String lastModified = getFileModifiedTime(f);
									HTTPResponse = createHTTPResponse(statusCode200, timeString, lastModified, "bytes", Integer.toString((int) fileSizeBytes), connection, contentType);
									byte[] httpToBytes = HTTPResponse.getBytes(Charset.forName("UTF-8"));
									FileInputStream fileInputStream = null;
									byte[] fileToBytes = new byte[(int) f.length()];

									fileInputStream = new FileInputStream(f);
									fileInputStream.read(fileToBytes);
									fileInputStream.close();

									finalBytesArray = new byte[httpToBytes.length + fileToBytes.length];
									System.arraycopy(httpToBytes, 0, finalBytesArray, 0, httpToBytes.length);
									System.arraycopy(fileToBytes, 0, finalBytesArray, httpToBytes.length, fileToBytes.length);
								}

								// HTTP 404
								else {
									HTTPResponse = createHTTPResponse(statusCode404, timeString, null, null, null, connection, null);
									finalBytesArray = HTTPResponse.getBytes(Charset.forName("UTF-8"));
								}
							}

							// HTTP 400
							catch (Exception e) {
								HTTPResponse = createHTTPResponse(statusCode400, timeString, null, null, null, connection, null);
								finalBytesArray = HTTPResponse.getBytes(Charset.forName("UTF-8"));
							}
						}

						// HTTP 505
						else {
							HTTPResponse = createHTTPResponse(statusCode505, timeString, null, null, null, connection, null);
							finalBytesArray = HTTPResponse.getBytes(Charset.forName("UTF-8"));
						}
					}

					OutputStream out = socket.getOutputStream();
					DataOutputStream dos = new DataOutputStream(out);
					dos.writeInt(finalBytesArray.length);
					dos.write(finalBytesArray, 0, finalBytesArray.length);
					socket.close();
					uniqueTCPSocket.close();

					for (int i = 0; i < PeerServer.peerCUniqueList.size(); i++) {
						if (PeerServer.peerCUniqueList.get(i).equals(this)) {
							PeerServer.peerCUniqueList.remove(i);
							TCPRunnableThread.stop();
							break;
						}
					}
				}
				catch (Exception e) {
				}
			}
		};
		// Present Time
	public String getCurrentTime() {
				Date cdate = new Date();
				Scanner scan = new Scanner(cdate.toString());
				String dName = scan.next();
				String month = scan.next();
				String dNumber = scan.next();
				DateFormat timeFormat = new SimpleDateFormat("yyyy HH:mm:ss");
				Date time = new Date();
				String timeString = dName + ", " + dNumber + " " + month + " " + timeFormat.format(time) + " GMT";
				return timeString;
			}

	// Modified Time
	public String getFileModifiedTime(File f) {
				Date date = new Date(f.lastModified());
				Scanner scan = new Scanner(date.toString());
				String dName = scan.next();
				String month = scan.next();
				String dNumber = scan.next();
				DateFormat timeFormat = new SimpleDateFormat("yyyy HH:mm:ss");
				Date time = new Date(f.lastModified());
				String timeString = dName + ", " + dNumber + " " + month + " " + timeFormat.format(time) + " GMT";
				return timeString;
			}

// HTTP Response
	public String createHTTPResponse(int code, String currentDate, String fileModifiedDate, String acceptARange, String length, String connection, String content_Type) {
				String rep = "";

				// If the response code is 200 OK.
				if (code == statusCode200) {
					rep += "HTTP/1.1 " + code + " " + "OK\r\n";
					rep += "Connection: " + connection + "\r\n";
					rep += "Date: " + currentDate + "\r\n";
					rep += "Last-Modified: " + fileModifiedDate + "\r\n";
					rep += "Accept-Ranges: " + acceptARange + "\r\n";
					rep += "Content-Length: " + length + "\r\n";
					rep += "Content-Type: " + content_Type + "\r\n\r\n";
				}
				else {
					// HTTP 400
					if (code == statusCode400) {
						rep += "HTTP/1.1 " + code + " " + "Bad Request\r\n";
					}
					// HTTP 404
					else if (code == statusCode404) {
						rep += "HTTP/1.1 " + code + " " + "Not Found\r\n";
					}
					// HTTP 505
					else if (code == statusCode505) {
						rep += "HTTP/1.1 " + code + " "
								+ "HTTP Version Not Supported\r\n";
					}
					rep += "Connection: " + connection + "\r\n";
					rep += "Date: " + currentDate + "\r\n\r\n";
				}
				return rep;
			}



	}
}
