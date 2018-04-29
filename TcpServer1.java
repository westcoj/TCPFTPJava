
import java.io.*;
import java.net.*;

/***********************************************************************
 * The TcpServer1 class starts up a server on a requested socket with a
 * user defined port. A secondary file socket is set up to 
 * handle file transfers. Any client connections are refereed to a thread
 * method to handle multiple clients
 *  
 * @author Cody West
 * @version Project 1 TCP
 * @date 9/22/2017
 **********************************************************************/
public class TcpServer1 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		
		/** Socket set up for client to connect to and server to run on */
		Socket socket = null;
		ServerSocket ssc = null;
		
		/** Second server socket to handle file transfers */
		ServerSocket sscFile = null;
		
		/** Two ints to determine ports for sockets, defaults are listed */
		int port = 100;
		int filePort = 110;

		// Server start up loop, continues until it succeeds (unless exit is
		// specified)
		while (true) {
			Console cons = System.console();
			String m = cons.readLine("Enter port number: ");
			if (m.matches("[0-9]+")) {
				port = Integer.parseInt(m);
			} else if (m.equals("exit")) {
				System.exit(0);
			} else {
				continue;
			}

			try {
				ssc = new ServerSocket(port);
				break;
			} catch (IOException e) {
				System.out.println("Unavailable Port");
				continue;
			}
		}
		// A similar loop to set up the port for the file server socket
		while (true) {
			Console cons = System.console();
			String m = cons.readLine("Enter file port number: ");
			if (m.matches("[0-9]+")) {
				filePort = Integer.parseInt(m);
			} else if (m.equals("exit")) {
				System.exit(0);
			} else {
				continue;
			}

			try {
				sscFile = new ServerSocket(filePort);
				break;
			} catch (IOException e) {
				System.out.println("Unavailable Port");
				continue;
			}
		}

		// Main server loop, doesn't end. Handles multiple clients.
		while (true) {
			try {
				socket = ssc.accept();
				System.out.println("Client Connected!");
				ClientHandler ch = new ClientHandler(socket, sscFile);
				// Thread start
				ch.start();
			} catch (Exception e) {
				System.out.println("OH no , client down, client down!");
				continue;
			}
		} // End main server loop

	}// Main bracket
}// tcpServer1 class

/*****************************************************************************
 * ClientHandler class handles client connection threads and how they
 * interact with the server. The client can request a directory list, a
 * file, or to exit the server
 *  
 * @author Cody West
 * @version Project 1 TCP
 * @date 9/22/2017
 ****************************************************************************/
class ClientHandler extends Thread {
	
	/** Sockets for handling main connection and secondary file connection */
	Socket sck;
	ServerSocket sckFile;
	Socket clientH;
	Socket fileSocket;
	
	/** Streams and readers for handling data transfer */
	BufferedInputStream inputS;
	BufferedReader bufferR;
	BufferedOutputStream outputS;
	FileInputStream fileSt;
	PrintWriter printW;
	
	/** command received from client which decides server action */
	String command;
	
	/** File placeholder for when client requests files */
	File file;
	
	/** File array of server directory */
	File[] fileList;
	
	/** Byte array that will hold bytes of file to send */
	byte[] bytes;

	/*************************************************************************
	 * Constructor creates client thread with proper sockets 
	 * @param sck socket the client connects on
	 * @param sckFile server socket for file handling
	 ************************************************************************/
	public ClientHandler(Socket sck, ServerSocket sckFile) {
		this.sck = sck;
		this.sckFile = sckFile;
	}

