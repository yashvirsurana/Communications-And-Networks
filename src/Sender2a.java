/* Yashvir Surana s1368177 */

// Go back n sender: start the receiver first
/**
 * Created by s1368177 on 20/03/16.
 */
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Semaphore;

public class Sender2a {

    // Declarations
    public static int port = 0;
    public static InetAddress host = null;
    public static int retryTimeout = 0;
    public static int window = 0;
    public static FileHandler2a fileHandler = null;
    public static PacketList2a msgList = null;
    public static ACKHandler2a ackHandler2a = null;
    public static Semaphore semaphore = null;


    public static void main(String[] args) throws IOException {

        long time = System.currentTimeMillis();

		// Parse arguments

        // --1-- host
        host = InetAddress.getByName(args[0]);

        // --2-- port
        port = Integer.parseInt(args[1]);

        // --3-- filename is in args[2]
        try {
            fileHandler = new FileHandler2a(args[2]);
            int sequence = 0;
            msgList = new PacketList2a();
            while (fileHandler.isDataLeft()) {
                msgList.add(sequence++,
                        fileHandler.getNextPiece(),
                        !fileHandler.isDataLeft());
            }
        } catch (IOException e) {
            System.out.println("Failed to initialise data handler");
            System.exit(-1);
        }

        // --4-- retransmit timeout is in args[3]
        retryTimeout = Integer.parseInt(args[3]);

        // -5- window size
        window = Integer.parseInt(args[4]);

        System.out.println("host: " + host + "\nport: " + port
                + "\nfile: " + args[2] + " : "
                + fileHandler.fileSize + " B");

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
        ackHandler2a = new ACKHandler2a(msgList, senderSocket);
        Thread ackHndThread = new Thread(ackHandler2a);
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
                 ++i) {
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
                (float) fileHandler.fileSizeInt / time);
        senderSocket.close();

    } //    END public static void Main( String[] args )

}
