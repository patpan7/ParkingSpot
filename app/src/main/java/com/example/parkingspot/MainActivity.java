package com.example.parkingspot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private Button printButton;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "BluetoothPrinter";
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        username = intent.getStringExtra("username"); // Λήψη του ονόματος του χρήστη


        editText = findViewById(R.id.editText);
        printButton = findViewById(R.id.printButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            finish();
        }

        printButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    selectBluetoothDevice();
                }
            }
        });

        // Ζητάμε το δικαίωμα BLUETOOTH_CONNECT αν είμαστε σε Android 12 ή πιο πάνω
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                selectBluetoothDevice();
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to print", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void selectBluetoothDevice() {
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            ArrayList<String> deviceNames = new ArrayList<>();
            final ArrayList<BluetoothDevice> devices = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                deviceNames.add(device.getName());
                devices.add(device);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Bluetooth Device");
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    bluetoothDevice = devices.get(which);
                    try {
                        // Προσπαθούμε να ανακτήσουμε τα UUIDs της συσκευής
                        fetchDeviceUUIDs(bluetoothDevice);
                        openBluetoothPrinter();
                        printData(editText.getText().toString());
                    } catch (IOException e) {
                        Log.e(TAG, "Error connecting to Bluetooth device", e);
                        Toast.makeText(MainActivity.this, "Error connecting to Bluetooth device", Toast.LENGTH_LONG).show();
                    }
                }
            });
            builder.show();
        } else {
            Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchDeviceUUIDs(BluetoothDevice device) {
        try {
            // Λήψη των UUIDs ως ParcelUuid
            ParcelUuid[] parcelUuids = device.getUuids();
            if (parcelUuids != null) {
                UUID[] uuids = new UUID[parcelUuids.length];
                for (int i = 0; i < parcelUuids.length; i++) {
                    uuids[i] = parcelUuids[i].getUuid();
                    Log.i(TAG, "UUID: " + uuids[i].toString());
                }
            } else {
                Toast.makeText(this, "UUIDs not found", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving UUIDs", e);
        }
    }

    @SuppressLint("MissingPermission")
    private void openBluetoothPrinter() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID for SPP
        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        bluetoothSocket.connect();
        outputStream = bluetoothSocket.getOutputStream();
    }

//    private void printData(String data) throws IOException {
//        int textLength = data.length();
//        int numSpaces = (48 - textLength) / 2;
//        String centeredText = String.format("%" + numSpaces + "s%s", "", data +"\n\n\n\n\n\n");
//
//        // Εντολή για ρύθμιση μεγέθους γραμματοσειράς
//        // ESC ! n, n = 16 για διπλάσιο μέγεθος
//        byte[] setSizeCmd = new byte[]{0x1B, 0x21, 0x32};
//        outputStream.write(setSizeCmd);
//
//        // Εκτύπωση δεδομένων
//        outputStream.write(centeredText.getBytes("UTF-8"));
//
//        // Επαναφορά στο κανονικό μέγεθος γραμματοσειράς
//        byte[] resetSizeCmd = new byte[]{0x1B, 0x21, 0x00};
//        outputStream.write(resetSizeCmd);
//
//        outputStream.close();
//        bluetoothSocket.close();
//    }

    private void printData(String data) throws IOException {
        // Μέγεθος γραμματοσειράς 48 χαρακτήρες ανά γραμμή
        int lineLength = 48;

        // Λήψη τρέχουσας ημερομηνίας και ώρας
        String dateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date());

        // Υπολογισμός κεντραρισμένου κειμένου
        String centeredData = centerText(data, lineLength)+"\n\n\n\n";
        String centeredUser = "\n"+centerText(username,lineLength)+"\n\n";
        String centeredDateTime = dateTime+"\n\n\n\n\n\n\n";

        // Εντολή για ρύθμιση μεγέθους γραμματοσειράς (διπλάσιο μέγεθος)
        byte[] setSizeCmd = new byte[]{0x1B, 0x21, 0x32};
        outputStream.write(setSizeCmd);

        // Εκτύπωση δεδομένων
        outputStream.write(centeredUser.getBytes("UTF-8"));
        outputStream.write(centeredData.getBytes("UTF-8"));
        byte[] setSizeCmd2 = new byte[]{0x1B, 0x21, 0x20};
        outputStream.write(setSizeCmd2);
        outputStream.write(centeredDateTime.getBytes("UTF-8"));

        // Επαναφορά στο κανονικό μέγεθος γραμματοσειράς
        byte[] resetSizeCmd = new byte[]{0x1B, 0x21, 0x00};
        outputStream.write(resetSizeCmd);

        // Κλείσιμο του stream και του socket
        outputStream.close();
        bluetoothSocket.close();
    }

    private String centerText(String text, int lineLength) {
        int textLength = text.length();
        if (textLength >= lineLength) {
            return text;
        }

        int padding = (lineLength - textLength) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            sb.append(' ');
        }
        sb.append(text);

        return sb.toString();
    }
}