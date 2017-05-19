import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class UdpClient {

	static Socket socket = null;
	static InputStream in = null;
	static OutputStream out = null;
	static byte[] ipPacket = new byte[20 + 4096];
	static byte[] magicNumber = new byte[4];
	static byte[] port = new byte[2];
	static byte[] pseudoheader = new byte[20 + 4096];

	public static void main(String[] args) throws Exception {
		socket = new Socket("codebank.xyz", 38005);
		in = socket.getInputStream();
		out = socket.getOutputStream();

		ipPacket[0] = (byte) 0x45; // version and HeaderLength 0010 0101
		ipPacket[6] = (byte) 0x40; // flags 0100 0000
		ipPacket[8] = (byte) 50; // TTL in seconds
		ipPacket[9] = (byte) 0x11; // UDP protocol number
		
		//server ip address
		byte[] ip = socket.getInetAddress().getAddress();
		ipPacket[16] = ip[0];
		ipPacket[17] = ip[1];
		ipPacket[18] = ip[2];
		ipPacket[19] = ip[3];
		
		//pseudoheader construction
		pseudoheader[4] = ip[0];
		pseudoheader[5] = ip[1];
		pseudoheader[6] = ip[2];
		pseudoheader[7] = ip[3];
		
		pseudoheader[9] = (byte) 0x11; //protocol
		
		//do handshake
		createPacket(4);
		ipPacket[20] = (byte) 0xDE;
		ipPacket[21] = (byte) 0xAD;
		ipPacket[22] = (byte) 0xBE;
		ipPacket[23] = (byte) 0xEF;
		
		System.out.println("Sending Handshake");
		out.write(ipPacket, 0, 20 + 4); //send the bytes
		in.read(magicNumber);
		System.out.printf("Server Response to Handshake : %x%x%x%x\n",magicNumber[0],magicNumber[1],magicNumber[2],magicNumber[3]);
		System.out.println();
		
		//setting ports
		in.read(port);
		pseudoheader[14] = port[0];
		pseudoheader[15] = port[1];
		ipPacket[22] = port[0];
		ipPacket[23] = port[1];
		//handshake done


		for (int i = 2; i <= 4096; i *= 2) {
			createPacket(i + 8);
			ipPacket[24] = (byte) ((8 + i) >> 8);
			ipPacket[25] = (byte) ((8 + i) & 0xff);
			setUDPChecksum(i);
			
			System.out.println("Sending packet length: " + i);
			out.write(ipPacket, 0, 20 + 8 + i); //send the bytes
			in.read(magicNumber);
			System.out.printf("Server Response: %x%x%x%x\n",magicNumber[0],magicNumber[1],magicNumber[2],magicNumber[3]);
			System.out.println();
		}
	}
	
	public static void setUDPChecksum(int size){
		//UDP Length
		pseudoheader[10] = (byte) ((20 + size) >> 8);
		pseudoheader[11] = (byte) ((20 + size) & 0xff);
		
		//Length
		pseudoheader[16] = (byte) ((8 + size) >> 8);
		pseudoheader[17] = (byte) ((8 + size) & 0xff);
		
		setRandomSourcePort();
		short checksum = checksum(pseudoheader,0,20 + size);
		ipPacket[10] = (byte) (checksum >> 8);//write the checksum
		ipPacket[11] = (byte) (checksum & 0xff);
	}
	
	public static void setRandomSourcePort(){
		Random r = new Random();
		byte[] randomport = new byte[2];
		r.nextBytes(randomport);
		pseudoheader[12] = randomport[0];
		pseudoheader[13] = randomport[1];
		
		ipPacket[20] = randomport[0];
		ipPacket[21] = randomport[1];
	}
	
	public static void createPacket(int size){
		ipPacket[2] = (byte) ((size + 20) >> 8); //total length upper
		ipPacket[3] = (byte) ((size + 20) & 0xff); //total length lower
		ipPacket[10] = 0;//zero out the checksum
		ipPacket[11] = 0;
		short checksum = checksum(ipPacket,0,20);
		ipPacket[10] = (byte) (checksum >> 8);//write the checksum
		ipPacket[11] = (byte) (checksum & 0xff);
	}

	// perform checksum
	public static short checksum(byte[] b,int start, int size) {
		long sum = 0;
		for (int i = start; i < size; i += 2) {
			sum += (b[i] << 8) + (b[i + 1] & 0xff);
			if ((sum & 0xFFFF0000) != 0) {

				sum &= 0xFFFF;
				sum++;
			}
		}
		return (short) ~(sum & 0xFFFF);
	}
}