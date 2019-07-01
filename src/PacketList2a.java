/* Yashvir Surana s1368177 */

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by s1368177 on 20/03/16.
 */
// List of packets sent over the network
public class PacketList2a {
    private List<Packet2a> packet2AList;

    private int wndBase = 0; // Base of sender window

    // Constructor
    public PacketList2a() {
        packet2AList = new ArrayList<Packet2a>();
    }

    //Number of packets
    public synchronized int length() {
        return packet2AList.size();
    }

    //Sends packets with given sequence number
    public synchronized void send(int seq, DatagramSocket socket) {
        Packet2a ma = packet2AList.get(seq);
        ma.send(socket);
    }

    //Check if packets with sequence number seq is sent
    public synchronized boolean isSent(int seq) {
        Packet2a ma = packet2AList.get(seq);
        boolean ans = ma.state == stateOfPacket.SENT;
        return ans;
    }

    //Have all packets been received
    public boolean allRcvd() {
        for (Packet2a m : packet2AList) {
            if (m.state != stateOfPacket.RECEIVED)
                return false;
        }
        return true;
    }

    //Get state of the packets with sequence number seq
    public stateOfPacket getState(int seq) {
        return packet2AList.get(seq).state;
    }

    //Return time the packets with sequence number seq was sent; -1 otherwise
    public synchronized final long getSendingTime(int seq) {
        return packet2AList.get(seq).sendingTime;
    }

    public synchronized int getWindowBase() {
//			int sequence = 0;
        int i = wndBase;
        while (i < packet2AList.size()) {
            if (packet2AList.get(i).state != stateOfPacket.RECEIVED)
                break;
            else
                ++wndBase;
            ++i;
        }
        return wndBase;
    }

    // Add packets (sequence, data, whether its the last one)
    public synchronized boolean add(int s_, byte[] p_, boolean l_) {
        return packet2AList.add(new Packet2a(s_, p_, l_));
    }

    //Change state of packet with sequence seq to lost
    public synchronized void packetLost(int seq) {
        packet2AList.get(seq).state = stateOfPacket.LOST;
    }

    //Change state of packet with sequence seq to received
    public synchronized void packetRcvd(int seq, boolean cumulative) {
        if (!cumulative) {
            packet2AList.get(seq).state = stateOfPacket.RECEIVED;
        } else {
            int i = getWindowBase();
            while (i <= seq) {
                packet2AList.get(i).state = stateOfPacket.RECEIVED;
                ++i;
            }
        }
    }

}