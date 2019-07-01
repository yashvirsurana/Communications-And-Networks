/* Yashvir Surana s1368177 */

// GO_BACK_N RECEIVER
/**
 * Created by s1368177 on 20/03/16.
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver2a {

    /*-- Declarations --*/
    private static int port = -1;
    private static java.io.RandomAccessFile file = null;
    private static boolean lastPacket = false;
    private static int fileLength = 0;
    private static DatagramPacket rcvPacket = null;
    private static DatagramPacket ackPacket = null;


    public static void main(String[] args) throws IOException {
        // Parse Arguments

        port = Integer.parseInt(args[0]);

        // filename is in args[1]. Open it for writing

        file = new java.io.RandomAccessFile(args[1], "rw");
        file.setLength(0); // Delete any existing data

		// Open socket on port and wait for data
        DatagramSocket rcvSocket = new DatagramSocket(port);

        byte[] receiveData = new byte[1024 + 3];    // The full Byte Array of the UDP Packet
        byte[] ackData = new byte[2];               // the UDP ACK Byte Array - 2 bytes wide
        int sequenceRcv = 0;                        // above, converted
        int sequenceExp = 0;                        // expected sequence
        int lastSequence = -1;                      // last received packet sequence unubmer
        byte flags = 0;                             // byte flag - 0x1

		// Begin waiting
        rcvPacket = new DatagramPacket(receiveData, receiveData.length);
        System.out.printf("Waiting for first packet...\r");
        while (!lastPacket) { // Till the last packet hasn't been received

            rcvSocket.receive(rcvPacket);

			// Packet Handling

			// Sequence Number
            sequenceRcv =
                    (0xFF00 & ((int) rcvPacket.getData()[0] << 8))
                            + (0xFF & ((int) rcvPacket.getData()[1]));
            if (sequenceRcv == sequenceExp) {
                // Last packet
                ackData[0] = (byte) ((sequenceRcv & 0xFF00) >> 8);
                ackData[1] = (byte) (sequenceRcv & 0xFF);
                lastSequence = sequenceRcv;
                ++sequenceExp;

                // Check Flag
                flags = rcvPacket.getData()[2];
                if ((flags & 0x1) != 0) {
                    lastPacket = true;
                }

                // Write to file
                writeToFile();

            } else {
                // resend the last successfully received
                // packet number
                ackData[0] = (byte) ((lastSequence & 0xFF00) >> 8);
                ackData[1] = (byte) (lastSequence & 0xFF);

            }

			// Send ACK
            ackPacket = new DatagramPacket(ackData,
                    ackData.length,
                    rcvPacket.getAddress(),
                    rcvPacket.getPort());
            rcvSocket.send(ackPacket);

            if (lastPacket == true) {
                // send some more acks just in case some of them
                // are lost in transit
                // 6 9's certainty on a 10% plr
                int i = 0;
                while (i < 11) {
                    rcvSocket.send(ackPacket);
                    ++i;
                }
            }

            System.out.printf("\rReceived pkt: %d            ",
                    lastSequence);
			// End of packet handling
        }

        System.out.println("\rSuccessfully received file of length "
                + fileLength);
        file.close();
    }


    private static void writeToFile() throws IOException {
        if (!lastPacket) {
            file.write(rcvPacket.getData(), 3, 1024);
            fileLength += 1024;
        } else {
			/*
			 * This is the last packet. Since the buffer is
			 * larger (or equal) to the data size, traverse
			 * the array backwards until a data byte is
			 * found. That will give the final piece size
			 */
            int index = 1024;
            while (rcvPacket.getData()[--index + 3] == 0) {
                // Do nothing
            }
			/*
			 * the +1 negates the last pre-decrement of the
			 * while loop
			 */
            file.write(rcvPacket.getData(),
                    3,
                    index + 1);
            fileLength += index + 1;
        }
    }
}
