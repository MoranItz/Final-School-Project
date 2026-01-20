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
import com.example.chatit.Struct_Classes.Group;
import com.example.chatit.Struct_Classes.ImageUtils;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<Group> groupList;
    private final OnGroupClickListener interactionListener;

    // This interface is responsible for defining the click behavior when a group is selected.
    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    // This function is responsible for creating the adapter with a list of groups and a click listener.
    // Input: List<Group> groups, OnGroupClickListener listener.
    // Output: None.
    public GroupAdapter(List<Group> groups, OnGroupClickListener listener) {
        this.groupList = groups;
        this.interactionListener = listener;
    }

    // This function is responsible for inflating the XML layout for each group row.
    // Input: ViewGroup parent, int viewType.
    // Output: GroupViewHolder (the container for the row views).
    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.groupbox_item, parent, false);
        return new GroupViewHolder(rowView);
    }

    // This function is responsible for connecting group data to the views in a specific row.
    // Input: GroupViewHolder holder, int position.
    // Output: None.
    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bindData(groupList.get(position), interactionListener);
    }

    // This function is responsible for returning the total count of groups to be displayed.
    // Input: None.
    // Output: int (the number of groups).
    @Override
    public int getItemCount() { return groupList.size(); }

    // This class is responsible for holding property references to the UI parts of a group row.
    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameText, groupDescriptionText, groupTimeText;
        ImageView groupProfileImageView;

        // This function is responsible for finding the UI elements within the row view.
        // Input: View itemView.
        // Output: None.
        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameText = itemView.findViewById(R.id.groupName);
            groupDescriptionText = itemView.findViewById(R.id.lastMessage);
            groupTimeText = itemView.findViewById(R.id.groupTime);
            groupProfileImageView = itemView.findViewById(R.id.groupProfileImage);
        }

        // This function is responsible for filling the row views with specific group information.
        // Input: Group group, OnGroupClickListener listener.
        // Output: None.
        void bindData(Group group, OnGroupClickListener listener) {
            groupNameText.setText(group.getGroupName());
            groupDescriptionText.setText(group.getDescription());
            groupTimeText.setText(""); 

            String base64Image = group.getImageBase64();
            if (base64Image != null && !base64Image.isEmpty()) {
                Bitmap decodedBitmap = ImageUtils.convertBase64ToBitmap(base64Image);
                if (decodedBitmap != null) {
                    groupProfileImageView.setImageBitmap(decodedBitmap);
                } else {
                    groupProfileImageView.setImageResource(R.drawable.creategroup_icon);
                }
            } else {
                groupProfileImageView.setImageResource(R.drawable.creategroup_icon);
            }

            itemView.setOnClickListener(v -> listener.onGroupClick(group));
        }
    }

    // This function is responsible for swapping the current group list with a new one and refreshing the UI.
    // Input: List<Group> newGroups.
    // Output: None.
    public void updateGroups(List<Group> newGroups) {
        this.groupList = newGroups;
        notifyDataSetChanged();
    }
}
