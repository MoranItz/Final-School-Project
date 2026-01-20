package com.example.chatit.Struct_Classes;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

public class Message {
    private final String sender, content, fileType;
    private final long timestamp;

    // CONSTRUCTOR
    public Message(String sender, String content, String fileType, long timestamp) {
        this.sender = sender;
        this.content = content;
        this.fileType = fileType;
        this.timestamp = timestamp;
    }

    // GETTERS
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getFileType() { return fileType; }
    public long getTimestamp() { return timestamp; }



    // Encrypts the message content and uploads a new message object to a specific group's message sub-collection in Firestore.
    // Input: int groupId (target group ID), String sender (sender username), String content (raw message), String fileType (e.g., "text"), Activity context (UI context).
    // Output: None.
    public static void sendMessageToGroup(int groupId, String sender, String content, String fileType, Activity context) {
        // Create a message instance with the current system time as the timestamp
        Message msg = new Message(sender, base64Encryption(content), fileType, System.currentTimeMillis());

        // Navigate through the Firestore path: groups -> {id} -> messages -> [new random document]
        FirebaseFirestore.getInstance().collection("groups")
                .document(String.valueOf(groupId))
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(ref -> Log.d("FIREBASE", "Sent: " + ref.getId()))
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to send", Toast.LENGTH_SHORT).show());
    }

    // Removes a specific message document from the database based on the group ID and the message's unique document ID.
    // Input: int groupId (the ID of the group), String msgId (the Firestore document ID of the message), Activity context (UI context).
    // Output: None.
    public static void deleteMessage(int groupId, String msgId, Activity context) {
        // Accesses the specific document reference and triggers the delete operation
        FirebaseFirestore.getInstance().collection("groups")
                .document(String.valueOf(groupId))
                .collection("messages")
                .document(msgId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Log.e("FIREBASE", "Delete failed", e));
    }

    // Converts a standard string into a Base64 encoded string for a simple layer of data obfuscation.
    // Input: String content (the raw text).
    // Output: String (the Base64 encoded result).
    public static String base64Encryption(String content) {
        // Check for null to avoid processing errors, then convert bytes to Base64 format
        return content == null ? null : Base64.encodeToString(content.getBytes(), Base64.DEFAULT);
    }

    // Decodes a Base64 encoded string back into its original human-readable text format.
    // Input: String content (the Base64 encoded text).
    // Output: String (the decoded raw text).
    public static String base64Decryption(String content) {
        // Decodes the string back into bytes and initializes a new string object from those bytes
        return content == null ? null : new String(Base64.decode(content, Base64.DEFAULT));
    }
}
