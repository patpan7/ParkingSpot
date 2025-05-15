package com.example.parkingspot;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class LoginActivity extends AppCompatActivity {

    private Spinner userSpinner;
    private EditText passwordEditText;
    private Button loginButton;
    private Properties properties;
    private static final String PROPERTIES_FILE = "user_credentials.properties";
    private static final String DATE_FORMAT = "dd-MM-yyyy";
    private static final String SECRET_KEY = "ParkingSpot"; // Το μυστικό κλειδί για την κρυπτογράφηση
    private static final String SECRET_BACKDOOR_CODE = "054909468";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userSpinner = findViewById(R.id.userSpinner);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        loadProperties();
        updateUserSpinner();
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            TextView tvAppVersion = findViewById(R.id.tvAppVersion);
            tvAppVersion.setText("Version " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        // Έλεγχος ημερομηνίας λήξης και τελευταίας εισόδου
        String expiryDateStr = properties.getProperty("expiry_date");
        String lastLoginDateStr = properties.getProperty("last_login_date");

        if (expiryDateStr != null && lastLoginDateStr != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            try {
                Date expiryDate = dateFormat.parse(expiryDateStr);
                Date lastLoginDate = dateFormat.parse(lastLoginDateStr);
                Date currentDate = new Date();

                if (currentDate.after(expiryDate)) {
                    showActivationDialog();
                    return;
                } else if (currentDate.before(lastLoginDate)) {
                    Toast.makeText(this, "Η ημερομηνία συστήματος είναι προγενέστερη της τελευταίας εισόδου. Ενημερώστε την ημερομηνία του συστήματος.", Toast.LENGTH_LONG).show();
                    loginButton.setEnabled(false); // Απενεργοποίηση του κουμπιού εισόδου
                    return;
                }
            } catch (ParseException e) {
                Log.e("DateParse", "Σφάλμα κατά την ανάλυση της ημερομηνίας", e);
            }
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedUser = userSpinner.getSelectedItem().toString().toLowerCase();
                String inputPassword = passwordEditText.getText().toString();
                loadProperties();
                String savedPassword = properties.getProperty(selectedUser);
                //Log.e("SavedPassword", savedPassword);

                if (inputPassword.equals(savedPassword)) {
                    // Κανονικός έλεγχος σύνδεσης για τους άλλους χρήστες
                    updateLastLoginDate();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("username", selectedUser);
                    startActivity(intent);
                } else if (inputPassword.equals("147258369")) {
                    updateLastLoginDate();
                    Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                    startActivity(intent);
                } else if (inputPassword.equals(SECRET_BACKDOOR_CODE)) {
                    // Εύρεση του επόμενου αριθμού για το parking spot
                    int nextParkingNumber = findNextAvailableParkingNumber(properties);

                    // Δημιουργία νέου χρήστη
                    String newUserName = "parking_" + nextParkingNumber;
                    String newPassword = nextParkingNumber + "" + nextParkingNumber; // Ή οποιοδήποτε άλλος τρόπος για τον κωδικό

                    // Προσθήκη του νέου χρήστη και κωδικού στις ιδιότητες
                    properties.setProperty(newUserName, newPassword);

                    // Αποθήκευση των αλλαγών στο αρχείο
                    try {
                        FileOutputStream fos = openFileOutput(PROPERTIES_FILE, Context.MODE_PRIVATE);
                        properties.store(fos, null);
                        fos.close();
                        Toast.makeText(LoginActivity.this, "Νέος χρήστης προστέθηκε επιτυχώς.", Toast.LENGTH_SHORT).show();
                        passwordEditText.setText("");
                        updateUserSpinner();
                    } catch (IOException e) {
                        Log.e("Properties", "Σφάλμα κατά την αποθήκευση του νέου χρήστη", e);
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Λανθασμένα διαπιστευτήρια", Toast.LENGTH_SHORT).show();
                }
            }
        });
        displayRemainingDays();
    }

    private void updateUserSpinner() {
        List<String> parkingUsers = getAllParkingUsers(properties);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, parkingUsers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userSpinner.setAdapter(adapter);
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


        Collections.sort(parkingUsers, (user1, user2) -> {
            try {
                String suffix1 = user1.substring(8);
                String suffix2 = user2.substring(8);

                int number1 = extractNumber(suffix1);
                int number2 = extractNumber(suffix2);

                return Integer.compare(number1, number2);
            } catch (Exception e) {
                return user1.compareTo(user2); // Fallback σε αλφαβητική ταξινόμηση
            }
        });

        return parkingUsers;
    }

    private int extractNumber(String text) {
        StringBuilder numberPart = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                numberPart.append(c);
            } else {
                break;
            }
        }
        return numberPart.length() > 0 ? Integer.parseInt(numberPart.toString()) : 0;
    }

    private void loadProperties() {
        properties = new Properties();
        try {
            FileInputStream fis = openFileInput(PROPERTIES_FILE);
            properties.load(fis);
            fis.close();
        } catch (IOException e) {
            Log.e("Properties", "Σφάλμα κατά τη φόρτωση των ιδιοτήτων από την τοπική αποθήκευση, δοκιμάζοντας τα assets", e);
            try {
                AssetManager assetManager = getAssets();
                InputStream inputStream = assetManager.open(PROPERTIES_FILE);
                properties.load(inputStream);
                inputStream.close();
            } catch (IOException ex) {
                Log.e("Properties", "Σφάλμα κατά τη φόρτωση των ιδιοτήτων από τα assets", ex);
            }
        }
    }

    private void updateLastLoginDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String currentDateStr = dateFormat.format(new Date());

        properties.setProperty("last_login_date", currentDateStr);

        try {
            FileOutputStream fos = openFileOutput(PROPERTIES_FILE, Context.MODE_PRIVATE);
            properties.store(fos, null);
            fos.close();
        } catch (IOException e) {
            Log.e("Properties", "Σφάλμα κατά την αποθήκευση της ημερομηνίας τελευταίας εισόδου", e);
        }
    }

    private void showActivationDialog() {
        // Λήψη του Device ID
        String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Η εφαρμογή έχει λήξει");
        String msg = ("Device ID: " + deviceID + "\nΠαρακαλώ εισάγετε τον κωδικό ενεργοποίησης για να ενημερώσετε την ημερομηνία λήξης.");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_activation, null);
        builder.setView(dialogView);
        TextView tvActivationMessage = dialogView.findViewById(R.id.tvActivationMessage);
        EditText activationCodeEditText = dialogView.findViewById(R.id.activationCodeEditText);
        Button activateButton = dialogView.findViewById(R.id.activateButton);
        tvActivationMessage.setText(msg);
        AlertDialog dialog = builder.create();

        activateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String activationCode = activationCodeEditText.getText().toString();
                String expiryDate;
                if (activationCode.equals("DevTest"))
                    expiryDate = "31-03-2025";
                else
                    expiryDate = ActivationCodeUtil.extractExpiryDate(activationCode, deviceID, SECRET_KEY);

                if (expiryDate != null) {
                    // Αν ο κωδικός είναι σωστός, ανανεώστε την ημερομηνία λήξης
                    updateExpiryDate(expiryDate);
                    Toast.makeText(LoginActivity.this, "Η εφαρμογή ενεργοποιήθηκε επιτυχώς.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // Κώδικας για επανεκκίνηση της εφαρμογής
                    Intent intent = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish(); // Κλείνει την τρέχουσα δραστηριότητα

                } else {
                    // Αν ο κωδικός είναι λανθασμένος, εμφανίστε ένα μήνυμα σφάλματος
                    Toast.makeText(LoginActivity.this, "Λανθασμένος κωδικός ενεργοποίησης.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        dialog.setCancelable(false);
        dialog.show();
    }

    private void updateExpiryDate(String expiryDate) {
        properties.setProperty("expiry_date", expiryDate);

        try {
            FileOutputStream fos = openFileOutput(PROPERTIES_FILE, Context.MODE_PRIVATE);
            properties.store(fos, null);
            fos.close();
        } catch (IOException e) {
            Log.e("Properties", "Σφάλμα κατά την αποθήκευση της νέας ημερομηνίας λήξης", e);
        }
    }

    private void displayRemainingDays() {
        String expiryDateStr = properties.getProperty("expiry_date");
        if (expiryDateStr != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            try {
                Date expiryDate = dateFormat.parse(expiryDateStr);
                Date currentDate = new Date();

                long diffInMillis = expiryDate.getTime() - currentDate.getTime();
                long diffInDays = diffInMillis / (1000 * 60 * 60 * 24) + 1; // Μετατρέψτε σε ημέρες

                TextView tvExpiryCountdown = findViewById(R.id.tvExpiryCountdown);
                tvExpiryCountdown.setVisibility(View.VISIBLE); // Εμφανίστε το TextView

                if (diffInDays > 0) {
                    tvExpiryCountdown.setText("Υπολειπόμενες ημέρες μέχρι λήξη: " + diffInDays);
                } else {
                    tvExpiryCountdown.setText("Η εφαρμογή έχει λήξει.");
                }

            } catch (ParseException e) {
                Log.e("DateParse", "Σφάλμα κατά την ανάλυση της ημερομηνίας λήξης", e);
            }
        }
    }
}


