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

    // This function is responsible for creating a new group object with all its initial data.
    // Input: int groupId, String groupName, String owner, String description, Vector<User> members, User userOwner.
    // Output: None.
    public Group(int groupId, String groupName, String owner, String description, Vector<User> members, User userOwner) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.owner = owner;
        this.description = description;
        this.members = members != null ? members : new Vector<>();
        if (userOwner != null) this.members.add(userOwner);
    }

    // This function is responsible for returning the unique group identifier.
    // Input: None.
    // Output: int (group ID).
    public int getGroupId() { return groupId; }

    // This function is responsible for returning the name of the group.
    // Input: None.
    // Output: String (group name).
    public String getGroupName() { return groupName; }

    // This function is responsible for returning the username of the group owner.
    // Input: None.
    // Output: String (owner name).
    public String getOwner() { return owner; }

    // This function is responsible for returning the group description.
    // Input: None.
    // Output: String (description).
    public String getDescription() { return description; }

    // This function is responsible for returning the list of members in the group.
    // Input: None.
    // Output: Vector<User> (members).
    public Vector<User> getMembers() { return members; }

    // This function is responsible for returning the group image in Base64 format.
    // Input: None.
    // Output: String (Base64 data).
    public String getImageBase64() { return imageBase64; }

    // This function is responsible for setting a new unique identifier for the group.
    // Input: int groupId.
    // Output: None.
    public void setGroupId(int groupId) { this.groupId = groupId; }

    // This function is responsible for setting a new name for the group.
    // Input: String groupName.
    // Output: None.
    public void setGroupName(String groupName) { this.groupName = groupName; }

    // This function is responsible for setting a new owner for the group.
    // Input: String owner.
    // Output: None.
    public void setGroupOwner(String owner) { this.owner = owner; }

    // This function is responsible for setting a new description for the group.
    // Input: String description.
    // Output: None.
    public void setDescription(String description) { this.description = description; }

    // This function is responsible for setting a new list of members for the group.
    // Input: Vector<User> members.
    // Output: None.
    public void setMembers(Vector<User> members) { this.members = members; }

    // This function is responsible for setting a new group image in Base64 format.
    // Input: String imageBase64.
    // Output: None.
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
