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



    // Checks if a username is available in the Firestore database and creates a new user document if the name is not taken.
    // Input: User user (the new user data), Activity context (the registration activity).
    // Output: None.
    public static void registerUser(User user, Activity context) {
        // Create a reference to the document using the username as the unique identifier
        DocumentReference ref = FirebaseFirestore.getInstance().collection("users").document(user.getUsername());

        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e("FIREBASE", "Check failed: ", task.getException());
                return;
            }
            // Prevent duplicate accounts by verifying the document does not already exist
            if (task.getResult().exists()) {
                Toast.makeText(context, "Username '" + user.getUsername() + "' taken!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Save the user object to the database upon successful validation
            ref.set(user).addOnSuccessListener(aVoid -> {
                saveUserAndNavigate(user.getUsername(), user.getEmail(), user.getPassword(), context);
            }).addOnFailureListener(e -> Log.e("FIREBASE", "Error saving user", e));
        });
    }

    // Verifies the user's credentials against the Firestore database and logs them into the application if they match.
    // Input: String username (input name), String password (input password), Activity context (the login activity).
    // Output: None.
    public static void loginUser(String username, String password, Activity context) {
        FirebaseFirestore.getInstance().collection("users").document(username).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(context, "Login failed!", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentSnapshot doc = task.getResult();
            // Validate that the user exists in the database
            if (!doc.exists()) {
                Toast.makeText(context, "User doesn't exist!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Check if the stored password matches the user's input
            if (!password.equals(doc.getString("password"))) {
                Toast.makeText(context, "Wrong password!", Toast.LENGTH_SHORT).show();
                return;
            }
            saveUserAndNavigate(username, doc.getString("email"), password, context);
        });
    }

    // Stores the user's details in SharedPreferences for session persistence and redirects the user to the Home screen.
    // Input: String user (username), String email (user email), String pass (user password), Activity context (current activity).
    // Output: None.
    private static void saveUserAndNavigate(String user, String email, String pass, Activity context) {
        SharedPreferences.Editor editor = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit();
        // Update local session flags and credentials
        editor.putString("username", user);
        editor.putString("email", email);
        editor.putString("password", pass);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        // Clear the activity stack so the user cannot return to the login/register screens via the back button
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        context.finish();
    }
}
