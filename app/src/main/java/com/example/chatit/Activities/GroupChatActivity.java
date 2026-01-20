package com.example.chatit.Activities;

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
import android.content.Intent;

import com.example.chatit.Adapters.MessageAdapter;
import com.example.chatit.R;
import com.example.chatit.Struct_Classes.Message;
import com.example.chatit.Struct_Classes.ImageUtils;
import android.graphics.Bitmap;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupChatActivity extends AppCompatActivity {

    private EditText messageInput;
    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private int groupId;
    private String currentUsername;
    private FirebaseFirestore database;
    private ListenerRegistration chatListener;

    // This function is responsible for initializing the activity and starting the setup operations.
    // Input: Bundle savedInstanceState (the saved state).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groupchat_view);

        if (!loadGroupInformation()) return;

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadInitialMessages();
        startRealtimeUpdates();
    }

    // This function is responsible for getting group and user data from the intent and local storage.
    // Input: None.
    // Output: boolean (returns true if data was found).
    private boolean loadGroupInformation() {
        groupId = getIntent().getIntExtra("groupId", -1);
        String groupName = getIntent().getStringExtra("groupName");

        if (groupId == -1) {
            Toast.makeText(this, "Error loading group", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUsername = preferences.getString("username", "");

        TextView titleView = findViewById(R.id.chatTitle);
        titleView.setText(groupName);

        ImageView groupImageView = findViewById(R.id.groupChatImageView);
        FirebaseFirestore.getInstance().collection("groups").document(String.valueOf(groupId))
                .get().addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String imageBase64 = document.getString("imageBase64");
                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                            Bitmap bitmap = ImageUtils.convertBase64ToBitmap(imageBase64);
                            if (bitmap != null) {
                                groupImageView.setImageBitmap(bitmap);
                            }
                        }
                    }
                });

        return true;
    }

    // This function is responsible for getting the database instance.
    // Input: None.
    // Output: None.
    private void initializeFirebase() {
        database = FirebaseFirestore.getInstance();
    }

    // This function is responsible for connecting properties to the XML layout components.
    // Input: None.
    // Output: None.
    private void initializeViews() {
        messageInput = findViewById(R.id.messageInput);
        messagesRecyclerView = findViewById(R.id.chatRecyclerView);
    }

    // This function is responsible for configuring the RecyclerView for displaying chat bubbles.
    // Input: None.
    // Output: None.
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUsername);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    // This function is responsible for handling clicks on the back button, send button, and group title.
    // Input: None.
    // Output: None.
    private void setupClickListeners() {
        ImageView backButton = findViewById(R.id.backBtn);
        ImageView sendButton = findViewById(R.id.sendBtn);
        TextView titleView = findViewById(R.id.chatTitle);

        backButton.setOnClickListener(v -> finish());
        sendButton.setOnClickListener(v -> sendNewMessage());
        
        titleView.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupProfileActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });
    }

    // This function is responsible for downloading existing messages once when the screen opens.
    // Input: None.
    // Output: None.
    private void loadInitialMessages() {
        createMessageQuery()
                .get()
                .addOnSuccessListener(snapshots -> {
                    messageList.clear();
                    snapshots.forEach(this::processDownloadedDocument);
                    messageAdapter.updateMessages(messageList);
                    scrollToLastMessage();
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                });
    }

    // This function is responsible for listening to new messages being added to the database.
    // Input: None.
    // Output: None.
    private void startRealtimeUpdates() {
        chatListener = createMessageQuery()
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots == null) return;

                    snapshots.getDocumentChanges().stream()
                            .filter(change -> change.getType() == DocumentChange.Type.ADDED)
                            .forEach(this::handleAddedMessage);
                });
    }

    // This function is responsible for adding a newly detected message to the chat screen.
    // Input: DocumentChange change (the detected item).
    // Output: None.
    private void handleAddedMessage(DocumentChange change) {
        Message newMessage = parseDocumentToMessage(change.getDocument());
        if (newMessage == null) return;

        if (!isMessageAlreadyInList(newMessage)) {
            messageAdapter.addMessage(newMessage);
            scrollToLastMessage();
        }
    }

    // This function is responsible for adding a generic document to the internal list.
    // Input: QueryDocumentSnapshot document.
    // Output: None.
    private void processDownloadedDocument(QueryDocumentSnapshot document) {
        Message message = parseDocumentToMessage(document);
        if (message != null) {
            messageList.add(message);
        }
    }

    // This function is responsible for converting raw database data into a Message object.
    // Input: QueryDocumentSnapshot document (raw data).
    // Output: Message (the converted object).
    private Message parseDocumentToMessage(QueryDocumentSnapshot document) {
        try {
            String sender = document.getString("sender");
            String content = document.getString("content");
            String type = document.getString("fileType");
            Long time = document.getLong("timestamp");

            if (sender == null || content == null || type == null || time == null) {
                return null;
            }

            return new Message(sender, content, type, time);
        } catch (Exception error) {
            return null;
        }
    }

    // This function is responsible for verifying if a message is already being displayed.
    // Input: Message target.
    // Output: boolean (true if existing).
    private boolean isMessageAlreadyInList(Message target) {
        return messageList.stream()
                .anyMatch(msg -> msg.getTimestamp() == target.getTimestamp()
                        && msg.getSender().equals(target.getSender()));
    }

    // This function is responsible for defining the sorting and location of the messages in Firestore.
    // Input: None.
    // Output: Query (the database query).
    private Query createMessageQuery() {
        return database.collection("groups")
                .document(String.valueOf(groupId))
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    // This function is responsible for taking user input and saving it as a new message in Firestore.
    // Input: None.
    // Output: None.
    private void sendNewMessage() {
        String outgoingText = messageInput.getText().toString().trim();

        if (outgoingText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        Message.sendMessageToGroup(groupId, currentUsername, outgoingText, "text", this);
        messageInput.setText("");
    }

    // This function is responsible for moving the chat view to the most recent message.
    // Input: None.
    // Output: None.
    private void scrollToLastMessage() {
        if (messageAdapter.getItemCount() > 0) {
            messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    // This function is responsible for stopping the chat listener when the user leaves the screen.
    // Input: None.
    // Output: None.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) {
            chatListener.remove();
        }
    }
}
