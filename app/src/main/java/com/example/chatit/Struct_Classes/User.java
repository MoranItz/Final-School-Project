package com.example.chatit.Struct_Classes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.chatit.Activities.HomeActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class User {
    private String username, password, email, biography, profilePicture;

    // CONSTRUCTOR
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // GETTERS
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getBiography() { return biography; }
    public String getProfilePicture() { return profilePicture; }

    // SETTERS
    public void setUsername(String username){ this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setBiography(String biography) { this.biography = biography; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }


    // This function is responsible for checking if a name is taken and adding a new user to Firestore.
    // Input: User newlyCreatedUser, Activity context.
    // Output: None.
    public static void registerUser(User newlyCreatedUser, Activity context) {
        DocumentReference userReference = FirebaseFirestore.getInstance().collection("users").document(newlyCreatedUser.getUsername());

        userReference.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            
            if (task.getResult().exists()) {
                Toast.makeText(context, "Username taken!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            userReference.set(newlyCreatedUser).addOnSuccessListener(aVoid -> {
                saveUserDataRemotely(newlyCreatedUser.getUsername(), newlyCreatedUser.getEmail(), newlyCreatedUser.getPassword(), context);
            });
        });
    }

    // This function is responsible for verifying the username and password in Firestore.
    // Input: String inputName, String inputPassword, Activity context.
    // Output: None.
    public static void loginUser(String inputName, String inputPassword, Activity context) {
        FirebaseFirestore.getInstance().collection("users").document(inputName).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(context, "Login failed!", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentSnapshot document = task.getResult();
            if (!document.exists()) {
                Toast.makeText(context, "User doesn't exist!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!inputPassword.equals(document.getString("password"))) {
                Toast.makeText(context, "Wrong password!", Toast.LENGTH_SHORT).show();
                return;
            }
            saveUserDataRemotely(inputName, document.getString("email"), inputPassword, context);
        });
    }

    // This function is responsible for saving user credentials locally and moving to the home screen.
    // Input: String name, String email, String password, Activity context.
    // Output: None.
    private static void saveUserDataRemotely(String name, String email, String password, Activity context) {
        SharedPreferences.Editor preferencesEditor = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit();
        preferencesEditor.putString("username", name);
        preferencesEditor.putString("email", email);
        preferencesEditor.putString("password", password);
        preferencesEditor.putBoolean("isLoggedIn", true);
        preferencesEditor.apply();

        Intent homeIntent = new Intent(context, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(homeIntent);
        context.finish();
    }
}
