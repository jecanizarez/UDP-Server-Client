package servidorUDP;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

public class Servidor {

	public final static String ARCHIVO1 = "100.mp4";
	public final static String ARCHIVO2 = "250.mp4";
	public final static int PUERTO = 3001;
	public final static String CARPETA_LOG = "data/logs/";
	public final static String CARPETA_ARCHIVOS = "data/files/";
	public final static int BUFFER_SIZE = 64000;

	public static BufferedWriter writer;

	public static void main(String argv[]) {

		Scanner consola = new Scanner(System.in);
		DatagramSocket socket;
		DatagramPacket outdata = null; 
		DatagramPacket indata = null; 
		byte[] senddata, receivedata; 

		try {	
			String time = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(Calendar.getInstance().getTime());
			File logFile = new File(CARPETA_LOG + time + ".txt");
			writer = new BufferedWriter(new FileWriter(logFile));
			String timeLog = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime());
			writer.write("Fecha y hora: " + timeLog);
			writer.newLine();
			writer.flush();

			socket = new DatagramSocket(PUERTO);
			socket.setSendBufferSize(BUFFER_SIZE);
			System.out.println("Ingrese el numero de conexiones que se van a realizar: ");
			int conexiones = consola.nextInt();
		
			writer.write("Numero de conexiones: " + conexiones);
			writer.newLine();
			writer.flush();

			System.out.println("Ingrese el numero del archivo que quiere enviar: (1) para el de 100 MiB o (2) para el de 250 MiB");
			int numeroArchivo = consola.nextInt();
			consola.close();
			String arch = CARPETA_ARCHIVOS;
			if (numeroArchivo == 1) {
				writer.write("Se va a realizar el envio del archivo " + ARCHIVO1);
				arch+=ARCHIVO1;
			}
			else {
				writer.write("Se va a realizar el envio del archivo" + ARCHIVO2);
				arch+=ARCHIVO2;
			}
			writer.newLine();
			writer.flush();

			DatagramPacket[] socketClientes = new DatagramPacket[conexiones];
			int clientes = 0;
			receivedata = new byte[1000]; 
			senddata = new byte[1000]; 
			System.out.println("Esperando clientes");
			while (clientes < conexiones) {
				try {
		
					indata = new DatagramPacket(receivedata, receivedata.length); 
					socket.receive(indata);
					socketClientes[clientes] = indata;
					clientes++;
					System.out.println("Se ha conectado el cliente" + clientes + " y esta esperando el envio del archivo");
	
					writer.write("Se ha conectado el cliente " + clientes);
					writer.newLine();
					writer.flush();
				} catch (Exception e) {
					System.out.println("Hubo un problema con algun cliente");
				}
			}
			System.out.println("Enviando archivos a los clientes");
			for (int i = 0; i < socketClientes.length; i++) {
				ThreadConexion thread = new ThreadConexion(socket, socketClientes[i], arch, writer, (i + 1));
				thread.start();
			}
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
			try {
				writer.write("Hubo un error con el envio: "+ e.getMessage());
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}
	}
}