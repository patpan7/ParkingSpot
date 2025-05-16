package com.example.parkingspot;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.parkingspot.databinding.ActivityAdminBinding;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private Properties properties;
    private static final String PROPERTIES_FILE = "user_credentials.properties";
    private static final String SECRET_BACKDOOR_CODE = "054909468";
    private DatabaseHelper databaseHelper;
    private boolean isRenaming = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        databaseHelper = new DatabaseHelper(this);
        loadProperties();
        initializeUI();
    }

    private void initializeUI() {
        setupUserManagement();
        setupReportSection();
        setupDatePickers();
    }

    private void setupUserManagement() {
        // Αρχικοποίηση spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, getAllParkingUsers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.adminUserSpinner.setAdapter(adapter);

        // Κουμπιά διαχείρισης
        binding.saveButton.setOnClickListener(v -> handleSaveAction());
        binding.renameUserButton.setOnClickListener(v -> toggleRenameMode());
    }

    private void handleSaveAction() {
        if (isRenaming) {
            renameUser();
        } else {
            updatePassword();
        }
    }

    private void toggleRenameMode() {
        isRenaming = !isRenaming;

        if (isRenaming) {
            binding.newUsernameLayout.setVisibility(View.VISIBLE);
            binding.newPasswordEditText.setHint("Νέος Κωδικός (Προαιρετικά)");
            binding.renameUserButton.setText("Ακύρωση");
            binding.saveButton.setText("Επιβεβαίωση");
        } else {
            resetRenameUI();
        }
    }

    private void resetRenameUI() {
        binding.newUsernameLayout.setVisibility(View.GONE);
        binding.newPasswordEditText.setHint("Νέος Κωδικός");
        binding.renameUserButton.setText("Μετανομασία");
        binding.saveButton.setText("Αποθήκευση");
        binding.newUsernameEditText.setText("");
        binding.newPasswordEditText.setText("");
        isRenaming = false;
    }

    private void renameUser() {
        String oldUser = binding.adminUserSpinner.getSelectedItem().toString();
        String newUser = binding.newUsernameEditText.getText().toString().trim().toLowerCase();
        String newPass = binding.newPasswordEditText.getText().toString().trim();

        if (newUser.isEmpty()) {
            showToast("Συμπληρώστε νέο όνομα χρήστη");
            return;
        }

        if (properties.containsKey(newUser)) {
            showToast("Το όνομα χρησιμοποιείται ήδη");
            return;
        }

        // Διατήρηση παλιού κωδικού αν δεν δοθεί νέος
        String password = newPass.isEmpty() ?
                properties.getProperty(oldUser) : newPass;

        properties.remove(oldUser);
        properties.setProperty(newUser, password);
        saveProperties();
        resetRenameUI();
        refreshUserList();
        showToast("Μετονομασία επιτυχής");
    }

    private void updatePassword() {
        String user = binding.adminUserSpinner.getSelectedItem().toString();
        String newPass = binding.newPasswordEditText.getText().toString().trim();

        if (newPass.isEmpty()) {
            showToast("Συμπληρώστε κωδικό");
            return;
        }

        properties.setProperty(user, newPass);
        saveProperties();
        binding.newPasswordEditText.setText("");
        showToast("Κωδικός ενημερώθηκε");
    }

    private void refreshUserList() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, getAllParkingUsers());
        binding.adminUserSpinner.setAdapter(adapter);
    }

    // Υπόλοιπες μέθοδοι
    private List<String> getAllParkingUsers() {
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

    private int getUserNumber(String username) {
        return Integer.parseInt(username.replace("parking_", ""));
    }

    private void loadProperties() {
        properties = new Properties();
        try {
            FileInputStream fis = openFileInput(PROPERTIES_FILE);
            properties.load(fis);
            fis.close();
        } catch (IOException e) {
            Log.e("Properties", "Σφάλμα φόρτωσης", e);
            loadFromAssets();
        }
    }

    private void loadFromAssets() {
        try {
            AssetManager am = getAssets();
            InputStream is = am.open(PROPERTIES_FILE);
            properties.load(is);
            is.close();
            saveProperties();
        } catch (IOException ex) {
            Log.e("Properties", "Σφάλμα φόρτωσης από assets", ex);
        }
    }

    private void saveProperties() {
        try {
            FileOutputStream fos = openFileOutput(PROPERTIES_FILE, Context.MODE_PRIVATE);
            properties.store(fos, null);
            fos.close();
        } catch (IOException e) {
            Log.e("Properties", "Σφάλμα αποθήκευσης", e);
        }
    }

    private void setupReportSection() {
        binding.recyclerViewReports.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewReports.setAdapter(new ReportAdapter(new ArrayList<>()));

        binding.btnGenerateReport.setOnClickListener(v -> generateReport());
    }

    private void generateReport() {
        String from = binding.etDateFrom.getText().toString();
        String to = binding.etDateTo.getText().toString();

        if (from.isEmpty() || to.isEmpty()) {
            showToast("Επιλέξτε ημερομηνίες");
            return;
        }

        List<Object> data = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getTickets(from, to)) {
            while (cursor.moveToNext()) {
                String operator = cursor.getString(0);
                int count = cursor.getInt(1);
                data.add(new ReportItem(operator, count));

                try (Cursor tickets = databaseHelper.getTicketsByOperator(operator, from, to)) {
                    while (tickets.moveToNext()) {
                        data.add(new TicketItem(
                                operator,
                                tickets.getString(2),
                                tickets.getString(3)
                        ));
                    }
                }
            }
        }
        ((ReportAdapter) binding.recyclerViewReports.getAdapter()).updateData(data);
    }

    private void setupDatePickers() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String today = sdf.format(Calendar.getInstance().getTime());
        binding.etDateFrom.setText(today);
        binding.etDateTo.setText(today);

        binding.etDateFrom.setOnClickListener(v -> showDatePicker(binding.etDateFrom));
        binding.etDateTo.setOnClickListener(v -> showDatePicker(binding.etDateTo));
    }

    private void showDatePicker(android.widget.EditText et) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(y, m, d);
            et.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}