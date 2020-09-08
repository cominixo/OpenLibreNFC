package me.cominixo.openlibrenfc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.openlibrenfc.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static me.cominixo.openlibrenfc.LibreNfcUtils.bytesToHexStr;
import static me.cominixo.openlibrenfc.LibreNfcUtils.hexStrToBytes;
import static me.cominixo.openlibrenfc.LibreNfcUtils.sendCmd;

public class MainActivity extends AppCompatActivity {

    enum SelectedAction {
        SCAN, RESET_AGE, ACTIVATE, LOAD_DUMP
    }

    private SelectedAction selectedAction = SelectedAction.SCAN;

    private TextView idView;
    private TextView uidView;
    private TextView typeView;
    private TextView ageView;
    private TextView regionView;
    private TextView statusView;

    private TextView selectedActionView;

    private byte[] memory = new byte[360];


    public void onScanClick(View view) {

        selectedAction = SelectedAction.SCAN;
        selectedActionView.setText(getString(R.string.selected_action, getString(R.string.scan)));

    }

    public void onResetAgeClick(View view) {

        selectedAction = SelectedAction.RESET_AGE;
        selectedActionView.setText(getString(R.string.selected_action, getString(R.string.reset_age)));

    }

    public void onActivateClick(View view) {

        selectedAction = SelectedAction.ACTIVATE;
        selectedActionView.setText(getString(R.string.selected_action, getString(R.string.start)));

    }

    public void dumpMemory(View view) {

        File file = getFile();

        FileOutputStream os;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            os = new FileOutputStream(file);
            os.write(bytesToHexStr(memory).getBytes());
            os.close();

            Toast.makeText(this, "Memory dumped to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {

            Toast.makeText(this, "Couldn't dump memory", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    public void loadMemory(View view) {

        new AlertDialog.Builder(this)
                .setTitle("Load memory")
                .setMessage("This will overwrite the current memory on the sensor with the most recent memory dump. If you edited the memory dump, make sure the checksums are correct.")
                .setPositiveButton(android.R.string.ok, null)
                .show();

        selectedAction = SelectedAction.LOAD_DUMP;
        selectedActionView.setText(getString(R.string.selected_action, getString(R.string.load_memory)));
    }

    private File getFile() {
        // External storage path (SD card)
        File sdcard = Environment.getExternalStorageDirectory();

        File dir = new File(sdcard.getAbsolutePath() + "/openlibrenfc/");

        dir.mkdirs();

        File file = new File(dir, "memory_dump.txt");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }

        return file;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LibreNfcUtils.updateActivity(this);

        setContentView(R.layout.activity_main);

        idView = findViewById(R.id.libreid);
        uidView = findViewById(R.id.uid);
        typeView = findViewById(R.id.type);
        ageView = findViewById(R.id.age);
        regionView = findViewById(R.id.region);
        statusView = findViewById(R.id.status);
        selectedActionView = findViewById(R.id.selected_action);

        selectedActionView.setText(getString(R.string.selected_action, getString(R.string.scan)));

        idView.setText("");
        uidView.setText("");
        typeView.setText("");
        ageView.setText("");
        regionView.setText("");
        statusView.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent nfcIntent = new Intent(this, getClass());
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, 0);
        IntentFilter[] intentFiltersArray = new IntentFilter[]{};
        String[][] techList = new String[][]{{android.nfc.tech.Ndef.class.getName()}, {android.nfc.tech.NdefFormatable.class.getName()}};
        NfcAdapter nfcAdpt = NfcAdapter.getDefaultAdapter(this);
        nfcAdpt.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techList);

    }

    @Override
    public void onNewIntent(Intent intent) {
        Tag nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);


        NfcV handle = NfcV.get(nfcTag);


        byte[] getIdCmd = {
                (byte) 0x02, // Flag for un-addressed communication
                (byte) 0xa1, // Get Patch Info
                (byte) 0x07  // Vendor Identifier
        };

        byte[] getUidCmd = {
                (byte) 0x26,
                (byte) 0x01,
                (byte) 0x00
        };


        byte[] receivedId = sendCmd(handle, getIdCmd);

        byte[] receivedUid = sendCmd(handle, getUidCmd);


