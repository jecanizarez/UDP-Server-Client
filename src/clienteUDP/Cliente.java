package clienteUDP;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.nio.file.Files;
import java.net.DatagramPacket; 
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress; 

public class Cliente {

	private static final int MESSAGE_SIZE = 32768;
	private static final int PUERTO = 3001; 
	private static final String DIR_DESCARGA = "data/descargas/";
	public final static String UBICACION_LOG = "data/logs/";	
	private static BufferedWriter writer;
	public final static int BUFFER_SIZE = 64000;
	

	// método principal de la clase
	public static void main(String argv[]) {

		Scanner lectorConsola = new Scanner(System.in);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		DatagramSocket socket;
		DatagramPacket outdata; 
		DatagramPacket indata;
		byte [] senddata, receivedata; 

		try {
			//Se crea el log especifico para la prueba. 
			String time = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(Calendar.getInstance().getTime());
			File logFile = new File(UBICACION_LOG + time + ".txt");
			writer = new BufferedWriter(new FileWriter(logFile));
			System.out.println("Escriba la direccion ip del servidor");
			String direccion = lectorConsola.next(); 
			//Se crea el socket
			socket = new DatagramSocket();
			socket.setReceiveBufferSize(BUFFER_SIZE);
			socket.setSendBufferSize(BUFFER_SIZE);
			System.out.println("Socket UDP creado");
			
			String mensaje = "EL BICHOOO"; 
			senddata = mensaje.getBytes();
			InetSocketAddress address = new InetSocketAddress(direccion, PUERTO);

			outdata = new DatagramPacket(senddata, senddata.length,address.getAddress(),PUERTO);
			socket.send(outdata);
			System.out.println("Enviando mensaje al servidor para que nos reconozca");
			String nombreArch = "";
			
			receivedata = new byte[1000]; 
			indata = new DatagramPacket(receivedata, receivedata.length); 
			socket.receive(indata);
			nombreArch = new String(indata.getData());
			nombreArch = nombreArch.trim();
			System.out.println("Se va a descargar el archivo: " + nombreArch);
			Cliente cli = new Cliente();
			cli.descargar(socket, nombreArch,direccion);

		}

		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public void descargar(DatagramSocket socket, String nombreArch, String direccion) {
		int bytesRead = 0;
		int current = 0;
		
		FileOutputStream fos;
		BufferedOutputStream bos;
		
		DatagramPacket outdata; 
		DatagramPacket indata;
		byte[] receivedata, senddata;
		
		try {


			String timeLog = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime());
			writer.write("Fecha y hora: " + timeLog);
			writer.newLine();
			writer.flush();

			// Recibir Hash
			System.out.println("Se va a recibir el hash");
			receivedata = new byte [1000];
			indata = new DatagramPacket(receivedata, receivedata.length);
			socket.receive(indata);
			String hashRecibido = new String(indata.getData());
			System.out.println("Se recibio el hash ");
			writer.write("Se van a recibir paquetes de " + MESSAGE_SIZE + " bytes.");
			writer.newLine();
			writer.flush();

			// Comenzar a recibir el archivo
			byte[] mybytearray = new byte[700000000];
			//InputStream is = socket.getInputStream();
			//Recibo el numero de paquetes que se van a enviar por parte del servidor 
			receivedata = new byte[1000];
			indata = new DatagramPacket(receivedata, receivedata.length);
			socket.receive(indata);
			String packetsString = new String(indata.getData());
			double numPackets = Double.parseDouble(packetsString);
			fos = new FileOutputStream(DIR_DESCARGA + nombreArch);
			bos = new BufferedOutputStream(fos);
			
			System.out.println("Recibiendo el archivo");


			// Iniciar medición tiempo descarga de un archivo.
			socket.setSoTimeout(500);
			long startTime = System.currentTimeMillis();
			

			int bytesReceived = 0;
			int numPaquetes = 0;
			int numPaquetesLoss = 0;
			receivedata = new byte [MESSAGE_SIZE];
			for(int i = 0; i < numPackets; i++) {
				indata = new DatagramPacket(receivedata, receivedata.length);
				try {
					socket.receive(indata);
					bos.write(indata.getData());
					bytesReceived += indata.getLength();
					receivedata = new byte [MESSAGE_SIZE];
				}
				catch (Exception e) {
					numPaquetesLoss = (int) (numPackets - (i+1));
					break;
				}
			}
			long endTime = System.currentTimeMillis();
			System.out.println("La descarga tomó " + (endTime - startTime) + " milisegundos");
			
			
			writer.write("El archivo se entrego, peso: (" + current + " bytes leidos)");
			writer.newLine();
			writer.flush();

			writer.write("Se recibieron " + numPaquetes + " paquetes.");
			writer.newLine();
			writer.flush();

			writer.write("La descarga tomo " + (endTime - startTime) + " milisegundos");
			writer.newLine();
			writer.flush();

			

			// Verificación del hash
			File myFile = new File(DIR_DESCARGA + nombreArch);
			byte[] myByteArray = Files.readAllBytes(myFile.toPath());
			byte[] hashSacado = new byte[61440];
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			hashSacado = md.digest(myByteArray);
			StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < hashSacado.length; i++) {
	          sb.append(Integer.toString((hashSacado[i] & 0xff) + 0x100, 16).substring(1));
	        }
	         
	        String hashGenerado = sb.toString();
	        String estado = "";
			if (hashRecibido.equals(hashGenerado)) {
				System.out.println("Archivo verificado, todo en orden");
				writer.write("El archivo no fue modificado");
				writer.newLine();
				writer.flush();
			} else {
				System.out.println("El archivo ha sido modificado! o se perdió data, Paquetes perdidos: " + numPaquetesLoss);
				estado = "F";
				writer.write("El archivo fue modificado");
				writer.newLine();
				writer.flush();
			}
			
			InetSocketAddress address = new InetSocketAddress(direccion, PUERTO);
			byte [] send = estado.getBytes(); 
			outdata = new DatagramPacket(send, send.length, address.getAddress(), PUERTO);
			socket.send(outdata);
			fos.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			try {
				writer.write("Hubo un error con el envío");
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				System.out.println("Ocurrio un error: " + ex.getMessage());
			}
		}
	}

}