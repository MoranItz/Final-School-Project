package com.example.chatit.Activities.LoginAndRegistration;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatit.R;

public class IntroductionActivity extends AppCompatActivity {

    // The variables from the layout file
    public Button getStartedBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initiate layout instance
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction_view);

        // Find the user input variables in the .xml file
        getStartedBtn = findViewById(R.id.getStartedBtn);


        // When the user Tries to register move him to the "handleRegister" function
        getStartedBtn.setOnClickListener(v -> navigateToLogin());
    }

    // This function starts a new Intent for the "LoginActivity" window
    private void navigateToLogin() {
        // Initiates the Intent to LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        this.startActivity(intent);
        this.finish();
    }
}
