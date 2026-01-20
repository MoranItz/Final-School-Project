package com.example.chatit.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.chatit.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private EditText userNameInput, userBioInput;
    private AppCompatButton saveProfileBtn, deleteAccountBtn;
    private String currentUsername;
    private FirebaseFirestore db;

    // Initializes the profile screen and loads the user's current data from Firestore.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_view);

        db = FirebaseFirestore.getInstance();
        currentUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", "");

        initViews();
        loadUserData();
        setupClickListeners();
    }

    // Connects UI elements to their corresponding views and configures username field as read-only.
    // Input: None.
    // Output: None.
    private void initViews() {
        userNameInput = findViewById(R.id.userNameInput);
        userBioInput = findViewById(R.id.userBioInput);
        saveProfileBtn = findViewById(R.id.saveProfileBtn);
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn);
        
        // Make username non-editable (display only)
        userNameInput.setEnabled(false);
        userNameInput.setFocusable(false);
    }

    // Fetches the user's profile data from Firestore and populates the input fields.
    // Input: None.
    // Output: None.
    private void loadUserData() {
        if (currentUsername.isEmpty()) return;

        db.collection("users").document(currentUsername).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userNameInput.setText(currentUsername);
                        String bio = doc.getString("bio");
                        if (bio != null) {
                            userBioInput.setText(bio);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    // Sets up click listeners for back button, save button, and delete account button.
    // Input: None.
    // Output: None.
    private void setupClickListeners() {
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        saveProfileBtn.setOnClickListener(v -> saveProfile());
        deleteAccountBtn.setOnClickListener(v -> confirmDeleteAccount());
    }

    // Saves the updated biography to Firestore and displays a success or error message.
    // Input: None.
    // Output: None.
    private void saveProfile() {
        String biography = userBioInput.getText().toString().trim();

        db.collection("users").document(currentUsername)
                .update("biography", biography)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    // Displays a confirmation dialog to ensure the user wants to delete their account.
    // Input: None.
    // Output: None.
    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Deletes the user's account from Firestore, clears local session data, and returns to login screen.
    // Input: None.
    // Output: None.
    private void deleteAccount() {
        db.collection("users").document(currentUsername)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Clear user session
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                    
                    // Return to introduction
                    startActivity(new Intent(this, IntroductionActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                });
    }
}
