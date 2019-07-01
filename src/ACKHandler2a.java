/* Yashvir Surana s1368177 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

/**
 * Created by s1368177 on 20/03/16.
 */
// ACK Handler class waits for ACKs to arrive and also writes.
public class ACKHandler2a extends Sender2a implements Runnable {

    public byte[] ackData;
    public DatagramPacket ackPacket;
    public PacketList2a msgList;
    public DatagramSocket socket;
    public long timeNow;

    public ACKHandler2a(PacketList2a packetList_2a_,
                        DatagramSocket socket_) {
        ackData = new byte[2];
        ackPacket = new DatagramPacket(ackData, 2);                 // packet
        msgList = packetList_2a_;                                     // Packet2a list containing all packet data
        socket = socket_;                                           // socket through which to send
    }

    // Main loop of secondary thread, wait for ACKs until all msgs recvd
    @Override
    public void run() {
        while (!msgList.allRcvd()) {

            int windowBase = msgList.getWindowBase();
            int windowEnd = (windowBase + window - 1 >=
                    msgList.length()) ?
                    msgList.length() - 1 :
                    windowBase + window - 1;
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                //do nothing
            }
            int i = windowBase;
            while (i<=windowEnd) {
                receiveACK(i);
                ++i;
            }

            semaphore.release();

        }
    }

    //Waits for an ACK until the packets retransmit timeout is reached
    // takes in a sequence number
    private void receiveACK(int i) {
        try {
            if (!msgList.isSent(i)) {
                return;

            } else {

                timeNow = System.currentTimeMillis();
                long diff =
                        retryTimeout -
                                (timeNow -
                                        msgList.getSendingTime(i));

                if (diff <= 0)
                    socket.setSoTimeout(1);
                else
                    socket.setSoTimeout(
                            (int) (diff));

                socket.receive(ackPacket);
                // We've got an ACK, signal that this
                // packets is in
                int seq = ((((int)
                        ackPacket.getData()[0]) & 0xFF) << 8)
                        + ((int) ackPacket.getData()[1] & 0xFF);
                msgList.packetRcvd(seq, true);
                // consider the packets lost just in
                // case (if the rcvd ack is not the one
                // we expected). If we rcv a later ack,
                // just move our expectation. Only works
                // on GBN, not on SEL_RPT
                if (seq >= i)
                    i = seq;
                else
                    msgList.packetLost(i);
            }

        } catch (SocketTimeoutException e) {
            // Socket timed out - wait for next ACK
            msgList.packetLost(i);
            return;
        } catch (IOException e) {
            // Could not access socket
//				e.printStackTrace();
            System.exit(-1);
        }
    }

}