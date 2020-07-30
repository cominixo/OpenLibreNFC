package me.cominixo.openlibrenfc;

import android.app.Activity;
import android.nfc.tech.NfcV;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LibreNfcUtils {

    private static WeakReference<Activity> mainActivityRef;
    public static void updateActivity(Activity activity) {
        mainActivityRef = new WeakReference<>(activity);
    }

    // Reads the F-RAM memory from the Libre
    public static byte[] readMemory(NfcV handle) {

        byte[] received = new byte[LibreConstants.MEMORY_SIZE];

        // Loop through all blocks, 3 at a time
        for (int i = 0; i < 43; i += 3){

            byte[] cmd = {
                    (byte) 0x02, // Flag for un-addressed communication
                    (byte) 0x23, // Read Multiple Blocks
                    (byte) i, // Start block
                    (byte) 0x02 // Number of blocks to read (starts at 0 apparently, 2 is actually 3)
            };


            byte[] response = sendCmd(handle, cmd);

            // ignore first 0 to get 24 bytes (8 per block)
            if (response.length == 25) {
                System.arraycopy(response, 1, received, i * 8, response.length - 1);
            } else {
                // Some error while scanning, will just return an empty array
                return new byte[0];
            }

        }

        // TODO remove dump for debug
        System.out.println(bytesToHexStr(received));

        return received;

    }

    public static void writeMemory(NfcV handle, byte[] newMemory) {

        unlock(handle);
        for (int index = 0; index < 43; index++)
        {

            byte[] newData = new byte[8];

            for (int i = 0; i < 8; i++) {
                newData[i] = newMemory[index*8+i];

            }

            byte[] cmd =
            {
                (byte) 2, // Flags
                (byte) 0x21, // Write single block
                (byte) index, // Block to write
            };

            byte[] cmdWithBlocks = Arrays.copyOf(cmd, cmd.length + newData.length);

            System.arraycopy(newData, 0, cmdWithBlocks, cmd.length, newData.length);

            sendCmd(handle, cmdWithBlocks);

        }

        lock(handle);
    }

    public static byte[] sendCmd(NfcV handle, byte[] cmd) {

        long startTime = System.currentTimeMillis();

        while (true) {
            try {

                if (handle.isConnected()) {
                    handle.close();
                }

                handle.connect();
                byte[] received = handle.transceive(cmd);
                handle.close();

                return received;

            } catch (IOException ioException) {
                if (System.currentTimeMillis() > startTime + 3000) {
                    Toast.makeText(mainActivityRef.get(), "Scan timed out!", Toast.LENGTH_SHORT).show();
                    return new byte[0];
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    return new byte[0];
                }

            }
        }

    }


    public static String bytesToHexStr(byte[] bytes) {
        StringBuilder out = new StringBuilder();

        for (Byte b : bytes) {
            out.append(String.format("%02X", b)).append(" ");
        }

        return out.toString();

    }

    public static byte[] hexStrToBytes(String string) {
        byte[] bytes = new byte[360];

        List<String> cleanString = new ArrayList<>();

        for (String s : string.split(" ")) {
            if (!s.trim().isEmpty())
                cleanString.add(s.trim());
        }

        if (cleanString.size() != 360) {
            System.out.println(string);
            return null;
        }

        for (int i = 0; i < cleanString.size(); i++) {

            int byteInt = Integer.parseInt(cleanString.get(i), 16);
            bytes[i] = (byte) byteInt;

        }
        return bytes;
    }

    public static int crc16 (int[] data) {
        int crc = 0x0000FFFF;

        for (int i = 0; i < data.length; i++) {
            crc = ((crc >> 8) & 0x0000ffff) | ((crc <<  8) & 0x0000ffff);
            crc ^= bitRev((byte)data[i]);

            crc ^= (((crc & 0xff) >> 4) & 0x0000ffff);
            crc ^= ((crc << 12) & 0x0000ffff);
            crc ^=(((crc & 0xff) << 5) & 0x0000ffff);
        }
        return crc;

    }

    public static int bitRev(byte data) {
        return ((data << 7) & 0x80) | ((data << 5)& 0x40) | (data << 3) & 0x20 | (data << 1) &0x10 | (data >> 7) & 0x01 | (data >> 5) &0x02 | (data >> 3) & 0x04 | (data >> 1) &0x08;
    }

    public static void unlock(NfcV handle)
    {
        byte[] cmd =
        {
            (byte) 0x02, // Flag for un-addressed communication
            (byte) 0xA4, // Unlock
            (byte) 0x07  // Vendor identifier
        };


        byte[] cmdWithPassowrd = Arrays.copyOf(cmd, cmd.length + LibreConstants.PASSWORD.length);

        System.arraycopy(LibreConstants.PASSWORD, 0, cmdWithPassowrd, cmd.length, LibreConstants.PASSWORD.length);

        sendCmd(handle, cmdWithPassowrd);
    }

    public static void lock(NfcV handle) {

        byte[] cmd =
        {
            (byte) 0x02, // Flag for un-addressed communication
            (byte) 0XA2, // Lock
            (byte) 0x07  // Vendor identifier
        };

        byte[] cmdWithPassowrd = Arrays.copyOf(cmd, cmd.length + LibreConstants.PASSWORD.length);

        System.arraycopy(LibreConstants.PASSWORD, 0, cmdWithPassowrd, cmd.length, LibreConstants.PASSWORD.length);

        sendCmd(handle, cmdWithPassowrd);

    }
}
