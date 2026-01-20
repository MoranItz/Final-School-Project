package com.example.chatit.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatit.R;
import com.example.chatit.Struct_Classes.Message;
import com.example.chatit.Struct_Classes.ImageUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1, TYPE_RECEIVED = 2;
    private List<Message> messages;
    private final String currentUser;
    private final Map<String, String> profilePicsCache = new HashMap<>();

    // Constructor that sets the initial message list and identifies the current user to distinguish sent/received messages.
    // Input: List<Message> messages (data source), String currentUser (the username of the person logged in).
    // Output: None.
    public MessageAdapter(List<Message> messages, String currentUser) {
        this.messages = messages;
        this.currentUser = currentUser;
    }

    // Determines whether a message was sent or received based on the sender's username.
    // Input: int pos (position of the item in the list).
    // Output: int (returns TYPE_SENT or TYPE_RECEIVED).
    @Override
    public int getItemViewType(int pos) {
        // Compares the message sender with the local user to decide which layout to use
        return messages.get(pos).getSender().equals(currentUser) ? TYPE_SENT : TYPE_RECEIVED;
    }

    // Inflates the appropriate layout (left or right bubble) based on the view type.
    // Input: ViewGroup parent (the RecyclerView), int viewType (the result from getItemViewType).
    // Output: RecyclerView.ViewHolder (either SentViewHolder or ReceivedViewHolder).
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Choose the layout file based on whether the message is outgoing or incoming
        int layout = (viewType == TYPE_SENT) ? R.layout.messagesent_item : R.layout.messagerecieved_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return (viewType == TYPE_SENT) ? new SentViewHolder(view) : new ReceivedViewHolder(view);
    }

    // Identifies the specific ViewHolder type and binds the message data to the UI.
    // Input: RecyclerView.ViewHolder holder (the generic holder), int pos (data position).
    // Output: None.
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        Message msg = messages.get(pos);
        if (holder instanceof SentViewHolder) ((SentViewHolder) holder).bind(msg);
        else if (holder instanceof ReceivedViewHolder) ((ReceivedViewHolder) holder).bind(msg, profilePicsCache);
    }

    // Returns the total number of messages in the chat history.
    // Input: None.
    // Output: int (list size).
    @Override
    public int getItemCount() { return messages.size(); }

    // ViewHolder class for messages sent by the current user.
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView content, time;
        SentViewHolder(View v) {
            super(v);
            content = v.findViewById(R.id.sentMessageText);
            time = v.findViewById(R.id.sentMessageTime);
        }
        // Decrypts the message content and sets the formatted time.
        void bind(Message msg) {
            // Decodes the Base64 content back into readable text
            content.setText(Message.base64Decryption(msg.getContent()));
            time.setText(formatTime(msg.getTimestamp()));
        }
    }

    // ViewHolder class for messages received from other group members.
    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView sender, content, time;
        ImageView senderPic;
        ReceivedViewHolder(View v) {
            super(v);
            sender = v.findViewById(R.id.senderName);
            content = v.findViewById(R.id.receivedMessageText);
            time = v.findViewById(R.id.receivedMessageTime);
            senderPic = v.findViewById(R.id.senderProfilePic);
        }
        // Sets the sender's name, decrypts content, and displays the time.
        void bind(Message msg, Map<String, String> cache) {
            sender.setText(msg.getSender());
            content.setText(Message.base64Decryption(msg.getContent()));
            time.setText(formatTime(msg.getTimestamp()));

            // Set profile picture
            String senderName = msg.getSender();
            if (cache.containsKey(senderName)) {
                setPic(cache.get(senderName));
            } else {
                senderPic.setImageResource(R.drawable.creategroup_icon);
                FirebaseFirestore.getInstance().collection("users").document(senderName)
                        .get().addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String pic = doc.getString("profilePicture");
                                if (pic != null) {
                                    cache.put(senderName, pic);
                                    setPic(pic);
                                }
                            }
                        });
            }
        }

        private void setPic(String base64) {
            if (base64 == null || base64.isEmpty()) {
                senderPic.setImageResource(R.drawable.creategroup_icon);
                return;
            }
            Bitmap bitmap = ImageUtils.convertBase64ToBitmap(base64);
            if (bitmap != null) {
                senderPic.setImageBitmap(bitmap);
            } else {
                senderPic.setImageResource(R.drawable.creategroup_icon);
            }
        }
    }

    // Converts a long millisecond timestamp into a human-readable "hh:mm AM/PM" format.
    // Input: long ts (the timestamp from Firestore).
    // Output: String (formatted time).
    private static String formatTime(long ts) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(ts));
    }

    // Replaces the entire message list and refreshes the adapter.
    // Input: List<Message> newMessages (the full updated list).
    // Output: None.
    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    // Appends a single new message to the end of the list and notifies the RecyclerView of the insertion.
    // Input: Message msg (the new message object).
    // Output: None.
    public void addMessage(Message msg) {
        this.messages.add(msg);
        // Only refreshes the newly added item for better performance
        notifyItemInserted(messages.size() - 1);
    }
}
