
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/***********************************************************************
 * The TcpClient1 class connects a user to a server based on user input for the
 * address and port. A secondary connection is set up for file transfers. The
 * client can enter commands via console to interact with the server
 * 
 * @author Cody West
 * @version Project 1 TCP
 * @date 9/22/2017
 **********************************************************************/
public class TcpClient1 {

	public static void main(String args[]) throws IOException {
		
		/** Sockets for the main connection and file transfer */
		Socket s1 = null;
		Socket s2 = null;
		
		/** The command entered by the user */
		String line = null;
		
		/** String for getting lines from server */
		String response;
		
		/** Holds either "-1" for no file or size of the incoming file */
		String fileSizeS;
		
		/** Streams and readers for data transfer handling */
		BufferedReader br = null;
		BufferedReader is = null;
		InputStream inputS = null;
		PrintWriter os = null;
		FileOutputStream fileStream = null;
		BufferedOutputStream outputBRS = null;
		
		/** Console for various input from user */
		Console cons = System.console();
		
		/** user defined ip address */
		String ip;
		
		/** File to be received from server, takes name from local command */
		File file;
		
		/** File size (in bytes) of file to be received */
		long fileSize;
		
		/** int used for counting bytes read from a stream */
		int byteNumber;
		
		/** int used for determine when file is transfered */
		int byteCounter;
		
		/** ints that hold user input ports */
		int port;
		int portF;

		// Loop until client is successfully connected
		while (true) {
			try {
				// Set up main socket,related streams and readers
				ip = cons.readLine("Enter IP Address (x.x.x.x,args): ");
				String portS = cons.readLine("Enter port number: ");
				port = Integer.parseInt(portS);
				s1 = new Socket(ip, port);
				br = new BufferedReader(new InputStreamReader(System.in));
				is = new BufferedReader(
						new InputStreamReader(s1.getInputStream()));
				os = new PrintWriter(s1.getOutputStream());
				System.out.println("Connecting to server...");
				// On successful connection leave loop
				break;

			} catch (IOException e) {
				System.out.println("No server found");
				continue;
			}
		} // End of set up loop

		System.out.println("Client Address : " + ip);
		System.out.println("Enter ls for directory, exit to disconnect, "
				+ "or file to download");

		try {
			// Initial default command;
			line = "0";

			// Main loop keeping client connected to server, use "exit" to quit
			while (true) {
				System.out.print("Command: ");
				line = br.readLine();
				// No or invalid command entered
				if (line.equals("") || line.equals(null)) {
					System.out.println("Enter valid command");
					continue;
				}

				// User asks for file directory
				else if (line.equals("ls")) {
					os.println(line);
					os.flush();
					response = is.readLine();
					System.out.println("Directory List : " + response);
					continue;
				}

				// User wants to leave server
				else if (line.equalsIgnoreCase("exit")) {
					os.println(line);
					os.flush();
					break;
				}

				// User wants a file (default)
				else {
					while (true) {
						try {
							// Set up file socket connection
							String portFS = cons
									.readLine("Enter port number: ");
							portF = Integer.parseInt(portFS);
							if(portF==port){
								System.out.print("File socket port cannot be" +
							" the same as server port");
							}
							s2 = new Socket(ip, portF);
							inputS = s2.getInputStream();
							break;
						}

						catch (Exception E) {
							System.out.println("No file connection found");
							continue;
						}
					}

					try {
						// Get file size in bytes from server
						os.println(line);
						os.flush();
						fileSizeS = is.readLine();
						// -1 if the file doesn't exist
						if (fileSizeS.equals("-1")) {
							System.out.println("There is no such file");
							continue;
						}

						// Prepare file, streams, and readers for file transfer
						// Also lists directory where files are saved
						file = new File("/home/westco/net/client/" + line);
						fileStream = new FileOutputStream(file);
						outputBRS = new BufferedOutputStream(fileStream);
						fileSize = Long.parseLong(fileSizeS);
						byte[] fileBytes = new byte[(int) fileSize];
						// Receive first file bytes
						byteNumber = inputS.read(fileBytes, 0,
								fileBytes.length);
						byteCounter = byteNumber;
						// Get rest of file bytes
						do {
							byteNumber = inputS.read(fileBytes, byteCounter,
									fileBytes.length - byteCounter);
							if (byteNumber >= 0) {
								byteCounter += byteNumber;
							}
						} while (byteCounter < fileSize);

						// Write bytes to file
						outputBRS.write(fileBytes, 0, byteCounter);
						outputBRS.flush();
						System.out.println("Recieved: " + line);

						// Close down file socket connection
						try {
							if (inputS != null)
								inputS.close();
							if (fileStream != null)
								fileStream.close();
							if (s2 != null)
								s2.close();
						}

						catch (Exception e) {
							System.out.println("Issue closing file connection");
						}
						continue;

					}

					// General file error handling
					catch (Exception e) {
						System.out.println("File downloading error, try again");
						continue;
					}
				}
			}
		}

		// Catch issue with original socket
		catch (IOException e) {
			e.printStackTrace();
			System.out.println("Socket read Error");
		}

		// Close down main server connection
		finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
			if (br != null)
				br.close();
			if (s1 != null)
				s1.close();
			System.out.println("Connection Closed");

		}

	}

}
