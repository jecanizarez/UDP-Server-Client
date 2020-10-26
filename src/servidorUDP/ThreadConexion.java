package servidorUDP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.nio.file.Files;

public class ThreadConexion extends Thread {

	public final static int MESSAGE_SIZE = 32768;

	public String archivo;

	private DatagramPacket packet = null;
	
	private DatagramSocket socket = null; 

	private BufferedWriter writer;

	private int cliente;

	public ThreadConexion(DatagramSocket pSocket, DatagramPacket pPacket, String arch, BufferedWriter writer, int cliente) {
		packet = pPacket;
		socket = pSocket;
		this.writer = writer;
		this.cliente = cliente;
		archivo = arch;
	}

	public void run() {

		try {
			Thread.sleep(10L);
			enviar();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				System.out.println("Error enviando el archivo "+ e.getMessage());
				writer.write("Hubo un error con el envio del archivo");
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				System.out.println("Error escribiendo el error "+ ex.getMessage());
			}
		}

	}

	public void enviar() {

		FileInputStream fis = null;
		BufferedInputStream bis = null;

		
		DatagramPacket outdata = null; 
		byte[] senddata = null;
		
		OutputStream os = null;

		try {
			File myFile = new File(archivo);
			byte[] myByteArray = Files.readAllBytes(myFile.toPath());

			
			fis = new FileInputStream(myFile);
			bis = new BufferedInputStream(fis);

			byte[] hash = new byte[61440];
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			hash = md.digest(myByteArray);
			StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < hash.length; i++) {
	            sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
	        }
	        //Enviar hash al cliente 
	        String hashEnviar = sb.toString();
	        senddata = hashEnviar.getBytes();
	        outdata  = new DatagramPacket(senddata, senddata.length,packet.getAddress(),packet.getPort());
			socket.send(outdata);
			//
			
			//Calcualo el Numero de paquetes necesarios para enviar el archivo
			double numPackets = Math.ceil(myByteArray.length / MESSAGE_SIZE);
			byte[] snd = new byte[1024];
			String packets = String.valueOf(numPackets); 
			snd = packets.getBytes();
			outdata = new DatagramPacket(snd, snd.length, packet.getAddress(), packet.getPort());
			//Envio al cliente el numero de paquetes que se van a enviar
			socket.send(outdata);
			//
			System.out.println("Enviando (" + myByteArray.length + " bytes)");
			for( int i = 0; i < numPackets-1; i++) {
				byte[] b = new byte[MESSAGE_SIZE];
				bis.read(b,0,b.length);
				outdata = new DatagramPacket(b,b.length,packet.getAddress(),packet.getPort());
				socket.send(outdata);
				Thread.yield();
			}
			byte[] b = new byte[ (myByteArray.length - ((int)numPackets-1)*MESSAGE_SIZE)];
			bis.read(b,0,b.length);
			outdata = new DatagramPacket(b,b.length,packet.getAddress(),packet.getPort());
			socket.send(outdata);
			
			System.out.println("Ya se enviaron todos los paquetes");
			String cadena = "Se enviaron " + numPackets + " paquetes al cliente " + cliente + " para un total de " + numPackets*MESSAGE_SIZE + " bytes.";
			writer.write(cadena);
			writer.newLine();
			writer.flush();

			writer.write("Cada paquete con un tamanio de " + MESSAGE_SIZE + " bytes");
			writer.newLine();
			writer.flush();
			byte[] receivedata = new byte[1000];
			DatagramPacket indata = new DatagramPacket(receivedata, receivedata.length);
			socket.receive(indata);
			String respuesta = new String(indata.getData());
			if(respuesta == "1") {
				respuesta = "enviado exitosamente";
				
			}
			else {
				respuesta = "no fue enviado exitosamente, se perdio  data o fue modificado, ";
			}
			
			System.out.println("Archivo "+ respuesta + " al cliente "+ cliente );
			writer.write("Archivo "+ respuesta +" al cliente " + cliente);
			writer.newLine();
			writer.flush();

			writer.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			try {
				System.out.println("Error enviando el archivo "+ e.getMessage());
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				System.out.println("Error escribiendo el error "+ ex.getMessage());
			}
		}
	}
}
