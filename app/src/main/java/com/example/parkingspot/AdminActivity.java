package com.example.parkingspot;


import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class AdminActivity extends AppCompatActivity {

    private Spinner adminUserSpinner;
    private EditText newPasswordEditText;
    private Button saveButton;
    private Properties properties;
    private static final String PROPERTIES_FILE = "user_credentials.properties";
    private static final String SECRET_BACKDOOR_CODE = "054909468";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        adminUserSpinner = findViewById(R.id.adminUserSpinner);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        saveButton = findViewById(R.id.saveButton);

        loadProperties();
        List<String> parkingUsers = getAllParkingUsers(properties);
        // Δημιουργεί έναν ArrayAdapter από τη λίστα των χρηστών
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, parkingUsers);

        // Ορίζει τον τύπο του αναδυόμενου καταλόγου
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Ορίζει τον ArrayAdapter στον Spinner
        adminUserSpinner.setAdapter(adapter);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedUser = adminUserSpinner.getSelectedItem().toString().toLowerCase();
                String newPassword = newPasswordEditText.getText().toString();

                if (!newPassword.isEmpty()) {
                    if (newPassword.equals(SECRET_BACKDOOR_CODE)) {
                        // Εύρεση του επόμενου αριθμού για το parking spot
                        int nextParkingNumber = findNextAvailableParkingNumber(properties);

                        // Δημιουργία νέου χρήστη
                        String newUserName = "parking_" + nextParkingNumber;
                        String newPassword1 = generateRandomPassword(); // Ή οποιοδήποτε άλλος τρόπος για τον κωδικό

                        // Προσθήκη του νέου χρήστη και κωδικού στις ιδιότητες
                        properties.setProperty(newUserName, newPassword);

                        // Αποθήκευση των αλλαγών στο αρχείο
                        try {
                            FileOutputStream fos = openFileOutput(PROPERTIES_FILE, Context.MODE_PRIVATE);
                            properties.store(fos, null);
                            fos.close();
                            Toast.makeText(AdminActivity.this, "Νέος χρήστης προστέθηκε επιτυχώς.", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.e("Properties", "Σφάλμα κατά την αποθήκευση του νέου χρήστη", e);
                        }
                    } else {
                        properties.setProperty(selectedUser, newPassword);
                        Toast.makeText(AdminActivity.this, selectedUser + " " + newPassword, Toast.LENGTH_SHORT).show();
                        saveProperties();
                        newPasswordEditText.setText("");
                        Toast.makeText(AdminActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AdminActivity.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Μετάβαση στο LoginActivity
        Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Κλείσιμο του MainActivity για να αποτραπεί η επιστροφή με το back button
    }

        private int findNextAvailableParkingNumber(Properties properties) {
            int nextNumber = 1;
            while (properties.containsKey("parking_" + nextNumber)) {
                nextNumber++;
            }
            return nextNumber;
        }

        private String generateRandomPassword() {
            // Υλοποιήστε έναν τρόπο για τη δημιουργία ενός τυχαίου κωδικού
            // Παράδειγμα:
            return "generated_password"; // Προσαρμόστε ανάλογα
        }

    private List<String> getAllParkingUsers(Properties properties) {
        List<String> parkingUsers = new ArrayList<>();

        // Επαναφέρει όλα τα κλειδιά (ονόματα χρηστών) από τις ιδιότητες
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("parking_")) {
                parkingUsers.add(key); // Προσθέτει το όνομα του χρήστη στη λίστα
            }
        }

        // Ταξινομεί τη λίστα χρησιμοποιώντας τον αριθμό του πάρκινγκ
        Collections.sort(parkingUsers, (user1, user2) -> {
            // Παίρνει τον αριθμό του πάρκινγκ από τα ονόματα των χρηστών
            int number1 = Integer.parseInt(user1.substring(8)); // Αφαιρεί το "parking_" και μετατρέπει σε αριθμό
            int number2 = Integer.parseInt(user2.substring(8)); // Αφαιρεί το "parking_" και μετατρέπει σε αριθμό
            return Integer.compare(number1, number2);
        });

        return parkingUsers;
    }

    private void loadProperties() {
        properties = new Properties();
        try {
            // Προσπάθησε να ανοίξεις το αρχείο από τον τοπικό αποθηκευτικό χώρο
            FileInputStream fis = openFileInput("user_credentials.properties");
            properties.load(fis);
            fis.close();
        } catch (IOException e) {
            Log.e("Properties", "Error loading properties from local storage, trying assets", e);
            // Αν αποτύχει, προσπάθησε να το φορτώσεις από τα assets
            try {
                AssetManager assetManager = getAssets();
                InputStream inputStream = assetManager.open("user_credentials.properties");
                properties.load(inputStream);
                inputStream.close();
                saveProperties(); // Αποθηκεύει το αρχείο τοπικά την πρώτη φορά
            } catch (IOException ex) {
                Log.e("Properties", "Error loading properties from assets", ex);
            }
        }
    }

    private void saveProperties() {
        try {
            // Αποθήκευσε το αρχείο στον τοπικό αποθηκευτικό χώρο
            FileOutputStream fos = openFileOutput("user_credentials.properties", Context.MODE_PRIVATE);
            properties.store(fos, null);
            StringBuilder propertiesContent = new StringBuilder();
            for (String key : properties.stringPropertyNames()) {
                propertiesContent.append(key).append("=").append(properties.getProperty(key)).append("\n");
            }
            Log.d("PropertiesContent", propertiesContent.toString());
            Toast.makeText(this, propertiesContent.toString(), Toast.LENGTH_LONG).show();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}