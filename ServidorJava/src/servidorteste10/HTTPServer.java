package servidorteste10;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;


public class HTTPServer implements Runnable{ 
	
	static final File WEB_ROOT = new File(".");
	static final String PAGINA_PRINCIPAL = "index.html";
	static final String ARQUIVO_NAO_ENCONTRADO = "404.html";
	static final String NAO_SUPORTADO = "not_supported.html";
	// porta de conexão
	static final int PORT = 6789;
	
	// log
	static final boolean log = true;
	
	// Client Connection via Socket Class
	private Socket conexao;
	
	public HTTPServer(Socket c) {
		conexao = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket conexaoServidor = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			// we listen until user halts server execution
			while (true) {
				HTTPServer servidor = new HTTPServer(conexaoServidor.accept());
				
				if (log) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}
				
				// create dedicated thread to manage the client connection
				Thread thread = new Thread(servidor);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// administra conexão com o cliente
		BufferedReader entrada = null;
		PrintWriter saida = null;
		BufferedOutputStream saidaDados = null;
		String arquivoRequisitado = null;
		
		try {
			// le os atributos de entrada
			entrada = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
			// captura os caracteres de saida do cliente
			saida = new PrintWriter(conexao.getOutputStream());
			// captura a saida binaria do cliente
			saidaDados = new BufferedOutputStream(conexao.getOutputStream());
			
			// get first line of the request from the client
			String ent = entrada.readLine();
			// we parse the request with a string tokenizer
			StringTokenizer st = new StringTokenizer(ent);
			String method = st.nextToken().toUpperCase(); // we get the HTTP method of the client
			// we get file requested
			arquivoRequisitado = st.nextToken().toLowerCase();
			
			// we support only GET and HEAD methods, we check
			if (!method.equals("GET")  &&  !method.equals("HEAD")) {
				if (log) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}
				
				// we return the not supported file to the client
				File file = new File(WEB_ROOT, NAO_SUPORTADO);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
					
				// we send HTTP Headers with data to client
				saida.println("HTTP/1.1 501 Not Implemented");
				saida.println("Server: Java HTTP Server: 1.0");
				saida.println("Date: " + new Date());
				saida.println("Content-type: " + contentMimeType);
				saida.println("Content-length: " + fileLength);
				saida.println(); // blank line between headers and content, very important !
				saida.flush(); // flush character output stream buffer
				// file
				saidaDados.write(fileData, 0, fileLength);
				saidaDados.flush();
				
			} else {
				// GET or HEAD method
				if (arquivoRequisitado.endsWith("/")) {
					arquivoRequisitado += PAGINA_PRINCIPAL;
				}
				
				File file = new File(WEB_ROOT, arquivoRequisitado);
				int fileLength = (int) file.length();
				String content = getContentType(arquivoRequisitado);
				
				if (method.equals("GET")) { // GET method so we return content
					byte[] fileData = readFileData(file, fileLength);
					
					// send HTTP Headers
					saida.println("HTTP/1.1 200 OK");
					saida.println("Server: Java HTTP Server: 1.0");
					saida.println("Date: " + new Date());
					saida.println("Content-type: " + content);
					saida.println("Content-length: " + fileLength);
					saida.println(); // blank line between headers and content, very important !
					saida.flush(); // flush character output stream buffer
					
					saidaDados.write(fileData, 0, fileLength);
					saidaDados.flush();
				}
				
				if (log) {
					System.out.println("File " + arquivoRequisitado + " of type " + content + " returned");
				}
				
			}
			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(saida, saidaDados, arquivoRequisitado);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
			
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				entrada.close();
				saida.close();
				saidaDados.close();
				conexao.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (log) {
				System.out.println("Connection closed.\n");
			}
		}
		
		
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
	
	// return supported MIME Types
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else
			return "text/plain";
}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, ARQUIVO_NAO_ENCONTRADO);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server: 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush(); // flush character output stream buffer
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
		if (log) {
			System.out.println("File " + fileRequested + " not found");
		}
	}
	
}
