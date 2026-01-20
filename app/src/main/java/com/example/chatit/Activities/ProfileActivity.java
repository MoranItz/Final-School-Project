package com.example.chatit.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.chatit.R;
import com.example.chatit.Struct_Classes.ImageUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText userNameInput, userBioInput;
    private AppCompatButton saveProfileBtn, deleteAccountBtn;
    private ImageView profileImageView;
    private String currentUsername;
    private FirebaseFirestore db;
    
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Initializes the profile screen and loads the user's current data from Firestore.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_view);

        db = FirebaseFirestore.getInstance();
        currentUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", "");

        initViews();
        loadUserData();
        setupClickListeners();
    }

    // Connects UI elements to their corresponding views and configures username field as read-only.
    // Input: None.
    // Output: None.
    private void initViews() {
        userNameInput = findViewById(R.id.userNameInput);
        userBioInput = findViewById(R.id.userBioInput);
        saveProfileBtn = findViewById(R.id.saveProfileBtn);
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn);
        profileImageView = findViewById(R.id.profileImageView);
        
        // Make username non-editable (display only)
        userNameInput.setEnabled(false);
        userNameInput.setFocusable(false);
    }

    // Fetches the user's profile data from Firestore and populates the input fields.
    // Input: None.
    // Output: None.
    private void loadUserData() {
        if (currentUsername.isEmpty()) return;

        db.collection("users").document(currentUsername).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userNameInput.setText(currentUsername);
                        String bio = doc.getString("biography");
                        if (bio != null) {
                            userBioInput.setText(bio);
                        }
                        
                        // Load profile picture
                        String profilePictureBase64 = doc.getString("profilePicture");
                        if (profilePictureBase64 != null && !profilePictureBase64.isEmpty()) {
                            Bitmap bitmap = ImageUtils.convertBase64ToBitmap(profilePictureBase64);
                            if (bitmap != null) {
                                profileImageView.setImageBitmap(bitmap);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    // Sets up click listeners for back button, save button, and delete account button.
    // Input: None.
    // Output: None.
    private void setupClickListeners() {
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        saveProfileBtn.setOnClickListener(v -> saveProfile());
        deleteAccountBtn.setOnClickListener(v -> confirmDeleteAccount());
        
        // Click to select/change profile picture
        profileImageView.setOnClickListener(v -> showImagePickerDialog());
        findViewById(R.id.cameraIconOverlay).setOnClickListener(v -> showImagePickerDialog());
    }

    // Saves the updated biography to Firestore and displays a success or error message.
    // Input: None.
    // Output: None.
    private void saveProfile() {
        String biography = userBioInput.getText().toString().trim();

        db.collection("users").document(currentUsername)
                .update("biography", biography)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    // Displays a confirmation dialog to ensure the user wants to delete their account.
    // Input: None.
    // Output: None.
    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Deletes the user's account from Firestore, clears local session data, and returns to login screen.
    // Also cleans up groups: deletes owned groups and removes user from others.
    // Input: None.
    // Output: None.
    private void deleteAccount() {
        // Step 1: Handle all groups (owned or member)
        db.collection("groups").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                String owner = doc.getString("owner");
                String docId = doc.getId();

                if (currentUsername.equals(owner)) {
                    // Scenario 1: User is the owner -> Delete the whole group
                    db.collection("groups").document(docId).delete();
                } else {
                    // Scenario 2: User is a member -> Remove from members array
                    List<Map<String, Object>> members = (List<Map<String, Object>>) doc.get("members");
                    if (members != null) {
                        boolean wasMember = members.removeIf(m -> currentUsername.equals(m.get("username")));
                        if (wasMember) {
                            db.collection("groups").document(docId).update("members", members);
                        }
                    }
                }
            }

            // Step 2: Delete the user's main profile document
            db.collection("users").document(currentUsername)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Clear user session
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        prefs.edit().clear().apply();

                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

                        // Return to introduction
                        startActivity(new Intent(this, IntroductionActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete account document", Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error accessing groups for cleanup", Toast.LENGTH_SHORT).show();
        });
    }
    
    // Shows dialog to choose between camera or gallery
    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        
        new AlertDialog.Builder(this)
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else if (which == 1) {
                        checkGalleryPermissionAndOpen();
                    }
                })
                .show();
    }

    // Check and request camera permission
    private void checkCameraPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            } else {
                openCamera();
            }
        } else {
            openCamera();
        }
    }

    // Check and request gallery permission
    private void checkGalleryPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else {
            openGallery();
        }
    }

    // Opens device camera
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    // Opens gallery picker
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, determine which action to take
                for (String permission : permissions) {
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Handle image selection result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST || requestCode == CAMERA_REQUEST) {
                Uri imageUri;
                
                if (requestCode == CAMERA_REQUEST) {
                    // Camera returns bitmap in extras
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    // Convert bitmap to URI (save temporarily)
                    imageUri = getImageUri(imageBitmap);
                } else {
                    // Gallery returns URI directly
                    imageUri = data.getData();
                }
                
                if (imageUri != null) {
                    selectedImageUri = imageUri;
                    // Display the selected image
                    profileImageView.setImageURI(imageUri);
                    // Convert to base64 and save
                    uploadProfilePicture(imageUri);
                }
            }
        }
    }

    // Helper: Convert bitmap to URI
    private Uri getImageUri(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(path);
    }

    // Upload profile picture to Firestore as base64
    private void uploadProfilePicture(Uri imageUri) {
        String base64Image = ImageUtils.convertImageToBase64(this, imageUri);
        
        if (base64Image == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update Firestore with base64 image
        db.collection("users").document(currentUsername)
                .update("profilePicture", base64Image)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }
}
