package com.example.chatit.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatit.R;
import com.example.chatit.Struct_Classes.Group;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<Group> groups;
    private final OnGroupClickListener listener;

    // Interface used to handle click events on specific group items within the list.
    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    // Constructor that initializes the adapter with a list of groups and a click listener.
    // Input: List<Group> groups (the data source), OnGroupClickListener listener (the click handler).
    // Output: None.
    public GroupAdapter(List<Group> groups, OnGroupClickListener listener) {
        this.groups = groups;
        this.listener = listener;
    }

    // Creates a new ViewHolder by inflating the group item layout.
    // Input: ViewGroup parent (the RecyclerView), int viewType (the type of the view).
    // Output: GroupViewHolder (the newly created holder).
    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the individual group item XML layout into a View object
        // Use groupbox_item instead of item_group
        return new GroupViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.groupbox_item, parent, false));
    }

    // Binds the group data at a specific position to the provided ViewHolder.
    // Input: GroupViewHolder holder (the holder to update), int pos (position in the list).
    // Output: None.
    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int pos) {
        holder.bind(groups.get(pos), listener);
    }

    // Returns the total number of items currently in the group list.
    // Input: None.
    // Output: int (the size of the groups list).
    @Override
    public int getItemCount() { return groups.size(); }

    // Inner class that holds references to the views for each group item in the list.
    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView name, lastMessage, time;

        // Constructor that finds and initializes the sub-views within the group item layout.
        // Input: View v (the root view of the item layout).
        // Output: None.
        GroupViewHolder(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.groupName);
            lastMessage = v.findViewById(R.id.lastMessage);
            time = v.findViewById(R.id.groupTime);
        }

        // Sets the text for the group details and handles the logic for the pluralization of the member count.
        // Input: Group group (the group data), OnGroupClickListener listener (the click callback).
        // Output: None.
        void bind(Group group, OnGroupClickListener listener) {
            name.setText(group.getGroupName());
            
            // Map description to lastMessage for now since old logic used description
            lastMessage.setText(group.getDescription());
            
            // Set time to empty or default as it is not in old logic
            time.setText(""); 

            // Attach the listener to the entire row so the group chat opens when clicked
            itemView.setOnClickListener(v -> listener.onGroupClick(group));
        }
    }

    // Updates the adapter's data source with a new list and refreshes the entire UI.
    // Input: List<Group> newGroups (the updated list of groups).
    // Output: None.
    public void updateGroups(List<Group> newGroups) {
        this.groups = newGroups;
        // Forces the RecyclerView to redraw all visible items with the new data
        notifyDataSetChanged();
    }
}
