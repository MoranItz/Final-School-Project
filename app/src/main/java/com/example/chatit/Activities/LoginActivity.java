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

    // The variables from the layout file
    private EditText etUsername, etPassword;
    public Button btnLogin;
    public TextView registerLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initiate layout instance
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_view);

        // Find the user input variables in the .xml file
        etUsername = findViewById(R.id.usernameInput);
        etPassword = findViewById(R.id.passwordInput);
        btnLogin = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);


        // When the user Tries to register (Presses the button)
        btnLogin.setOnClickListener(v -> handleLogin());
        registerLink.setOnClickListener(v -> navigateToRegister());
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        this.startActivity(intent);
        this.finish();
    }

    // This function is in charge of handling the user login.
    // It is activated after the user tried to login using the button.
    // Input: None
    // Output: Void
    private void handleLogin() {

        // Get the data from the .xml file view
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // If any of the variables are empty notify user of the error
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log in the user using the "loginUser" function
        User.loginUser(username, password, this);
    }
}
