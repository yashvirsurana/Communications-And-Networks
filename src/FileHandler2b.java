/* Yashvir Surana s1368177 */

import java.io.IOException;

/**
 * Created by s1368177 on 20/03/16.
 */
// Takes input file and gets bytes
public class FileHandler2b {
    public final long fileSize;
    public final int fileSizeInt;
    private byte[] data;
    private int index;

    // input file - stores in a byte array
    public FileHandler2b(String filename) throws IOException {
        index = 0;
        java.io.RandomAccessFile file = new java.io.RandomAccessFile(filename, "r");
        fileSize = file.length(); // the file length in bytes
        fileSizeInt = (int) fileSize;
        data = new byte[fileSizeInt];
        file.readFully(data);
        file.close();
    }

    //if we have reached the end of the file
    public boolean isDataLeft() {
        return (index < data.length);
    }

    // return next 1KB piece of data, if it is the last one and smaller than 1KB - it is padded
    public byte[] getNextPiece() {
        byte[] piece = new byte[1024];
        System.arraycopy(
                data, index,
                piece, 0,
                index + 1024 >= data.length ?
                        data.length
                                - index
                        : 1024);
        index += 1024;
        return piece;
    }
}