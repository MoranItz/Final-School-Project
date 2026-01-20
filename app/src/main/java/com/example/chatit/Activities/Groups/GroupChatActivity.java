package com.example.chatit.Activities.Groups;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatit.Adapters.MessageAdapter;
import com.example.chatit.R;
import com.example.chatit.Struct_Classes.Message;
import com.example.chatit.Helper_Classes.FireBaseHelper;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupChatActivity extends AppCompatActivity {

    private EditText etMessageInput;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private int groupId;
    private String currentUsername;
    private FirebaseFirestore db;
    private ListenerRegistration messageListener;

    // Initializes the chat activity and calls the sequence of setup methods to prepare the UI and database.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groupchat_view);

        // Stop initialization if group data cannot be retrieved from the Intent
        if (!initializeGroupData()) return;

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadMessages();
        setupRealtimeListener();
    }

    // Retrieves the group ID and name from the Intent and the sender's username from SharedPreferences.
    // Input: None.
    // Output: boolean (true if data was loaded successfully, false otherwise).
    private boolean initializeGroupData() {
        groupId = getIntent().getIntExtra("groupId", -1);
        String groupName = getIntent().getStringExtra("groupName");

        // Validate that a valid group ID was passed to avoid database errors
        if (groupId == -1) {
            Toast.makeText(this, "Error loading group", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUsername = prefs.getString("username", "");

        TextView tvGroupName = findViewById(R.id.chatTitle);
        tvGroupName.setText(groupName);

        return true;
    }

    // Gets the singleton instance of the Firestore database.
    // Input: None.
    // Output: None.
    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    // Connects the class variables to the specific views defined in the XML layout.
    // Input: None.
    // Output: None.
    private void initializeViews() {
        etMessageInput = findViewById(R.id.messageInput);
        recyclerViewMessages = findViewById(R.id.chatRecyclerView);
    }

    // Prepares the RecyclerView with an adapter and a layout manager that stacks items from the bottom.
    // Input: None.
    // Output: None.
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUsername);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // Ensure that new messages appear at the bottom of the list
        layoutManager.setStackFromEnd(true);

        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    // Sets up the click events for the back button and the message send button.
    // Input: None.
    // Output: None.
    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.backBtn);
        ImageView btnSend = findViewById(R.id.sendBtn);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    // Fetches the initial set of messages from the Firestore database for the current group.
    // Input: None.
    // Output: None.
    private void loadMessages() {
        getMessagesQuery()
                .get()
                .addOnSuccessListener(snapshots -> {
                    messageList.clear();
                    // Process each document in the snapshot result
                    snapshots.forEach(this::parseAndAddMessage);
                    messageAdapter.updateMessages(messageList);
                    scrollToBottom();
                    Log.d("FIREBASE", "Loaded " + messageList.size() + " messages");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                    Log.e("FIREBASE", "Error loading messages", e);
                });
    }

    // Attaches a snapshot listener to the database to receive real-time updates when new messages are added.
    // Input: None.
    // Output: None.
    private void setupRealtimeListener() {
        messageListener = getMessagesQuery()
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE", "Listen failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    // Filter specifically for "ADDED" changes to ignore modified or deleted documents
                    snapshots.getDocumentChanges().stream()
                            .filter(dc -> dc.getType() == DocumentChange.Type.ADDED)
                            .forEach(this::handleNewMessage);
                });
    }

    // Processes a new document change by converting it into a Message object and adding it to the list if it's unique.
    // Input: DocumentChange dc (the change detected in the database).
    // Output: None.
    private void handleNewMessage(DocumentChange dc) {
        Message message = parseMessage(dc.getDocument());
        if (message == null) return;

        // Prevent duplicate messages from appearing if loadMessages and setupRealtimeListener overlap
        if (!messageExists(message)) {
            messageAdapter.addMessage(message);
            scrollToBottom();
        }
    }

    // Converts a single Firestore document snapshot into a Message object and adds it to the message list.
    // Input: QueryDocumentSnapshot document (the raw database document).
    // Output: None.
    private void parseAndAddMessage(QueryDocumentSnapshot document) {
        Message message = parseMessage(document);
        if (message != null) {
            messageList.add(message);
        }
    }

    // Maps the fields from a Firestore document into a Message class instance.
    // Input: QueryDocumentSnapshot document (the raw database document).
    // Output: Message (the parsed message object, or null if fields are missing).
    private Message parseMessage(QueryDocumentSnapshot document) {
        try {
            String sender = document.getString("sender");
            String content = document.getString("content");
            String fileType = document.getString("fileType");
            Long timestamp = document.getLong("timestamp");

            // Verify that all required fields exist to prevent NullPointerExceptions later
            if (sender == null || content == null || fileType == null || timestamp == null) {
                return null;
            }

            return new Message(sender, content, fileType, timestamp);
        } catch (Exception e) {
            Log.e("FIREBASE", "Error parsing message", e);
            return null;
        }
    }

    // Checks if a message already exists in the current list by comparing the timestamp and sender.
    // Input: Message message (the message to check).
    // Output: boolean (true if it exists, false otherwise).
    private boolean messageExists(Message message) {
        // Uses stream API to check if any message in the list has the same unique signature
        return messageList.stream()
                .anyMatch(m -> m.getTimestamp() == message.getTimestamp()
                        && m.getSender().equals(message.getSender()));
    }

    // Creates a Firestore query to get messages for the specific group, sorted by their creation time.
    // Input: None.
    // Output: Query (the configured Firestore query).
    private Query getMessagesQuery() {
        return db.collection("groups")
                .document(String.valueOf(groupId))
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    // Validates input text and uses the Message structure class to upload a new message to the database.
    // Input: None.
    // Output: None.
    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Static helper method in Message class to handle the Firebase write operation
        Message.sendMessageToGroup(groupId, currentUsername, messageText, "text", this);
        etMessageInput.setText("");
    }

    // Smoothly scrolls the RecyclerView to the last item so the user sees the most recent message.
    // Input: None.
    // Output: None.
    private void scrollToBottom() {
        if (messageAdapter.getItemCount() > 0) {
            recyclerViewMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    // Cleans up the real-time database listener when the activity is destroyed to prevent memory leaks.
    // Input: None.
    // Output: None.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detach the listener so it doesn't continue running in the background
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
