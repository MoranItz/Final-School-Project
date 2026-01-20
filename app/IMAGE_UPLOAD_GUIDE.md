# Image Upload Implementation Guide

## Overview
This guide shows how to implement camera and gallery image upload for User Profile Pictures and Group Profile Pictures using base64 encoding.

## What's Already Set Up

### 1. Data Models
- **User class**: Has `profilePicture` field (String) with getter/setter
- **Group class**: Has `imageBase64` field (String) with getter/setter  
- **ImageUtils class**: Provides methods to convert images to/from base64

### 2. Message class base64 methods
- `Message.base64Encryption(String content)` - Converts string → base64
- `Message.base64Decryption(String content)` - Converts base64 → string

## Required Permissions (AndroidManifest.xml)

Add these permissions:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

## Implementation Steps

### Step 1: Update XML Layout

For **profile_view.xml**, replace the `profileIcon` ImageView with:

```xml
<!-- Circular profile picture -->
<ImageView
    android:id="@+id/profileImageView"
    android:layout_width="120dp"
    android:layout_height="120dp"
    android:layout_marginTop="24dp"
    android:src="@drawable/creategroup_icon"
    android:scaleType="centerCrop"
    android:background="@drawable/button_background"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@id/profileTitle" />

<!-- Camera icon overlay -->
<ImageView
    android:id="@+id/cameraIconOverlay"
    android:layout_width="36dp"
    android:layout_height="36dp"
    android:src="@android:drawable/ic_menu_camera"
    android:background="@drawable/button_background"
    android:padding="6dp"
    app:layout_constraintBottom_toBottomOf="@id/profileImageView"
    app:layout_constraintEnd_toEndOf="@id/profileImageView"
    app:tint="@color/black" />
```

### Step 2: Update ProfileActivity.java

Add these fields to the class:
```java
private ImageView profileImageView;
private Uri selectedImageUri;
private static final int PICK_IMAGE_REQUEST = 1;
private static final int CAMERA_REQUEST = 2;
private static final int PERMISSION_REQUEST_CODE = 100;
```

In `initViews()`:
```java
profileImageView = findViewById(R.id.profileImageView);
```

In `setupClickListeners()`:
```java
// Click to select/change profile picture
profileImageView.setOnClickListener(v -> showImagePickerDialog());
findViewById(R.id.cameraIconOverlay).setOnClickListener(v -> showImagePickerDialog());
```

Add these methods to ProfileActivity:

```java
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
```

In `loadUserData()` method, add code to load profile picture:

```java
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
```

Add these imports:
```java
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import com.example.chatit.Struct_Classes.ImageUtils;
```

### Step 3: For Group Profile Pictures

The same approach works for group profiles! Just:
1. Create a similar UI in `group_profile_view.xml` (if you have one)
2. Add the same image picker logic in the corresponding activity
3. Update Firestore group document with `imageBase64` field

Example for updating group image:
```java
db.collection("groups").document(String.valueOf(groupId))
        .update("imageBase64", base64Image)
        .addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Group picture updated!", Toast.LENGTH_SHORT).show();
        });
```

## Important Notes

1. **Base64 Size**: Images are automatically resized to max 800px and compressed to 80% quality to keep base64 strings reasonable
2. **Firestore Limits**: Max document size is 1MB. If images are too large even after compression, consider lowering quality in ImageUtils
3. **Optional Fields**: Both `profilePicture` and `imageBase64` are optional and can be null in Firestore
4. **Display Images**: Always check if base64 string is not null before converting to Bitmap

## Testing

1. Run the app
2. Navigate to Profile tab
3. Tap the profile image
4. Choose "Take Photo" or "Choose from Gallery"
5. Select/capture an image
6. Image should display immediately and save to Firestore
7. Restart app - image should persist

## Troubleshooting

- **Camera not opening**: Check AndroidManifest permissions
- **Image not saving**: Check Logcat for Firestore errors
- **Image quality poor**: Adjust compression quality in ImageUtils.java (line 32)
- **Firestore size error**: Reduce maxSize in ImageUtils.java (line 29) from 800 to 600 or 400
