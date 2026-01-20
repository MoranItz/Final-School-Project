package com.example.chatit.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.chatit.R;

public class SettingsActivity extends AppCompatActivity {

    // This function is responsible for creating the settings activity and initializing the layout.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);

        initializeActionListeners();
    }

    // This function is responsible for setting up actions for the back and logout buttons.
    // Input: None.
    // Output: None.
    private void initializeActionListeners() {
        ImageView backActionButton = findViewById(R.id.backBtn);
        backActionButton.setOnClickListener(v -> finish());

        AppCompatButton logoutActionButton = findViewById(R.id.logoutBtn);
        logoutActionButton.setOnClickListener(v -> performUserLogout());
    }

    // This function is responsible for clearing the user's login session and returning to the intro screen.
    // Input: None.
    // Output: None.
    private void performUserLogout() {
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        preferences.edit().putBoolean("isLoggedIn", false).apply();

        stopService(new Intent(this, com.example.chatit.Services.MessageNotificationService.class));

        startActivity(new Intent(this, IntroductionActivity.class));
        finish();
    }
}
