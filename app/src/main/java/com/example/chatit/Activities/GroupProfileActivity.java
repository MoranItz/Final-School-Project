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
    private List<Map<String, Object>> rawMembersData; // Store raw data for updates

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

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

    private void loadGroupData() {
        db.collection("groups").document(String.valueOf(groupId)).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("groupName");
                        groupOwner = doc.getString("owner");
                        groupNameDisplay.setText(name);

                        // Configure action button
                        if (currentUsername.equals(groupOwner)) {
                            actionButton.setText("Delete Group");
                            addMemberBtn.setVisibility(View.VISIBLE);
                        } else {
                            actionButton.setText("Leave Group");
                            addMemberBtn.setVisibility(View.GONE);
                        }

                        // Load image
                        String imageBase64 = doc.getString("imageBase64");
                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                            Bitmap bitmap = ImageUtils.convertBase64ToBitmap(imageBase64);
                            if (bitmap != null) {
                                groupImageView.setImageBitmap(bitmap);
                            }
                        }

                        // Load members
                        fetchMembers(doc);
                        
                        // Only owner can change picture
                        if (!currentUsername.equals(groupOwner)) {
                            findViewById(R.id.cameraIconOverlay).setVisibility(View.GONE);
                        }
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void fetchMembers(DocumentSnapshot groupDoc) {
        rawMembersData = (List<Map<String, Object>>) groupDoc.get("members");
        if (rawMembersData == null) return;

        memberList.clear();
        for (Map<String, Object> data : rawMembersData) {
            String username = (String) data.get("username");
            // Fetch detailed user info (for bios) from users collection
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

    private void confirmDeleteGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Group")
                .setMessage("Are you sure you want to delete this group? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

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

    private void confirmLeaveGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Leave", (dialog, which) -> leaveGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveGroup() {
        rawMembersData.removeIf(m -> currentUsername.equals(m.get("username")));
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

    private void showKickDialog(String username) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to kick " + username + " from the group?")
                .setPositiveButton("Kick", (dialog, which) -> kickUser(username))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void kickUser(String username) {
        rawMembersData.removeIf(m -> username.equals(m.get("username")));
        db.collection("groups").document(String.valueOf(groupId))
                .update("members", rawMembersData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, username + " was removed", Toast.LENGTH_SHORT).show();
                    loadGroupData(); // Refresh list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove user", Toast.LENGTH_SHORT).show());
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Update Group Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermissionAndOpen();
                    else if (which == 1) checkGalleryPermissionAndOpen();
                }).show();
    }

    private void checkCameraPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            } else openCamera();
        } else openCamera();
    }

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

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

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

    private void showAddMemberDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.add_member_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        EditText searchInput = dialogView.findViewById(R.id.searchUserInput);
        RecyclerView usersRv = dialogView.findViewById(R.id.usersRecyclerView);
        androidx.appcompat.widget.AppCompatButton closeBtn = dialogView.findViewById(R.id.closeDialogBtn);
        androidx.appcompat.widget.AppCompatButton confirmBtn = dialogView.findViewById(R.id.confirmAddBtn);

        List<String> searchResults = new ArrayList<>();
        Set<String> selectedToUpdate = new HashSet<>();
        UserSearchAdapter searchAdapter = new UserSearchAdapter(searchResults, selectedToUpdate);
        
        usersRv.setLayoutManager(new LinearLayoutManager(this));
        usersRv.setAdapter(searchAdapter);

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

    private void searchUsers(String query, UserSearchAdapter adapter, List<String> results) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .addOnSuccessListener(snapshots -> {
                    results.clear();
                    // Current members usernames for filtering
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

    private void addSelectedMembers(Set<String> usernames, AlertDialog dialog) {
        List<String> userListToAdd = new ArrayList<>(usernames);
        fetchAndAddRecursively(userListToAdd, 0, dialog);
    }

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
                Map<String, Object> newMember = new HashMap<>();
                newMember.put("username", username);
                newMember.put("email", doc.getString("email"));
                newMember.put("password", doc.getString("password"));
                rawMembersData.add(newMember);
            }
            fetchAndAddRecursively(usernames, index + 1, dialog);
        });
    }

    // Inner Search Adapter
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

    private void uploadGroupPicture(Uri uri) {
        String base64String = ImageUtils.convertImageToBase64(this, uri);
        if (base64String == null) return;

        db.collection("groups").document(String.valueOf(groupId))
                .update("imageBase64", base64String)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Group photo updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }

    // Member Adapter Inner Class
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
            
            // Set member profile picture
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
