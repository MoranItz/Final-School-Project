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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatit.R;
import com.example.chatit.Struct_Classes.ImageUtils;
import com.example.chatit.Struct_Classes.User;
import com.example.chatit.Struct_Classes.Group;
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
    private MemberAdapter memberAdapter;
    private List<User> memberList;
    
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

        initViews();
        loadGroupData();
        setupClickListeners();
    }

    // This function is responsible for connecting the UI elements in the XML to the Java objects.
    // Input: None.
    // Output: None.
    private void initViews() {
        groupImageView = findViewById(R.id.groupImageView);
        backBtn = findViewById(R.id.backBtn);
        addMemberBtn = findViewById(R.id.addMemberBtn);
        groupNameDisplay = findViewById(R.id.groupNameDisplay);
        membersRecyclerView = findViewById(R.id.groupMembersRecyclerView);
        actionButton = findViewById(R.id.actionButton);

        memberList = new ArrayList<>();
        memberAdapter = new MemberAdapter(memberList, user -> {
            if (currentUsername.equals(groupOwner) && !user.getUsername().equals(currentUsername)) {
                showKickDialog(user.getUsername());
            }
        });
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersRecyclerView.setAdapter(memberAdapter);
    }

    // This function is responsible for fetching group details from Firestore and updating the UI.
    // Input: None.
    // Output: None.
    private void loadGroupData() {
        db.collection("groups").document(String.valueOf(groupId)).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("groupName");
                        groupOwner = doc.getString("owner");
                        groupNameDisplay.setText(name);

                        if (currentUsername.equals(groupOwner)) {
                            actionButton.setText("Delete Group");
                            addMemberBtn.setVisibility(View.VISIBLE);
                        } else {
                            actionButton.setText("Leave Group");
                            addMemberBtn.setVisibility(View.GONE);
                        }

                        String imageBase64 = doc.getString("imageBase64");
                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                            Bitmap bitmap = ImageUtils.convertBase64ToBitmap(imageBase64);
                            if (bitmap != null) {
                                groupImageView.setImageBitmap(bitmap);
                            }
                        }

                        fetchMembers(doc);
                        
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
    private void fetchMembers(DocumentSnapshot groupDoc) {
        rawMembersData = (List<Map<String, Object>>) groupDoc.get("members");
        if (rawMembersData == null) return;

        memberList.clear();
        for (Map<String, Object> data : rawMembersData) {
            String username = (String) data.get("username");
            db.collection("users").document(username).get().addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    User user = new User(
                            userDoc.getString("username"),
                            userDoc.getString("password"),
                            userDoc.getString("email")
                    );
                    user.setBiography(userDoc.getString("biography"));
                    user.setProfilePicture(userDoc.getString("profilePicture"));
                    memberList.add(user);
                    memberAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    // This function is responsible for setting up actions for interactive UI elements.
    // Input: None.
    // Output: None.
    private void setupClickListeners() {
        backBtn.setOnClickListener(v -> finish());

        View cameraOverlay = findViewById(R.id.cameraIconOverlay);
        cameraOverlay.setOnClickListener(v -> {
            if (currentUsername.equals(groupOwner)) {
                showImagePickerDialog();
            } else {
                Toast.makeText(this, "Only the owner can change the group photo", Toast.LENGTH_SHORT).show();
            }
        });

        actionButton.setOnClickListener(v -> {
            if (currentUsername.equals(groupOwner)) {
                confirmDeleteGroup();
            } else {
                confirmLeaveGroup();
            }
        });

        addMemberBtn.setOnClickListener(v -> showAddMemberDialog());
    }

    // This function is responsible for showing a confirmation message before deleting a group.
    // Input: None.
    // Output: None.
    private void confirmDeleteGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Group")
                .setMessage("Are you sure you want to delete this group? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // This function is responsible for removing the group document from Firestore.
    // Input: None.
    // Output: None.
    private void deleteGroup() {
        db.collection("groups").document(String.valueOf(groupId)).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show());
    }

    // This function is responsible for showing a confirmation message before a member leaves the group.
    // Input: None.
    // Output: None.
    private void confirmLeaveGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Leave", (dialog, which) -> leaveGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // This function is responsible for removing the user from the group members list in Firestore.
    // Input: None.
    // Output: None.
    private void leaveGroup() {
        rawMembersData.removeIf(member -> currentUsername.equals(member.get("username")));
        db.collection("groups").document(String.valueOf(groupId))
                .update("members", rawMembersData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "You left the group", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to leave group", Toast.LENGTH_SHORT).show());
    }

    // This function is responsible for showing a confirmation message before kicking a member.
    // Input: String username (the user to be removed).
    // Output: None.
    private void showKickDialog(String username) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to kick " + username + " from the group?")
                .setPositiveButton("Kick", (dialog, which) -> kickUser(username))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // This function is responsible for removing a specific member from the group list in Firestore.
    // Input: String username (the user to be removed).
    // Output: None.
    private void kickUser(String username) {
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
    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Update Group Photo")
                .setItems(options, (dialog, which) -> {
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
            } else openCamera();
        } else openCamera();
    }

    // This function is responsible for requesting storage or media access from the user.
    // Input: None.
    // Output: None.
    private void checkGalleryPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else openGallery();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else openGallery();
        } else openGallery();
    }

    // This function is responsible for launching the device camera application.
    // Input: None.
    // Output: None.
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) startActivityForResult(intent, CAMERA_REQUEST);
    }

    // This function is responsible for launching the device image gallery.
    // Input: None.
    // Output: None.
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // This function is responsible for handling the results from the camera or gallery activities.
    // Input: int requestCode, int resultCode, Intent data.
    // Output: None.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri imageUri = null;
            if (requestCode == CAMERA_REQUEST) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imageUri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "GroupPic", null));
            } else if (requestCode == PICK_IMAGE_REQUEST) {
                imageUri = data.getData();
            }

            if (imageUri != null) {
                groupImageView.setImageURI(imageUri);
                uploadGroupPicture(imageUri);
            }
        }
    }

    // This function is responsible for showing a dialog to find and add new users to the group.
    // Input: None.
    // Output: None.
    private void showAddMemberDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.add_member_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        EditText searchInput = dialogView.findViewById(R.id.searchUserInput);
        RecyclerView usersRecyclerView = dialogView.findViewById(R.id.usersRecyclerView);
        androidx.appcompat.widget.AppCompatButton closeBtn = dialogView.findViewById(R.id.closeDialogBtn);
        androidx.appcompat.widget.AppCompatButton confirmBtn = dialogView.findViewById(R.id.confirmAddBtn);

        List<String> searchResults = new ArrayList<>();
        Set<String> selectedToUpdate = new HashSet<>();
        UserSearchAdapter searchAdapter = new UserSearchAdapter(searchResults, selectedToUpdate);
        
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(searchAdapter);

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) searchUsers(s.toString(), searchAdapter, searchResults);
                else { searchResults.clear(); searchAdapter.notifyDataSetChanged(); }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        confirmBtn.setOnClickListener(v -> {
            if (selectedToUpdate.isEmpty()) {
                Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
                return;
            }
            addSelectedMembers(selectedToUpdate, dialog);
        });

        dialog.show();
    }

    // This function is responsible for querying Firestore for users that match a search string.
    // Input: String query, UserSearchAdapter adapter, List<String> results.
    // Output: None.
    private void searchUsers(String query, UserSearchAdapter adapter, List<String> results) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .addOnSuccessListener(snapshots -> {
                    results.clear();
                    Set<String> currentMemberNames = new HashSet<>();
                    for (User u : memberList) currentMemberNames.add(u.getUsername());

                    for (DocumentSnapshot doc : snapshots) {
                        String name = doc.getString("username");
                        if (name != null && !currentMemberNames.contains(name)) {
                            results.add(name);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // This function is responsible for starting the process of adding selected users to the group.
    // Input: Set<String> usernames (set of names to add), AlertDialog dialog (the open dialog).
    // Output: None.
    private void addSelectedMembers(Set<String> usernames, AlertDialog dialog) {
        List<String> listToProcess = new ArrayList<>(usernames);
        fetchAndAddRecursively(listToProcess, 0, dialog);
    }

    // This function is responsible for fetching each selected user and adding them to the group list.
    // Input: List<String> usernames, int index, AlertDialog dialog.
    // Output: None.
    private void fetchAndAddRecursively(List<String> usernames, int index, AlertDialog dialog) {
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

        String username = usernames.get(index);
        db.collection("users").document(username).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Map<String, Object> newMemberEntry = new HashMap<>();
                newMemberEntry.put("username", username);
                newMemberEntry.put("email", doc.getString("email"));
                newMemberEntry.put("password", doc.getString("password"));
                rawMembersData.add(newMemberEntry);
            }
            fetchAndAddRecursively(usernames, index + 1, dialog);
        });
    }

    // This class is responsible for managing the small list of users found during a search.
    private static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private final List<String> users;
        private final Set<String> selected;

        UserSearchAdapter(List<String> users, Set<String> selected) {
            this.users = users;
            this.selected = selected;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = users.get(position);
            holder.name.setText(name);
            holder.checkBox.setChecked(selected.contains(name));
            holder.itemView.setOnClickListener(v -> {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
                if (holder.checkBox.isChecked()) selected.add(name);
                else selected.remove(name);
            });
            holder.checkBox.setOnClickListener(v -> {
                if (holder.checkBox.isChecked()) selected.add(name);
                else selected.remove(name);
            });
        }

        @Override public int getItemCount() { return users.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            android.widget.CheckBox checkBox;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.memberName);
                checkBox = v.findViewById(R.id.memberCheckbox);
            }
        }
    }

    // This function is responsible for converting the group image to Base64 and saving it to Firestore.
    // Input: Uri uri (location of the image).
    // Output: None.
    private void uploadGroupPicture(Uri uri) {
        String base64String = ImageUtils.convertImageToBase64(this, uri);
        if (base64String == null) return;

        db.collection("groups").document(String.valueOf(groupId))
                .update("imageBase64", base64String)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Group photo updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }

    // This class is responsible for displaying the main member list for the group.
    private static class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {
        private final List<User> members;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(User user);
        }

        MemberAdapter(List<User> members, OnItemClickListener listener) { 
            this.members = members;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_info_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = members.get(position);
            holder.name.setText(user.getUsername());
            holder.bio.setText(user.getBiography() != null ? user.getBiography() : "No bio available");
            
            String profilePicBase64 = user.getProfilePicture();
            if (profilePicBase64 != null && !profilePicBase64.isEmpty()) {
                Bitmap bitmap = ImageUtils.convertBase64ToBitmap(profilePicBase64);
                if (bitmap != null) {
                    holder.profilePic.setImageBitmap(bitmap);
                } else {
                    holder.profilePic.setImageResource(R.drawable.creategroup_icon);
                }
            } else {
                holder.profilePic.setImageResource(R.drawable.creategroup_icon);
            }

            holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
        }

        @Override
        public int getItemCount() { return members.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, bio;
            ImageView profilePic;
            ViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.memberName);
                bio = view.findViewById(R.id.memberBio);
                profilePic = view.findViewById(R.id.memberProfilePic);
            }
        }
    }
}
