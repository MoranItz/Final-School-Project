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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupProfileActivity extends AppCompatActivity {

    private ImageView groupImageView, backBtn;
    private TextView groupNameDisplay;
    private RecyclerView membersRecyclerView;
    private MemberAdapter memberAdapter;
    private List<User> memberList;
    
    private int groupId;
    private String groupOwner;
    private String currentUsername;
    private FirebaseFirestore db;

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
        groupNameDisplay = findViewById(R.id.groupNameDisplay);
        membersRecyclerView = findViewById(R.id.groupMembersRecyclerView);

        memberList = new ArrayList<>();
        memberAdapter = new MemberAdapter(memberList);
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
        List<Map<String, Object>> membersData = (List<Map<String, Object>>) groupDoc.get("members");
        if (membersData == null) return;

        memberList.clear();
        for (Map<String, Object> data : membersData) {
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

        MemberAdapter(List<User> members) { this.members = members; }

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
