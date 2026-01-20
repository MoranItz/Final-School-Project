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
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.chatit.R;
import com.example.chatit.Struct_Classes.ImageUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
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

    // This function is responsible for creating the activity and loading the user's data.
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

    // This function is responsible for connecting UI elements and setting initial states.
    // Input: None.
    // Output: None.
    private void initViews() {
        userNameInput = findViewById(R.id.userNameInput);
        userBioInput = findViewById(R.id.userBioInput);
        saveProfileBtn = findViewById(R.id.saveProfileBtn);
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn);
        profileImageView = findViewById(R.id.profileImageView);
        
        userNameInput.setEnabled(false);
        userNameInput.setFocusable(false);
    }

    // This function is responsible for fetching current user data from Firestore.
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

    // This function is responsible for setting up actions for buttons and images.
    // Input: None.
    // Output: None.
    private void setupClickListeners() {
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        saveProfileBtn.setOnClickListener(v -> saveProfile());
        deleteAccountBtn.setOnClickListener(v -> confirmDeleteAccount());
        
        profileImageView.setOnClickListener(v -> showImagePickerDialog());
        findViewById(R.id.cameraIconOverlay).setOnClickListener(v -> showImagePickerDialog());
    }

    // This function is responsible for updating the biography in Firestore.
    // Input: None.
    // Output: None.
    private void saveProfile() {
        String biographyText = userBioInput.getText().toString().trim();

        db.collection("users").document(currentUsername)
                .update("biography", biographyText)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    // This function is responsible for showing a final warning before account deletion.
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

    // This function is responsible for removing all user data from Firestore and local storage.
    // Input: None.
    // Output: None.
    private void deleteAccount() {
        db.collection("groups").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String ownerName = doc.getString("owner");
                String groupDocId = doc.getId();

                if (currentUsername.equals(ownerName)) {
                    db.collection("groups").document(groupDocId).delete();
                } else {
                    List<Map<String, Object>> membersList = (List<Map<String, Object>>) doc.get("members");
                    if (membersList != null) {
                        boolean wasInGroup = membersList.removeIf(member -> currentUsername.equals(member.get("username")));
                        if (wasInGroup) {
                            db.collection("groups").document(groupDocId).update("members", membersList);
                        }
                    }
                }
            }

            db.collection("users").document(currentUsername)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        preferences.edit().clear().apply();

                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

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
    
    // This function is responsible for offering ways to select a new profile image.
    // Input: None.
    // Output: None.
    private void showImagePickerDialog() {
        String[] pickOptions = {"Take Photo", "Choose from Gallery", "Cancel"};
        
        new AlertDialog.Builder(this)
                .setTitle("Select Profile Picture")
                .setItems(pickOptions, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else if (which == 1) {
                        checkGalleryPermissionAndOpen();
                    }
                })
                .show();
    }

    // This function is responsible for ensuring the app has permission to use the camera.
    // Input: None.
    // Output: None.
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

    // This function is responsible for ensuring the app has permission to access the image gallery.
    // Input: None.
    // Output: None.
    private void checkGalleryPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else {
            openGallery();
        }
    }

    // This function is responsible for starting the camera interface.
    // Input: None.
    // Output: None.
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    // This function is responsible for starting the gallery selection interface.
    // Input: None.
    // Output: None.
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    // This function is responsible for receiving the result from a permission request.
    // Input: int requestCode, String[] permissions, int[] grantResults.
    // Output: None.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    // This function is responsible for handling the image data returned by the camera or gallery.
    // Input: int requestCode, int resultCode, Intent data.
    // Output: None.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST || requestCode == CAMERA_REQUEST) {
                Uri imageUri;
                
                if (requestCode == CAMERA_REQUEST) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imageUri = getImageUri(imageBitmap);
                } else {
                    imageUri = data.getData();
                }
                
                if (imageUri != null) {
                    selectedImageUri = imageUri;
                    profileImageView.setImageURI(imageUri);
                    uploadProfilePicture(imageUri);
                }
            }
        }
    }

    // This function is responsible for creating a temporary Uri from a Bitmap.
    // Input: Bitmap bitmap (image from camera).
    // Output: Uri (reference to the image).
    private Uri getImageUri(Bitmap bitmap) {
        String imagePath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(imagePath);
    }

    // This function is responsible for uploading the new profile picture as Base64 to Firestore.
    // Input: Uri imageUri (reference to the image).
    // Output: None.
    private void uploadProfilePicture(Uri imageUri) {
        String base64ImageString = ImageUtils.convertImageToBase64(this, imageUri);
        
        if (base64ImageString == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        db.collection("users").document(currentUsername)
                .update("profilePicture", base64ImageString)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }
}
