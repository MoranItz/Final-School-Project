package com.example.chatit.Struct_Classes;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Vector;

public class Group {
    private int groupId;
    private String groupName, owner, description;
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

    // SETTERS
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setGroupOwner(String owner) { this.owner = owner; }
    public void setDescription(String description) { this.description = description; }
    public void setMembers(Vector<User> members) { this.members = members; }



    // Uploads the group object to the Firestore "groups" collection using its unique ID as the document name.
    // Input: Group group (the group instance to save), Activity context (the activity calling the function for UI updates).
    // Output: None.
    public static void registerGroup(Group group, Activity context) {
        // Creates a reference to a document in the groups collection using the integer ID converted to a string
        DocumentReference ref = FirebaseFirestore.getInstance().collection("groups").document(String.valueOf(group.getGroupId()));

        // Attempts to save the entire Group object to Firestore
        ref.set(group)
                .addOnSuccessListener(aVoid -> {
                    // Notifies the user of success and closes the current setup screen
                    Toast.makeText(context, "Group created!", Toast.LENGTH_SHORT).show();
                    context.finish();
                })
                .addOnFailureListener(e -> Log.e("FIREBASE", "Error saving group", e));
    }
}
