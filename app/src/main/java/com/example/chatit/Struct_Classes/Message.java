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


    // This function is responsible for encrypting a message and uploading it to a specific group chat.
    // Input: int groupId, String sender, String content, String fileType, Activity context.
    // Output: None.
    public static void sendMessageToGroup(int groupId, String sender, String content, String fileType, Activity context) {
        Message newMessage = new Message(sender, base64Encryption(content), fileType, System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("groups")
                .document(String.valueOf(groupId))
                .collection("messages")
                .add(newMessage)
                .addOnSuccessListener(reference -> Log.d("FIREBASE", "Sent: " + reference.getId()))
                .addOnFailureListener(error -> Toast.makeText(context, "Failed to send", Toast.LENGTH_SHORT).show());
    }

    // This function is responsible for deleting a specific message from the database.
    // Input: int groupId, String messageId, Activity context.
    // Output: None.
    public static void deleteMessage(int groupId, String messageId, Activity context) {
        FirebaseFirestore.getInstance().collection("groups")
                .document(String.valueOf(groupId))
                .collection("messages")
                .document(messageId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(error -> Log.e("FIREBASE", "Delete failed", error));
    }

    // This function is responsible for encoding message text into Base64 format.
    // Input: String content.
    // Output: String (encoded text).
    public static String base64Encryption(String content) {
        return content == null ? null : Base64.encodeToString(content.getBytes(), Base64.DEFAULT);
    }

    // This function is responsible for decoding Base64 text back into normal readable text.
    // Input: String content.
    // Output: String (decoded text).
    public static String base64Decryption(String content) {
        return content == null ? null : new String(Base64.decode(content, Base64.DEFAULT));
    }
}