	/**************************************************************************
	 * The start method for the client thread. allows for multiple clients on
	 * server
	 *************************************************************************/
	public void run() {
		try {
			System.out.println("A new thread is here");
			printW = new PrintWriter(sck.getOutputStream());
			bufferR = new BufferedReader(
					new InputStreamReader(sck.getInputStream()));
		} catch (IOException e) {
			System.out.println("Thread had an IO error");
		}

		try {

			command = bufferR.readLine();

			// Loop for keeping client connection up until satisfied
			// Methods handle requests to keep start method cleaner.
			while (true) {
				System.out.println("Command from client: " + command);

				// Client wants file directory
				if (command.equals("ls")) {
					String fileDir = this.getFiles();
					// System.out.println(fileDir);
					printW.println(fileDir);
					printW.flush();
					// command = bufferR.readLine();
				}

				// Client wants to quit connection
				else if (command.equalsIgnoreCase("exit")) {
					System.out.println("Client exiting server");
					break;
				}

				// Client has requested a file (default request)
				else {
					System.out.println("Client wants: " + command);
					this.fileRetrieval(command);
				}

				command = bufferR.readLine();
			} // Client connection loop, kept up with commands from client
		}

		// General error capture
		catch (Exception IOException) {
			System.out.println("IO error with client");
		}

		// Closing connections when client calls exit
		finally {
			try {
				System.out.println("Connection Closing..");
				if (inputS != null) {
					inputS.close();
					System.out.println(" Socket Input Stream Closed");
				}

				if (printW != null) {
					printW.close();
					System.out.println("Socket Out Closed");
				}
				if (sck != null) {
					sck.close();
					System.out.println("Socket Closed");
				}

			} catch (IOException ie) {
				System.out.println("Socket Close Error");
			}
		}
	}// Thread death

	/*************************************************************************
	 * Retrieves the list of files from the server directory
	 * @return list of filenames
	 *************************************************************************/
	public String getFiles() {
		// Set server directory, change to update directory
		fileList = new File("/home/westco/net/server/").listFiles();
		System.out.println(fileList);

		// Get directory list loop
		String fileNames = "";
		for (File file : fileList) {
			if (file.isFile()) {
				fileNames = fileNames + file.getName() + "   ";
			}
		}
		return fileNames;
	}

	/**************************************************************************
	 * Handles client requests for files, either retrieving the the file
	 * @param fileName command given by client which translates to name of
	 * requested file
	 *************************************************************************/
	public void fileRetrieval(String fileName) {
		//Prepare file, byte array, and fileSize
		File file = null;
		int fileSize = 0;
		bytes = null;

		// File preparation
		try {
			file = new File("/home/westco/net/server/" + fileName);
			fileSize = (int) file.length();
			// String fileSizeS = String.valueOf(fileSize);
			bytes = new byte[(int) file.length()];
		}

		catch (Exception e) {
			System.out.print("File not found");
			printW.println("-1");
			printW.flush();
			return;
		}

		// Setting up second connection and stream
		try {
			clientH = this.sckFile.accept();
			outputS = new BufferedOutputStream(clientH.getOutputStream());
			System.out.println("Client File Handler Connected");
		}

		catch (Exception e) {
			System.out.println("Error connecting file port");
		}

		// Actual sending of file, closing connection once done
		try {
			fileSt = new FileInputStream(file);
			inputS = new BufferedInputStream(fileSt);
			inputS.read(bytes, 0, bytes.length);
			System.out.println(bytes.length);
			System.out.println(
					"Sending " + file + "(" + bytes.length + " bytes)");
			printW.println(file.length());
			printW.flush();
			System.out.println(bytes.length);
			outputS.write(bytes, 0, bytes.length);
			outputS.flush();
			System.out.println("Done.");

		} catch (Exception e) {
			printW.println("-1");
			printW.flush();
			System.out.println("File not found");
		}
		
		//Close connections
		try {
			if (inputS != null)
				inputS.close();
			if (fileSt != null)
				fileSt.close();
			if (outputS != null)
				outputS.close();
			if (clientH != null)
				clientH.close();
		}
		
		//Catch any issues closing sockets
		catch (Exception e) {
			System.out.println("Issue closing second socket");
		}

	}

}
