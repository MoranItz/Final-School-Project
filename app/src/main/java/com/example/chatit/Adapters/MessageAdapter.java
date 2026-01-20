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

    // This function is responsible for creating the adapter with the message data and the current user's name.
    // Input: List<Message> messages, String currentUser.
    // Output: None.
    public MessageAdapter(List<Message> messages, String currentUser) {
        this.messages = messages;
        this.currentUser = currentUser;
    }

    // This function is responsible for deciding if a message is sent or received.
    // Input: int position (index in list).
    // Output: int (view type constant).
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSender().equals(currentUser) ? TYPE_SENT : TYPE_RECEIVED;
    }

    // This function is responsible for creating the layout holder for each message item.
    // Input: ViewGroup parent, int viewType.
    // Output: RecyclerView.ViewHolder.
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == TYPE_SENT) ? R.layout.messagesent_item : R.layout.messagerecieved_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return (viewType == TYPE_SENT) ? new SentViewHolder(view) : new ReceivedViewHolder(view);
    }

    // This function is responsible for filling the message layout with data like text and time.
    // Input: RecyclerView.ViewHolder holder, int position.
    // Output: None.
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).bind(message, profilePicsCache);
        }
    }

    // This function is responsible for returning the total amount of messages in the list.
    // Input: None.
    // Output: int (size).
    @Override
    public int getItemCount() { return messages.size(); }

    // This class is responsible for holding the UI parts for a sent message.
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView contentText, timeText;
        SentViewHolder(View view) {
            super(view);
            contentText = view.findViewById(R.id.sentMessageText);
            timeText = view.findViewById(R.id.sentMessageTime);
        }
        
        // This function is responsible for displaying the text and time for a sent message.
        // Input: Message message.
        // Output: None.
        void bind(Message message) {
            contentText.setText(Message.base64Decryption(message.getContent()));
            timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    // This class is responsible for holding the UI parts for a received message.
    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView senderName, contentText, timeText;
        ImageView senderProfilePic;
        ReceivedViewHolder(View view) {
            super(view);
            senderName = view.findViewById(R.id.senderName);
            contentText = view.findViewById(R.id.receivedMessageText);
            timeText = view.findViewById(R.id.receivedMessageTime);
            senderProfilePic = view.findViewById(R.id.senderProfilePic);
        }

        // This function is responsible for displaying the name, text, and PFP for a received message.
        // Input: Message message, Map<String, String> cache.
        // Output: None.
        void bind(Message message, Map<String, String> cache) {
            senderName.setText(message.getSender());
            contentText.setText(Message.base64Decryption(message.getContent()));
            timeText.setText(formatTime(message.getTimestamp()));

            String sender = message.getSender();
            if (cache.containsKey(sender)) {
                setProfilePicFromBase64(cache.get(sender));
            } else {
                senderProfilePic.setImageResource(R.drawable.creategroup_icon);
                FirebaseFirestore.getInstance().collection("users").document(sender)
                        .get().addOnSuccessListener(document -> {
                            if (document.exists()) {
                                String profilePicBase64 = document.getString("profilePicture");
                                if (profilePicBase64 != null) {
                                    cache.put(sender, profilePicBase64);
                                    setProfilePicFromBase64(profilePicBase64);
                                }
                            }
                        });
            }
        }

        // This function is responsible for converting a Base64 string into a PFP image.
        // Input: String base64Data.
        // Output: None.
        private void setProfilePicFromBase64(String base64Data) {
            if (base64Data == null || base64Data.isEmpty()) {
                senderProfilePic.setImageResource(R.drawable.creategroup_icon);
                return;
            }
            Bitmap bitmap = ImageUtils.convertBase64ToBitmap(base64Data);
            if (bitmap != null) {
                senderProfilePic.setImageBitmap(bitmap);
            } else {
                senderProfilePic.setImageResource(R.drawable.creategroup_icon);
            }
        }
    }

    // This function is responsible for formatting the raw timestamp into a readable time.
    // Input: long timestamp.
    // Output: String (formatted time).
    private static String formatTime(long timestamp) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
    }

    // This function is responsible for replacing the entire message list with a new one.
    // Input: List<Message> newMessages.
    // Output: None.
    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    // This function is responsible for adding a single new message to the existing list.
    // Input: Message newMessage.
    // Output: None.
    public void addMessage(Message newMessage) {
        this.messages.add(newMessage);
        notifyItemInserted(messages.size() - 1);
    }
}
