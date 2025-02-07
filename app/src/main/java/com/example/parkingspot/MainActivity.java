package com.example.parkingspot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private Button printButton;
    private Button changePrinterButton;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "BluetoothPrinter";
    private static final String PREFS_NAME = "PrinterPrefs";
    private static final String PREF_KEY_MAC = "PrinterMAC";
    private SharedPreferences sharedPreferences;
    String username;

    private KeyboardView keyboardView;
    private Keyboard keyboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        username = intent.getStringExtra("username"); // Λήψη του ονόματος του χρήστη
        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(username);
        keyboardView = findViewById(R.id.keyboard_view);
        editText = findViewById(R.id.editText);
        printButton = findViewById(R.id.printButton);
        changePrinterButton = findViewById(R.id.changePrinterButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            finish();
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        keyboard = new Keyboard(this, R.xml.custom_keyboard);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(new KeyboardView.OnKeyboardActionListener() {
            @Override
            public void onKey(int primaryCode, int[] keyCodes) {
                if (primaryCode == -1) {
                    // Backspace
                    int length = editText.getText().length();
                    if (length > 0) {
                        editText.getText().delete(length - 1, length);
                    }
                } else if (primaryCode == 32) {
                    // Space
                    String text = editText.getText().toString();
                    editText.setText(text+" ");

                } else if (primaryCode == 45) {
                    // Hyphen
                    String text = editText.getText().toString();
                    editText.setText(text+"-");
                } else {
                    // Add character
                    editText.getText().append(Character.toString((char) primaryCode));
                }
            }
            @Override
            public void onPress(int primaryCode) {
            }

            @Override
            public void onRelease(int primaryCode) {
            }

            @Override
            public void onText(CharSequence text) {
            }

            @Override
            public void swipeLeft() {
            }

            @Override
            public void swipeRight() {
            }

            @Override
            public void swipeDown() {
            }

            @Override
            public void swipeUp() {
            }
        });

        // Απόκρυψη του πληκτρολογίου του συστήματος και εμφάνιση του προσαρμοσμένου πληκτρολογίου
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    hideSystemKeyboard();
                    keyboardView.setVisibility(View.VISIBLE);
                } else {
                    keyboardView.setVisibility(View.GONE);
                }
            }
        });

        // Απόκρυψη του πληκτρολογίου του συστήματος όταν το EditText λαμβάνει εστίαση
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!editText.hasFocus()) {
                    keyboardView.setVisibility(View.GONE);
                }
            }
        });
        editText.setShowSoftInputOnFocus(false);
        hideSystemKeyboard();

        printButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    String savedMAC = sharedPreferences.getString(PREF_KEY_MAC, null);
                    if (savedMAC != null) {
                        // Αν υπάρχει αποθηκευμένη συσκευή, συνδεόμαστε σε αυτήν
                        bluetoothDevice = bluetoothAdapter.getRemoteDevice(savedMAC);
                        try {
                            openBluetoothPrinter();
                            printData(editText.getText().toString());
                        } catch (IOException e) {
                            Log.e(TAG, "Error connecting to Bluetooth device", e);
                            Toast.makeText(MainActivity.this, "Error connecting to Bluetooth device", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Αν δεν υπάρχει αποθηκευμένη συσκευή, εμφανίζουμε τη λίστα
                        selectBluetoothDevice();
                    }
                }
            }
        });

        changePrinterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Εμφανίζει τη λίστα επιλογής συσκευών για αλλαγή του εκτυπωτή
                selectBluetoothDevice();
            }
        });

        // Ζητάμε το δικαίωμα BLUETOOTH_CONNECT αν είμαστε σε Android 12 ή πιο πάνω
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }

    // Μέθοδος για απόκρυψη του πληκτρολογίου του συστήματος
    private void hideSystemKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        // Μετάβαση στο LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Κλείσιμο του MainActivity για να αποτραπεί η επιστροφή με το back button
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
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
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
                        // Αποθήκευση του MAC address της συσκευής που επιλέχθηκε
                        sharedPreferences.edit().putString(PREF_KEY_MAC, bluetoothDevice.getAddress()).apply();

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
    private void openBluetoothPrinter() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID for SPP
        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        bluetoothSocket.connect();
        outputStream = bluetoothSocket.getOutputStream();
    }

    private void printData(String data) throws IOException {
        // Λήψη τρέχουσας ημερομηνίας και ώρας
        String dateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date());

        // Προετοιμασία κειμένου
        String centeredUser = "\n" + username + "\n\n";
        String centeredData = data + "\n\n";
        String centeredDateTime = dateTime + "\n\n\n\n\n\n\n";

        // Μεγέθυνση γραμματοσειράς
        byte[] setSizeCmd = new byte[]{0x1D, 0x21, 0x22};
        outputStream.write(setSizeCmd);

        // Κεντράρισμα κειμένου
        byte[] setAlignCmd = new byte[]{0x1B, 0x61, 0x01};
        outputStream.write(setAlignCmd);

        // Εκτύπωση ονόματος χρήστη και δεδομένων
        outputStream.write(centeredUser.getBytes("UTF-8"));
        outputStream.write(centeredData.getBytes("UTF-8"));

        // Εκτύπωση QR Code
        printQRCode("https://www.mysite.com");

        // Εκτύπωση ημερομηνίας σε κανονικό μέγεθος
        byte[] setSizeCmd2 = new byte[]{0x1D, 0x21, 0x11};
        outputStream.write(setSizeCmd2);
        outputStream.write(centeredDateTime.getBytes("UTF-8"));

        // Επαναφορά στο κανονικό μέγεθος γραμματοσειράς
        byte[] resetSizeCmd = new byte[]{0x1B, 0x21, 0x00};
        outputStream.write(resetSizeCmd);



        // Κλείσιμο του stream και του socket
        outputStream.close();
        bluetoothSocket.close();
    }

    // Μέθοδος για εκτύπωση QR Code
    private void printQRCode(String qrData) throws IOException {
        byte[] modelCommand = {0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x02, 0x00}; // Επιλογή QR Code
        byte[] sizeCommand = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x08}; // Μικρότερο QR για καλύτερη εκτύπωση
        byte[] errorCorrection = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x34}; // Καλύτερη ανθεκτικότητα εκτύπωσης
        byte[] densityCommand = {0x1D, 0x21, 0x11}; // Βελτίωση ποιότητας εκτύπωσης

        byte[] storeCommand = new byte[]{0x1D, 0x28, 0x6B,
                (byte) (qrData.length() + 3), 0x00,
                0x31, 0x50, 0x30
        };
        byte[] printCommand = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30};

        // Κεντράρισμα QR Code
        byte[] setAlignCmd = new byte[]{0x1B, 0x61, 0x01};
        outputStream.write(setAlignCmd);

        outputStream.write(densityCommand); // Αυξάνει την ποιότητα εκτύπωσης
        outputStream.write(modelCommand);
        outputStream.write(sizeCommand);
        outputStream.write(errorCorrection);
        outputStream.write(storeCommand);
        outputStream.write(qrData.getBytes("UTF-8"));
        outputStream.write(printCommand);

        // Πρόσθεσε περισσότερα κενά μετά το QR Code για καθαρότερη εκτύπωση
        outputStream.write("\n\n\n".getBytes("UTF-8"));
    }


}