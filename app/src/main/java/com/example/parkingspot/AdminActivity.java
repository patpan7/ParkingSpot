package com.example.parkingspot;


import android.content.Context;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class AdminActivity extends AppCompatActivity {

    private Spinner adminUserSpinner;
    private EditText newPasswordEditText;
    private Button saveButton;
    private Properties properties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        adminUserSpinner = findViewById(R.id.adminUserSpinner);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        saveButton = findViewById(R.id.saveButton);

        loadProperties();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedUser = adminUserSpinner.getSelectedItem().toString().toLowerCase();
                String newPassword = newPasswordEditText.getText().toString();

                if (!newPassword.isEmpty()) {
                    properties.setProperty(selectedUser, newPassword);
                    Toast.makeText(AdminActivity.this, selectedUser+" "+newPassword, Toast.LENGTH_SHORT).show();
                    saveProperties();
                    newPasswordEditText.setText("");
                    Toast.makeText(AdminActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AdminActivity.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
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