package com.example.chatit.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.chatit.R;

public class SettingsActivity extends AppCompatActivity {

    // Initializes the settings screen and sets up button listeners.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);

        setupClickListeners();
    }

    // Sets up click listeners for the back button and logout button.
    // Input: None.
    // Output: None.
    private void setupClickListeners() {
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        AppCompatButton logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(v -> logout());
    }

    // Logs the user out by clearing the login status and returning to the introduction screen.
    // Input: None.
    // Output: None.
    private void logout() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isLoggedIn", false).apply();

        startActivity(new Intent(this, IntroductionActivity.class));
        finish();
    }
}