        if (receivedId.length != 0 && receivedUid.length != 0) {

            // Remove zeros
            receivedId = Arrays.copyOfRange(receivedId, 1, receivedId.length);
            receivedUid = Arrays.copyOfRange(receivedUid, 2, receivedUid.length-2);

            byte[] typeIdentifier = Arrays.copyOfRange(receivedId, 0, 3);

            String libreType;

            if (Arrays.equals(typeIdentifier, LibreConstants.LIBRE1_NEW_ID)) {
                libreType = "Libre 1 New";
            } else if (Arrays.equals(typeIdentifier, LibreConstants.LIBRE1_OLD_ID)) {
                libreType = "Libre 1 Old";
            } else if (Arrays.equals(typeIdentifier, LibreConstants.LIBRE1_JAPAN_ID)) {
                libreType = "Libre 1 Japan";
            } else {
                libreType = "Unsupported";
            }

            memory = LibreNfcUtils.readMemory(handle);


            if (memory.length == 0) {
                return;
            }

            switch (selectedAction) {

                case RESET_AGE:
                    memory[317] = 0;
                    memory[316] = 0;

                    // Convert to int array first

                    int[] memoryInt = new int[memory.length];

                    for (int i = 0; i < memory.length; i++) {

                        memoryInt[i] = memory[i] & 0xff;

                    }

                    int out = LibreNfcUtils.crc16(Arrays.copyOfRange(memoryInt, 26, 294+26));


                    byte[] crc = ByteBuffer.allocate(4).putInt(out).array();

                    memory[24] = crc[3];
                    memory[25] = crc[2];

                    LibreNfcUtils.unlock(handle);

                    LibreNfcUtils.writeMemory(handle, memory);

                    LibreNfcUtils.lock(handle);

                    memory = LibreNfcUtils.readMemory(handle);

                    if (memory.length == 0) {
                        return;
                    }
                    break;

                case ACTIVATE:
                    byte[] activateCmd = new byte[]
                    {
                        (byte) 0x02,
                        (byte) 0xA0,
                        (byte) 0x07,
                        (byte) 0XC2,
                        (byte) 0xAD,
                        (byte) 0x75,
                        (byte) 0x21
                    };

                    sendCmd(handle, activateCmd);
                    break;
                case LOAD_DUMP:
                    StringBuilder text = new StringBuilder();

                    File file = getFile();

                    try {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        String line;

                        while ((line = br.readLine()) != null) {
                            text.append(line);
                            text.append('\n');
                        }
                        br.close();
                    }
                    catch (IOException e) {
                        vibrate();

                        Toast.makeText(this, "Couldn't read the memory dump, is the file there?", Toast.LENGTH_SHORT).show();

                        e.printStackTrace();
                        return;
                    }

                    byte[] newMemory = hexStrToBytes(text.toString());


                    if (newMemory == null) {
                        vibrate();
                        new AlertDialog.Builder(this)
                                .setTitle("Memory load failed!")
                                .setMessage("The memory dump length was not the expected one! Check your memory_dump.txt")
                                .setPositiveButton(android.R.string.ok, null)
                                .show();

                        return;
                    }

                    memory = newMemory;

                    LibreNfcUtils.unlock(handle);

                    LibreNfcUtils.writeMemory(handle, memory);

                    LibreNfcUtils.lock(handle);


            }


            // Regular scan stuff
            float age = 256 * (memory[317] & 0xFF) + (memory[316] & 0xFF);
            int region = memory[323];
            int status = memory[4];

            String statusString;

            switch (status) {
                case 1:
                    statusString = "New (not activated)";
                    break;
                case 2:
                    statusString = "In warmup";
                    break;
                case 3:
                    statusString = "Active";
                    break;
                case 5:
                    statusString = "Expired";
                    break;
                case 6:
                    statusString = "Error";
                    break;
                default:
                    statusString = "Unknown!";
                    break;
            }


            String id = bytesToHexStr(receivedId);

            String uid = bytesToHexStr(receivedUid);

            idView.setText(getString(R.string.libreid, id));
            uidView.setText(getString(R.string.uid, uid));
            typeView.setText(getString(R.string.type, libreType));
            ageView.setText(getString(R.string.age, age/1440));
            regionView.setText( getString(R.string.region, region));
            statusView.setText( getString(R.string.status, status, statusString));

            Toast.makeText(this, "Scanned Successfully", Toast.LENGTH_SHORT).show();

            vibrate();
        }

    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(500);
        }
    }


}
