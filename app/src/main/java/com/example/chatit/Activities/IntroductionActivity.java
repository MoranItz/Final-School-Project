package com.example.chatit.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatit.R;

public class IntroductionActivity extends AppCompatActivity {

    private Button getStartedButton;

    // This function is responsible for initializing the introduction screen.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction_view);

        initializeInterface();
    }

    // This function is responsible for connecting the UI button and setting its click action.
    // Input: None.
    // Output: None.
    private void initializeInterface() {
        getStartedButton = findViewById(R.id.getStartedBtn);
        getStartedButton.setOnClickListener(v -> jumpToLoginScreen());
    }

    // This function is responsible for navigating the user to the login screen and closing this activity.
    // Input: None.
    // Output: None.
    private void jumpToLoginScreen() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }
}
