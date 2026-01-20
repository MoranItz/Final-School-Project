package com.example.chatit.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatit.R;
import com.example.chatit.Struct_Classes.User;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInputField, passwordInputField;
    private Button loginActionButton;
    private TextView registrationLink;

    // This function is responsible for setting up the login screen and its interactions.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_view);

        initializeWidgets();
        setupInteractionListeners();
    }

    // This function is responsible for connecting the Java variables to the XML layout components.
    // Input: None.
    // Output: None.
    private void initializeWidgets() {
        usernameInputField = findViewById(R.id.usernameInput);
        passwordInputField = findViewById(R.id.passwordInput);
        loginActionButton = findViewById(R.id.loginButton);
        registrationLink = findViewById(R.id.registerLink);
    }

    // This function is responsible for defining what happens when the login button or register link is clicked.
    // Input: None.
    // Output: None.
    private void setupInteractionListeners() {
        loginActionButton.setOnClickListener(v -> performLoginSequence());
        registrationLink.setOnClickListener(v -> jumpToRegistrationScreen());
    }

    // This function is responsible for closing the login activity and opening the registration activity.
    // Input: None.
    // Output: None.
    private void jumpToRegistrationScreen() {
        Intent registrationIntent = new Intent(this, RegisterActivity.class);
        startActivity(registrationIntent);
        finish();
    }

    // This function is responsible for validating the user input and attempting a login through Firestore.
    // Input: None.
    // Output: None.
    private void performLoginSequence() {
        String inputUsername = usernameInputField.getText().toString().trim();
        String inputPassword = passwordInputField.getText().toString().trim();

        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        User.loginUser(inputUsername, inputPassword, this);
    }
}
