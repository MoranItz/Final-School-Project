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

    // The variables from the layout file
    private EditText etUsername, etEmail, etPassword;
    public Button btnRegister;
    public TextView loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initiate layout instance
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_view);

        // Find the user input variables in the .xml file
        etUsername = findViewById(R.id.nameInput);
        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        btnRegister = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);


        // When the user Tries to register (Presses the button)
        btnRegister.setOnClickListener(v -> handleRegistration());
        loginLink.setOnClickListener(v -> navigateToLogin());
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        this.startActivity(intent);
        this.finish();
    }

    // This function is in charge of handling the user registration.
    // It is activated after the user tried to register using the button.
    // Input: None
    // Output: Void
    private void handleRegistration() {

        // Get the data from the .xml file view
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // If any of the variables are empty notify user of the error
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create new user object
        User newUser = new User(username, password, email);
        // Register user using the "registerUser" function
        User.registerUser(newUser, this);
    }
}
