/* Yashvir Surana s1368177 */

// SELECTIVE_REPEAT RECEIVER
/**
 * Created by s1368177 on 20/03/16.
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class Receiver2b {

    /*-- Declarations --*/
    public static int port = -1;
    public static java.io.RandomAccessFile file = null;
    public static int window = -1;
    public static boolean lastPacket = false;
    public static int fileLength = 0;
    public static DatagramPacket rcvPacket = null;
    public static DatagramPacket ackPacket = null;

    public static void main(String[] args) throws IOException {
        // Parse Arguments

        port = Integer.parseInt(args[0]);

        // filename is in args[1]. Open it for writing

        file = new java.io.RandomAccessFile(args[1], "rw");
        file.setLength(0); // Delete any existing data

        // window size in args[2]
        window = Integer.parseInt(args[2]);

		/*-- Open up UDP port and listen for data --*/
        DatagramSocket rcvSocket = new DatagramSocket(port);

        byte[] receiveData = new byte[1024 + 3];    // Full Byte Array from the UDP Packet
        byte[] ackData = new byte[2];               // UDP ACK Byte Array - 2 bytes wide
        int seqRcv = 0;                             // above, converted
        byte flags = 0;                             // Last sequence number that has been received

        PacketList msgLst = new PacketList();

        // Begin waiting
        rcvPacket = new DatagramPacket(receiveData, receiveData.length);
        System.out.printf("Waiting for first packet...\r");
        while (!msgLst.allRcvd()) {

            rcvSocket.receive(rcvPacket);

            // Packet Handling

            int windowBase = msgLst.getWindowBase();

            // Sequence Number
            seqRcv =
                    (0xFF00 & ((int) rcvPacket.getData()[0] << 8))
                            + (0xFF & ((int) rcvPacket.getData()[1]));
//			System.out.println("seq: " + seqRcv + " base: " + wndBase);

            // if sequenceRcv < wndBase, we just ack
            // and do nothing else
            // if sequenceRcv > wndBase + window - 1, we got a prob
            if (seqRcv >= windowBase + window) {
                System.out.println("make sure that sender and \n " +
                        "receiver have the same " +
                        "window sizes");
                System.exit(-1);
            }

            // Check Flag
            flags = rcvPacket.getData()[2];
            if ((flags & 0x1) != 0) {
                lastPacket = true;
            }

			// Add Packet to list
            byte[] packetData = new byte[1024];
            System.arraycopy(rcvPacket.getData(), 3, packetData,
                    0, 1024);

            msgLst.addMsg(packetData, seqRcv, lastPacket);

			//  send the ack
            // always return the ack number of the packet just rcvd
            ackData[0] = rcvPacket.getData()[0];
            ackData[1] = rcvPacket.getData()[1];
            if (ackPacket == null)
                ackPacket = new DatagramPacket(ackData,
                        ackData.length,
                        rcvPacket.getAddress(),
                        rcvPacket.getPort());
            rcvSocket.send(ackPacket);

            System.out.printf("\rReceived pkt: %d            ",
                    seqRcv);
            // End of packet handling
        }

        //if ACK was lost
        while (true) {
            try {
				/* if we don't get anything within a sec,
				 * then call it quits.
				 */
                rcvSocket.setSoTimeout(1000);
                rcvSocket.receive(rcvPacket);
            } catch (SocketTimeoutException e) {
                break;
            }

            // return the lost last ack
            ackData[0] = rcvPacket.getData()[0];
            ackData[1] = rcvPacket.getData()[1];
            if (ackPacket == null)
                ackPacket = new DatagramPacket(ackData,
                        ackData.length,
                        rcvPacket.getAddress(),
                        rcvPacket.getPort());
            rcvSocket.send(ackPacket);
            for (int i = 0; i < 9; ++i) {
                rcvSocket.send(ackPacket); //do it more times
            }
        }

        // Write to file
        msgLst.writeToFile(file);
        System.out.println("\rSuccessfully received file of length "
                + fileLength);
        file.close();
    }

    private static class PacketList {

        private List<Packet> msgLst;

        public PacketList() {
            msgLst = new ArrayList<Packet>();
        }

        public void addMsg(byte[] data_, int seq_, boolean last_) {
            if (seq_ == msgLst.size()) {
                // sequence number is in order, so just add
                msgLst.add(new Packet(data_, last_));
            } else if (seq_ > msgLst.size()) {
                // we need to add placeholder packets
                for (int i = msgLst.size(); i < seq_; ++i) {
                    msgLst.add(new Packet(null, false));
                }
                msgLst.add(new Packet(data_, last_));
            } else {
                // we need to update existing invalid entry
                // or do nothing with the duplicate pkt
                if (msgLst.get(seq_).valid)
                    // we have a duplicate pkt - do nothing
                    return;
                msgLst.get(seq_).validate(data_);
            }
        }

        public boolean allRcvd() {
            if (msgLst.size() == 0) {
                return false;
            }
            if (msgLst.get(msgLst.size() - 1).last == false)
                return false;
            else {
                for (Packet m : msgLst) {
                    if (m.valid == false)
                        return false;
                }
            }
            return true;
        }

        public int getWindowBase() {

            int i = 0;
            while ( i < msgLst.size() ) {
                if (i == msgLst.size() - 1)
                    if (msgLst.get(i).valid)
                        return msgLst.size();

                if (msgLst.get(i).valid == false) {
                    return i;
                }
                ++i;
            }
            return 0;
        }

        public void writeToFile(java.io.RandomAccessFile file_)
                throws IOException {
            // For each packet in packets list
            int i = 0;
            while (i < msgLst.size()) {
                if (i == msgLst.size() - 1) {
                    // Last packet, since the buffer is larger or equal to the
                    // data size, traverse the array backwards until a data byte
                    // is found. That's the final piece size.

                    int index = 1024;
                    while (msgLst.get(i)
                            .data[--index] == 0) {
                        // Do nothing
                    }
					// the +1 negates the pre-decrement of the while loop
                    file.write(msgLst.get(i).data,
                            0, index + 1);
                    fileLength += index + 1;
                } else {
                    // not the last packet
                    file.write(msgLst.get(i).data,
                            0,
                            1024);
                    fileLength += 1024;
                }
                ++i;
            }
        }
    }
    // Packet class
    private static class Packet {
        public byte[] data;
        public boolean valid;
        public boolean last;

        public Packet(byte[] data_, boolean last_) {
            data = data_;
            valid = (data == null) ? false : true;
            last = last_;
        }

        public void validate(byte[] data_) {
            if (!valid) {
                data = data_;
                if (data == null)
                    return;
                valid = true;
            }
            return;
        }
    }
}
