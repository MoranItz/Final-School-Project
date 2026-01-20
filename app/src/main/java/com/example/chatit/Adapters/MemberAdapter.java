package com.example.chatit.Adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatit.R;
import com.example.chatit.Struct_Classes.ImageUtils;
import com.example.chatit.Struct_Classes.User;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {
    private final List<User> members;
    private final OnItemClickListener interactionListener;

    // This interface is responsible for handling clicks on members in the list.
    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    // This function is responsible for creating the adapter with the list of members and a click listener.
    // Input: List<User> members, OnItemClickListener listener.
    // Output: None.
    public MemberAdapter(List<User> members, OnItemClickListener listener) { 
        this.members = members;
        this.interactionListener = listener;
    }

    // This function is responsible for inflating the layout for each member row.
    // Input: ViewGroup parent, int viewType.
    // Output: ViewHolder (the row container).
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_info_item, parent, false);
        return new ViewHolder(rowView);
    }

    // This function is responsible for filling each row with the name, bio, and PFP of a member.
    // Input: ViewHolder holder, int position.
    // Output: None.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User member = members.get(position);
        holder.memberNameText.setText(member.getUsername());
        holder.memberBioText.setText(member.getBiography() != null ? member.getBiography() : "No bio available");
        
        String base64Image = member.getProfilePicture();
        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap decodedBitmap = ImageUtils.convertBase64ToBitmap(base64Image);
            if (decodedBitmap != null) {
                holder.memberProfilePicImage.setImageBitmap(decodedBitmap);
            } else {
                holder.memberProfilePicImage.setImageResource(R.drawable.creategroup_icon);
            }
        } else {
            holder.memberProfilePicImage.setImageResource(R.drawable.creategroup_icon);
        }

        holder.itemView.setOnClickListener(v -> interactionListener.onItemClick(member));
    }

    // This function is responsible for returning the total number of members in the list.
    // Input: None.
    // Output: int (size).
    @Override
    public int getItemCount() { return members.size(); }

    // This class is responsible for holding the UI parts of a single member row.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView memberNameText, memberBioText;
        ImageView memberProfilePicImage;
        
        // This function is responsible for finding the UI components in the row layout.
        // Input: View view.
        // Output: None.
        public ViewHolder(View view) {
            super(view);
            memberNameText = view.findViewById(R.id.memberName);
            memberBioText = view.findViewById(R.id.memberBio);
            memberProfilePicImage = view.findViewById(R.id.memberProfilePic);
        }
    }
}
