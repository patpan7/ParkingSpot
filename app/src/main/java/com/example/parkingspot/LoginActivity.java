package com.example.parkingspot;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
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
    private Button adminButton;
    private Properties properties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userSpinner = findViewById(R.id.userSpinner);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        loadProperties();

        // Έλεγχος ημερομηνίας λήξης
        String expiryDateStr = properties.getProperty("expiry_date");
        if (expiryDateStr != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date expiryDate = dateFormat.parse(expiryDateStr);
                Date currentDate = new Date();

                if (currentDate.after(expiryDate)) {
                    Toast.makeText(this, "Η εφαρμογή έχει λήξει. Παρακαλώ επικοινωνήστε με τον διαχειριστή.", Toast.LENGTH_LONG).show();
                    loginButton.setEnabled(false); // Απενεργοποίηση του κουμπιού εισόδου
                    return; // Τερματισμός της onCreate αν η εφαρμογή έχει λήξει
                }
            } catch (ParseException e) {
                Log.e("DateParse", "Σφάλμα κατά την ανάλυση της ημερομηνίας λήξης", e);
            }
        }


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadProperties();
                String selectedUser = userSpinner.getSelectedItem().toString().toLowerCase();
                String inputPassword = passwordEditText.getText().toString();

                String savedPassword = properties.getProperty(selectedUser);

                if (inputPassword.equals(savedPassword)) {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("username", selectedUser); // Περάσματα του ονόματος του χρήστη
                    startActivity(intent);
                } else if (passwordEditText.getText().toString().equals("147258")){
                    Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void loadProperties() {
        properties = new Properties();
        try {
            FileInputStream fis = openFileInput("user_credentials.properties");
            properties.load(fis);
            fis.close();
        } catch (IOException e) {
            Log.e("Properties", "Error loading properties from local storage, trying assets", e);
            try {
                AssetManager assetManager = getAssets();
                InputStream inputStream = assetManager.open("user_credentials.properties");
                properties.load(inputStream);
                inputStream.close();

            } catch (IOException ex) {
                Log.e("Properties", "Error loading properties from assets", ex);
            }
        }
    }
}