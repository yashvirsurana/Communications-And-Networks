/* Yashvir Surana s1368177 */

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by s1368177 on 20/03/16.
 */
// List of packets sent over the network
public class PacketList2b {
    private List<Packet2b> packet2BList;

    private int wndBase = 0; // Base of sender window

    // Constructor
    public PacketList2b() {
        packet2BList = new ArrayList<Packet2b>();
    }

    //Number of packets
    public synchronized int length() {
        return packet2BList.size();
    }

    //Sends packets with given sequence number
    public synchronized void send(int seq, DatagramSocket socket) {
        Packet2b mb = packet2BList.get(seq);
        mb.send(socket);
    }

    //Check if packets with sequence number seq is sent
    public synchronized boolean isSent(int seq) {
        Packet2b mb=  packet2BList.get(seq);
        boolean ans = mb.state == stateOfPacket.SENT;
        return ans;
    }

    //Have all packets been received
    public boolean allRcvd() {
        for (Packet2b m : packet2BList) {
            if (m.state != stateOfPacket.RECEIVED)
                return false;
        }
        return true;
    }

    //Get state of the packets with sequence number seq
    public stateOfPacket getState(int seq) {
        return packet2BList.get(seq).state;
    }

    //Return time the packets with sequence number seq was sent; -1 otherwise
    public synchronized final long getSendingTime(int seq) {
        return packet2BList.get(seq).sendingTime;
    }

    public synchronized int getWindowBase() {
//			int sequence = 0;
        int i = wndBase;
        while (i < packet2BList.size()) {
            if (packet2BList.get(i).state != stateOfPacket.RECEIVED)
                break;
            else
                ++wndBase;
            ++i;
        }
        return wndBase;
    }
    // Add packets (sequence, data, whether its the last one)
    public synchronized boolean add(int s_, byte[] p_, boolean l_) {
        return packet2BList.add(new Packet2b(s_, p_, l_));
    }

    //Change state of packet with sequence seq to lost
    public synchronized void packetLost(int seq) {
        packet2BList.get(seq).state = stateOfPacket.LOST;
    }

    //Change state of packet with sequence seq to received
    public synchronized void packetRcvd(int seq, boolean cumulative) {
        if (!cumulative) {
            packet2BList.get(seq).state = stateOfPacket.RECEIVED;
        } else {
            int i = getWindowBase();
            while (i <= seq) {
                packet2BList.get(i).state = stateOfPacket.RECEIVED;
                ++i;
            }
        }
    }

}