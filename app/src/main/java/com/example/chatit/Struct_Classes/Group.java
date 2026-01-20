package com.example.chatit.Struct_Classes;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Vector;

public class Group {
    private int groupId;
    private String groupName, owner, description, imageBase64;
    private Vector<User> members;

    // CONSTRUCTOR
    public Group(int groupId, String groupName, String owner, String description, Vector<User> members, User userOwner) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.owner = owner;
        this.description = description;
        this.members = members != null ? members : new Vector<>();
        if (userOwner != null) this.members.add(userOwner);
    }

    // GETTERS
    public int getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getOwner() { return owner; }
    public String getDescription() { return description; }
    public Vector<User> getMembers() { return members; }
    public String getImageBase64() { return imageBase64; }

    // SETTERS
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setGroupOwner(String owner) { this.owner = owner; }
    public void setDescription(String description) { this.description = description; }
    public void setMembers(Vector<User> members) { this.members = members; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }


    // This function is responsible for saving the newly created group to the Firestore database.
    // Input: Group group, Activity context.
    // Output: None.
    public static void registerGroup(Group group, Activity context) {
        DocumentReference groupReference = FirebaseFirestore.getInstance().collection("groups").document(String.valueOf(group.getGroupId()));

        groupReference.set(group)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Group created!", Toast.LENGTH_SHORT).show();
                    context.finish();
                })
                .addOnFailureListener(error -> Log.e("FIREBASE", "Error saving group", error));
    }
}
