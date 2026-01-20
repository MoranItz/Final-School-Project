package com.example.chatit.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.chatit.R;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageNotificationService extends Service {

    private String currentUsername;
    private long serviceStartTime;
    private Set<String> listenedGroupIds;
    private List<ListenerRegistration> activeListeners;
    private static final String CHANNEL_ID = "chatit_new_messages_channel";

    // This function is responsible for initializing the service, setting the start time, and creating the notification channel.
    // Input: None.
    // Output: None.
    @Override
    public void onCreate() {
        super.onCreate();
        serviceStartTime = System.currentTimeMillis();
        listenedGroupIds = new HashSet<>();
        activeListeners = new ArrayList<>();
        createNotificationChannel();
        
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", null);
        Log.d("SERVICE_DEBUG", "Service Create. Username: " + currentUsername + " StartTime: " + serviceStartTime);
    }

    // This function is responsible for starting the group scanning process when the service is triggered.
    // Input: Intent intent, int flags, int startId.
    // Output: int (START_STICKY).
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (currentUsername != null) {
            scanAndListenToGroups();
        }
        return START_STICKY;
    }

    // This function is responsible for binding the service (not used here).
    // Input: Intent intent.
    // Output: IBinder (null).
    @Nullable
    @Override
    // Must have class since Service is an abstract class :(
    public IBinder onBind(Intent intent) {
        return null;
    }

    // This function is responsible for querying all groups and setting up listeners for the ones the user belongs to.
    // Input: None.
    // Output: None.
    private void scanAndListenToGroups() {
        FirebaseFirestore.getInstance().collection("groups").get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot groupDoc : snapshots) {
                        processGroupForListening(groupDoc);
                    }
                })
                .addOnFailureListener(e -> Log.e("SERVICE", "Error fetching groups", e));
    }

    // This function is responsible for checking if the user is in a group and attaching a message listener if so.
    // Input: QueryDocumentSnapshot groupDoc.
    // Output: None.
    private void processGroupForListening(QueryDocumentSnapshot groupDoc) {
        String groupId = groupDoc.getId();
        
        // Avoid adding duplicate listeners for the same group
        if (listenedGroupIds.contains(groupId)) return;

        List<Object> membersList = (List<Object>) groupDoc.get("members");
        if (membersList == null) return;

        boolean isMember = false;
        for (Object memberObj : membersList) {
            if (memberObj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) memberObj;
                String username = (String) map.get("username");
                if (currentUsername.equals(username)) {
                    isMember = true;
                    break;
                }
            }
        }

        if (isMember) {
            listenedGroupIds.add(groupId);
            String groupName = groupDoc.getString("groupName");
            Log.d("SERVICE_DEBUG", "Listening to group: " + groupName + " (" + groupId + ")");
            attachMessageListener(groupId, groupName);
        }
    }

    // This function is responsible for listening to the messages collection of a specific group for new additions.
    // Input: String groupId, String groupName.
    // Output: None.
    private void attachMessageListener(String groupId, String groupName) {
        ListenerRegistration registration = FirebaseFirestore.getInstance()
                .collection("groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Map<String, Object> data = dc.getDocument().getData();
                            processNewMessage(data, groupName);
                        }
                    }
                });
        
        activeListeners.add(registration);
    }

    // This function is responsible for validating a new message and triggering a notification if it's relevant.
    // Input: Map<String, Object> messageData, String groupName.
    // Output: None.
    private void processNewMessage(Map<String, Object> messageData, String groupName) {
        try {
            String sender = (String) messageData.get("sender");
            Long timestamp = (Long) messageData.get("timestamp");

            Log.d("SERVICE_DEBUG", "New message detected in " + groupName + " from: " + sender + " at: " + timestamp);
            Log.d("SERVICE_DEBUG", "Service Start Time: " + serviceStartTime);
            
            // Ignore messages sent by self
            if (currentUsername.equals(sender)) {
                Log.d("SERVICE_DEBUG", "Ignored: Message sent by self. (Logged in as: " + currentUsername + " == Sender: " + sender + ")");
                return;
            }

            // Ignore old messages loaded on initial connect
            if (timestamp != null && timestamp <= serviceStartTime) {
                Log.d("SERVICE_DEBUG", "Ignored: Old message. MsgTime: " + timestamp + " <= StartTime: " + serviceStartTime);
                return;
            }

            Log.d("SERVICE_DEBUG", "Message accepted! Sending notification.");

            // Decrypt content (assuming it's base64 encoded as per Message class)
            // We duplicate decryption logic here briefly or just show raw if complex. 
            // Message.base64Decryption is distinct. Let's try to assume text.
            String rawContent = (String) messageData.get("content");
            String content = decodeContent(rawContent);

            sendNotification(groupName, sender, content);
            
            // Update service time to avoid re-notifying if listener resets? 
            // Better not, just rely on the timestamp check.
            
        } catch (Exception e) {
            Log.e("SERVICE", "Error processing message", e);
        }
    }

    // This function is responsible for decoding Base64 message content.
    // Input: String encoded.
    // Output: String (decoded).
    private String decodeContent(String encoded) {
        if (encoded == null) return "New Message";
        try {
            return new String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT));
        } catch (Exception e) {
            return encoded;
        }
    }

    // This function is responsible for building and displaying the system notification.
    // Input: String groupName, String sender, String message.
    // Output: None.
    private void sendNotification(String groupName, String sender, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon_foreground) // Use foreground for small icon to avoid black box issues
                .setContentTitle("Message sent in: " + groupName)
                .setContentText(sender + ": " + message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        // Use a unique ID for each notification based on time
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // This function is responsible for registering the notification channel for Android O and above.
    // Input: None.
    // Output: None.
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chatit New Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new group messages");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // This function is responsible for cleaning up listeners when the service is destroyed.
    // Input: None.
    // Output: None.
    @Override
    public void onDestroy() {
        super.onDestroy();
        for (ListenerRegistration reg : activeListeners) {
            reg.remove();
        }
        activeListeners.clear();
    }
}
