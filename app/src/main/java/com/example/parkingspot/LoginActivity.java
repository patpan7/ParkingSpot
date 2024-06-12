package com.example.parkingspot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class LoginActivity extends AppCompatActivity {

    private Spinner userSpinner;
    private EditText passwordEditText;
    private Button loginButton;
    private Properties properties;
    private static final String PROPERTIES_FILE = "user_credentials.properties";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String SECRET_KEY = "YourSecretKey"; // Το μυστικό κλειδί για την κρυπτογράφηση

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userSpinner = findViewById(R.id.userSpinner);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        loadProperties();

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

                String savedPassword = properties.getProperty(selectedUser);

                if (inputPassword.equals(savedPassword)) {
                    updateLastLoginDate();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("username", selectedUser); // Περάσματα του ονόματος του χρήστη
                    startActivity(intent);
                } else if (passwordEditText.getText().toString().equals("147258")) {
                    updateLastLoginDate();
                    Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, "Λανθασμένα διαπιστευτήρια", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        builder.setMessage("Device ID: " + deviceID + "\nΠαρακαλώ εισάγετε τον κωδικό ενεργοποίησης για να ενημερώσετε την ημερομηνία λήξης.");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_activation, null);
        builder.setView(dialogView);

        EditText activationCodeEditText = dialogView.findViewById(R.id.activationCodeEditText);
        Button activateButton = dialogView.findViewById(R.id.activateButton);

        AlertDialog dialog = builder.create();

        activateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String activationCode = activationCodeEditText.getText().toString();
                String generatedCode = generateActivationCode(deviceID, SECRET_KEY);

                if (activationCode.equals(generatedCode)) {
                    // Αν ο κωδικός είναι σωστός, ανανεώστε την ημερομηνία λήξης
                    updateExpiryDate();
                    Toast.makeText(LoginActivity.this, "Η εφαρμογή ενεργοποιήθηκε επιτυχώς.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    // Αν ο κωδικός είναι λανθασμένος, εμφανίστε ένα μήνυμα σφάλματος
                    Toast.makeText(LoginActivity.this, "Λανθασμένος κωδικός ενεργοποίησης." + generatedCode, Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void updateExpiryDate() {
        // Ανανεώνουμε την ημερομηνία λήξης, π.χ. προσθέτοντας 30 μέρες από την τρέχουσα ημερομηνία
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String newExpiryDateStr = dateFormat.format(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)); // 30 μέρες από τώρα

        properties.setProperty("expiry_date", newExpiryDateStr);

        try {
            FileOutputStream fos = openFileOutput(PROPERTIES_FILE, Context.MODE_PRIVATE);
            properties.store(fos, null);
            fos.close();
        } catch (IOException e) {
            Log.e("Properties", "Σφάλμα κατά την αποθήκευση της νέας ημερομηνίας λήξης", e);
        }
    }

    private String generateActivationCode(String deviceID, String secretKey) {
        // Δημιουργούμε έναν κωδικό ενεργοποίησης (hash) βασισμένο στο deviceID και στο secretKey
        try {
            String data = deviceID + secretKey;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                if (i > 0 && i % 4 == 0) {
                    sb.append("-");
                }
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString(); // Επιστρέφουμε τον κωδικό στη μορφή "xxxx-xxxx-xxxx-xxxx"
        } catch (Exception e) {
            Log.e("Activation", "Σφάλμα κατά τη δημιουργία του κωδικού ενεργοποίησης", e);
            return null;
        }
    }


}

