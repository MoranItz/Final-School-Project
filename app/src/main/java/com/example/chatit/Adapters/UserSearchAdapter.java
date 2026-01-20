package com.example.chatit.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatit.R;
import java.util.List;
import java.util.Set;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
    private final List<String> userResults;
    private final Set<String> selectedUsernames;

    // This function is responsible for creating the adapter with the search results and the selection set.
    // Input: List<String> users, Set<String> selected.
    // Output: None.
    public UserSearchAdapter(List<String> users, Set<String> selected) {
        this.userResults = users;
        this.selectedUsernames = selected;
    }

    // This function is responsible for inflating the search result item layout.
    // Input: ViewGroup parent, int viewType.
    // Output: ViewHolder (the container for the search result).
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item, parent, false);
        return new ViewHolder(rowView);
    }

    // This function is responsible for binding the user name and selection state to the search result row.
    // Input: ViewHolder holder, int position.
    // Output: None.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = userResults.get(position);
        holder.nameText.setText(name);
        holder.selectionCheckBox.setChecked(selectedUsernames.contains(name));
        
        // Handles clicking the entire row to toggle selection
        holder.itemView.setOnClickListener(v -> {
            holder.selectionCheckBox.setChecked(!holder.selectionCheckBox.isChecked());
            if (holder.selectionCheckBox.isChecked()) selectedUsernames.add(name);
            else selectedUsernames.remove(name);
        });
        
        // Handles clicking the checkbox directly
        holder.selectionCheckBox.setOnClickListener(v -> {
            if (holder.selectionCheckBox.isChecked()) selectedUsernames.add(name);
            else selectedUsernames.remove(name);
        });
    }

    // This function is responsible for returning the total count of search results.
    // Input: None.
    // Output: int (list size).
    @Override public int getItemCount() { return userResults.size(); }

    // This class is responsible for holding the UI parts of a search result row.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        CheckBox selectionCheckBox;
        
        // This function is responsible for finding the UI components in the search result layout.
        // Input: View view.
        // Output: None.
        public ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.memberName);
            selectionCheckBox = view.findViewById(R.id.memberCheckbox);
        }
    }
}
