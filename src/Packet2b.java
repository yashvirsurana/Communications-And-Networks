/* Yashvir Surana s1368177 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by s1368177 on 20/03/16.
 */
// Single packet - has sequence state and dispatch time
public class Packet2b extends Sender2b {

    public final int sequence;
    DatagramPacket packet;
    public byte[] msgData;
    public long sendingTime;
    public stateOfPacket state;

    // Constructor for the packets
    public Packet2b(int sequence_, byte[] payload_,
                    boolean lastPacket_) {

        sequence = sequence_;
        msgData = new byte[1027];
        System.arraycopy(payload_, 0, msgData, 3,
                payload_.length);
        sendingTime = -1;
        state = stateOfPacket.NOTSENT;

        msgData[0] = (byte) ((sequence & 0xFF00) >> 8);
        msgData[1] = (byte) (sequence & 0xFF);
        msgData[2] = lastPacket_ ? (byte) 0x1
                : (byte) 0;

        packet = new DatagramPacket(msgData, msgData.length,
                host, port);
    }

    public byte[] getMsgData() {
        return msgData;
    }

    public void setMsgData(byte[] msgData) {
        this.msgData = msgData;
    }

    //Send packet through given socket
    public void send(DatagramSocket socket) {
        try {
            socket.send(packet);
            sendingTime = System.currentTimeMillis();
            state = stateOfPacket.SENT;
        } catch (IOException e) {
            System.out.println("Failed to send");
        }
    }

}
