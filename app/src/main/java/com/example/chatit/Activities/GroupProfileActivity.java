package com.example.chatit.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatit.R;
import com.example.chatit.Struct_Classes.ImageUtils;
import com.example.chatit.Struct_Classes.User;
import com.example.chatit.Adapters.MemberAdapter;
import com.example.chatit.Adapters.UserSearchAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import android.widget.EditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupProfileActivity extends AppCompatActivity {

    private ImageView groupImageView, backBtn, addMemberBtn;
    private TextView groupNameDisplay;
    private androidx.appcompat.widget.AppCompatButton actionButton;
    private RecyclerView membersRecyclerView;
    private MemberAdapter groupMemberAdapter;
    private List<User> groupMemberList;
    
    private int groupId;
    private String groupOwner;
    private String currentUsername;
    private FirebaseFirestore db;
    private List<Map<String, Object>> rawMembersData;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // This function is responsible for creating the activity and setting up the basic group information.
    // Input: Bundle savedInstanceState (state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_profile_view);

        db = FirebaseFirestore.getInstance();
        groupId = getIntent().getIntExtra("groupId", -1);
        currentUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", "");

        if (groupId == -1) {
            Toast.makeText(this, "Error: Group not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadGroupData();
        initializeClickListeners();
    }

    // This function is responsible for connecting the UI elements in the XML to the Java objects.
    // Input: None.
    // Output: None.
    private void initializeViews() {
        groupImageView = findViewById(R.id.groupImageView);
        backBtn = findViewById(R.id.backBtn);
        addMemberBtn = findViewById(R.id.addMemberBtn);
        groupNameDisplay = findViewById(R.id.groupNameDisplay);
        membersRecyclerView = findViewById(R.id.groupMembersRecyclerView);
        actionButton = findViewById(R.id.actionButton);

        groupMemberList = new ArrayList<>();
        groupMemberAdapter = new MemberAdapter(groupMemberList, user -> {
            if (currentUsername.equals(groupOwner) && !user.getUsername().equals(currentUsername)) {
                showRemovalConfirmationDialog(user.getUsername());
            }
        });
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersRecyclerView.setAdapter(groupMemberAdapter);
    }

    // This function is responsible for fetching group details from Firestore and updating the UI.
    // Input: None.
    // Output: None.
    private void loadGroupData() {
        db.collection("groups").document(String.valueOf(groupId)).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("groupName");
                        groupOwner = document.getString("owner");
                        groupNameDisplay.setText(name);

                        if (currentUsername.equals(groupOwner)) {
                            actionButton.setText("Delete Group");
                            addMemberBtn.setVisibility(View.VISIBLE);
                        } else {
                            actionButton.setText("Leave Group");
                            addMemberBtn.setVisibility(View.GONE);
                        }

                        String base64String = document.getString("imageBase64");
                        if (base64String != null && !base64String.isEmpty()) {
                            Bitmap bitmap = ImageUtils.convertBase64ToBitmap(base64String);
                            if (bitmap != null) {
                                groupImageView.setImageBitmap(bitmap);
                            }
                        }

                        fetchAndDisplayMembers(document);
                        
                        if (!currentUsername.equals(groupOwner)) {
                            findViewById(R.id.cameraIconOverlay).setVisibility(View.GONE);
                        }
                    }
                });
    }

    // This function is responsible for extracting the list of members from the group document.
    // Input: DocumentSnapshot groupDoc (the group data from Firestore).
    // Output: None.
    @SuppressWarnings("unchecked")
    private void fetchAndDisplayMembers(DocumentSnapshot groupDoc) {
        rawMembersData = (List<Map<String, Object>>) groupDoc.get("members");
        if (rawMembersData == null) return;

        groupMemberList.clear();
        for (Map<String, Object> data : rawMembersData) {
            String username = (String) data.get("username");
            db.collection("users").document(username).get().addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    User userObject = new User(
                            userDoc.getString("username"),
                            userDoc.getString("password"),
                            userDoc.getString("email")
                    );
                    userObject.setBiography(userDoc.getString("biography"));
                    userObject.setProfilePicture(userDoc.getString("profilePicture"));
                    groupMemberList.add(userObject);
                    groupMemberAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    // This function is responsible for setting up actions for interactive UI elements.
    // Input: None.
    // Output: None.
    private void initializeClickListeners() {
        backBtn.setOnClickListener(v -> finish());

        View cameraOverlay = findViewById(R.id.cameraIconOverlay);
        cameraOverlay.setOnClickListener(v -> {
            if (currentUsername.equals(groupOwner)) {
                showImageSelectionDialog();
            } else {
                Toast.makeText(this, "Only the owner can change the group photo", Toast.LENGTH_SHORT).show();
            }
        });

        actionButton.setOnClickListener(v -> {
            if (currentUsername.equals(groupOwner)) {
                showGroupDeletionConfirmation();
            } else {
                showLeaveGroupConfirmation();
            }
        });

        addMemberBtn.setOnClickListener(v -> displayAddMemberDialog());
    }

    // This function is responsible for showing a confirmation message before deleting a group.
    // Input: None.
    // Output: None.
    private void showGroupDeletionConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Group")
                .setMessage("Are you sure you want to delete this group? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> executeGroupDeletion())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // This function is responsible for removing the group document from Firestore.
    // Input: None.
    // Output: None.
    private void executeGroupDeletion() {
        db.collection("groups").document(String.valueOf(groupId)).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show();
                    Intent homeIntent = new Intent(this, HomeActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show());
    }

    // This function is responsible for showing a confirmation message before a member leaves the group.
    // Input: None.
    // Output: None.
    private void showLeaveGroupConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Leave", (dialog, which) -> executeLeaveGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // This function is responsible for removing the user from the group members list in Firestore.
    // Input: None.
    // Output: None.
    private void executeLeaveGroup() {
        rawMembersData.removeIf(member -> currentUsername.equals(member.get("username")));
        db.collection("groups").document(String.valueOf(groupId))
                .update("members", rawMembersData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "You left the group", Toast.LENGTH_SHORT).show();
                    Intent homeIntent = new Intent(this, HomeActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to leave group", Toast.LENGTH_SHORT).show());
    }

    // This function is responsible for showing a confirmation message before kicking a member.
    // Input: String username (the user to be removed).
    // Output: None.
    private void showRemovalConfirmationDialog(String username) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to kick " + username + " from the group?")
                .setPositiveButton("Kick", (dialog, which) -> kickMember(username))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // This function is responsible for removing a specific member from the group list in Firestore.
    // Input: String username (the user to be removed).
    // Output: None.
    private void kickMember(String username) {
        rawMembersData.removeIf(member -> username.equals(member.get("username")));
        db.collection("groups").document(String.valueOf(groupId))
                .update("members", rawMembersData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, username + " was removed", Toast.LENGTH_SHORT).show();
                    loadGroupData();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove user", Toast.LENGTH_SHORT).show());
    }

    // This function is responsible for presenting options to the user for choosing a new group image.
    // Input: None.
    // Output: None.
    private void showImageSelectionDialog() {
        String[] pickOptions = {"Take Photo", "Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Update Group Photo")
                .setItems(pickOptions, (dialog, which) -> {
                    if (which == 0) checkCameraPermissionAndOpen();
                    else if (which == 1) checkGalleryPermissionAndOpen();
                }).show();
    }

    // This function is responsible for requesting camera access from the user.
    // Input: None.
    // Output: None.
    private void checkCameraPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            } else launchCamera();
        } else launchCamera();
    }

    // This function is responsible for requesting storage or media access from the user.
    // Input: None.
    // Output: None.
    private void checkGalleryPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else launchGallery();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else launchGallery();
        } else launchGallery();
    }

    // This function is responsible for launching the device camera application.
    // Input: None.
    // Output: None.
    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    // This function is responsible for launching the device image gallery.
    // Input: None.
    // Output: None.
    private void launchGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    // This function is responsible for handling the results from the camera or gallery activities.
    // Input: int requestCode, int resultCode, Intent data.
    // Output: None.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri photoUri = null;
            if (requestCode == CAMERA_REQUEST) {
                Bitmap photoBitmap = (Bitmap) data.getExtras().get("data");
                photoUri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), photoBitmap, "GroupPic", null));
            } else if (requestCode == PICK_IMAGE_REQUEST) {
                photoUri = data.getData();
            }

            if (photoUri != null) {
                groupImageView.setImageURI(photoUri);
                updateGroupPhoto(photoUri);
            }
        }
    }

    // This function is responsible for showing a dialog to find and add new users to the group.
    // Input: None.
    // Output: None.
    private void displayAddMemberDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.add_member_dialog, null);
        AlertDialog addMemberDialog = new AlertDialog.Builder(this).setView(dialogView).create();

        EditText searchInputField = dialogView.findViewById(R.id.searchUserInput);
        RecyclerView usersListRv = dialogView.findViewById(R.id.usersRecyclerView);
        androidx.appcompat.widget.AppCompatButton closeActionButton = dialogView.findViewById(R.id.closeDialogBtn);
        androidx.appcompat.widget.AppCompatButton confirmActionButton = dialogView.findViewById(R.id.confirmAddBtn);

        List<String> userSearchResults = new ArrayList<>();
        Set<String> selectedNamesToUpdate = new HashSet<>();
        UserSearchAdapter userSearchAdapter = new UserSearchAdapter(userSearchResults, selectedNamesToUpdate);
        
        usersListRv.setLayoutManager(new LinearLayoutManager(this));
        usersListRv.setAdapter(userSearchAdapter);

        searchInputField.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) performUserSearch(s.toString(), userSearchAdapter, userSearchResults);
                else { userSearchResults.clear(); userSearchAdapter.notifyDataSetChanged(); }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        closeActionButton.setOnClickListener(v -> addMemberDialog.dismiss());
        confirmActionButton.setOnClickListener(v -> {
            if (selectedNamesToUpdate.isEmpty()) {
                Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
                return;
            }
            addChosenMembers(selectedNamesToUpdate, addMemberDialog);
        });

        addMemberDialog.show();
    }

    // This function is responsible for querying Firestore for users that match a search string.
    // Input: String query, UserSearchAdapter adapter, List<String> results.
    // Output: None.
    private void performUserSearch(String query, UserSearchAdapter adapter, List<String> results) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .addOnSuccessListener(snapshots -> {
                    results.clear();
                    Set<String> currentMemberUsernames = new HashSet<>();
                    for (User member : groupMemberList) currentMemberUsernames.add(member.getUsername());

                    for (DocumentSnapshot doc : snapshots) {
                        String name = doc.getString("username");
                        if (name != null && !currentMemberUsernames.contains(name)) {
                            results.add(name);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // This function is responsible for starting the process of adding selected users to the group.
    // Input: Set<String> usernames (set of names to add), AlertDialog dialog (the open dialog).
    // Output: None.
    private void addChosenMembers(Set<String> usernames, AlertDialog dialog) {
        List<String> searchResultList = new ArrayList<>(usernames);
        addMembersStepByStep(searchResultList, 0, dialog);
    }

    // This function is responsible for fetching each selected user and adding them to the group list.
    // Input: List<String> usernames, int index, AlertDialog dialog.
    // Output: None.
    private void addMembersStepByStep(List<String> usernames, int index, AlertDialog dialog) {
        if (index >= usernames.size()) {
            db.collection("groups").document(String.valueOf(groupId))
                    .update("members", rawMembersData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Members added successfully!", Toast.LENGTH_SHORT).show();
                        loadGroupData();
                        dialog.dismiss();
                    });
            return;
        }

        String targetName = usernames.get(index);
        db.collection("users").document(targetName).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                Map<String, Object> memberMapEntry = new HashMap<>();
                memberMapEntry.put("username", targetName);
                memberMapEntry.put("email", document.getString("email"));
                memberMapEntry.put("password", document.getString("password"));
                rawMembersData.add(memberMapEntry);
            }
            addMembersStepByStep(usernames, index + 1, dialog);
        });
    }

    // This function is responsible for converting the group image to Base64 and saving it to Firestore.
    // Input: Uri uri (location of the image).
    // Output: None.
    private void updateGroupPhoto(Uri uri) {
        String base64ImageString = ImageUtils.convertImageToBase64(this, uri);
        if (base64ImageString == null) return;

        db.collection("groups").document(String.valueOf(groupId))
                .update("imageBase64", base64ImageString)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Group photo updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }
}
