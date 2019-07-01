/* Yashvir Surana s1368177 */

// SELECTIVE_REPEAT - Start Receiver First

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Semaphore;
/**
 * Created by s1368177 on 20/03/16.
 */

public class Sender2b {
    // Declarations
    public static int port = 0;
    public static InetAddress host = null;
    public static int retryTimeout = 0;
    public static int window = 0;
    public static FileHandler2b fileHandler2B = null;
    public static PacketList2b msgList = null;
    public static ACKHandler2b ackHandler2b = null;
    public static Semaphore semaphore = null;

    public static void main(String[] args) throws IOException {
        long time = System.currentTimeMillis();

        // --1-- host
        host = InetAddress.getByName(args[0]);

        // --2-- port.
        port = Integer.parseInt(args[1]);

        // --3-- filename is in args[2]
        try {
            fileHandler2B = new FileHandler2b(args[2]);
            int sequence = 0;
            msgList = new PacketList2b();
            while (fileHandler2B.isDataLeft()) {
                msgList.add(sequence++,
                        fileHandler2B.getNextPiece(),
                        !fileHandler2B.isDataLeft());
            }
        } catch (IOException e) {
            System.out.println("Failed to init data handler");
            System.exit(-1);
        }

        // --4-- retransmit timeout is in args[3]
        retryTimeout = Integer.parseInt(args[3]);

        // --5-- window size
        window = Integer.parseInt(args[4]);

        System.out.println("host: " + host + "\nport: " + port
                + "\nfile: " + args[2] + " : "
                + fileHandler2B.fileSize + " B");

        // Open up UDP port
        DatagramSocket senderSocket = new DatagramSocket();
        senderSocket.setSoTimeout(retryTimeout);

        // set up the semaphore
        semaphore = new Semaphore(1);

		/*-- Pretty print --*/
        char[] symbols =
                {'-', '\\', '|', '/'};
        int currSym = 0;

        // Set up the ACK Handler
        ackHandler2b = new ACKHandler2b(msgList, senderSocket);
        Thread ackHndThread = new Thread(ackHandler2b);
        ackHndThread.start();

        // START Sending
        System.out.printf("Sending: [%c]\b\b", symbols[currSym]);
        int resendCount = 0;
        int windowBase = 0;
        while (!msgList.allRcvd()) {
			/* print rotating thingy */
            System.out.printf("%c\b",
                    symbols[((++currSym) % 4)]);

            // move the window base to its proper position
            windowBase = msgList.getWindowBase();

            int windowEnd = windowBase + window - 1 >= msgList
                    .length() ? msgList.length() - 1
                    : windowBase + window - 1;
			/* for each packet in the window:
			 * if it is not sent, send it.
			 * if lost, re-send it.
			 * if sent or received, do nothing
			 */

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                // do nothing
            }

            for (int i = windowBase;
                 i <= windowEnd;
                 ++i)

            {
                switch (msgList.getState(i)) {

                    case LOST:
                        ++resendCount;
                    case NOTSENT:
                        msgList.send(i, senderSocket);
                        break;
                    case SENT:
                    case RECEIVED:
                    default:
                        break;
                }
            }
            semaphore.release();
        }
        //  END Send loop
        // The ACK handler has stopped on its own by now
        time = System.currentTimeMillis() - time;
        System.out.printf(
                "\rFile sent in %ds\nNumber of resends: %d\nAvg throughput: %6.2f KiB/s\n",
                time / 1000, resendCount,
                (float) fileHandler2B.fileSizeInt / time);
        senderSocket.close();

    } //    END public static void Main( String[] args )

}
