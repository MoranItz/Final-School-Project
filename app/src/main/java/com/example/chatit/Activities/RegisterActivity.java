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

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInputField, emailInputField, passwordInputField;
    private Button registerActionButton;
    private TextView loginRedirectLink;

    // This function is responsible for setting up the registration screen and connecting listeners.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_view);

        initializeComponents();
        setupClickActions();
    }

    // This function is responsible for linking the Java objects to the XML layout components.
    // Input: None.
    // Output: None.
    private void initializeComponents() {
        usernameInputField = findViewById(R.id.nameInput);
        emailInputField = findViewById(R.id.emailInput);
        passwordInputField = findViewById(R.id.passwordInput);
        registerActionButton = findViewById(R.id.registerButton);
        loginRedirectLink = findViewById(R.id.loginLink);
    }

    // This function is responsible for defining the user interactions for registration and navigation.
    // Input: None.
    // Output: None.
    private void setupClickActions() {
        registerActionButton.setOnClickListener(v -> initiateRegistrationSequence());
        loginRedirectLink.setOnClickListener(v -> jumpBackToLogin());
    }

    // This function is responsible for closing the current screen and returning to the login activity.
    // Input: None.
    // Output: None.
    private void jumpBackToLogin() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    // This function is responsible for gathering data from inputs and registering a new account.
    // Input: None.
    // Output: None.
    private void initiateRegistrationSequence() {
        String inputName = usernameInputField.getText().toString().trim();
        String inputEmail = emailInputField.getText().toString().trim();
        String inputPassword = passwordInputField.getText().toString().trim();

        if (inputName.isEmpty() || inputEmail.isEmpty() || inputPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        User newlyCreatedUser = new User(inputName, inputPassword, inputEmail);
        User.registerUser(newlyCreatedUser, this);
    }
}
